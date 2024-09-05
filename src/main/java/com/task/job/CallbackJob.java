package com.task.job;

import com.task.model.TaskState;
import com.task.repo.TaskRepo;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class CallbackJob {

    final TaskRepo taskRepo;

    public CallbackJob(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Scheduled(fixedDelay = 5, initialDelay = 60, timeUnit = TimeUnit.SECONDS)
    public void callBack() {
        try {
            var taskDone = taskRepo.loadTaskBelongToWorker(TaskState.DONE)
                    .stream().filter(it -> it.getCallbackCount() < 5)
                    .toList();
            taskDone.forEach(e -> {
                try {
                    var count = e.getCallbackCount();
                    var clone = SerializationUtils.clone(e);
                    var callbackModel = e.convertToCallbackJob();
                    var result = taskRepo.callBackTask(callbackModel);
                    var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                    if (result) {
                        clone.setTaskStatus(TaskState.CALLBACK.name());
                    } else {
                        clone.setCallbackCount(count + 1);
                    }
                    clone.setUpdateAt(updateAt);
                    taskRepo.saveTaskModel(clone);
                } catch (Exception ex) {
                    log.log(Level.WARNING, MessageFormat.format("entry-task >> CallbackJob >> callBack >> taskId: {0} >> Exception:", e.getId()), e);
                }

            });
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> CallbackJob >> callBack >> Exception:", e);
        }
    }
}
