package com.iwip.ssademo1.config;


import com.alibaba.cloud.ai.memory.redis.LettuceRedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisMemoryConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.database}")
    private int database;

    private final static int MAX_MESSAGES = 20;

    /**
     * 配置redis内存
     * @return
     */
    @Bean
    public LettuceRedisChatMemoryRepository redisChatMemoryRepository() {
        //存储层（负责把数据写进 Redis）。
        return LettuceRedisChatMemoryRepository.builder()
                .host(host)
                .port(port)
                .database(database)
                .build();
    }

    /**
     * 配置redis内存
     * @return
     */
    @Bean("geminiChatClient")
    public ChatClient chatClient(@Qualifier("googleGenAiChatModel") ChatModel chatModel,
                                 LettuceRedisChatMemoryRepository redisChatMemoryRepository) {

        //MessageWindowChatMemory：记忆的管理策略（滑动窗口）
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(MAX_MESSAGES) //允许最大从redis取近20条数据，供大模型去检索上下文
                .build();

        // MessageChatMemoryAdvisor：自动处理上下文的拦截器(用户提出问题，进行拦截，去redis找当前用户的数据，为LLM定向检索上下文提供数据
        // ，大模型，返回数据后，同样继续拦截，将数据存储到redis里面)
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(windowChatMemory).build())
                .build();
    }

}
