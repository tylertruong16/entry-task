package com.task.repo;

import com.task.common.HttpUtil;
import com.task.common.JsonConverter;
import com.task.model.TaskModel;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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
    @Value("${system.worker-task-table-url}")
    private String workerTaskTableUrl;

    @Value("${system.email-profile}")
    private String emailProfile;


    public List<TaskModel> loadTaskBelongToWorker() {
        var workerId = Base64.encodeBase64String(emailProfile.getBytes(StandardCharsets.UTF_8));
        try {
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            var response = HttpUtil.sendRequest(workerTaskTableUrl, header).getBody();
            return Arrays.stream(JsonConverter.convertToObject(response, TaskModel[].class)
                            .orElse(new TaskModel[]{}))
                    .filter(it -> StringUtils.equalsIgnoreCase(it.getWorkerId(), workerId))
                    .toList();
        } catch (Exception e) {
            log.log(Level.WARNING, "entry-task >> loadTaskBelongToWorker >> Exception:", e);
            return new ArrayList<>();
        }
    }

    public boolean saveTaskModel(TaskModel data) {
        var logId = UUID.randomUUID().toString();
        var json = JsonConverter.convertObjectToJson(data);
        try {
            var url = MessageFormat.format("{0}/{1}", workerTaskTableUrl, "insert");
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

}
