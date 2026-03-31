package com.iwip.ssademo2.config;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.SecureUtil;
import com.google.api.client.util.SecurityUtils;
import org.apache.catalina.security.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class InitVectorDatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(InitVectorDatabaseConfig.class);

    private static final String VECTOR_STORE_KEY = "vector-iwip";

    @Autowired
    private VectorStore vectorStore;

    @Value("classpath:ops.txt")
    private Resource opsFile;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() {

        TextReader textReader = new TextReader(opsFile);
        textReader.setCharset(StandardCharsets.UTF_8);
        // 1. 读取文件内容并转换为 Document 列表
        List<Document> documents = textReader.get();
        // 2. 将读取到的错误码文档批量存入向量数据库
//        vectorStore.add(documents);

        //3.改进，防止每次重启时重复加载向量文件，

        //3.1 通过TextReader获取对应向量化数据库的中加载的"source"的文件名(带后缀)
        String sourceMetadata = (String) textReader.getCustomMetadata().get("source");

        System.out.println(sourceMetadata);

        //3.2 注意：要对加载的文件进行加密，防止被篡改
        String sourceHash = SecureUtil.md5(sourceMetadata);

        String redisKey = VECTOR_STORE_KEY + sourceHash;

        String uuid = UUID.fastUUID().toString();
        Boolean resultFlag = redisTemplate.opsForValue().setIfAbsent(redisKey,uuid);
        if (Boolean.TRUE.equals(resultFlag)) {
            vectorStore.add(documents);
        } else {
            log.info("向量数据库已存在");
        }
    }
}
