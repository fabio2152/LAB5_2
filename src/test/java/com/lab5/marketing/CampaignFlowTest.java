package com.lab5.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.lab5.marketing.domain.Campaign;
import com.lab5.marketing.domain.CampaignStatus;
import com.lab5.marketing.media.CreativeRenderer;
import com.lab5.marketing.media.ImageUtils;
import com.lab5.marketing.service.CampaignService;

/** Prueba los pasos 1-3 en modo demo (sin API key ni FFmpeg). */
@SpringBootTest
class CampaignFlowTest {

    @Autowired
    CampaignService service;

    @Autowired
    CreativeRenderer renderer;

    @Test
    void createsCampaignWithThreeScripts() {
        var photo = new MockMultipartFile("photo", "foto.jpg", "image/jpeg", ImageUtils.toJpeg(samplePhoto(), 0.9f));

        Campaign campaign = service.create("Café Andino", "Café de especialidad", "Jóvenes", photo);

        assertThat(campaign.getId()).isNotNull();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SCRIPTS_PROPOSED);
        assertThat(campaign.getScripts()).hasSize(3);
        assertThat(campaign.getScripts()).allSatisfy(s -> assertThat(s.getHeadlines()).hasSize(3));
    }

    @Test
    void rendersThreePostsAndReelFrames() {
        var copy = new CreativeRenderer.Copy("Café Andino",
                java.util.List.of("Despierta tu día", "Tostado artesanal", "Sabor de los Andes"),
                "¿Ya tomaste tu primer café?", "Mañanas andinas", "Pide el tuyo", "#8B4513");

        var posts = renderer.renderPosts(samplePhoto(), copy);

        assertThat(posts).hasSize(3);
        assertThat(posts).allSatisfy(p -> {
            assertThat(p.getWidth()).isEqualTo(CreativeRenderer.POST_W);
            assertThat(p.getHeight()).isEqualTo(CreativeRenderer.POST_H);
        });
        BufferedImage frame = renderer.renderReelFrame(posts.get(0), copy.hook(), copy.brand());
        assertThat(frame.getHeight()).isEqualTo(CreativeRenderer.REEL_H);
    }

    private static BufferedImage samplePhoto() {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x8B5A2B));
        g.fillRect(0, 0, 800, 600);
        g.setColor(Color.WHITE);
        g.fillOval(250, 150, 300, 300);
        g.dispose();
        return img;
    }
}
