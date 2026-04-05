package com.iwip.common.minio.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    /**
     * 初始化 MinIO 客户端
     *
     * @param minioProperties MinIO 配置属性
     * @return MinioClient
     */
    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {
        return MinioClient.builder()
            .endpoint(minioProperties.getUrl())
            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
            .build();
    }
}
