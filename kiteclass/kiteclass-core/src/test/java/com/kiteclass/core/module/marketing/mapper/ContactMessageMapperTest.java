package com.kiteclass.core.module.marketing.mapper;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.marketing.dto.request.CreateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.response.ContactMessageResponse;
import com.kiteclass.core.module.marketing.entity.ContactMessage;
import com.kiteclass.core.testutil.ContactMessageTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContactMessageMapper}.
 *
 * @since 2.10
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
class ContactMessageMapperTest {

    @Autowired
    private ContactMessageMapper contactMessageMapper;

    @Test
    void toResponse_shouldMapAllFields() {
        // Given
        ContactMessage message = ContactMessageTestDataBuilder.createDefaultContactMessage();

        // When
        ContactMessageResponse response = contactMessageMapper.toResponse(message);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(message.getId());
        assertThat(response.getName()).isEqualTo(message.getName());
        assertThat(response.getEmail()).isEqualTo(message.getEmail());
        assertThat(response.getPhone()).isEqualTo(message.getPhone());
        assertThat(response.getSubject()).isEqualTo(message.getSubject());
        assertThat(response.getMessage()).isEqualTo(message.getMessage());
        assertThat(response.getIsRead()).isEqualTo(message.getIsRead());
        assertThat(response.getReadAt()).isEqualTo(message.getReadAt());
        assertThat(response.getReadBy()).isEqualTo(message.getReadBy());
    }

    @Test
    void toEntity_shouldMapCreateRequestCorrectly() {
        // Given
        CreateContactMessageRequest request = ContactMessageTestDataBuilder.createDefaultCreateRequest();

        // When
        ContactMessage message = contactMessageMapper.toEntity(request);

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getName()).isEqualTo(request.getName());
        assertThat(message.getEmail()).isEqualTo(request.getEmail());
        assertThat(message.getPhone()).isEqualTo(request.getPhone());
        assertThat(message.getSubject()).isEqualTo(request.getSubject());
        assertThat(message.getMessage()).isEqualTo(request.getMessage());
        // isRead defaults to false (set in entity)
        assertThat(message.getIsRead()).isFalse();
    }

    @Test
    void updateEntity_shouldIgnoreNullAndProtectedFields() {
        // Given
        ContactMessage message = ContactMessageTestDataBuilder.createDefaultContactMessage();
        String originalName = message.getName();
        String originalEmail = message.getEmail();
        Boolean originalIsRead = message.getIsRead();

        UpdateContactMessageRequest request = UpdateContactMessageRequest.builder()
                .subject("Updated Subject")    // Update
                .message("Updated message")    // Update
                .build();

        // When
        contactMessageMapper.updateEntity(message, request);

        // Then
        assertThat(message.getSubject()).isEqualTo("Updated Subject");
        assertThat(message.getMessage()).isEqualTo("Updated message");
        // Protected fields should not be updated
        assertThat(message.getName()).isEqualTo(originalName);
        assertThat(message.getEmail()).isEqualTo(originalEmail);
        assertThat(message.getIsRead()).isEqualTo(originalIsRead);
    }
}
