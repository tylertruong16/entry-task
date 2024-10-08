package com.task.job;

import com.task.model.ProfileStatus;
import com.task.repo.ProfileManagerRepo;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class CollectProfileJob {

    @Value("${system.email-profile}")
    private String emailProfile;

    @Value("${system.disable-keep-alive-profile}")
    private boolean disableKeepAliveProfile;

    final ProfileManagerRepo profileManagerRepo;

    public CollectProfileJob(ProfileManagerRepo profileManagerRepo) {
        this.profileManagerRepo = profileManagerRepo;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void keepAliveProfile() {
        try {
            if (disableKeepAliveProfile) {
                return;
            }
            var profile = profileManagerRepo.getProfileByEmail(emailProfile);
            if (profile.isPresent() && profile.get().offlineProfile()) {
                var clone = SerializationUtils.clone(profile.get());
                var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                clone.setStatus(ProfileStatus.ONLINE.name());
                clone.setUpdateDate(updateAt);
                log.log(Level.INFO, "entry-task >> CollectProfileJob >> keepAliveProfile >> email: {0} >> updateAt: {1}", new Object[]{emailProfile, updateAt});
                profileManagerRepo.saveProfileItem(clone);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("entry-task >> CollectProfileJob >> keepAliveProfile >> email: {0} >> Exception:", emailProfile), e);
        }

    }

}
