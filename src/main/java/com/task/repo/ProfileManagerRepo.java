package com.task.repo;


import com.task.common.HttpUtil;
import com.task.common.JsonConverter;
import com.task.model.ProfileItem;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

@Repository
@Log
public class ProfileManagerRepo {

    private static final String HEADER_KEY_NAME = "realm";

    @Value("${system.id}")
    private String headerKey;

    @Value("${system.database-json-url}")
    private String databaseJsonUrl;

    public Optional<ProfileItem> getProfileByEmail(String email) {
        var id = new String(Base64.getEncoder().encode(email.getBytes(StandardCharsets.UTF_8)));
        var url = databaseJsonUrl + "/profile_manager/"  + id;
        var header = HttpUtil.getHeaderPostRequest();
        header.add(HEADER_KEY_NAME, headerKey);
        var response = HttpUtil.sendRequest(url, header).getBody();
        log.log(Level.INFO, "entry-task >> getProfileByEmail >> email: {0} >> url: {1} >> response: {2}", new Object[]{email, url, response});
        return JsonConverter.convertToObject(response, ProfileItem.class);
    }

    public boolean saveProfileItem(ProfileItem profileItem) {
        var logId = UUID.randomUUID().toString();
        var json = JsonConverter.convertObjectToJson(profileItem);
        try {
            var url = databaseJsonUrl + "/profile_manager/insert";
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            log.log(Level.INFO, "entry-task >> saveProfileItem >> json: {0} >> logId: {1}", new Object[]{JsonConverter.convertObjectToJson(profileItem), logId});
            var response = HttpUtil.sendPostRequest(url, json, header).getBody();
            log.log(Level.INFO, "entry-task >> saveProfileItem >> json: {0} >> logId: {1} >> response: {2}", new Object[]{JsonConverter.convertObjectToJson(profileItem), logId, response});
            var jsonObject = new JSONObject(response);
            return jsonObject.has("updated");
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("entry-task >> saveProfileItem >> logId: {0} >> json: {1} >> Exception:", logId, json), e);
            return false;
        }

    }


}
