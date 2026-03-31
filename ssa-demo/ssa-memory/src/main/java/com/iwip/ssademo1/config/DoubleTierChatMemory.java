package com.iwip.ssademo1.config; // 注意保持你的包名

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 企业级双层聊天记忆体：L1 (Redis) + L2 (MySQL)
 * 【防反序列化崩溃终极版】
 */
public class DoubleTierChatMemory implements ChatMemory {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatMemory dbMemory; // 底层的 MySQL 记忆体
    private static final String CACHE_PREFIX = "chat:mem:";

    public DoubleTierChatMemory(RedisTemplate<String, Object> redisTemplate, ChatMemory dbMemory) {
        this.redisTemplate = redisTemplate;
        this.dbMemory = dbMemory;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 1. 数据永久落盘：写进 MySQL
        dbMemory.add(conversationId, messages);

        // 2. 获取最新上下文
        List<Message> latestMessages = dbMemory.get(conversationId);
        if (latestMessages != null && !latestMessages.isEmpty()) {
            // 【核心绝杀】：将复杂的 Message 对象剥离成纯 Map 集合，彻底绕过 Jackson 实例化报错！
            List<Map<String, String>> cacheData = serializeToMap(latestMessages);
            redisTemplate.opsForValue().set(CACHE_PREFIX + conversationId, cacheData, 2, TimeUnit.HOURS);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        String key = CACHE_PREFIX + conversationId;

        // 1. 从 Redis 极速获取纯 Map 数据
        Object cachedObj = redisTemplate.opsForValue().get(key);
        if (cachedObj instanceof List) {
            List<Map<String, String>> cachedData = (List<Map<String, String>>) cachedObj;
            if (!cachedData.isEmpty()) {
                // 【核心绝杀】：手动还原成 Spring AI 需要的对象
                return deserializeFromMap(cachedData);
            }
        }

        // 2. 缓存未命中，去 L2 (MySQL) 捞底
        List<Message> dbMessages = dbMemory.get(conversationId);

        // 3. 将捞到的数据回写到 Redis
        if (dbMessages != null && !dbMessages.isEmpty()) {
            redisTemplate.opsForValue().set(key, serializeToMap(dbMessages), 2, TimeUnit.HOURS);
        }

        return dbMessages != null ? dbMessages : List.of();
    }

    @Override
    public void clear(String conversationId) {
        dbMemory.clear(conversationId);
        redisTemplate.delete(CACHE_PREFIX + conversationId);
    }

    // ================= 私有转换辅助方法 =================

    /**
     * 将对象集合降维打击，转成最普通的 Map 集合
     */
    private List<Map<String, String>> serializeToMap(List<Message> messages) {
        List<Map<String, String>> cacheData = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, String> map = new HashMap<>();
            map.put("type", msg.getMessageType().name()); // 记录是 USER 还是 ASSISTANT
            map.put("content", msg.getText());         // 记录聊天内容
            cacheData.add(map);
        }
        return cacheData;
    }

    /**
     * 根据 Map 里的标识，手动 new 出对应的对象
     */
    private List<Message> deserializeFromMap(List<Map<String, String>> cachedData) {
        List<Message> result = new ArrayList<>();
        for (Map<String, String> map : cachedData) {
            String type = map.get("type");
            String content = map.get("content");
            if ("USER".equalsIgnoreCase(type)) {
                result.add(new UserMessage(content));
            } else if ("ASSISTANT".equalsIgnoreCase(type)) {
                result.add(new AssistantMessage(content));
            } else if ("SYSTEM".equalsIgnoreCase(type)) {
                result.add(new SystemMessage(content));
            }
        }
        return result;
    }
}