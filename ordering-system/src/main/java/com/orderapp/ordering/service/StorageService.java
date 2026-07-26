package com.orderapp.ordering.service;

import com.orderapp.ordering.config.StorageProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class StorageService {

    private final MinioClient minioClient;
    private final StorageProperties props;

    @Autowired
    public StorageService(@Autowired(required = false) MinioClient minioClient,
                          StorageProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }

    /**
     * Uploads a base64 data URL to R2 and returns the public URL.
     * Returns the original value unchanged if storage is disabled or input is not a data URL.
     */
    public String uploadImageDataUrl(String folder, String imageDataUrl) {
        if (!props.isEnabled() || minioClient == null) {
            log.warn("Storage disabled or MinioClient null — skipping upload (enabled={}, client={})",
                    props.isEnabled(), minioClient != null ? "present" : "null");
            return imageDataUrl;
        }
        if (imageDataUrl == null || !imageDataUrl.startsWith("data:")) {
            return imageDataUrl;
        }

        try {
            // Format: data:<mimeType>;base64,<data>
            int commaIdx = imageDataUrl.indexOf(',');
            if (commaIdx < 0) return imageDataUrl;

            String header = imageDataUrl.substring(5, commaIdx); // e.g. image/jpeg;base64
            String base64Data = imageDataUrl.substring(commaIdx + 1);

            String mimeType = header.contains(";") ? header.substring(0, header.indexOf(';')) : "image/jpeg";
            String extension = extensionFor(mimeType);

            byte[] bytes = Base64.getDecoder().decode(base64Data);
            String objectKey = folder + "/" + UUID.randomUUID() + "." + extension;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getMinio().getBucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(mimeType)
                    .build());

            String publicUrl = props.getMinio().getPublicUrl().replaceAll("/$", "") + "/" + objectKey;
            log.info("Image uploaded to R2: {}", publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("Failed to upload image to R2, falling back to original value: {}", e.getMessage());
            return imageDataUrl;
        }
    }

    private String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
