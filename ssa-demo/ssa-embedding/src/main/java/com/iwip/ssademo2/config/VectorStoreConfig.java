package com.iwip.ssademo2.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore myRedisVectorStore(
            // 🌟 重点：如果有多个模型，在这里精准指定你要注入哪一个！
            GoogleGenAiTextEmbeddingModel geminiModel,
            RedisConnectionDetails redisConnectionDetails) {

        // 我们手动把 Gemini 的模型塞进 Redis 向量库里
        return new RedisVectorStore(
                RedisVectorStore.builder(new JedisPooled(),geminiModel).build());
    }
}