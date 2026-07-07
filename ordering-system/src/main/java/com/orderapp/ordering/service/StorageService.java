package com.orderapp.ordering.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;

/**
 * Gestisce l'upload/download di immagini su MinIO.
 * Attivo solo quando app.storage.enabled=true.
 * Se disabilitato, i metodi sono no-op e restituiscono il valore originale.
 */
@Slf4j
@Service
public class StorageService {

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${app.storage.enabled:false}")
    private boolean enabled;

    @Value("${app.storage.minio.bucket:orderapp}")
    private String bucket;

    @Value("${app.storage.minio.public-url:http://localhost:9000}")
    private String publicUrl;

    @PostConstruct
    public void init() {
        if (!enabled || minioClient == null) return;
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                String policy = """
                        {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}
                        """.formatted(bucket);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
                log.info("MinIO bucket '{}' creato con policy pubblica", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO init: impossibile inizializzare il bucket '{}': {}", bucket, e.getMessage());
        }
    }

    /**
     * Se l'input è un base64 data URL, lo carica su MinIO e restituisce l'URL pubblico.
     * Se storage è disabilitato o l'input è già un URL, restituisce il valore invariato.
     * In caso di errore MinIO, restituisce il base64 originale come fallback.
     */
    public String storeImage(String dataUrlOrUrl, String folder, String objectId) {
        if (!enabled || minioClient == null || dataUrlOrUrl == null) {
            return dataUrlOrUrl;
        }
        if (!dataUrlOrUrl.startsWith("data:")) {
            return dataUrlOrUrl;
        }
        try {
            int commaIdx = dataUrlOrUrl.indexOf(',');
            if (commaIdx < 0) return dataUrlOrUrl;

            byte[] bytes = Base64.getMimeDecoder().decode(dataUrlOrUrl.substring(commaIdx + 1));
            String objectName = folder + "/" + objectId + "-" + System.currentTimeMillis() + ".png";

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("image/png")
                    .build());

            String resultUrl = publicUrl + "/" + bucket + "/" + objectName;
            log.debug("MinIO upload: {} → {}", objectName, resultUrl);
            return resultUrl;
        } catch (Exception e) {
            log.warn("MinIO upload fallito per {}/{}: {} — fallback a base64", folder, objectId, e.getMessage());
            return dataUrlOrUrl;
        }
    }

    /**
     * Elimina un oggetto da MinIO se l'URL appartiene a questo storage.
     */
    public void deleteIfOwned(String url) {
        if (!enabled || minioClient == null || url == null) return;
        if (!url.startsWith(publicUrl)) return;
        try {
            String prefix = publicUrl + "/" + bucket + "/";
            if (!url.startsWith(prefix)) return;
            String objectName = url.substring(prefix.length());
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
            log.debug("MinIO delete: {}", objectName);
        } catch (Exception e) {
            log.warn("MinIO delete fallito per {}: {}", url, e.getMessage());
        }
    }

    /**
     * Scarica i byte di un'immagine da un URL MinIO o decodifica un base64 data URL.
     * Usato da StationQrCodeService per caricare il logo sul PDF.
     */
    public byte[] fetchImageBytes(String urlOrDataUrl) throws IOException {
        if (urlOrDataUrl == null || urlOrDataUrl.isBlank()) {
            throw new IOException("Sorgente immagine nulla o vuota");
        }
        if (urlOrDataUrl.startsWith("data:")) {
            int commaIdx = urlOrDataUrl.indexOf(',');
            if (commaIdx < 0) throw new IOException("Data URL non valido");
            return Base64.getMimeDecoder().decode(urlOrDataUrl.substring(commaIdx + 1));
        }
        // SSRF guard: only allow http/https and block private/internal addresses
        try {
            java.net.URL parsed = URI.create(urlOrDataUrl).toURL();
            String scheme = parsed.getProtocol();
            if (!"https".equals(scheme) && !"http".equals(scheme)) {
                throw new IOException("Schema URL non consentito: " + scheme);
            }
            java.net.InetAddress addr = java.net.InetAddress.getByName(parsed.getHost());
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                throw new IOException("URL risolve a un indirizzo privato/interno");
            }
        } catch (java.net.UnknownHostException | IllegalArgumentException ex) {
            throw new IOException("URL immagine non valido: " + ex.getMessage());
        }
        try {
            java.net.URLConnection conn = URI.create(urlOrDataUrl).toURL().openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);
            try (InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException ex) {
            throw new IOException("Impossibile recuperare immagine da URL: " + ex.getMessage(), ex);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
