package com.lab5.marketing.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Respuesta estructurada que se le pide a Claude: análisis de la foto + 3 guiones. */
public record ScriptProposal(
        @JsonPropertyDescription("Descripción breve de lo que se ve en la foto y del producto o servicio que promociona")
        String productSummary,
        @JsonPropertyDescription("Exactamente 3 guiones con enfoques creativos distintos")
        List<Draft> scripts) {

    public record Draft(
            @JsonPropertyDescription("Nombre corto del guion, máximo 5 palabras")
            String title,
            @JsonPropertyDescription("Enfoque creativo, por ejemplo: emocional, beneficio funcional, urgencia, humor")
            String angle,
            @JsonPropertyDescription("Gancho de los primeros 3 segundos del video, máximo 12 palabras")
            String hook,
            @JsonPropertyDescription("Texto completo para narrar en un reel de 15 segundos")
            String voiceover,
            @JsonPropertyDescription("Exactamente 3 titulares de máximo 6 palabras, uno por cada foto publicitaria")
            List<String> headlines,
            @JsonPropertyDescription("Llamado a la acción de máximo 4 palabras, por ejemplo 'Pide el tuyo hoy'")
            String callToAction,
            @JsonPropertyDescription("Texto del post de Instagram, 2 a 4 oraciones, con emojis moderados y sin hashtags")
            String caption,
            @JsonPropertyDescription("Entre 5 y 8 hashtags relevantes, cada uno empezando con #")
            List<String> hashtags,
            @JsonPropertyDescription("Color de acento de la marca en formato hexadecimal #RRGGBB que combine con la foto")
            String accentColor) {
    }
}
