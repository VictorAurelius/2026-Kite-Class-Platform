package com.kiteclass.core.testutil;

import com.kiteclass.core.module.marketing.dto.request.CreateContactMessageRequest;
import com.kiteclass.core.module.marketing.entity.ContactMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * Test data builder for ContactMessage-related objects.
 *
 * @since 2.10
 */
public class ContactMessageTestDataBuilder {

    /**
     * Creates a default ContactMessage entity for testing.
     *
     * @return ContactMessage with default test data
     */
    public static ContactMessage createDefaultContactMessage() {
        ContactMessage message = new ContactMessage();
        message.setId(1L);
        message.setInstanceId(UUID.randomUUID());
        message.setName("Le Van Contact");
        message.setEmail("contact@example.com");
        message.setPhone("0923456789");
        message.setSubject("Question about courses");
        message.setMessage("I have a question about your Math course. Can you provide more details?");
        message.setIsRead(false);
        message.setReplied(false);
        message.setDeleted(false);
        return message;
    }

    /**
     * Creates a ContactMessage with custom email.
     *
     * @param email the contact email
     * @return ContactMessage with specified email
     */
    public static ContactMessage createContactMessageWithEmail(String email) {
        ContactMessage message = createDefaultContactMessage();
        message.setEmail(email);
        return message;
    }

    /**
     * Creates a ContactMessage that is already read.
     *
     * @return ContactMessage with isRead = true
     */
    public static ContactMessage createReadContactMessage() {
        ContactMessage message = createDefaultContactMessage();
        message.setIsRead(true);
        message.setReadAt(Instant.now());
        message.setReadBy("admin@example.com");
        return message;
    }

    /**
     * Creates a default CreateContactMessageRequest for testing.
     *
     * @return CreateContactMessageRequest with default test data
     */
    public static CreateContactMessageRequest createDefaultCreateRequest() {
        return CreateContactMessageRequest.builder()
                .name("Pham Thi Contact")
                .email("newcontact@example.com")
                .phone("0934567890")
                .subject("Inquiry about enrollment")
                .message("Hello, I would like to know more about your enrollment process and fees.")
                .build();
    }

    /**
     * Creates a CreateContactMessageRequest with custom subject and message.
     *
     * @param subject the message subject
     * @param message the message content
     * @return CreateContactMessageRequest with specified fields
     */
    public static CreateContactMessageRequest createRequestWithSubjectAndMessage(String subject, String message) {
        return CreateContactMessageRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .subject(subject)
                .message(message)
                .build();
    }
}
