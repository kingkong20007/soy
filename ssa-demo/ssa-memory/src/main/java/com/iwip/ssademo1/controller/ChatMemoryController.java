package com.iwip.ssademo1.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/memory")
public class ChatMemoryController {

    @Autowired
    @Qualifier("geminiChatClient")
    private ChatClient geminiClient;

    /**
     * 使用带有记忆功能的 ChatClient 进行流式对话 (Google Gemini)
     */
    @GetMapping("/gemini/chat")
    public Flux<String> geminiChat(@RequestParam String sessionId, @RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return geminiClient.prompt()
                .user(msg)
                .advisors(advisorSpec -> advisorSpec
                        .param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, sessionId)
                )
                .stream()
                .content();
    }

}
