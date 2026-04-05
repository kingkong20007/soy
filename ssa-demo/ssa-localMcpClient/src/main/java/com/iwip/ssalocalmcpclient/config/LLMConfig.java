package com.iwip.ssalocalmcpclient.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider tools) {
       return builder
               .defaultToolCallbacks(tools.getToolCallbacks())//mcp协议， 配置见yml文件
               .build();
    }


}
