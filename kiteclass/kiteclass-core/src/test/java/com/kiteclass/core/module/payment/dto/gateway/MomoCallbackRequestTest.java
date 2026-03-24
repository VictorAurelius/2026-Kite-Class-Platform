package com.kiteclass.core.module.payment.dto.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MomoCallbackRequest}.
 *
 * @since 1.1.0
 */
class MomoCallbackRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserialize_shouldMapKnownFields() throws Exception {
        // Given
        String json = """
                {
                    "partnerCode": "MOMO",
                    "orderId": "order-123",
                    "requestId": "req-456",
                    "amount": "50000",
                    "resultCode": "0",
                    "signature": "abc123"
                }
                """;

        // When
        MomoCallbackRequest request = objectMapper.readValue(json, MomoCallbackRequest.class);

        // Then
        assertThat(request.getPartnerCode()).isEqualTo("MOMO");
        assertThat(request.getOrderId()).isEqualTo("order-123");
        assertThat(request.getRequestId()).isEqualTo("req-456");
        assertThat(request.getAmount()).isEqualTo("50000");
        assertThat(request.getResultCode()).isEqualTo("0");
        assertThat(request.getSignature()).isEqualTo("abc123");
    }

    @Test
    void deserialize_shouldCaptureUnknownFieldsInExtraParams() throws Exception {
        // Given
        String json = """
                {
                    "partnerCode": "MOMO",
                    "orderId": "order-123",
                    "customField": "customValue"
                }
                """;

        // When
        MomoCallbackRequest request = objectMapper.readValue(json, MomoCallbackRequest.class);

        // Then
        assertThat(request.getExtraParams()).containsEntry("customField", "customValue");
    }

    @Test
    void toMap_shouldContainAllFields() {
        // Given
        MomoCallbackRequest request = new MomoCallbackRequest();
        request.setPartnerCode("MOMO");
        request.setOrderId("order-123");
        request.setAmount("50000");
        request.setResultCode("0");
        request.setSignature("sig");

        // When
        Map<String, String> map = request.toMap();

        // Then
        assertThat(map).containsEntry("partnerCode", "MOMO");
        assertThat(map).containsEntry("orderId", "order-123");
        assertThat(map).containsEntry("amount", "50000");
        assertThat(map).containsEntry("resultCode", "0");
        assertThat(map).containsEntry("signature", "sig");
    }

    @Test
    void toMap_shouldIncludeExtraParams() {
        // Given
        MomoCallbackRequest request = new MomoCallbackRequest();
        request.setPartnerCode("MOMO");
        request.setExtraParam("customField", "customValue");

        // When
        Map<String, String> map = request.toMap();

        // Then
        assertThat(map).containsEntry("partnerCode", "MOMO");
        assertThat(map).containsEntry("customField", "customValue");
    }

    @Test
    void toMap_shouldSkipNullFields() {
        // Given
        MomoCallbackRequest request = new MomoCallbackRequest();
        request.setPartnerCode("MOMO");
        // orderId is null

        // When
        Map<String, String> map = request.toMap();

        // Then
        assertThat(map).containsEntry("partnerCode", "MOMO");
        assertThat(map).doesNotContainKey("orderId");
    }
}
