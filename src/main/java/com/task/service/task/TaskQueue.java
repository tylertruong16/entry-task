package com.task.service.task;

import com.task.common.JsonConverter;
import com.task.model.TaskModel;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

@Service
@Log
public class TaskQueue {

    public final Queue<TaskModel> queue = new LinkedBlockingQueue<>();


    public void push(TaskModel task) {
        log.log(Level.INFO, "entry-task >> TaskQueue >> add new item: {0}", new Object[]{JsonConverter.convertObjectToJson(task)});
        this.queue.add(task);
    }

    public boolean taskInQueue(TaskModel task) {
        return queue.stream().anyMatch(it -> StringUtils.equalsIgnoreCase(it.getId(), task.getId()));
    }

    public Optional<TaskModel> popTask() {
        return Optional.ofNullable(this.queue.poll());
    }

}
