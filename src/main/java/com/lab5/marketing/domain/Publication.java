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

/** Resultado de publicar el carrusel o el reel en la red social. */
@Entity
public class Publication {

    public enum Kind { CAROUSEL, REEL }

    public enum Status { PUBLISHED, SIMULATED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(nullable = false)
    private String platform = "INSTAGRAM";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String mediaId;

    private String permalink;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Instant createdAt = Instant.now();

    protected Publication() {
    }

    public Publication(Kind kind, Status status, String mediaId, String permalink, String message) {
        this.kind = kind;
        this.status = status;
        this.mediaId = mediaId;
        this.permalink = permalink;
        this.message = message;
    }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public String getPlatform() { return platform; }
    public Kind getKind() { return kind; }
    public Status getStatus() { return status; }
    public String getMediaId() { return mediaId; }
    public String getPermalink() { return permalink; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
