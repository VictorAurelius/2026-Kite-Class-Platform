package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.settings.dto.request.UpdateUserPreferencesRequest;
import com.kiteclass.core.module.settings.dto.response.UserPreferencesResponse;
import com.kiteclass.core.module.settings.entity.UserPreferences;
import com.kiteclass.core.module.settings.enums.Language;
import com.kiteclass.core.module.settings.enums.Theme;
import com.kiteclass.core.module.settings.mapper.UserPreferencesMapper;
import com.kiteclass.core.module.settings.repository.UserPreferencesRepository;
import com.kiteclass.core.testutil.UserPreferencesTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserPreferencesService.
 *
 * @since 2.9
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferencesService Tests")
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserPreferencesMapper userPreferencesMapper;

    @InjectMocks
    private UserPreferencesServiceImpl userPreferencesService;

    private UUID testInstanceId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testInstanceId = UUID.randomUUID();
        testUserId = 1L;
        TenantContext.setCurrentTenant(testInstanceId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should get existing user preferences")
    void shouldGetExistingUserPreferences() {
        // Given
        UserPreferences preferences = UserPreferencesTestDataBuilder
                .createDefaultPreferences(testInstanceId, testUserId);
        UserPreferencesResponse expectedResponse = UserPreferencesResponse.builder()
                .id(1L)
                .userId(testUserId)
                .language("vi")
                .theme("light")
                .build();

        when(userPreferencesRepository.findByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(Optional.of(preferences));
        when(userPreferencesMapper.toResponse(preferences)).thenReturn(expectedResponse);

        // When
        UserPreferencesResponse result = userPreferencesService.getUserPreferences(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getLanguage()).isEqualTo("vi");
        assertThat(result.getTheme()).isEqualTo("light");
        verify(userPreferencesRepository).findByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId);
    }

    @Test
    @DisplayName("Should return default preferences when not exists")
    void shouldReturnDefaultPreferencesWhenNotExists() {
        // Given
        UserPreferencesResponse expectedResponse = UserPreferencesResponse.builder()
                .userId(testUserId)
                .language("vi")
                .theme("light")
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        when(userPreferencesRepository.findByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(Optional.empty());
        when(userPreferencesMapper.toResponse(any(UserPreferences.class))).thenReturn(expectedResponse);

        // When
        UserPreferencesResponse result = userPreferencesService.getUserPreferences(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getLanguage()).isEqualTo("vi");
        assertThat(result.getTheme()).isEqualTo("light");
    }

    @Test
    @DisplayName("Should update existing preferences")
    void shouldUpdateExistingPreferences() {
        // Given
        UserPreferences existingPreferences = UserPreferencesTestDataBuilder
                .createDefaultPreferences(testInstanceId, testUserId);

        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .language("en")
                .theme("dark")
                .timezone("America/New_York")
                .build();

        UserPreferencesResponse expectedResponse = UserPreferencesResponse.builder()
                .userId(testUserId)
                .language("en")
                .theme("dark")
                .timezone("America/New_York")
                .build();

        when(userPreferencesRepository.findByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(Optional.of(existingPreferences));
        when(userPreferencesRepository.save(existingPreferences)).thenReturn(existingPreferences);
        when(userPreferencesMapper.toResponse(existingPreferences)).thenReturn(expectedResponse);

        // When
        UserPreferencesResponse result = userPreferencesService.updateUserPreferences(testUserId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getTheme()).isEqualTo("dark");
        verify(userPreferencesMapper).updateFromRequest(request, existingPreferences);
        verify(userPreferencesRepository).save(existingPreferences);
    }

    @Test
    @DisplayName("Should create new preferences when updating non-existent")
    void shouldCreateNewPreferencesWhenUpdatingNonExistent() {
        // Given
        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .language("en")
                .build();

        UserPreferences savedPreferences = UserPreferencesTestDataBuilder
                .createEnglishPreferences(testInstanceId, testUserId);

        UserPreferencesResponse expectedResponse = UserPreferencesResponse.builder()
                .userId(testUserId)
                .language("en")
                .build();

        when(userPreferencesRepository.findByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(savedPreferences);
        when(userPreferencesMapper.toResponse(savedPreferences)).thenReturn(expectedResponse);

        // When
        UserPreferencesResponse result = userPreferencesService.updateUserPreferences(testUserId, request);

        // Then
        assertThat(result).isNotNull();
        verify(userPreferencesRepository, times(2)).save(any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should initialize default preferences")
    void shouldInitializeDefaultPreferences() {
        // Given
        UserPreferences savedPreferences = UserPreferencesTestDataBuilder
                .createDefaultPreferences(testInstanceId, testUserId);

        UserPreferencesResponse expectedResponse = UserPreferencesResponse.builder()
                .userId(testUserId)
                .language("vi")
                .theme("light")
                .timezone("Asia/Ho_Chi_Minh")
                .notificationPreferences(new HashMap<>())
                .build();

        when(userPreferencesRepository.existsByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(false);
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(savedPreferences);
        when(userPreferencesMapper.toResponse(savedPreferences)).thenReturn(expectedResponse);

        // When
        UserPreferencesResponse result = userPreferencesService.initializeDefaultPreferences(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should not initialize if preferences already exist")
    void shouldNotInitializeIfPreferencesAlreadyExist() {
        // Given
        UserPreferences existingPreferences = UserPreferencesTestDataBuilder
                .createDefaultPreferences(testInstanceId, testUserId);

        UserPreferencesResponse expectedResponse = UserPreferencesResponse.builder()
                .userId(testUserId)
                .build();

        when(userPreferencesRepository.existsByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(true);
        when(userPreferencesRepository.findByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(Optional.of(existingPreferences));
        when(userPreferencesMapper.toResponse(existingPreferences)).thenReturn(expectedResponse);

        // When
        UserPreferencesResponse result = userPreferencesService.initializeDefaultPreferences(testUserId);

        // Then
        assertThat(result).isNotNull();
        verify(userPreferencesRepository, never()).save(any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should update notification preferences")
    void shouldUpdateNotificationPreferences() {
        // Given
        UserPreferences existingPreferences = UserPreferencesTestDataBuilder
                .createDefaultPreferences(testInstanceId, testUserId);

        Map<String, Boolean> newNotificationPrefs = new HashMap<>();
        newNotificationPrefs.put("email", false);
        newNotificationPrefs.put("push", true);
        newNotificationPrefs.put("sms", true);

        UpdateUserPreferencesRequest request = UpdateUserPreferencesRequest.builder()
                .notificationPreferences(newNotificationPrefs)
                .build();

        UserPreferencesResponse expectedResponse = UserPreferencesResponse.builder()
                .userId(testUserId)
                .notificationPreferences(newNotificationPrefs)
                .build();

        when(userPreferencesRepository.findByInstanceIdAndUserIdAndDeletedFalse(testInstanceId, testUserId))
                .thenReturn(Optional.of(existingPreferences));
        when(userPreferencesRepository.save(existingPreferences)).thenReturn(existingPreferences);
        when(userPreferencesMapper.toResponse(existingPreferences)).thenReturn(expectedResponse);

        // When
        UserPreferencesResponse result = userPreferencesService.updateUserPreferences(testUserId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNotificationPreferences()).isEqualTo(newNotificationPrefs);
        verify(userPreferencesRepository).save(existingPreferences);
    }
}
