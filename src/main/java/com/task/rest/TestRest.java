package com.task.rest;

import com.task.service.task.ChatGptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestRest {

    final ChatGptService chatGptService;

    public TestRest(ChatGptService chatGptService) {
        this.chatGptService = chatGptService;
    }

    @GetMapping("/chat-gpt")
    public Object testPage(@RequestParam String email) {
        chatGptService.connectChatGpt(email);
        return Map.of("message", "ok");
    }

}
