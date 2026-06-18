package com.iwip.common.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** 服务地址 */
    private String url;

    /** 用户名 */
    private String accessKey;

    /** 密码 */
    private String secretKey;

    /** 存储桶名称 */
    private String bucketName;
}
