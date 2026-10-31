package com.lab5.marketing.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/** Una campaña de marketing: la foto que sube la empresa y todo lo que se genera a partir de ella. */
@Entity
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String productDescription;

    private String targetAudience;

    /** Nombre del archivo de la foto original dentro de la carpeta de la campaña. */
    private String originalImage;

    /** Descripción de la foto que devuelve Claude. */
    @Column(columnDefinition = "TEXT")
    private String productSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.SCRIPTS_PROPOSED;

    /** true si los guiones se generaron sin API key (contenido de ejemplo). */
    private boolean demoMode;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<Script> scripts = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("type, position")
    private List<Asset> assets = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt")
    private List<Publication> publications = new ArrayList<>();

    public void addScript(Script script) {
        script.setCampaign(this);
        scripts.add(script);
    }

    public void addAsset(Asset asset) {
        asset.setCampaign(this);
        assets.add(asset);
    }

    public void addPublication(Publication publication) {
        publication.setCampaign(this);
        publications.add(publication);
    }

    public Optional<Script> getSelectedScript() {
        return scripts.stream().filter(Script::isSelected).findFirst();
    }

    public List<Asset> getImages() {
        return assets.stream().filter(a -> a.getType() == AssetType.IMAGE).toList();
    }

    public Optional<Asset> getVideo() {
        return assets.stream().filter(a -> a.getType() == AssetType.VIDEO).findFirst();
    }

    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public String getOriginalImage() { return originalImage; }
    public void setOriginalImage(String originalImage) { this.originalImage = originalImage; }
    public String getProductSummary() { return productSummary; }
    public void setProductSummary(String productSummary) { this.productSummary = productSummary; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public boolean isDemoMode() { return demoMode; }
    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Script> getScripts() { return scripts; }
    public List<Asset> getAssets() { return assets; }
    public List<Publication> getPublications() { return publications; }
}
