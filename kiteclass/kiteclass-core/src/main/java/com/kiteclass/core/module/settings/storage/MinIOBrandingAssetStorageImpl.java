package com.kiteclass.core.module.settings.storage;

import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.storage.BrandingStoragePaths;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * MinIO / S3-compatible storage for tenant branding assets (logo, favicon).
 *
 * <p>Concrete implementation of {@link BrandingAssetStorage} wired against the
 * {@link S3Client} + {@link S3Presigner} beans configured by {@code S3Config}.
 * MinIO-compatible (path-style access, custom endpoint).
 *
 * <p><b>Key layout</b> (per {@link BrandingStoragePaths} ADR-005):
 * {@code static/{tenantId}/{type}/{sanitized-filename}} inside the
 * {@code kite-branding-assets} bucket.
 *
 * <p><b>Renderable URL strategy:</b> returns a presigned GET URL so the asset
 * can be rendered without making the bucket public. TTL is capped at 7 days
 * (S3 SigV4 presigned-URL maximum). The FE persists the returned URL in
 * {@code branding.logoUrl} / {@code branding.faviconUrl}. Because that signature
 * expires after the TTL, {@code BrandingServiceImpl.getBranding()} re-derives the
 * object key from the stored URL and calls {@link #renderableUrl} on every READ
 * (GAP-1072) so the FE always receives a live URL. A future enhancement may serve assets through a
 * public CDN path; this strategy keeps Phase 1 BETA simple + private-bucket
 * safe.
 *
 * <p><b>Strategy pattern</b> per {@code design-patterns.md} §1.1 — interface
 * allows swapping (AWS S3 native / local FS for dev / mock for tests).
 *
 * @since GAP-804
 */
@Slf4j
@Service
public class MinIOBrandingAssetStorageImpl implements BrandingAssetStorage {

    /** Presigned GET URL TTL — capped at S3 SigV4 maximum of 7 days. */
    static final Duration RENDER_URL_TTL = Duration.ofDays(7);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public MinIOBrandingAssetStorageImpl(
            S3Client s3Client,
            @Qualifier("brandingPresigner") S3Presigner s3Presigner,
            @Value("${storage.branding.bucket:" + BrandingStoragePaths.BUCKET + "}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    /**
     * Ensure the branding bucket exists at startup (GAP-1036).
     *
     * <p>On a fresh MinIO/S3 instance the {@code kite-branding-assets} bucket may
     * not exist yet — the first logo {@link #store} call would then fail with
     * {@code NoSuchBucket} (HTTP 500 to the user). Creating it eagerly on bean
     * init makes the upload path work out-of-the-box.
     *
     * <p>Best-effort: never throws. If MinIO is unreachable at boot (e.g. infra
     * still starting), a WARN is logged and the bucket is retried lazily by
     * whatever storage call hits it next — startup must not crash on this.
     */
    @PostConstruct
    void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.debug("Branding bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            createBucket();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                createBucket();
            } else {
                log.warn("Could not verify branding bucket '{}' ({}). Logo upload may fail until it exists.",
                        bucket, e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Branding bucket check failed for '{}': {}. Logo upload may fail until it exists.",
                    bucket, e.getMessage());
        }
    }

    private void createBucket() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created branding bucket '{}' (GAP-1036 ensure-bucket)", bucket);
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            log.debug("Branding bucket '{}' already exists (created concurrently)", bucket);
        } catch (Exception e) {
            log.warn("Failed to create branding bucket '{}': {}", bucket, e.getMessage());
        }
    }

    @Override
    public String store(UUID tenantId, ResourceType type, String filename,
                        String contentType, byte[] content) {
        if (tenantId == null) {
            throw new ValidationException("BRANDING_ASSET_TENANT_REQUIRED", new Object[0]);
        }
        if (type == null) {
            throw new ValidationException("BRANDING_ASSET_TYPE_REQUIRED", new Object[0]);
        }
        if (filename == null || filename.isBlank()) {
            throw new ValidationException("BRANDING_ASSET_FILENAME_REQUIRED", new Object[0]);
        }
        if (content == null || content.length == 0) {
            throw new ValidationException("BRANDING_ASSET_CONTENT_REQUIRED", new Object[0]);
        }

        String safeName = sanitize(filename);
        String objectKey = BrandingStoragePaths.staticPath(tenantId, type, safeName);

        PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentLength((long) content.length);
        if (contentType != null && !contentType.isBlank()) {
            putBuilder.contentType(contentType);
        }
        s3Client.putObject(putBuilder.build(), RequestBody.fromBytes(content));

        String url = presignGet(objectKey);

        log.info("Stored branding asset tenant={} type={} key={} size={}B",
                tenantId, type, objectKey, content.length);
        return url;
    }

    @Override
    public String renderableUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ValidationException("BRANDING_ASSET_KEY_REQUIRED", new Object[0]);
        }
        return presignGet(objectKey);
    }

    /**
     * Presign a GET request for {@code objectKey} with the standard
     * {@link #RENDER_URL_TTL}. Shared by {@link #store} (post-upload) and
     * {@link #renderableUrl} (on-read regeneration) so both produce identical
     * URL shape from the same bucket + presigner.
     */
    private String presignGet(String objectKey) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(RENDER_URL_TTL)
                .getObjectRequest(get)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
        return presigned.url().toString();
    }

    /**
     * Strip path-traversal characters while preserving the rest of the
     * filename (including Vietnamese diacritics) per
     * {@code vn-localization-audit-checklist.md} §5 — no aggressive ASCII-only
     * filter that would mangle VN file names.
     */
    private static String sanitize(String filename) {
        return filename
                .replace("..", "_")
                .replace("/", "_")
                .replace("\\", "_");
    }
}
