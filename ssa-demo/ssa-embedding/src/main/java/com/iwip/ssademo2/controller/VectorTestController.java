package controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vector")
public class VectorTestController {

    // 🌟 Spring AI 已经为你准备好了这把神器！
    @Autowired
    private VectorStore vectorStore;

    /**
     * 测试接口：将中文转化为向量存入 Redis
     * 访问地址：http://localhost:8082/add?text=镍铁仓库今天入库了50吨矿石
     */
    @GetMapping("/add")
    public String addVector(@RequestParam String text) {

        // 1. 将文本包装成 Document，顺便打上业务标签 (Metadata)
        Document document = new Document(
                text,
                Map.of("author", "sys-admin", "project", "npi-warehouse")
        );

        // 2. 存入 Redis！
        // 框架会自动调用 Gemini 获取 768 维度的向量，然后连同文本一起存进本地 Redis 6380
        vectorStore.add(List.of(document));

        return "🎉 太不容易了！文字：【" + text + "】已经成功向量化并存入 Redis 啦！";
    }
}
