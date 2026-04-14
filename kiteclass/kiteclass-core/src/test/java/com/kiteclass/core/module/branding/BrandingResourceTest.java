package com.kiteclass.core.module.branding;

import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandingResourceTest {

    @Test
    void template_requires_templateId() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.BANNER)
                .category(ResourceCategory.TEMPLATE)
                .build();

        assertThatThrownBy(res::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TEMPLATE");
    }

    @Test
    void full_ai_requires_jobId() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.HERO)
                .category(ResourceCategory.FULL_AI)
                .build();

        assertThatThrownBy(res::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FULL_AI");
    }

    @Test
    void static_rejects_templateId() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.LOGO)
                .category(ResourceCategory.STATIC)
                .templateId(99L)
                .build();

        assertThatThrownBy(res::validateInvariants)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STATIC");
    }

    @Test
    void static_rejects_aiJobId() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.LOGO)
                .category(ResourceCategory.STATIC)
                .aiJobId(UUID.randomUUID())
                .build();

        assertThatThrownBy(res::validateInvariants)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void valid_static_passes() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.LOGO)
                .category(ResourceCategory.STATIC)
                .storageUrl("s3://bucket/logo.png")
                .build();

        assertThatCode(res::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    void valid_template_passes() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.BANNER)
                .category(ResourceCategory.TEMPLATE)
                .templateId(42L)
                .build();

        assertThatCode(res::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    void valid_full_ai_passes() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.HERO)
                .category(ResourceCategory.FULL_AI)
                .aiJobId(UUID.randomUUID())
                .build();

        assertThatCode(res::validateInvariants).doesNotThrowAnyException();
    }
}
