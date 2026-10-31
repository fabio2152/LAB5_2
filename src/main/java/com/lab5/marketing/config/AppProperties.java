package com.lab5.marketing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración de la app; cada valor se puede sobrescribir con variables de entorno (ver application.yml). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String storageDir,
        String publicBaseUrl,
        String ffmpegPath,
        Claude claude,
        S3 s3,
        Instagram instagram) {

    public record Claude(String apiKey, String model) {
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record S3(String bucket, String region, String publicUrl) {
        public boolean enabled() {
            return bucket != null && !bucket.isBlank();
        }
    }

    public record Instagram(String accessToken, String userId, String graphUrl) {
        public boolean enabled() {
            return accessToken != null && !accessToken.isBlank() && userId != null && !userId.isBlank();
        }
    }
}
