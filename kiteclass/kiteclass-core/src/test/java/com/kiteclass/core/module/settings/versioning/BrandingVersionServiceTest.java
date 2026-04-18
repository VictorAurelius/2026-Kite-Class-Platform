package com.kiteclass.core.module.settings.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.entity.BrandingVersion;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.module.settings.repository.BrandingVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandingVersionService — snapshot + rollback")
class BrandingVersionServiceTest {

    @Mock
    private BrandingVersionRepository versionRepository;

    @Mock
    private BrandingRepository brandingRepository;

    private BrandingVersionServiceImpl service;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        service = new BrandingVersionServiceImpl(
                versionRepository,
                brandingRepository,
                new ObjectMapper(),
                null);
    }

    @Test
    @DisplayName("snapshot creates a new active version and deactivates previous active one")
    void snapshot_deactivatesPreviousActive() {
        BrandingVersion previous = BrandingVersion.builder()
                .versionNumber(3)
                .active(true)
                .snapshotJson("{}")
                .build();
        previous.setInstanceId(instanceId);
        previous.setId(300L);

        when(versionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.of(previous));
        when(versionRepository.maxVersionNumber(instanceId)).thenReturn(3);
        when(versionRepository.save(any(BrandingVersion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Branding current = new Branding();
        current.setInstanceId(instanceId);
        current.setDisplayName("ABC");
        current.setPrimaryColor("#112233");
        current.setSecondaryColor("#445566");
        current.setAccentColor("#778899");

        BrandingVersion saved = service.snapshot(current, null);

        assertThat(previous.getActive()).isFalse();
        assertThat(saved.getVersionNumber()).isEqualTo(4);
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getInstanceId()).isEqualTo(instanceId);

        verify(versionRepository, times(2)).save(any(BrandingVersion.class));
    }

    @Test
    @DisplayName("rollback restores snapshot to Branding entity and creates a new version")
    void rollback_restoresBrandingFields() {
        // Snapshot captured when the brand was red.
        String snapshotJson = """
                {
                  "displayName":"Old Name",
                  "primaryColor":"#FF0000",
                  "secondaryColor":"#AA0000",
                  "accentColor":"#FFAA00"
                }
                """;
        BrandingVersion target = BrandingVersion.builder()
                .versionNumber(2)
                .active(false)
                .snapshotJson(snapshotJson)
                .build();
        target.setInstanceId(instanceId);
        target.setId(200L);

        when(versionRepository.findByInstanceIdAndVersionNumber(instanceId, 2))
                .thenReturn(Optional.of(target));

        Branding current = new Branding();
        current.setInstanceId(instanceId);
        current.setDisplayName("New Name");
        current.setPrimaryColor("#00FF00");
        current.setSecondaryColor("#00AA00");
        current.setAccentColor("#00FFAA");

        when(brandingRepository.findByInstanceIdAndDeletedFalse(instanceId))
                .thenReturn(Optional.of(current));
        when(brandingRepository.save(any(Branding.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.maxVersionNumber(instanceId)).thenReturn(5);
        when(versionRepository.findActiveByInstanceId(instanceId)).thenReturn(Optional.empty());
        when(versionRepository.save(any(BrandingVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandingVersion result = service.rollback(instanceId, 2);

        assertThat(current.getDisplayName()).isEqualTo("Old Name");
        assertThat(current.getPrimaryColor()).isEqualTo("#FF0000");
        ArgumentCaptor<BrandingVersion> captor = ArgumentCaptor.forClass(BrandingVersion.class);
        verify(versionRepository).save(captor.capture());
        assertThat(captor.getValue().getRollbackOf()).isEqualTo(200L);
        assertThat(result.getVersionNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("rollback throws when version doesn't exist")
    void rollback_whenVersionMissing_throws() {
        when(versionRepository.findByInstanceIdAndVersionNumber(eq(instanceId), eq(99)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rollback(instanceId, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
