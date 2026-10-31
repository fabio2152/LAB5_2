package com.lab5.marketing.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Foto o video generado para la campaña. */
@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType type;

    private int position;

    /** Ruta relativa que sirve la app en /media/... */
    @Column(nullable = false)
    private String localPath;

    /** URL pública (S3 o PUBLIC_BASE_URL) que se entrega a Instagram. */
    @Column(columnDefinition = "TEXT")
    private String publicUrl;

    private Instant createdAt = Instant.now();

    protected Asset() {
    }

    public Asset(AssetType type, int position, String localPath, String publicUrl) {
        this.type = type;
        this.position = position;
        this.localPath = localPath;
        this.publicUrl = publicUrl;
    }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public AssetType getType() { return type; }
    public int getPosition() { return position; }
    public String getLocalPath() { return localPath; }
    public String getPublicUrl() { return publicUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
