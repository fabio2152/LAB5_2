package com.lab5.marketing.media;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Compone las 3 fotos publicitarias (1080x1350, formato vertical de Instagram) a partir de la foto
 * original y del guion elegido, y los cuadros verticales (1080x1920) que se usan para el reel.
 */
@Component
public class CreativeRenderer {

    public static final int POST_W = 1080;
    public static final int POST_H = 1350;
    public static final int REEL_W = 1080;
    public static final int REEL_H = 1920;

    private static final String FONT = pickFont();
    private static final Color DEFAULT_ACCENT = new Color(0xE4572E);

    /** Datos del guion que necesitan los diseños. */
    public record Copy(String brand, List<String> headlines, String hook, String title, String callToAction, String accentHex) {
    }

    public List<BufferedImage> renderPosts(BufferedImage photo, Copy copy) {
        Color accent = parseColor(copy.accentHex());
        return List.of(
                impact(photo, copy, headline(copy, 0), accent),
                split(photo, copy, headline(copy, 1), accent),
                frame(photo, copy, headline(copy, 2), accent));
    }

    /** Cuadro vertical para el reel: fondo desenfocado, el post al centro y un texto arriba. */
    public BufferedImage renderReelFrame(BufferedImage post, String topText, String brand) {
        BufferedImage out = new BufferedImage(REEL_W, REEL_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        ImageUtils.quality(g);
        g.drawImage(ImageUtils.blur(post, REEL_W, REEL_H), 0, 0, null);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, REEL_W, REEL_H);

        int postY = (REEL_H - POST_H) / 2;
        g.drawImage(post, 0, postY, POST_W, POST_H, null);

        TextBlock top = layout(g, topText, Font.BOLD, 72, 44, REEL_W - 140, 3);
        top.draw(g, 70, Math.max(60, (postY - top.height()) / 2), Color.WHITE, true, REEL_W - 140);

        TextBlock bottom = layout(g, brand.toUpperCase(), Font.BOLD, 44, 30, REEL_W - 140, 1);
        bottom.draw(g, 70, postY + POST_H + (postY - bottom.height()) / 2, Color.WHITE, true, REEL_W - 140);
        g.dispose();
        return out;
    }

    // ---- Diseño 1: foto completa con degradado y titular grande ----
    private BufferedImage impact(BufferedImage photo, Copy copy, String headline, Color accent) {
        BufferedImage out = canvas();
        Graphics2D g = out.createGraphics();
        ImageUtils.quality(g);
        ImageUtils.drawCover(g, photo, 0, 0, POST_W, POST_H);

        g.setPaint(new GradientPaint(0, POST_H * 0.35f, new Color(0, 0, 0, 0), 0, POST_H, new Color(0, 0, 0, 225)));
        g.fillRect(0, 0, POST_W, POST_H);

        brandTag(g, copy.brand(), 60, 60, accent);

        TextBlock title = layout(g, headline, Font.BOLD, 110, 60, POST_W - 160, 3);
        int ctaY = POST_H - 170;
        title.draw(g, 80, ctaY - 50 - title.height(), Color.WHITE, false, POST_W - 160);
        pill(g, copy.callToAction(), 80, ctaY, accent, contrast(accent), false);
        g.dispose();
        return out;
    }

    // ---- Diseño 2: foto arriba y panel de color con el titular ----
    private BufferedImage split(BufferedImage photo, Copy copy, String headline, Color accent) {
        BufferedImage out = canvas();
        Graphics2D g = out.createGraphics();
        ImageUtils.quality(g);
        int photoH = 800;
        ImageUtils.drawCover(g, photo, 0, 0, POST_W, photoH);
        g.setColor(accent);
        g.fillRect(0, photoH, POST_W, POST_H - photoH);

        brandTag(g, copy.brand(), 60, 60, new Color(0, 0, 0, 150));

        Color text = contrast(accent);
        TextBlock title = layout(g, headline, Font.BOLD, 96, 56, POST_W - 160, 2);
        int y = photoH + 60;
        title.draw(g, 80, y, text, false, POST_W - 160);
        y += title.height() + 24;

        TextBlock hook = layout(g, copy.hook(), Font.PLAIN, 44, 30, POST_W - 160, 2);
        hook.draw(g, 80, y, withAlpha(text, 220), false, POST_W - 160);

        pill(g, copy.callToAction(), 80, POST_H - 140, text, accent, false);
        g.dispose();
        return out;
    }

    // ---- Diseño 3: foto enmarcada sobre fondo desenfocado ----
    private BufferedImage frame(BufferedImage photo, Copy copy, String headline, Color accent) {
        BufferedImage out = canvas();
        Graphics2D g = out.createGraphics();
        ImageUtils.quality(g);
        g.drawImage(ImageUtils.blur(photo, POST_W, POST_H), 0, 0, null);
        g.setColor(withAlpha(accent.darker().darker(), 170));
        g.fillRect(0, 0, POST_W, POST_H);

        TextBlock title = layout(g, headline, Font.BOLD, 84, 50, POST_W - 140, 2);
        title.draw(g, 70, 70 + (190 - title.height()) / 2, Color.WHITE, true, POST_W - 140);

        int size = 820;
        int x = (POST_W - size) / 2;
        int y = 290;
        var rect = new RoundRectangle2D.Float(x, y, size, size, 48, 48);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Float(x - 14, y - 14, size + 28, size + 28, 60, 60));
        var clip = g.getClip();
        g.clip(rect);
        ImageUtils.drawCover(g, photo, x, y, size, size);
        g.setClip(clip);

        pill(g, copy.callToAction(), 0, POST_H - 215, accent, contrast(accent), true);

        TextBlock brand = layout(g, copy.brand().toUpperCase(), Font.BOLD, 34, 24, POST_W - 140, 1);
        brand.draw(g, 70, POST_H - 90, Color.WHITE, true, POST_W - 140);
        g.dispose();
        return out;
    }

    private void brandTag(Graphics2D g, String brand, int x, int y, Color bg) {
        g.setFont(new Font(FONT, Font.BOLD, 34));
        FontMetrics fm = g.getFontMetrics();
        String text = brand.toUpperCase();
        int w = fm.stringWidth(text) + 48;
        int h = 64;
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
        g.setColor(bg.getAlpha() < 255 ? Color.WHITE : contrast(bg));
        g.drawString(text, x + 24, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void pill(Graphics2D g, String text, int x, int y, Color bg, Color fg, boolean centered) {
        g.setFont(new Font(FONT, Font.BOLD, 42));
        FontMetrics fm = g.getFontMetrics();
        String label = text.toUpperCase();
        int w = fm.stringWidth(label) + 90;
        int h = 96;
        int px = centered ? (POST_W - w) / 2 : x;
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Float(px, y, w, h, h, h));
        g.setColor(withAlpha(fg, 60));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Float(px, y, w, h, h, h));
        g.setColor(fg);
        g.drawString(label, px + 45, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    // ---- texto con ajuste de línea y tamaño automático ----

    private record TextBlock(List<String> lines, Font font, int lineHeight, int ascent) {
        int height() {
            return lines.size() * lineHeight;
        }

        void draw(Graphics2D g, int x, int y, Color color, boolean centered, int width) {
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int baseline = y + ascent;
            for (String line : lines) {
                int lx = centered ? x + (width - fm.stringWidth(line)) / 2 : x;
                g.setColor(new Color(0, 0, 0, 90));
                g.drawString(line, lx + 2, baseline + 3);
                g.setColor(color);
                g.drawString(line, lx, baseline);
                baseline += lineHeight;
            }
        }
    }

    private TextBlock layout(Graphics2D g, String text, int style, int maxSize, int minSize, int maxWidth, int maxLines) {
        String clean = text == null ? "" : text.strip();
        for (int size = maxSize; ; size -= 4) {
            Font font = new Font(FONT, style, Math.max(size, minSize));
            FontMetrics fm = g.getFontMetrics(font);
            List<String> lines = wrap(clean, fm, maxWidth);
            if ((lines.size() <= maxLines && fits(lines, fm, maxWidth)) || size <= minSize) {
                if (lines.size() > maxLines) {
                    lines = new ArrayList<>(lines.subList(0, maxLines));
                    lines.set(maxLines - 1, lines.get(maxLines - 1) + "…");
                }
                return new TextBlock(lines, font, (int) (font.getSize() * 1.15), fm.getAscent());
            }
        }
    }

    private static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : Arrays.asList(text.split("\\s+"))) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static boolean fits(List<String> lines, FontMetrics fm, int maxWidth) {
        return lines.stream().allMatch(l -> fm.stringWidth(l) <= maxWidth);
    }

    // ---- utilidades ----

    private static BufferedImage canvas() {
        return new BufferedImage(POST_W, POST_H, BufferedImage.TYPE_INT_RGB);
    }

    private static String headline(Copy copy, int i) {
        List<String> h = copy.headlines();
        if (h == null || h.isEmpty()) {
            return copy.title();
        }
        return h.get(Math.min(i, h.size() - 1));
    }

    static Color parseColor(String hex) {
        if (hex != null && hex.strip().matches("#?[0-9a-fA-F]{6}")) {
            return new Color(Integer.parseInt(hex.strip().replace("#", ""), 16));
        }
        return DEFAULT_ACCENT;
    }

    private static Color contrast(Color c) {
        double luminance = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255;
        return luminance > 0.6 ? new Color(0x111111) : Color.WHITE;
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static String pickFont() {
        var available = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String candidate : List.of("DejaVu Sans", "Segoe UI", "Arial")) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }
}
