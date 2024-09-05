package com.task.service.task;

import com.task.common.FileSplitter;
import com.task.common.FileUtil;
import com.task.model.TaskModel;
import com.task.model.TaskState;
import com.task.repo.TaskRepo;
import com.task.service.ChromeService;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

@Service
@Log
public class ChatGptService {

    final ChromeService chromeService;
    private static final String HOME_PAGE_CHAT_GPT = "https://chatgpt.com";

    @Value("${profile-folder.user-profile}")
    private String userProfileExtractFolder;

    @Value("${profile-folder.user-profile-download}")
    private String chromeProfileDownloadFolder;
    final TaskQueue taskQueue;
    final TaskRepo taskRepo;

    public ChatGptService(ChromeService chromeService, TaskQueue taskQueue, TaskRepo taskRepo) {
        this.chromeService = chromeService;
        this.taskQueue = taskQueue;
        this.taskRepo = taskRepo;
    }


    public void connectChatGpt() {
        var options = chromeService.createProfile("", new ChromeOptions());
        var driver = new ChromeDriver(options);
        try {
            byPassCloudFlare(driver);
            Thread.sleep(Duration.ofSeconds(5));
            if (canAccessChatGptHome(driver)) {
                log.log(Level.INFO, "entry-task >> ChatGptService >> connectChatGpt >> currentUrl: {0}", driver.getCurrentUrl());
                while (canAccessChatGptHome(driver)) {
                    Thread.sleep(Duration.ofSeconds(2));
                    var task = taskQueue.popTask();
                    handelTask(task, driver);

                }
            }
            Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> ChatGptService >> connectChatGpt >> Exception:", e);
        } finally {
            driver.quit();
        }
    }


    public void updateTask(TaskModel it, TaskState taskState, String response) {
        var clone = SerializationUtils.clone(it);
        clone.setTaskStatus(taskState.name());
        var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        clone.setUpdateAt(updateAt);
        clone.setOutput(Base64.encodeBase64String(response.getBytes(StandardCharsets.UTF_8)));
        taskRepo.saveTaskModel(clone);
    }


    @SneakyThrows
    public void byPassCloudFlare(WebDriver driver) {
        // Create an HTML file with a link
        var currentDir = Paths.get("").toAbsolutePath().toString();
        var filePath = currentDir + "/blank.html";
        try (var writer = new FileWriter(filePath)) {
            writer.write("<a href=\"" + HOME_PAGE_CHAT_GPT + "\" target=\"_blank\">link</a>");
        }
        driver.get("file://" + filePath);
        // Find all the links on the page
        var links = driver.findElements(By.xpath("//a[@href]"));
        // Pause execution for 10 seconds
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        // Click on the first link (open in new tab)
        links.getFirst().click();

        // Pause execution for 5 seconds
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        // Switch to the new tab
        var newTab = driver.getWindowHandles().toArray(new String[0])[1];
        driver.switchTo().window(newTab);
    }

    private void handelTask(Optional<TaskModel> taskOptional, ChromeDriver driver) {
        if (taskOptional.isPresent()) {
            var task = taskOptional.get();
            updateTask(task, TaskState.IN_PROGRESS, "");
            var message = new String(Base64.decodeBase64(task.getInput()), StandardCharsets.UTF_8);
            var textarea = driver.findElement(By.id("prompt-textarea"));
            textarea.sendKeys(message);
            textarea.sendKeys(Keys.RETURN);
            if (chatGptResponse(driver)) {
                var responseText = driver.findElements(By.xpath("//*[@data-message-id and @data-message-author-role]")).stream()
                        .filter(element ->
                                Optional.ofNullable(element.getAttribute("data-message-id")).isPresent() &&
                                        "assistant".equals(element.getAttribute("data-message-author-role")))
                        .findFirst()
                        .map(WebElement::getText).orElse("");
                log.log(Level.INFO, "entry-task >> chatResponse: {0}", responseText);
                updateTask(task, TaskState.DONE, StringUtils.defaultIfBlank(responseText, ""));
            }
            clearPreviousQuestion(driver);
        }
    }


    public boolean chatGptResponse(ChromeDriver driver) {
        try {
            var wait = new FluentWait<WebDriver>(driver)
                    .withTimeout(Duration.ofMinutes(1))
                    .pollingEvery(Duration.ofSeconds(5))
                    .ignoring(NoSuchElementException.class);

            // Define the condition to check for the profile icon
            var checkInputDisplay = new Function<WebDriver, Boolean>() {
                public Boolean apply(WebDriver driver) {
                    try {
                        // Check if any element has both attributes
                        var responseDisplay = driver.findElements(By.xpath("//*[@data-message-id and @data-message-author-role]")).stream()
                                .anyMatch(element ->
                                        Optional.ofNullable(element.getAttribute("data-message-id")).isPresent() &&
                                                "assistant".equals(element.getAttribute("data-message-author-role"))
                                );
                        var copyBtnDisplay = driver.findElements(By.xpath("//*[@aria-label='Copy']")).stream()
                                .anyMatch(element ->
                                        "Copy".equals(element.getAttribute("aria-label"))
                                );
                        return responseDisplay && copyBtnDisplay;
                    } catch (NoSuchElementException e) {
                        return false; // Profile icon not found, not signed in
                    }
                }
            };
            return wait.until(checkInputDisplay);
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> chatGptResponse >> Exception: ", e);
            return false;
        }
    }

    public void clearPreviousQuestion(WebDriver driver) {
        // Clear cookies
        driver.manage().deleteAllCookies();
        // Clear local storage and session storage using JavaScript
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.clear();");
        js.executeScript("window.sessionStorage.clear();");
        // Refresh the page
        driver.navigate().refresh();
    }


    public boolean canAccessChatGptHome(ChromeDriver driver) {
        try {
            var wait = new FluentWait<WebDriver>(driver)
                    .withTimeout(Duration.ofMinutes(2))
                    .pollingEvery(Duration.ofSeconds(5))
                    .ignoring(NoSuchElementException.class);

            // Define the condition to check for the profile icon
            var checkInputDisplay = new Function<WebDriver, Boolean>() {
                public Boolean apply(WebDriver driver) {
                    try {
                        return driver.findElement(By.id("prompt-textarea")).isDisplayed();
                    } catch (NoSuchElementException e) {
                        return false; // Profile icon not found, not signed in
                    }
                }
            };
            return wait.until(checkInputDisplay);
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> canAccessChatGptHome >> Exception: ", e);
            return false;
        }
    }

    @SneakyThrows
    public void cloneChromeProfile(String email) {
        var userFolderProfile = Paths.get(System.getProperty("user.home"), userProfileExtractFolder, "sub-task", "chat-gpt", email).toString();
        var folder = new File(userFolderProfile);
        if (!folder.exists()) {
            var created = folder.mkdirs();
            log.log(Level.INFO, "entry-task >> collectProfile >> create folder >> path: {0} >> result: {1}", new Object[]{folder.getAbsolutePath(), created});

        } else {
            FileUtil.deleteFolder(folder);
        }
        var userFolderDownLoadName = Paths.get(System.getProperty("user.home"), chromeProfileDownloadFolder, email).toString();
        var zipFileName = userFolderDownLoadName + File.separator + MessageFormat.format("{0}.zip", email);
        var fileZip = new File(zipFileName);
        if (fileZip.exists()) {
            var extractProfileFolder = Paths.get(System.getProperty("user.home"), userProfileExtractFolder, "sub-task", "chat-gpt").toString();
            FileSplitter.unzip(zipFileName, extractProfileFolder);
        }
    }

}
