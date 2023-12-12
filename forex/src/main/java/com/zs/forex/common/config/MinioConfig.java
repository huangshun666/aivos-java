package com.zs.forex.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * minio相关配置
 */

@Data
@Component
@ConfigurationProperties(value = "minio")
public class MinioConfig {

    public String endpoint;

    public String accessKey;

    public String secretKey;

    public String bucketName;

    private String openAddr;

}
