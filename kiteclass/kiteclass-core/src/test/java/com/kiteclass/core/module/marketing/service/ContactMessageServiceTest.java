package com.kiteclass.core.module.marketing.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.service.email.EmailService;
import com.kiteclass.core.module.marketing.dto.request.CreateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.response.ContactMessageResponse;
import com.kiteclass.core.module.marketing.entity.ContactMessage;
import com.kiteclass.core.module.marketing.mapper.ContactMessageMapper;
import com.kiteclass.core.module.marketing.repository.ContactMessageRepository;
import com.kiteclass.core.module.marketing.service.impl.ContactMessageServiceImpl;
import com.kiteclass.core.testutil.ContactMessageTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContactMessageServiceImpl}.
 *
 * @since 2.10
 */
@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @Mock
    private ContactMessageMapper contactMessageMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ContactMessageServiceImpl contactMessageService;

    private ContactMessage contactMessage;
    private ContactMessageResponse contactMessageResponse;
    private CreateContactMessageRequest createRequest;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        ReflectionTestUtils.setField(contactMessageService, "adminEmail", "admin@kitehub.me");

        contactMessage = ContactMessageTestDataBuilder.createDefaultContactMessage();
        contactMessage.setInstanceId(tenantId);
        contactMessageResponse = ContactMessageResponse.builder()
                .id(contactMessage.getId())
                .name(contactMessage.getName())
                .email(contactMessage.getEmail())
                .subject(contactMessage.getSubject())
                .message(contactMessage.getMessage())
                .isRead(contactMessage.getIsRead())
                .build();
        createRequest = ContactMessageTestDataBuilder.createDefaultCreateRequest();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createContactMessage_shouldCreateSuccessfully() {
        // Given
        when(contactMessageMapper.toEntity(any(CreateContactMessageRequest.class))).thenReturn(contactMessage);
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(contactMessage);
        when(contactMessageMapper.toResponse(any(ContactMessage.class))).thenReturn(contactMessageResponse);

        // When
        ContactMessageResponse result = contactMessageService.createContactMessage(createRequest, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(contactMessage.getName());
        verify(contactMessageRepository).save(any(ContactMessage.class));
    }

    @Test
    void createContactMessage_shouldDefaultSubjectWhenBlank() {
        // Given — GAP-1221: form public VN cho phép bỏ trống subject; server tự sinh default
        createRequest.setSubject(null);
        when(contactMessageMapper.toEntity(any(CreateContactMessageRequest.class))).thenReturn(contactMessage);
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(contactMessage);
        when(contactMessageMapper.toResponse(any(ContactMessage.class))).thenReturn(contactMessageResponse);

        // When
        contactMessageService.createContactMessage(createRequest, tenantId);

        // Then — subject defaulted server-side trước khi map entity + dùng cho email notify
        assertThat(createRequest.getSubject()).isEqualTo("Liên hệ từ " + createRequest.getName());
        verify(emailService).sendContactNotification(
                anyString(),
                eq(createRequest.getName()),
                anyString(),
                eq("Liên hệ từ " + createRequest.getName()),
                eq(createRequest.getMessage())
        );
    }

    @Test
    void createContactMessage_shouldAcceptMissingEmail() {
        // Given — GAP-1221: phụ huynh VN để SĐT, email optional
        createRequest.setEmail(null);
        when(contactMessageMapper.toEntity(any(CreateContactMessageRequest.class))).thenReturn(contactMessage);
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(contactMessage);
        when(contactMessageMapper.toResponse(any(ContactMessage.class))).thenReturn(contactMessageResponse);

        // When
        ContactMessageResponse result = contactMessageService.createContactMessage(createRequest, tenantId);

        // Then — vẫn tạo thành công; email notify nhận placeholder thay vì null
        assertThat(result).isNotNull();
        verify(contactMessageRepository).save(any(ContactMessage.class));
        verify(emailService).sendContactNotification(
                anyString(),
                eq(createRequest.getName()),
                eq("(không cung cấp email)"),
                anyString(),
                eq(createRequest.getMessage())
        );
    }

    @Test
    void markAsRead_shouldUpdateReadStatus() {
        // Given
        String readBy = "admin@example.com";
        when(contactMessageRepository.findByIdAndInstanceIdAndDeletedFalse(anyLong(), any())).thenReturn(Optional.of(contactMessage));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(contactMessage);
        when(contactMessageMapper.toResponse(any(ContactMessage.class))).thenReturn(contactMessageResponse);

        // When
        ContactMessageResponse result = contactMessageService.markAsRead(1L, readBy);

        // Then
        assertThat(result).isNotNull();
        assertThat(contactMessage.getIsRead()).isTrue();
        assertThat(contactMessage.getReadBy()).isEqualTo(readBy);
        assertThat(contactMessage.getReadAt()).isNotNull();
        verify(contactMessageRepository).save(contactMessage);
    }

    @Test
    void deleteContactMessage_shouldSoftDelete() {
        // Given
        when(contactMessageRepository.findByIdAndInstanceIdAndDeletedFalse(anyLong(), any())).thenReturn(Optional.of(contactMessage));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(contactMessage);

        // When
        contactMessageService.deleteContactMessage(1L);

        // Then
        verify(contactMessageRepository).save(contactMessage);
        assertThat(contactMessage.getDeleted()).isTrue();
    }

    @Test
    void createContactMessage_shouldUseConfiguredAdminEmail() {
        // Given
        String customAdminEmail = "teacher@myschool.com";
        ReflectionTestUtils.setField(contactMessageService, "adminEmail", customAdminEmail);

        when(contactMessageMapper.toEntity(any(CreateContactMessageRequest.class))).thenReturn(contactMessage);
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(contactMessage);
        when(contactMessageMapper.toResponse(any(ContactMessage.class))).thenReturn(contactMessageResponse);

        // When
        contactMessageService.createContactMessage(createRequest, tenantId);

        // Then - verify email is sent to the configured admin email
        verify(emailService).sendContactNotification(
                eq(customAdminEmail),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void countUnread_shouldReturnCount() {
        // Given
        when(contactMessageRepository.countUnreadByInstanceId(any())).thenReturn(5L);

        // When
        long count = contactMessageService.countUnread(tenantId);

        // Then
        assertThat(count).isEqualTo(5L);
        verify(contactMessageRepository).countUnreadByInstanceId(tenantId);
    }
}
