package com.task.job;

import com.task.model.TaskModel;
import com.task.model.TaskState;
import com.task.repo.TaskRepo;
import com.task.service.task.ChatGptService;
import com.task.service.task.TaskQueue;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class TaskCollectJob {


    final TaskQueue taskQueue;
    final TaskRepo taskRepo;
    final ChatGptService chatGptService;

    public TaskCollectJob(TaskRepo taskRepo, TaskQueue taskQueue, ChatGptService chatGptService) {
        this.taskRepo = taskRepo;
        this.taskQueue = taskQueue;
        this.chatGptService = chatGptService;
    }

    @Scheduled(fixedDelay = 7, timeUnit = TimeUnit.SECONDS)
    void loadNewTask() {
        try {
            var openTasks = taskRepo.loadTaskBelongToWorker(TaskState.OPEN)
                    .stream()
                    .filter(TaskModel::openTask)
                    .toList();
            if (CollectionUtils.isNotEmpty(openTasks)) {
                // push the task in queue
                openTasks.forEach(it -> {
                    var taskInQueue = taskQueue.taskInQueue(it);
                    if (!taskInQueue) {
                        taskQueue.push(it);
                        var clone = SerializationUtils.clone(it);
                        clone.setTaskStatus(TaskState.QUEUED.name());
                        var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                        clone.setUpdateAt(updateAt);
                        taskRepo.saveTaskModel(clone);
                    }
                });
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> TaskCollectJob >> loadNewTask >> Exception:", e);
        }
    }

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
    void initChatGpt() {
        chatGptService.connectChatGpt();
    }

}
