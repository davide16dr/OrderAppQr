package com.orderapp.ordering.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true")
public class MinioConfig {

    @Value("${app.storage.minio.endpoint}")
    private String endpoint;

    @Value("${app.storage.minio.access-key}")
    private String accessKey;

    @Value("${app.storage.minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
