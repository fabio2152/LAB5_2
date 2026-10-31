package com.lab5.marketing.social;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab5.marketing.config.AppProperties;

/**
 * Publica en Instagram mediante la Instagram Graph API (Content Publishing):
 * crea un contenedor por medio, espera a que Instagram lo procese y luego lo publica.
 * Requiere una cuenta profesional (Business o Creator) y que las URLs de los medios sean públicas.
 */
@Service
public class InstagramPublisher {

    private static final Logger log = LoggerFactory.getLogger(InstagramPublisher.class);

    /** Resultado de una publicación exitosa. */
    public record Published(String mediaId, String permalink) {
    }

    private final AppProperties.Instagram config;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public InstagramPublisher(AppProperties props, ObjectMapper mapper) {
        this.config = props.instagram();
        this.mapper = mapper;
    }

    public boolean isConfigured() {
        return config.enabled();
    }

    /** Carrusel con las fotos (JPEG con URL pública). */
    public Published publishCarousel(List<String> imageUrls, String caption) {
        List<String> children = imageUrls.stream()
                .map(url -> createContainer(Map.of("image_url", url, "is_carousel_item", "true")))
                .toList();
        for (String child : children) {
            waitUntilReady(child, Duration.ofMinutes(2));
        }
        String carousel = createContainer(Map.of(
                "media_type", "CAROUSEL",
                "children", String.join(",", children),
                "caption", caption));
        waitUntilReady(carousel, Duration.ofMinutes(2));
        return publish(carousel);
    }

    /** Reel con el video MP4 (URL pública). */
    public Published publishReel(String videoUrl, String caption) {
        String container = createContainer(Map.of(
                "media_type", "REELS",
                "video_url", videoUrl,
                "caption", caption,
                "share_to_feed", "true"));
        waitUntilReady(container, Duration.ofMinutes(5));
        return publish(container);
    }

    private String createContainer(Map<String, String> params) {
        JsonNode res = post("/" + config.userId() + "/media", params);
        return res.path("id").asText();
    }

    private Published publish(String containerId) {
        JsonNode res = post("/" + config.userId() + "/media_publish", Map.of("creation_id", containerId));
        String mediaId = res.path("id").asText();
        String permalink = null;
        try {
            permalink = get("/" + mediaId, Map.of("fields", "permalink")).path("permalink").asText(null);
        } catch (RuntimeException e) {
            log.warn("No se pudo obtener el permalink de {}: {}", mediaId, e.getMessage());
        }
        return new Published(mediaId, permalink);
    }

    /** Consulta status_code del contenedor hasta que sea FINISHED. */
    private void waitUntilReady(String containerId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            JsonNode res = get("/" + containerId, Map.of("fields", "status_code,status"));
            String status = res.path("status_code").asText();
            switch (status) {
                case "FINISHED", "PUBLISHED" -> {
                    return;
                }
                case "ERROR", "EXPIRED" -> throw new InstagramException(
                        "Instagram no pudo procesar el medio: " + res.path("status").asText(status));
                default -> {
                    if (System.nanoTime() > deadline) {
                        throw new InstagramException("Instagram tardó demasiado en procesar el medio (" + status + ")");
                    }
                    sleep(Duration.ofSeconds(4));
                }
            }
        }
    }

    // ---- HTTP ----

    private JsonNode post(String path, Map<String, String> params) {
        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("access_token", config.accessToken());
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.graphUrl() + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(all)))
                .build();
        return send(request);
    }

    private JsonNode get(String path, Map<String, String> params) {
        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("access_token", config.accessToken());
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.graphUrl() + path + "?" + form(all)))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        return send(request);
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = mapper.readTree(response.body());
            if (response.statusCode() >= 400 || body.has("error")) {
                String message = body.path("error").path("message").asText(response.body());
                throw new InstagramException("Instagram respondió " + response.statusCode() + ": " + message);
            }
            return body;
        } catch (IOException e) {
            throw new InstagramException("Error de red con Instagram: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InstagramException("Publicación interrumpida");
        }
    }

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InstagramException("Publicación interrumpida");
        }
    }

    public static class InstagramException extends RuntimeException {
        public InstagramException(String message) {
            super(message);
        }
    }
}
