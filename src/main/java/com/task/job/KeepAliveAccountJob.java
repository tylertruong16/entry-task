package com.task.job;

import com.task.common.FileUtil;
import com.task.model.AccountStatusTrack;
import com.task.model.ConnectStatus;
import com.task.model.ProfileStatus;
import com.task.service.ChromeService;
import com.task.service.ProfileManagerRepo;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class KeepAliveAccountJob {

    final ChromeService chromeService;
    final ProfileManagerRepo profileManagerRepo;

    @Value("${system.email-profile}")
    private String emailProfile;

    @Value("${profile-folder.user-profile-download}")
    private String chromeProfileDownloadFolder;

    @Value("${profile-folder.user-profile}")
    private String userProfileExtractFolder;

    Map<String, AccountStatusTrack> trackStatus = new ConcurrentHashMap<>();

    public KeepAliveAccountJob(ChromeService chromeService, ProfileManagerRepo profileManagerRepo) {
        this.chromeService = chromeService;
        this.profileManagerRepo = profileManagerRepo;
    }


//    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
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
                    var loginStatus = chromeService.connectGoogle(profileItem.getEmail());
                    var previous = Optional.ofNullable(trackStatus.get(it.getEmail())).orElse(new AccountStatusTrack(it.getEmail(), 0));
                    var clone = SerializationUtils.clone(previous);
                    if (loginStatus.equals(ConnectStatus.SUCCESS)) {
                        clone.setFailedCount(0);
                    } else {
                        clone.setFailedCount(previous.getFailedCount() + 1);
                    }
                    trackStatus.put(it.getEmail(), clone);
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> KeepAliveAccountJob >> connectGooglePage >> Exception:", e);
        }
    }

    @Scheduled(fixedDelay = 3, initialDelay = 3, timeUnit = TimeUnit.MINUTES)
    void clearProfileIfFailedLogin() {
        try {
            var profile = profileManagerRepo.getProfileByEmail(emailProfile);
            profile.ifPresent(it -> {
                var previous = Optional.ofNullable(trackStatus.get(it.getEmail())).orElse(new AccountStatusTrack(it.getEmail(), 0));
                var clone = SerializationUtils.clone(profile.get());
                if (previous.getFailedCount() > 10) {
                    var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                    clone.setUpdateDate(updateAt);
                    clone.setProfileFolderUrl("");
                    clone.setStatus(ProfileStatus.LOST_CONNECTION.name());
                    profileManagerRepo.saveProfileItem(clone);
                    var userFolderDownLoadName = Paths.get(System.getProperty("user.home"), chromeProfileDownloadFolder, it.getEmail()).toString();
                    var deletedResult = FileUtil.deleteFolder(new File(userFolderDownLoadName));
                    log.log(Level.INFO, "entry-task >> KeepAliveAccountJob >> clearProfileIfFailedLogin  >> email: {0} >> deleted: {1}", new Object[]{it.getEmail(), deletedResult});
                    trackStatus.remove(it.getEmail());
                }
            });


        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> KeepAliveAccountJob >> clearProfileIfFailedLogin >> Exception:", e);
        }
    }

}
