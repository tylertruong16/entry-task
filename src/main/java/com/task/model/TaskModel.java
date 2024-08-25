package com.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskModel implements Serializable {
    private String workerId;
    private String id;
    private String taskType;
    private String input;
    private String output;
    private String taskStatus;
    private String updateAt;


    public boolean openTask() {
        return StringUtils.equalsIgnoreCase(taskStatus, TaskState.OPEN.name());
    }

}
