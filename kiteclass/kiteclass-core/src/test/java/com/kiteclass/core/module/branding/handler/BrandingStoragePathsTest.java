package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.storage.BrandingStoragePaths;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BrandingStoragePathsTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID JOB = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test
    void static_path_lowercases_type_and_joins_filename() {
        String path = BrandingStoragePaths.staticPath(TENANT, ResourceType.LOGO, "school.png");

        assertThat(path).isEqualTo(
                "static/00000000-0000-0000-0000-000000000001/logo/school.png");
    }

    @Test
    void template_path_appends_hash_and_png() {
        String path = BrandingStoragePaths.templatePath(TENANT, ResourceType.BANNER, "abc123");

        assertThat(path).isEqualTo(
                "templates/00000000-0000-0000-0000-000000000001/banner/abc123.png");
    }

    @Test
    void ai_generated_path_uses_jobid() {
        String path = BrandingStoragePaths.aiGeneratedPath(TENANT, JOB);

        assertThat(path).isEqualTo(
                "ai-generated/00000000-0000-0000-0000-000000000001/"
                        + "00000000-0000-0000-0000-000000000099.png");
    }

    @Test
    void bucket_name_is_canonical() {
        assertThat(BrandingStoragePaths.BUCKET).isEqualTo("kite-branding-assets");
    }
}
