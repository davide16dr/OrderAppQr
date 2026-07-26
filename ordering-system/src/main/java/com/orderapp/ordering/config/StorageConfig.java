package com.orderapp.ordering.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final StorageProperties props;

    @Bean
    @ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true")
    public MinioClient minioClient() {
        StorageProperties.Minio m = props.getMinio();
        log.info("Initializing MinIO/R2 client — endpoint: {}, bucket: {}", m.getEndpoint(), m.getBucket());
        return MinioClient.builder()
                .endpoint(m.getEndpoint())
                .credentials(m.getAccessKey(), m.getSecretKey())
                .build();
    }
}
