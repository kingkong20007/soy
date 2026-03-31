package com.iwip.ssademo2.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vector")
public class VectorTestController {

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    public VectorTestController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        // 🌟 初始化 ChatClient
        this.chatClient = chatClientBuilder.build();
    }


    /**
     * 实战接口：一键完成【文本转向量 + 存入 Redis】
     * 测试地址：http://localhost:8082/vector/save?text=镍铁仓库今天入库了50吨高品位矿石，操作人是张三
     */
    @GetMapping("/save")
    public String saveToRedis(@RequestParam String text) {

        // 2. 封装数据：把你的业务文本包装成 Document 对象。
        // 旁边这个 Map 非常重要，这就是 Metadata（元数据），以后你可以根据这些标签进行精准过滤检索！
        Document document = new Document(
                text,
                Map.of(
                        "project", "IWIP-NPI",      // 所属项目：镍铁
                        "type", "inbound_log",      // 数据类型：入库日志
                        "timestamp", System.currentTimeMillis()
                )
        );

        // 3. 见证奇迹的时刻：执行 add 方法！
        // 💡 这一行代码在底层干了三件事：
        //   a. 调用 Google Gemini 把你的 text 变成 768 维的浮点数数组
        //   b. 把数组压缩成十六进制的 Byte 字节流
        //   c. 使用我们 YAML 里配的 custom-prefix 前缀，存进 6380 端口的 Redis 里面！
        vectorStore.add(List.of(document));

        return "🎉 帅炸了！数据已成功向量化并永久封存在 Redis 向量库中！\n存入的内容是：【" + text + "】";
    }


    /**
     * 访问地址： http://localhost:8082/vector/chat?question=今天仓库里进了什么货？
     *
     * @param question
     * @return
     */
    @GetMapping("/chat")
    public String chatWithData(@RequestParam String question) {

        // ==========================================
        // 步骤 1：去 Redis 里进行“向量相似度检索”
        // ==========================================
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(2) // 只取  "最相关的"   2 条记录
                .build();
        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);

        // 把查到的文档内容拼成一段字符串，当做“参考资料”
        StringBuilder context = new StringBuilder();
        for (Document doc : similarDocuments) {
            // 🌟 修复点 2：使用 getText() 获取文本内容
            context.append(doc.getText()).append("\n");
        }

        // ==========================================
        // 步骤 2：把“参考资料”和“用户问题”一起喂给大模型
        // ==========================================
        String systemPrompt = "你是一个智能仓库助手。请严格根据以下【参考资料】来回答用户的问题。如果资料里没有，你就回答不知道。\n\n【参考资料】:\n" + context.toString();

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        return "🤖 AI 仓库助手回答：\n" + answer;
    }
}
