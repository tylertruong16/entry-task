package com.task.job;

import com.task.service.ProfileManagerRepo;
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

    final ProfileManagerRepo profileManagerRepo;

    public CollectProfileJob(ProfileManagerRepo profileManagerRepo) {
        this.profileManagerRepo = profileManagerRepo;
    }

    @Scheduled(fixedDelay = 1, initialDelay = 2, timeUnit = TimeUnit.MINUTES)
    void keepAliveProfile() {
        try {
            var profile = profileManagerRepo.getProfileByEmail(emailProfile);
            if (profile.isPresent() && profile.get().offlineProfile()) {
                var clone = SerializationUtils.clone(profile.get());
                var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                clone.setStatus("ONLINE");
                clone.setUpdateDate(updateAt);
                log.log(Level.INFO, "cloud-shell-task >> CollectProfileJob >> keepAliveProfile >> email: {0} >> updateAt: {1}", new Object[]{emailProfile, updateAt});
            }
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("cloud-shell-task >> CollectProfileJob >> keepAliveProfile >> email: {0} >> Exception:", emailProfile), e);
        }

    }

}
