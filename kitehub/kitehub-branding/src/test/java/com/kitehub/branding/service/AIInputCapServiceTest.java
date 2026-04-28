package com.kitehub.branding.service;

import com.kitehub.branding.config.AIInputCapConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AIInputCapService")
class AIInputCapServiceTest {

    private AIInputCapConfig config;
    private MeterRegistry registry;
    private AIInputCapService service;

    @BeforeEach
    void setUp() {
        config = new AIInputCapConfig();
        // Defaults: FREE 2000, BASIC 4000, PREMIUM 8000, ENTERPRISE 16000.
        registry = new SimpleMeterRegistry();
        service = new AIInputCapService(config, registry);
    }

    @Test
    @DisplayName("under cap → null (allow)")
    void underCapAllows() {
        // FREE cap is 2000 tokens (~8000 chars).
        assertThat(service.checkInputSize("FREE", "x".repeat(100))).isNull();
    }

    @Test
    @DisplayName("over cap → 400 with structured error body")
    void overCapRejects() {
        // FREE cap = 2000 tokens, send 8001 chars (~2001 tokens) → reject.
        String oversize = "x".repeat(8001);

        ResponseEntity<Object> resp = service.checkInputSize("FREE", oversize);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("AI_INPUT_TOO_LONG");
        assertThat(body.get("maxTokens")).isEqualTo(2000);
        assertThat(body.get("estimatedTokens")).isEqualTo(2001);
        assertThat(body.get("tier")).isEqualTo("FREE");
    }

    @Test
    @DisplayName("rejection emits ai.input.token.rejection counter tagged with tier")
    void counterEmittedOnRejection() {
        service.checkInputSize("FREE", "x".repeat(8001));

        double count = registry.counter("ai.input.token.rejection", "tier", "FREE").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("ENTERPRISE with -1 cap is unlimited")
    void enterpriseUnlimited() {
        config.setEnterpriseMaxTokens(-1);
        AIInputCapService unlimited = new AIInputCapService(config, registry);

        // 1M chars (~250k tokens) — far above any other tier cap.
        assertThat(unlimited.checkInputSize("ENTERPRISE", "x".repeat(1_000_000))).isNull();
    }

    @Test
    @DisplayName("unknown tier defaults to FREE cap (fail-safe)")
    void unknownTierDefaultsFree() {
        ResponseEntity<Object> resp = service.checkInputSize("MYSTERY", "x".repeat(8001));

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("multiple input fields are summed")
    void multipleInputsSummed() {
        // FREE cap 2000. Three fields × 4001 chars each = ~3003 tokens > 2000 → reject.
        ResponseEntity<Object> resp = service.checkInputSize(
                "FREE", "x".repeat(4001), "y".repeat(4001), "z".repeat(4001));
        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("PREMIUM tier has higher cap than FREE")
    void premiumHigherThanFree() {
        // 12000 chars = 3000 tokens. FREE caps at 2000 (reject), PREMIUM at 8000 (allow).
        String input = "x".repeat(12000);

        assertThat(service.checkInputSize("FREE", input)).isNotNull();
        assertThat(service.checkInputSize("PREMIUM", input)).isNull();
    }
}
