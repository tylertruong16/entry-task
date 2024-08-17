package com.task.service;

import com.task.model.ConnectStatus;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

@Service
@Log
public class ChromeService {

    private static final String GOOGLE_ACCOUNT_PAGE = "https://accounts.google.com";
    private static final String GOOGLE_SHELL_PAGE = "https://console.cloud.google.com";
    private static final String SHELL_FRAME_NAME = "cloudshell-frame";

    @Value("${system.headless-mode}")
    private String headlessMode;
    @Value("${profile-folder.user-profile}")
    private String userProfileExtractFolder;


    public ConnectStatus connectGoogle(String email) {
        var options = createProfile(email, new ChromeOptions());
        var driver = new ChromeDriver(options);
        try {
            driver.get(GOOGLE_ACCOUNT_PAGE);
            Thread.sleep(Duration.ofSeconds(5));
            var loginSuccess = loginSuccess(driver, "ACCOUNT_PAGE");
            if (loginSuccess) {
                log.log(Level.INFO, "entry-task >> ChromeService >> connectGoogle >> email: {0} >> title: {1}", new Object[]{email, driver.getTitle()});
                driver.get(GOOGLE_SHELL_PAGE);
                var wait = new WebDriverWait(driver, Duration.ofSeconds(30));
                var cloudShellIcon = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@aria-label='Activate Cloud Shell']")));
                cloudShellIcon.click();
                Thread.sleep(Duration.ofSeconds(10));
                // wait 5 sec to open cloud shell cmd
                // Switch to the Cloud Shell iframe
                Thread.sleep(Duration.ofSeconds(10));
                var iframe = driver.findElement(By.className(SHELL_FRAME_NAME));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(SHELL_FRAME_NAME)));
                // wait 5 sec to open cloud shell cmd

                // Switch to the Cloud Shell iframe
                driver.switchTo().frame(iframe);


                log.log(Level.INFO, "entry-task >> ChromeService >> connectGoogle >> email: {0} >> title: {1}", new Object[]{email, driver.getTitle()});
                if (isDisplayTermPopUp(driver)) {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                }
                log.log(Level.INFO, "entry-task >> ChromeService >> connectGoogle >> email: {0} >> title: {1}", new Object[]{email, driver.getTitle()});
                var startTime = LocalDateTime.now();
                while (Duration.between(startTime, LocalDateTime.now()).toMinutes() < TimeUnit.MINUTES.toMinutes(5)) {
                    Thread.sleep(Duration.ofSeconds(5));
                    log.log(Level.INFO, "entry-task >> ChromeService >> connectGoogle >> email: {0} >> pageTitle: {1} >> url: {2}", new Object[]{email, driver.getTitle(), driver.getCurrentUrl()});
                    var canConnectCloudShellPage = loginSuccess(driver, "CLOUD_SHELL_PAGE");
                    if (!canConnectCloudShellPage) {
                        break;
                    }
                }
                return loginSuccess(driver, "CLOUD_SHELL_PAGE") ? ConnectStatus.SUCCESS : ConnectStatus.FAILURE;
            }
            return ConnectStatus.FAILURE;
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> ChromeService >> connectGoogle >> Exception:", e);
            return ConnectStatus.EXCEPTION;
        } finally {
            driver.quit();
        }
    }


    private boolean loginSuccess(ChromeDriver driver, String page) {
        try {
            var wait = new FluentWait<WebDriver>(driver)
                    .withTimeout(Duration.ofMinutes(2))
                    .pollingEvery(Duration.ofSeconds(5))
                    .ignoring(NoSuchElementException.class);

            // Define the condition to check for the profile icon
            var checkLogin = new Function<WebDriver, Boolean>() {
                public Boolean apply(WebDriver driver) {
                    try {
                        if (StringUtils.equalsIgnoreCase("CLOUD_SHELL_PAGE", page)) {
                            var currentUrl = driver.getCurrentUrl();
                            return StringUtils.containsIgnoreCase(currentUrl, "console.cloud.google.com");
                        }
                        driver.findElement(By.xpath("//a[contains(@href, 'accounts.google.com/SignOutOptions')]"));
                        return true; // Profile icon found, already signed in
                    } catch (NoSuchElementException e) {
                        return false; // Profile icon not found, not signed in
                    }
                }
            };
            return wait.until(checkLogin);
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> check login google >> Exception: ", e);
            return false;
        }

    }

    public boolean isDisplayTermPopUp(WebDriver driver) {
        var wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);
        var dialogCondition = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    var element = driver.findElement(By.cssSelector("mat-dialog-container"));
                    return element.isDisplayed();
                } catch (NoSuchElementException e) {
                    return false; // Profile icon not found, not signed in
                }
            }
        };
        var dialogDisplay = wait.until(dialogCondition);
        if (dialogDisplay) {
            var dialog = driver.findElement(By.cssSelector("mat-dialog-container"));
            WebElement termsLink = dialog.findElement(By.cssSelector("[article='GCP_TERMS_OF_SERVICE']"));
            var popupDisplay = Optional.ofNullable(termsLink).isPresent();
            if (popupDisplay) {
                var textInsidePopUp = dialog.getText();
                log.log(Level.INFO, "entry-task >> ChromeService >> isDisplayTermPopUp >> textInsidePopUp: {0}", textInsidePopUp);
                confirmPopUp(driver);

            }
            return popupDisplay;
        }

        return false;

    }

    private static void confirmPopUp(WebDriver driver) {
        var checkbox = driver.findElement(By.id("mat-mdc-checkbox-1-input"));
        checkbox.click();
        // Locate the submit button and click on it
        var submitButton = driver.findElement(By.xpath("//button[span[contains(text(),'Start Cloud Shell')]]"));
        submitButton.click();
    }

    public ChromeOptions createProfile(String folderName, ChromeOptions options) {
        try {
            var profilePath = Paths.get(System.getProperty("user.home"), userProfileExtractFolder, folderName).toString();
            options.addArguments(MessageFormat.format("user-data-dir={0}", profilePath));
            options.addArguments("--disable-web-security");
            // this option so important to bypass google detection
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);
            options.addArguments("--disable-blink-features=AutomationControlled");
            if (StringUtils.equalsIgnoreCase("true", headlessMode)) {
                options.addArguments("--headless");
                options.addArguments("--disable-gpu");
            }
            return options;
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> ChromeService >> createProfile >> Exception:", e);
        }
        return options;
    }


}
