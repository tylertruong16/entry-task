package com.task.service;


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
    @Value("${system.profile-table-url}")
    private String profileTableUrl;

    public List<ProfileItem> getAllProfile() {
        try {
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            var response = HttpUtil.sendRequest(profileTableUrl, header).getBody();
            return Arrays.stream(JsonConverter.convertToObject(response, ProfileItem[].class).orElse(new ProfileItem[]{})).toList();
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> getAllProfile >> Exception:", e);
            return new ArrayList<>();
        }
    }

    public Optional<ProfileItem> getProfileByEmail(String email) {
        var id = new String(Base64.getEncoder().encode(email.getBytes(StandardCharsets.UTF_8)));
        var url = profileTableUrl + "/" + id;
        var header = HttpUtil.getHeaderPostRequest();
        header.add(HEADER_KEY_NAME, headerKey);
        var response = HttpUtil.sendRequest(url, header).getBody();
        log.log(Level.INFO, "cloud-shell-task >> getProfileByEmail >> email: {0} >> url: {1} >> response: {2}", new Object[]{email, url, response});
        return JsonConverter.convertToObject(response, ProfileItem.class);
    }

    public boolean saveProfileItem(ProfileItem profileItem) {
        var logId = UUID.randomUUID().toString();
        var json = JsonConverter.convertObjectToJson(profileItem);
        try {
            var url = MessageFormat.format("{0}/{1}", profileTableUrl, "insert");
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            log.log(Level.INFO, "cloud-shell-task >> saveProfileItem >> json: {0} >> logId: {1}", new Object[]{JsonConverter.convertObjectToJson(profileItem), logId});
            var response = HttpUtil.sendPostRequest(url, json, header).getBody();
            log.log(Level.INFO, "cloud-shell-task >> saveProfileItem >> json: {0} >> logId: {1} >> response: {2}", new Object[]{JsonConverter.convertObjectToJson(profileItem), logId, response});
            var jsonObject = new JSONObject(response);
            return jsonObject.has("updated");
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("cloud-shell-task >> saveProfileItem >> logId: {0} >> json: {1} >> Exception:", logId, json), e);
            return false;
        }

    }


}
