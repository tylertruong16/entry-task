package com.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallBackTask {
    private String taskId;
    private String serviceCode;
    private String result;
}
