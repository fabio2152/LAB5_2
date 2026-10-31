package com.lab5.marketing.ai;

import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.BadRequestException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.lab5.marketing.config.AppProperties;

/**
 * Pide a Claude que analice la foto del producto y proponga 3 guiones de marketing.
 * Sin ANTHROPIC_API_KEY devuelve guiones de ejemplo para poder probar el flujo completo.
 */
@Service
public class ScriptGenerator {

    private static final Logger log = LoggerFactory.getLogger(ScriptGenerator.class);

    private static final String SYSTEM_PROMPT = """
            Eres un director creativo de una agencia de marketing digital en Latinoamérica.
            Recibes la foto de un producto o servicio de una empresa y creas campañas para Instagram
            (carrusel de 3 fotos + reel vertical de 10-15 segundos).
            Escribe todo en español neutro, con un tono cercano y persuasivo, sin prometer cosas que la
            foto no respalde. Los 3 guiones deben tener enfoques realmente distintos entre sí.
            """;

    private final AppProperties.Claude config;
    private final AnthropicClient client;

    public ScriptGenerator(AppProperties props) {
        this.config = props.claude();
        this.client = config.enabled()
                ? AnthropicOkHttpClient.builder().apiKey(config.apiKey()).build()
                : null;
    }

    public boolean isDemoMode() {
        return client == null;
    }

    /**
     * @param jpegImage foto ya redimensionada (lado mayor &lt;= 1568 px) en JPEG
     */
    public ScriptProposal propose(byte[] jpegImage, String companyName, String description, String audience) {
        if (client == null) {
            return demoProposal(companyName);
        }

        String prompt = """
                Empresa: %s
                Descripción del producto/servicio: %s
                Público objetivo: %s

                Analiza la foto adjunta y propone exactamente 3 guiones de campaña.
                Cada guion necesita exactamente 3 titulares (uno por foto del carrusel).
                """.formatted(companyName, orDash(description), orDash(audience));

        List<ContentBlockParam> content = List.of(
                ContentBlockParam.ofImage(ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                .data(Base64.getEncoder().encodeToString(jpegImage))
                                .build())
                        .build()),
                ContentBlockParam.ofText(TextBlockParam.builder().text(prompt).build()));

        StructuredMessage<ScriptProposal> response;
        try {
            response = client.messages().create(buildParams(content, true));
        } catch (BadRequestException e) {
            // Si la cuenta no acepta el parámetro beta de fallback, se reintenta sin él
            log.warn("Claude rechazó la petición con fallback, reintentando sin él: {}", e.getMessage());
            response = client.messages().create(buildParams(content, false));
        }

        if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            throw new IllegalStateException("Claude no pudo generar guiones para esta foto. Prueba con otra imagen.");
        }

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(typed -> typed.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude no devolvió guiones"));
    }

    private StructuredMessageCreateParams<ScriptProposal> buildParams(List<ContentBlockParam> content, boolean withFallback) {
        var builder = MessageCreateParams.builder()
                .model(config.model())
                .maxTokens(16000L)
                .system(SYSTEM_PROMPT)
                .outputConfig(ScriptProposal.class)
                .addUserMessageOfBlockParams(content);
        if (withFallback) {
            // Si el modelo rechaza la petición, la API reintenta con otro modelo automáticamente
            builder.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
        return builder.build();
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static ScriptProposal demoProposal(String company) {
        return new ScriptProposal(
                "Modo demo (sin ANTHROPIC_API_KEY): guiones de ejemplo para " + company + ".",
                List.of(
                        new ScriptProposal.Draft(
                                "Momentos que importan", "Emocional",
                                "¿Qué te hace sonreír cada día?",
                                "Hay cosas pequeñas que cambian tu día. En " + company
                                        + " las hacemos con cariño para que disfrutes cada momento.",
                                List.of("Hecho para ti", "Calidad que se siente", "Vive el momento"),
                                "Descúbrelo hoy",
                                "Cada detalle cuenta ✨ En " + company + " creamos lo que te hace feliz.",
                                List.of("#" + slug(company), "#hechoconamor", "#calidad", "#emprendimiento", "#peru"),
                                "#E4572E"),
                        new ScriptProposal.Draft(
                                "Beneficio claro", "Beneficio funcional",
                                "Lo que buscabas, por fin.",
                                "Rápido, práctico y pensado para tu día a día. " + company
                                        + " te da exactamente lo que necesitas.",
                                List.of("Simple y práctico", "Diseñado para durar", "Tu mejor elección"),
                                "Pide el tuyo",
                                "Menos complicaciones, más resultados 💪 Conoce lo nuevo de " + company + ".",
                                List.of("#" + slug(company), "#practico", "#novedad", "#compralocal", "#tendencia"),
                                "#2E86AB"),
                        new ScriptProposal.Draft(
                                "Solo por hoy", "Urgencia",
                                "Últimas unidades disponibles.",
                                "No te quedes sin el tuyo. Por tiempo limitado, " + company
                                        + " tiene una oferta que no vas a querer perder.",
                                List.of("Oferta limitada", "No te lo pierdas", "Últimas unidades"),
                                "Compra ahora",
                                "⏰ Solo por esta semana. Escríbenos y separa el tuyo antes de que se agote.",
                                List.of("#" + slug(company), "#oferta", "#promo", "#descuento", "#soloporhoy"),
                                "#6A4C93")));
    }

    private static String slug(String text) {
        String s = text.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return s.isEmpty() ? "marca" : s;
    }
}
