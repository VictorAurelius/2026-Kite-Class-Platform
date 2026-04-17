package com.kiteclass.core.testutil;

import com.kiteclass.core.module.marketing.dto.request.CreateLeadRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateLeadRequest;
import com.kiteclass.core.module.marketing.entity.Lead;
import com.kiteclass.core.module.marketing.enums.LeadSource;
import com.kiteclass.core.module.marketing.enums.LeadStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Test data builder for Lead-related objects.
 *
 * @since 2.10
 */
public class LeadTestDataBuilder {

    /**
     * Creates a default Lead entity for testing.
     *
     * @return Lead with default test data
     */
    public static Lead createDefaultLead() {
        Lead lead = new Lead();
        lead.setId(1L);
        lead.setInstanceId(UUID.randomUUID());
        lead.setEmail("lead@example.com");
        lead.setName("Nguyen Van Lead");
        lead.setPhone("0901234567");
        lead.setSource(LeadSource.LANDING_PAGE);
        lead.setStatus(LeadStatus.NEW);
        lead.setMessage("I want to try your course");
        lead.setRegistrationDate(Instant.now());
        lead.setDeleted(false);
        return lead;
    }

    /**
     * Creates a Lead with custom email.
     *
     * @param email the lead email
     * @return Lead with specified email
     */
    public static Lead createLeadWithEmail(String email) {
        Lead lead = createDefaultLead();
        lead.setEmail(email);
        return lead;
    }

    /**
     * Creates a Lead with custom status.
     *
     * @param status the lead status
     * @return Lead with specified status
     */
    public static Lead createLeadWithStatus(LeadStatus status) {
        Lead lead = createDefaultLead();
        lead.setStatus(status);
        return lead;
    }

    /**
     * Creates a Lead with custom source.
     *
     * @param source the lead source
     * @return Lead with specified source
     */
    public static Lead createLeadWithSource(LeadSource source) {
        Lead lead = createDefaultLead();
        lead.setSource(source);
        return lead;
    }

    /**
     * Creates a default CreateLeadRequest for testing.
     *
     * @return CreateLeadRequest with default test data
     */
    public static CreateLeadRequest createDefaultCreateRequest() {
        return CreateLeadRequest.builder()
                .email("newlead@example.com")
                .name("Tran Thi Lead")
                .phone("0912345678")
                .source(LeadSource.LANDING_PAGE)
                .message("Interested in English course")
                .build();
    }

    /**
     * Creates a CreateLeadRequest with custom email.
     *
     * @param email the lead email
     * @return CreateLeadRequest with specified email
     */
    public static CreateLeadRequest createRequestWithEmail(String email) {
        return CreateLeadRequest.builder()
                .email(email)
                .name("Test Lead")
                .phone("0123456789")
                .source(LeadSource.LANDING_PAGE)
                .build();
    }

    /**
     * Creates a default UpdateLeadRequest for testing.
     *
     * @return UpdateLeadRequest with default test data
     */
    public static UpdateLeadRequest createDefaultUpdateRequest() {
        return UpdateLeadRequest.builder()
                .name("Updated Lead Name")
                .phone("0999999999")
                .message("Updated message")
                .build();
    }
}
