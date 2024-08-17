package com.task.job;

import com.task.service.ChromeService;
import com.task.service.ProfileManagerRepo;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class KeepAliveAccountJob {

    final ChromeService chromeService;
    final ProfileManagerRepo profileManagerRepo;

    @Value("${system.email-profile}")
    private String emailProfile;


    @Value("${profile-folder.user-profile}")
    private String userProfileExtractFolder;

    public KeepAliveAccountJob(ChromeService chromeService, ProfileManagerRepo profileManagerRepo) {
        this.chromeService = chromeService;
        this.profileManagerRepo = profileManagerRepo;
    }


    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
    void connectGooglePage() {
        try {
            var profile = profileManagerRepo.getProfileByEmail(emailProfile);
            if (profile.isPresent() && profile.get().onlineProfile()) {
                var it = profile.get();
                var userFolderName = Paths.get(System.getProperty("user.home"), userProfileExtractFolder, it.getEmail()).toString();
                var folderProfile = new File((userFolderName));
                var totalFiles = Optional.ofNullable(folderProfile.listFiles()).map(e -> e.length).orElse(0);
                var validFolder = folderProfile.exists() && folderProfile.isDirectory() && totalFiles > 0;
                if (validFolder) {
                    var profileItem = profile.get();
                    chromeService.connectGoogle(profileItem.getEmail());
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> KeepAliveAccountJob >> connectGooglePage >> Exception:", e);
        }
    }

}
