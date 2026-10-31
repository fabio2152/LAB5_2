package com.lab5.marketing.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lab5.marketing.config.AppProperties;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Guarda los archivos de cada campaña en disco (volumen Docker) y, si S3_BUCKET está configurado,
 * los sube a S3 para tener una URL pública HTTPS que Instagram pueda descargar.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path root;
    private final AppProperties props;
    private final S3Client s3;

    public StorageService(AppProperties props) {
        this.props = props;
        this.root = Path.of(props.storageDir()).toAbsolutePath();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.s3 = props.s3().enabled()
                ? S3Client.builder().region(Region.of(props.s3().region())).build()
                : null;
    }

    /** Carpeta local de una campaña (se crea si no existe). */
    public Path campaignDir(long campaignId) {
        try {
            return Files.createDirectories(root.resolve(String.valueOf(campaignId)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path saveLocal(long campaignId, String fileName, byte[] bytes) {
        try {
            return Files.write(campaignDir(campaignId).resolve(fileName), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Ruta que usa la interfaz web para mostrar el archivo. */
    public String localUrl(long campaignId, String fileName) {
        return "/media/" + campaignId + "/" + fileName;
    }

    /** Publica un archivo ya guardado y devuelve su URL pública. */
    public String publish(long campaignId, String fileName, String contentType) {
        if (s3 == null) {
            return stripSlash(props.publicBaseUrl()) + localUrl(campaignId, fileName);
        }
        String key = "campaigns/" + campaignId + "/" + fileName;
        s3.putObject(PutObjectRequest.builder()
                        .bucket(props.s3().bucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(campaignDir(campaignId).resolve(fileName)));
        log.info("Subido a s3://{}/{}", props.s3().bucket(), key);

        String base = props.s3().publicUrl();
        if (base == null || base.isBlank()) {
            base = "https://" + props.s3().bucket() + ".s3." + props.s3().region() + ".amazonaws.com";
        }
        return stripSlash(base) + "/" + key;
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
