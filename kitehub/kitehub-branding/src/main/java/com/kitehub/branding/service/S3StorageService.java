package com.kitehub.branding.service;

import com.kitehub.branding.config.S3Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for S3 asset storage operations.
 * Supports mock mode for testing without real S3.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class S3StorageService {

    private final S3Config s3Config;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageService(
        S3Config s3Config,
        @Autowired(required = false) S3Client s3Client,
        @Autowired(required = false) S3Presigner s3Presigner
    ) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Upload asset to S3.
     *
     * @param inputStream File input stream
     * @param path S3 object key (path)
     * @param contentType MIME type
     * @param contentLength File size in bytes
     * @return Asset URL (CDN or S3)
     */
    public String uploadAsset(InputStream inputStream, String path, String contentType, long contentLength) {
        log.info("Uploading asset to S3: {}", path);

        if (s3Config.isMockMode()) {
            return mockUpload(path);
        }

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));

            String url = getAssetUrl(path);
            log.info("Asset uploaded successfully: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload asset to S3: {}", path, e);
            throw new RuntimeException("Failed to upload asset to S3", e);
        }
    }

    /**
     * Get asset URL (CDN or S3).
     *
     * @param path S3 object key
     * @return Asset URL
     */
    public String getAssetUrl(String path) {
        if (s3Config.isMockMode()) {
            return mockUrl(path);
        }

        // Use CDN domain if configured
        if (s3Config.getCdnDomain() != null && !s3Config.getCdnDomain().isEmpty()) {
            // Use http:// for localhost (MinIO), https:// for production CDN
            String protocol = s3Config.getCdnDomain().contains("localhost") ? "http" : "https";
            return String.format("%s://%s/%s", protocol, s3Config.getCdnDomain(), path);
        }

        // Generate presigned URL (valid for 1 hour)
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", path, e);
            // Fallback to S3 URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Config.getBucket(), s3Config.getRegion(), path);
        }
    }

    /**
     * Generate a short-lived presigned GET URL for an uploaded asset (GAP-1112 #1).
     *
     * <p>The raw object URL returned by {@link #getAssetUrl(String)} points at the
     * (private) bucket/CDN host — a browser cannot load it because the MinIO/S3
     * bucket is not public-read. For browser preview (wizard logo upload), return a
     * time-limited presigned GET URL instead, which the storage backend honours
     * without making the bucket public.</p>
     *
     * <p>Mock mode (tests / no real S3) returns the deterministic mock URL so callers
     * stay testable. If presigning fails (e.g. presigner not wired), falls back to
     * {@link #getAssetUrl(String)} so the caller still receives a usable reference.</p>
     *
     * @param path S3 object key (path)
     * @return presigned GET URL (valid ~1 hour) or a best-effort fallback URL
     */
    public String getPresignedAssetUrl(String path) {
        if (s3Config.isMockMode()) {
            return mockUrl(path);
        }
        if (s3Presigner == null) {
            log.warn("S3Presigner not available — falling back to raw asset URL for {}", path);
            return getAssetUrl(path);
        }
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL for {} — falling back to raw URL", path, e);
            return getAssetUrl(path);
        }
    }

    /**
     * Inline a MinIO/S3 image as a {@code data:} URI (GAP-1146b — banner portrait fix).
     *
     * <p>The banner is rasterised by Playwright running INSIDE the kitehub-branding
     * container. A browser-presigned URL points at {@code S3_PUBLIC_ENDPOINT}
     * (e.g. {@code http://localhost:9100}) which is the HOST port mapping — NOT
     * reachable from inside the container, so the portrait/logo {@code <img>} silently
     * fails to load and the banner renders without the teacher photo.</p>
     *
     * <p>Instead of re-presigning against the internal host (fragile — Host header binds
     * the signature), fetch the object bytes via the internal {@link S3Client}
     * ({@code S3_ENDPOINT=kite-minio:9000}, reachable in-container AND in prod) and
     * embed them as {@code data:<contentType>;base64,...}. Playwright then needs no
     * network fetch — works in every environment.</p>
     *
     * <p>Best-effort: any failure (mock mode, external URL, missing object) returns the
     * original {@code url} unchanged so the caller degrades to the prior behaviour.</p>
     *
     * @param url a presigned/stored asset URL (or null/blank/external)
     * @return a {@code data:} URI for the image, or the original url on any failure
     */
    public String inlineImageDataUri(String url) {
        if (url == null || url.isBlank() || s3Config.isMockMode() || s3Client == null) {
            return url;
        }
        String key = extractObjectKey(url);
        if (key == null) {
            return url; // external/unknown URL — leave as-is
        }
        try {
            GetObjectRequest req = GetObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(key)
                .build();
            ResponseBytes<GetObjectResponse> obj = s3Client.getObjectAsBytes(req);
            String contentType = obj.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/png";
            }
            String base64 = Base64.getEncoder().encodeToString(obj.asByteArray());
            return "data:" + contentType + ";base64," + base64;
        } catch (Exception e) {
            log.warn("inlineImageDataUri failed for key {} — keeping original URL", key, e);
            return url;
        }
    }

    /**
     * Recover the S3 object key from a stored/presigned/CDN URL. Objects live under
     * the {@code instances/} prefix, so return everything from there (query stripped).
     * Returns {@code null} when no recoverable key is found (external URLs).
     */
    private String extractObjectKey(String url) {
        String path = url;
        int q = path.indexOf('?');
        if (q > 0) {
            path = path.substring(0, q);
        }
        int idx = path.indexOf("/instances/");
        if (idx >= 0) {
            return path.substring(idx + 1); // drop leading slash → instances/...
        }
        return null;
    }

    /**
     * Delete asset from S3.
     *
     * @param path S3 object key
     */
    public void deleteAsset(String path) {
        log.info("Deleting asset from S3: {}", path);

        if (s3Config.isMockMode()) {
            log.info("Mock mode: Asset deletion simulated for {}", path);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Asset deleted successfully: {}", path);
        } catch (Exception e) {
            log.error("Failed to delete asset from S3: {}", path, e);
            throw new RuntimeException("Failed to delete asset from S3", e);
        }
    }

    /**
     * Generate S3 path for instance branding asset.
     *
     * @param instanceId Instance UUID
     * @param assetType Asset type (profile, hero, logos, etc.)
     * @param filename Filename with extension
     * @return S3 object key
     */
    public String generateAssetPath(UUID instanceId, String assetType, String filename) {
        // Add timestamp for versioning
        long timestamp = Instant.now().getEpochSecond();
        return String.format("instances/%s/branding/%s/%s_%d.%s",
            instanceId, assetType, getFileNameWithoutExtension(filename), timestamp, getFileExtension(filename));
    }

    /**
     * Mock upload for testing.
     *
     * @param path S3 object key
     * @return Mock URL
     */
    private String mockUpload(String path) {
        log.info("Mock mode: Asset upload simulated for {}", path);
        return mockUrl(path);
    }

    /**
     * Generate mock URL for testing.
     *
     * @param path S3 object key
     * @return Mock URL
     */
    private String mockUrl(String path) {
        return String.format("https://mock-cdn.kitehub.me/%s", path);
    }

    /**
     * Extract filename without extension.
     *
     * @param filename Filename
     * @return Filename without extension
     */
    private String getFileNameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }

    /**
     * Extract file extension.
     *
     * @param filename Filename
     * @return File extension
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}
