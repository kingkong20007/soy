package com.iwip.ssademo2.config;

import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration //多向量模型共存时，指定选择某一个模型
public class VectorStoreConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.ai.vectorstore.redis.index-name}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix}")
    private String prefix;

    @Value("${spring.ai.vectorstore.redis.initialize-schema}")
    private boolean initializeSchema;

    @Bean
    public VectorStore myRedisVectorStore(// 🌟 重点：如果有多个模型，在这里精准指定你要注入哪一个！
                                          GoogleGenAiTextEmbeddingModel geminiModel) {
        JedisPooled jedisPooled = new JedisPooled(redisHost, redisPort);
        // 🌟 3. 原生 Builder 组装
        return RedisVectorStore.builder(jedisPooled, geminiModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(initializeSchema)
                .build();
    }
}