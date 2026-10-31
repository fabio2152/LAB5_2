package com.lab5.marketing.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lab5.marketing.ai.ScriptGenerator;
import com.lab5.marketing.ai.ScriptProposal;
import com.lab5.marketing.config.AppProperties;
import com.lab5.marketing.domain.Asset;
import com.lab5.marketing.domain.AssetType;
import com.lab5.marketing.domain.Campaign;
import com.lab5.marketing.domain.CampaignStatus;
import com.lab5.marketing.domain.Publication;
import com.lab5.marketing.domain.Script;
import com.lab5.marketing.media.CreativeRenderer;
import com.lab5.marketing.media.ImageUtils;
import com.lab5.marketing.media.StorageService;
import com.lab5.marketing.media.VideoRenderer;
import com.lab5.marketing.repository.CampaignRepository;
import com.lab5.marketing.social.InstagramPublisher;

/**
 * Orquesta el flujo de la campaña:
 * 1) recibir foto → 2) proponer 3 guiones → 3) elegir uno → 4) generar 3 fotos + video y publicar.
 */
@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    private static final String ORIGINAL = "original.jpg";

    private final CampaignRepository repository;
    private final ScriptGenerator scriptGenerator;
    private final CreativeRenderer creativeRenderer;
    private final VideoRenderer videoRenderer;
    private final StorageService storage;
    private final InstagramPublisher instagram;
    private final AppProperties props;

    public CampaignService(CampaignRepository repository, ScriptGenerator scriptGenerator,
                           CreativeRenderer creativeRenderer, VideoRenderer videoRenderer,
                           StorageService storage, InstagramPublisher instagram, AppProperties props) {
        this.repository = repository;
        this.scriptGenerator = scriptGenerator;
        this.creativeRenderer = creativeRenderer;
        this.videoRenderer = videoRenderer;
        this.storage = storage;
        this.instagram = instagram;
        this.props = props;
    }

    public List<Campaign> recent() {
        return repository.findTop20ByOrderByCreatedAtDesc();
    }

    public Campaign get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la campaña " + id));
    }

    /** Pasos 1 y 2: recibe la foto y pide a Claude los 3 guiones. */
    @Transactional
    public Campaign create(String companyName, String description, String audience, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("Debes subir una foto del producto.");
        }
        BufferedImage image = ImageUtils.fit(ImageUtils.read(bytes(photo)), 2048);
        byte[] forClaude = ImageUtils.toJpeg(ImageUtils.fit(image, 1568), 0.85f);

        ScriptProposal proposal = scriptGenerator.propose(forClaude, companyName, description, audience);

        Campaign campaign = new Campaign();
        campaign.setCompanyName(companyName.strip());
        campaign.setProductDescription(description);
        campaign.setTargetAudience(audience);
        campaign.setProductSummary(proposal.productSummary());
        campaign.setDemoMode(scriptGenerator.isDemoMode());
        campaign.setOriginalImage(ORIGINAL);

        List<ScriptProposal.Draft> drafts = proposal.scripts() == null ? List.of() : proposal.scripts();
        for (int i = 0; i < Math.min(3, drafts.size()); i++) {
            campaign.addScript(toScript(drafts.get(i), i + 1));
        }
        if (campaign.getScripts().isEmpty()) {
            throw new IllegalStateException("No se recibieron guiones. Intenta de nuevo.");
        }

        repository.save(campaign);
        storage.saveLocal(campaign.getId(), ORIGINAL, ImageUtils.toJpeg(image, 0.92f));
        log.info("Campaña {} creada con {} guiones", campaign.getId(), campaign.getScripts().size());
        return campaign;
    }

    /** Pasos 3 y 4 (generación): marca el guion elegido y crea las 3 fotos y el video. */
    @Transactional
    public Campaign selectScript(long campaignId, long scriptId) {
        Campaign campaign = get(campaignId);
        if (campaign.getStatus() == CampaignStatus.PUBLISHED) {
            throw new IllegalStateException("La campaña ya fue publicada.");
        }
        Script chosen = campaign.getScripts().stream()
                .filter(s -> s.getId() == scriptId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Guion no encontrado"));
        campaign.getScripts().forEach(s -> s.setSelected(s == chosen));
        campaign.getAssets().clear();
        campaign.getPublications().clear();

        long id = campaign.getId();
        Path dir = storage.campaignDir(id);
        BufferedImage photo = ImageUtils.read(readFile(dir.resolve(campaign.getOriginalImage())));
        var copy = new CreativeRenderer.Copy(campaign.getCompanyName(), chosen.getHeadlines(), chosen.getHook(),
                chosen.getTitle(), chosen.getCallToAction(), chosen.getAccentColor());

        List<BufferedImage> posts = creativeRenderer.renderPosts(photo, copy);
        List<String> topTexts = List.of(chosen.getHook(), chosen.getTitle(), chosen.getCallToAction());
        List<Path> frames = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            String name = "post-" + (i + 1) + ".jpg";
            storage.saveLocal(id, name, ImageUtils.toJpeg(posts.get(i), 0.92f));
            campaign.addAsset(new Asset(AssetType.IMAGE, i + 1, storage.localUrl(id, name),
                    storage.publish(id, name, "image/jpeg")));

            BufferedImage frame = creativeRenderer.renderReelFrame(posts.get(i), topTexts.get(i), campaign.getCompanyName());
            frames.add(storage.saveLocal(id, "frame-" + (i + 1) + ".jpg", ImageUtils.toJpeg(frame, 0.92f)));
        }

        String video = "reel.mp4";
        videoRenderer.render(frames, dir.resolve(video));
        campaign.addAsset(new Asset(AssetType.VIDEO, 1, storage.localUrl(id, video),
                storage.publish(id, video, "video/mp4")));

        campaign.setStatus(CampaignStatus.ASSETS_GENERATED);
        campaign.setErrorMessage(null);
        log.info("Campaña {}: generadas {} fotos y 1 video", id, posts.size());
        return repository.save(campaign);
    }

    /** Paso 4 (despliegue): publica el carrusel de fotos y el reel en Instagram. */
    public Campaign publish(long campaignId) {
        Campaign campaign = get(campaignId);
        Script script = campaign.getSelectedScript()
                .orElseThrow(() -> new IllegalStateException("Primero elige un guion."));
        if (campaign.getVideo().isEmpty()) {
            throw new IllegalStateException("Primero genera el contenido.");
        }
        campaign.getPublications().clear();
        String caption = script.fullCaption();
        List<String> imageUrls = campaign.getImages().stream().map(Asset::getPublicUrl).toList();
        String videoUrl = campaign.getVideo().get().getPublicUrl();

        if (!instagram.isConfigured()) {
            String msg = "Modo demo: configura INSTAGRAM_ACCESS_TOKEN e INSTAGRAM_USER_ID para publicar de verdad.";
            campaign.addPublication(new Publication(Publication.Kind.CAROUSEL, Publication.Status.SIMULATED, null, null, msg));
            campaign.addPublication(new Publication(Publication.Kind.REEL, Publication.Status.SIMULATED, null, null, msg));
            return repository.save(campaign);
        }
        if (videoUrl.contains("localhost") || videoUrl.contains("127.0.0.1")) {
            throw new IllegalStateException("Instagram necesita URLs públicas: configura S3_BUCKET o PUBLIC_BASE_URL con tu dominio/IP pública.");
        }

        boolean ok = false;
        try {
            var res = instagram.publishCarousel(imageUrls, caption);
            campaign.addPublication(new Publication(Publication.Kind.CAROUSEL, Publication.Status.PUBLISHED,
                    res.mediaId(), res.permalink(), "Carrusel de 3 fotos publicado"));
            ok = true;
        } catch (RuntimeException e) {
            log.error("Falló el carrusel de la campaña {}", campaignId, e);
            campaign.addPublication(new Publication(Publication.Kind.CAROUSEL, Publication.Status.FAILED, null, null, e.getMessage()));
        }
        try {
            var res = instagram.publishReel(videoUrl, caption);
            campaign.addPublication(new Publication(Publication.Kind.REEL, Publication.Status.PUBLISHED,
                    res.mediaId(), res.permalink(), "Reel publicado"));
            ok = true;
        } catch (RuntimeException e) {
            log.error("Falló el reel de la campaña {}", campaignId, e);
            campaign.addPublication(new Publication(Publication.Kind.REEL, Publication.Status.FAILED, null, null, e.getMessage()));
        }
        campaign.setStatus(ok ? CampaignStatus.PUBLISHED : CampaignStatus.FAILED);
        return repository.save(campaign);
    }

    public boolean isInstagramConfigured() {
        return instagram.isConfigured();
    }

    public boolean isDemoMode() {
        return scriptGenerator.isDemoMode();
    }

    public boolean isS3Enabled() {
        return props.s3().enabled();
    }

    private static Script toScript(ScriptProposal.Draft d, int position) {
        Script s = new Script();
        s.setPosition(position);
        s.setTitle(d.title());
        s.setAngle(d.angle());
        s.setHook(d.hook());
        s.setVoiceover(d.voiceover());
        s.setHeadlines(d.headlines() == null ? List.of(d.title()) : d.headlines().stream().limit(3).toList());
        s.setCallToAction(d.callToAction());
        s.setCaption(d.caption());
        s.setHashtags(d.hashtags() == null ? "" : String.join(" ", d.hashtags()));
        String color = d.accentColor() == null ? "" : d.accentColor().strip();
        s.setAccentColor(color.matches("#[0-9a-fA-F]{6}") ? color : "#E4572E");
        return s;
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] readFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
