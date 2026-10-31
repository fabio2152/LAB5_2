package com.lab5.marketing.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.lab5.marketing.config.AppProperties;

/**
 * Arma el reel (MP4 vertical 1080x1920, H.264 + AAC) con FFmpeg a partir de 3 cuadros,
 * con transiciones entre ellos y una pista de audio silenciosa (Instagram la requiere).
 */
@Component
public class VideoRenderer {

    private static final double SECONDS_PER_FRAME = 3.5;
    private static final double TRANSITION = 0.5;
    private static final String[] TRANSITIONS = {"fade", "slideleft", "circleopen", "smoothup"};

    private final String ffmpeg;

    public VideoRenderer(AppProperties props) {
        this.ffmpeg = props.ffmpegPath();
    }

    public void render(List<Path> frames, Path output) {
        int n = frames.size();
        double total = n * SECONDS_PER_FRAME - (n - 1) * TRANSITION;

        List<String> cmd = new ArrayList<>(List.of(ffmpeg, "-y", "-loglevel", "error"));
        for (Path frame : frames) {
            cmd.addAll(List.of("-loop", "1", "-t", String.valueOf(SECONDS_PER_FRAME), "-i", frame.toString()));
        }
        cmd.addAll(List.of("-f", "lavfi", "-t", String.valueOf(total), "-i", "anullsrc=channel_layout=stereo:sample_rate=44100"));

        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < n; i++) {
            filter.append("[%d:v]scale=1080:1920,setsar=1,fps=30,format=yuv420p[v%d];".formatted(i, i));
        }
        String last = "v0";
        for (int i = 1; i < n; i++) {
            double offset = i * (SECONDS_PER_FRAME - TRANSITION);
            String out = i == n - 1 ? "vout" : "x" + i;
            filter.append("[%s][v%d]xfade=transition=%s:duration=%s:offset=%s[%s];"
                    .formatted(last, i, TRANSITIONS[(i - 1) % TRANSITIONS.length], TRANSITION, offset, out));
            last = out;
        }
        if (n == 1) {
            filter.append("[v0]null[vout];");
        }
        filter.setLength(filter.length() - 1);

        cmd.addAll(List.of(
                "-filter_complex", filter.toString(),
                "-map", "[vout]", "-map", n + ":a",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "128k",
                "-shortest", "-movflags", "+faststart",
                output.toString()));

        run(cmd);
    }

    private void run(List<String> cmd) {
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(3, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg tardó demasiado en generar el video");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("FFmpeg falló: " + log);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo ejecutar FFmpeg (" + ffmpeg + "). ¿Está instalado?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Generación de video interrumpida", e);
        }
    }
}
