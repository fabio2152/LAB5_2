package com.lab5.marketing.media;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class ImageUtils {

    private ImageUtils() {
    }

    /** Lee JPG/PNG y lo convierte a RGB (sin transparencia, como exige JPEG). */
    public static BufferedImage read(byte[] bytes) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null) {
                throw new IllegalArgumentException("Formato de imagen no soportado. Usa JPG o PNG.");
            }
            BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, src.getWidth(), src.getHeight());
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return rgb;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reduce la imagen para que su lado mayor no supere maxSide (no la agranda). */
    public static BufferedImage fit(BufferedImage src, int maxSide) {
        int longest = Math.max(src.getWidth(), src.getHeight());
        if (longest <= maxSide) {
            return src;
        }
        double scale = (double) maxSide / longest;
        return resize(src, (int) Math.round(src.getWidth() * scale), (int) Math.round(src.getHeight() * scale));
    }

    public static BufferedImage resize(BufferedImage src, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        quality(g);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    /** Dibuja la imagen cubriendo todo el rectángulo (recorta lo que sobra, como CSS object-fit: cover). */
    public static void drawCover(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
        double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int sw = (int) Math.ceil(img.getWidth() * scale);
        int sh = (int) Math.ceil(img.getHeight() * scale);
        var clip = g.getClip();
        g.clipRect(x, y, w, h);
        g.drawImage(img, x + (w - sw) / 2, y + (h - sh) / 2, sw, sh, null);
        g.setClip(clip);
    }

    /** Desenfoque barato: reduce y vuelve a ampliar con interpolación bilineal. */
    public static BufferedImage blur(BufferedImage src, int width, int height) {
        BufferedImage small = new BufferedImage(Math.max(1, width / 24), Math.max(1, height / 24), BufferedImage.TYPE_INT_RGB);
        Graphics2D gs = small.createGraphics();
        quality(gs);
        drawCover(gs, src, 0, 0, small.getWidth(), small.getHeight());
        gs.dispose();
        return resize(small, width, height);
    }

    public static byte[] toJpeg(BufferedImage img, float quality) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
            writer.dispose();
            ios.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
}
