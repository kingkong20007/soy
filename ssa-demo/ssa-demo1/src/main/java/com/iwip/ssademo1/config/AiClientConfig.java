package com.iwip.ssademo.config;

import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;
import com.iwip.ssademo.service.impl.DoubleTierChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiClientConfig {

    @Value("${ai.chat.memory.max-messages:100}")
    private int maxMessages;

    // ==================== 极其核心的记忆落盘配置 ====================
    // 全场只保留这一个带 Redis 和 MySQL 双引擎的 chatMemory Bean！
    @Bean
    public ChatMemory chatMemory(JdbcTemplate jdbcTemplate, RedisTemplate<String, Object> redisTemplate) {

        // 1. 底层 L2：MySQL 物理存储仓库
        MysqlChatMemoryRepository repository = MysqlChatMemoryRepository.mysqlBuilder()
                .jdbcTemplate(jdbcTemplate)
                .build();

        // 2. 逻辑层：滑动窗口控制器，限制 MySQL 最多取 maxMessages 条
        ChatMemory windowMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(maxMessages)
                .build();

        // 3. 终极防御塔：把 MySQL 记忆体塞进 Redis 缓存壳子里！
        return new DoubleTierChatMemory(redisTemplate, windowMemory);
    }

    // ==========================================================================================
    // 2. ChatClient Beans (各模型客户端装配)
    // ==========================================================================================

    /**
     * 阿里云通义千问客户端装配
     */
    @Bean(name = "aliMemoryClient")
    public ChatClient aliChatClient(@Qualifier("dashScopeChatModel") ChatModel aliChatModel,
                                    ChatMemory chatMemory) {
        return buildChatClient(aliChatModel, chatMemory);
    }

    /**
     * Google Gemini 客户端
     */
    @Bean(name = "geminiMemoryClient")
    public ChatClient geminiChatClient(@Qualifier("googleGenAiChatModel") ChatModel geminiChatModel,
                                       ChatMemory chatMemory) {
        return buildChatClient(geminiChatModel, chatMemory);
    }

    /**
     * 本地 Ollama 客户端
     */
    @Bean(name = "ollamaMemoryClient")
    public ChatClient ollamaChatClient(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
                                       ChatMemory chatMemory) {
        return buildChatClient(ollamaChatModel, chatMemory);
    }


    /**
     * 抽取公共的 ChatClient 构建逻辑，遵循 DRY 原则。
     *
     * @param chatModel  底层具体的大模型实现
     * @param chatMemory 注入的持久化记忆体
     * @return 组装了记忆拦截器 (Advisor) 的标准 ChatClient
     */
    private ChatClient buildChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}