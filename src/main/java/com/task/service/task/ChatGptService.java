package com.task.service.task;

import com.task.common.FileSplitter;
import com.task.common.FileUtil;
import com.task.service.ChromeService;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class ChatGptService {

    final ChromeService chromeService;
    private static final String HOME_PAGE_CHAT_GPT_1 = "https://openai.com/chatgpt";
    private static final String HOME_PAGE_CHAT_GPT = "https://chatgpt.com";

    @Value("${profile-folder.user-profile}")
    private String userProfileExtractFolder;

    @Value("${profile-folder.user-profile-download}")
    private String chromeProfileDownloadFolder;

    public ChatGptService(ChromeService chromeService) {
        this.chromeService = chromeService;
    }


    public void connectChatGpt(String email) {
        cloneChromeProfile(email);
        var folderName = "sub-task" + File.separator + "chat-gpt" + File.separator + email;
        var options = chromeService.createProfile(folderName, new ChromeOptions());
        var driver = new ChromeDriver(options);
        try {
            driver.get(ChromeService.GOOGLE_ACCOUNT_PAGE);
            Thread.sleep(Duration.ofSeconds(5));
            var loginSuccess = chromeService.loginSuccess(driver, "ACCOUNT_PAGE");
            driver.get(HOME_PAGE_CHAT_GPT_1);
//            Thread.sleep(Duration.ofSeconds(5));
//            driver.get(HOME_PAGE_CHAT_GPT);
//            var text = driver.findElement(By.id("prompt-textarea"));
            Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> ChatGptService >> connectChatGpt >> Exception:", e);
        } finally {
            driver.quit();
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
