package com.lab5.marketing.domain;

public enum CampaignStatus {
    SCRIPTS_PROPOSED("Guiones propuestos"),
    ASSETS_GENERATED("Contenido generado"),
    PUBLISHED("Publicado"),
    FAILED("Con errores");

    private final String label;

    CampaignStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
