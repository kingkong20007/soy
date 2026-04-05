package com.iwip.common.minio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component("minioProperties")
@ConfigurationProperties(prefix = "minio") // 配置前缀为 minio
public class MinioProperties {

    /**
     * 服务地址
     */
    private String url;

    /**
     * 用户名
     */
    private String accessKey;

    /**
     * 密码
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 文件预览地址
     */
    private String previewUrl;
}
