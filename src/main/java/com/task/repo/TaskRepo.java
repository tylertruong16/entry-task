package com.task.repo;

import com.task.common.HttpUtil;
import com.task.common.JsonConverter;
import com.task.model.CallBackTask;
import com.task.model.TaskModel;
import com.task.model.TaskState;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

@Repository
@Log
public class TaskRepo {


    private static final String HEADER_KEY_NAME = "realm";

    @Value("${system.id}")
    private String headerKey;

    @Value("${system.database-json-url}")
    private String databaseJsonUrl;

    @Value("${system.email-profile}")
    private String emailProfile;

    @Value("${system.callback-url}")
    private String callbackUrl;


    public List<TaskModel> loadTaskBelongToWorker(TaskState status) {
        var workerId = Base64.encodeBase64String(emailProfile.getBytes(StandardCharsets.UTF_8));
        var response = "";
        try {
            var url = MessageFormat.format("{0}/{1}", databaseJsonUrl, "worker_task");
            var byId = MessageFormat.format("/search?filters=workerId::::{0}", workerId);
            var byStatus = MessageFormat.format("&filters=taskStatus::::{0}", status.name());
            var fullUrl = MessageFormat.format("{0}{1}{2}", url, byId, byStatus);
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            response = HttpUtil.sendRequest(fullUrl, header).getBody();
            var jsonObject = new JSONObject(response);
            var data = jsonObject.getJSONArray("data").toString();
            return Arrays.stream(JsonConverter.convertToObject(data, TaskModel[].class)
                    .orElse(new TaskModel[]{})).toList();
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("entry-task >> loadTaskBelongToWorker >> response: {0} >> Exception:", response), e);
            return new ArrayList<>();
        }
    }

    public boolean saveTaskModel(TaskModel data) {
        var logId = UUID.randomUUID().toString();
        var json = JsonConverter.convertObjectToJson(data);
        try {
            var url = MessageFormat.format("{0}/{1}/insert", databaseJsonUrl, "worker_task");
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            log.log(Level.INFO, "entry-task >> saveTaskModel >> json: {0} >> logId: {1}", new Object[]{JsonConverter.convertObjectToJson(data), logId});
            var response = HttpUtil.sendPostRequest(url, json, header).getBody();
            log.log(Level.INFO, "entry-task >> saveTaskModel >> json: {0} >> logId: {1} >> response: {2}", new Object[]{JsonConverter.convertObjectToJson(data), logId, response});
            var jsonObject = new JSONObject(response);
            return jsonObject.has("updated");
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("entry-task >> saveTaskModel >> logId: {0} >> json: {1} >> Exception:", logId, json), e);
            return false;
        }
    }

    public boolean callBackTask(CallBackTask task) {
        var logId = UUID.randomUUID().toString();
        var json = JsonConverter.convertObjectToJson(task);
        var url = MessageFormat.format(callbackUrl, task.getTaskId());
        try {
            log.log(Level.INFO, "entry-task >> callBackTask >> logId: {0} >> url: {1} >> json: {2}", new Object[]{logId, url, json});
            var response = HttpUtil.sendPostRequest(url, json, HttpUtil.getHeaderPostRequest());
            log.log(Level.INFO, "entry-task >> callBackTask >> logId: {0} >> response: {1}", new Object[]{logId, response});
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("entry-task >> callBackTask >> logId: {0} >> json: {1} >> Exception:", logId, json), e);
            return false;
        }
    }

}
