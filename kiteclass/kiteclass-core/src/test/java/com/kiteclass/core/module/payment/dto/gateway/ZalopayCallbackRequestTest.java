package com.kiteclass.core.module.payment.dto.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZalopayCallbackRequest}.
 *
 * @since 1.1.0
 */
class ZalopayCallbackRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserialize_shouldMapKnownFields() throws Exception {
        // Given
        String json = """
                {
                    "data": "encrypted-payload",
                    "mac": "hmac-signature",
                    "type": "1",
                    "app_id": "2553",
                    "app_trans_id": "210101_abc123"
                }
                """;

        // When
        ZalopayCallbackRequest request = objectMapper.readValue(json, ZalopayCallbackRequest.class);

        // Then
        assertThat(request.getData()).isEqualTo("encrypted-payload");
        assertThat(request.getMac()).isEqualTo("hmac-signature");
        assertThat(request.getType()).isEqualTo("1");
        assertThat(request.getAppId()).isEqualTo("2553");
        assertThat(request.getAppTransId()).isEqualTo("210101_abc123");
    }

    @Test
    void deserialize_shouldCaptureUnknownFieldsInExtraParams() throws Exception {
        // Given
        String json = """
                {
                    "data": "payload",
                    "mac": "sig",
                    "unknownField": "unknownValue"
                }
                """;

        // When
        ZalopayCallbackRequest request = objectMapper.readValue(json, ZalopayCallbackRequest.class);

        // Then
        assertThat(request.getExtraParams()).containsEntry("unknownField", "unknownValue");
    }

    @Test
    void toMap_shouldContainAllFieldsWithJsonPropertyNames() {
        // Given
        ZalopayCallbackRequest request = new ZalopayCallbackRequest();
        request.setData("payload");
        request.setMac("sig");
        request.setType("1");
        request.setAppId("2553");
        request.setAppTransId("210101_abc");

        // When
        Map<String, String> map = request.toMap();

        // Then
        assertThat(map).containsEntry("data", "payload");
        assertThat(map).containsEntry("mac", "sig");
        assertThat(map).containsEntry("type", "1");
        assertThat(map).containsEntry("app_id", "2553");
        assertThat(map).containsEntry("app_trans_id", "210101_abc");
    }

    @Test
    void toMap_shouldIncludeExtraParams() {
        // Given
        ZalopayCallbackRequest request = new ZalopayCallbackRequest();
        request.setData("payload");
        request.setExtraParam("customField", "customValue");

        // When
        Map<String, String> map = request.toMap();

        // Then
        assertThat(map).containsEntry("data", "payload");
        assertThat(map).containsEntry("customField", "customValue");
    }

    @Test
    void toMap_shouldSkipNullFields() {
        // Given
        ZalopayCallbackRequest request = new ZalopayCallbackRequest();
        request.setData("payload");
        // mac is null

        // When
        Map<String, String> map = request.toMap();

        // Then
        assertThat(map).containsEntry("data", "payload");
        assertThat(map).doesNotContainKey("mac");
    }
}
