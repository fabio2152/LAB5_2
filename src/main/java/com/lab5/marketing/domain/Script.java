package com.lab5.marketing.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

/** Uno de los 3 guiones que propone el sistema para la campaña. */
@Entity
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    private int position;

    private String title;

    /** Enfoque creativo del guion (emocional, beneficio, urgencia…). */
    private String angle;

    @Column(columnDefinition = "TEXT")
    private String hook;

    /** Texto que se narraría en el video. */
    @Column(columnDefinition = "TEXT")
    private String voiceover;

    /** Un titular por cada foto generada. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "script_headline", joinColumns = @JoinColumn(name = "script_id"))
    @OrderColumn(name = "idx")
    @Column(name = "headline")
    private List<String> headlines = new ArrayList<>();

    private String callToAction;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String hashtags;

    /** Color de acento en formato #RRGGBB. */
    private String accentColor;

    private boolean selected;

    /** Texto final del post: caption + hashtags. */
    public String fullCaption() {
        return hashtags == null || hashtags.isBlank() ? caption : caption + "\n\n" + hashtags;
    }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAngle() { return angle; }
    public void setAngle(String angle) { this.angle = angle; }
    public String getHook() { return hook; }
    public void setHook(String hook) { this.hook = hook; }
    public String getVoiceover() { return voiceover; }
    public void setVoiceover(String voiceover) { this.voiceover = voiceover; }
    public List<String> getHeadlines() { return headlines; }
    public void setHeadlines(List<String> headlines) { this.headlines = new ArrayList<>(headlines); }
    public String getCallToAction() { return callToAction; }
    public void setCallToAction(String callToAction) { this.callToAction = callToAction; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getHashtags() { return hashtags; }
    public void setHashtags(String hashtags) { this.hashtags = hashtags; }
    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
