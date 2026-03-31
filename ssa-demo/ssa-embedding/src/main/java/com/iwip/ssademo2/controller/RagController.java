package com.iwip.ssademo2.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rag")
public class RagController {
    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    /**
     * 构造函数注入：自动装配向量数据库和聊天客户端
     *
     * @param vectorStore 注入在 VectorStoreConfig 中配置的 Redis 向量库
     * @param chatClientBuilder 用于构建 ChatClient 实例
     */
    public RagController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }


    /**
     * 基础 RAG 问答接口：结合向量数据库中的错误码信息回答问题
     * 测试地址：http://localhost:8082/rag/chat?question=A0102是什么错误？
     *
     * @param question 用户查询的问题
     * @return AI 结合上下文后的回答
     */
    @GetMapping("/chat")
    public Flux<String> ragChat(@RequestParam String question) {
        // 1. 相似度检索：从 Redis 向量库中查找最相关的 3 条错误码定义
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build()
        );

        // 2. 提取检索到的文本内容
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 3. 构建系统提示词，注入上下文
        String systemPrompt = """
                你是一个专业的仓储系统技术支持助手。
                请根据以下提供的【参考资料】（包含错误码及其含义）来回答用户的问题。
                如果资料中没有相关信息，请礼貌地告知用户无法从已知错误码库中找到答案。
                
                【参考资料】:
                %s
                """.formatted(context);

        //使用RAG检索-检索


        // 4. 调用大模型生成回答
        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content();
    }

}
