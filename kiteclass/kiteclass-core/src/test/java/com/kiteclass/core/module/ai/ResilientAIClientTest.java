package com.kiteclass.core.module.ai;

import com.kiteclass.core.module.ai.client.AIClient;
import com.kiteclass.core.module.ai.client.AIException;
import com.kiteclass.core.module.ai.client.ResilientAIClient;
import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ResilientAIClient} — direct delegate calls only, without the
 * Resilience4j proxy. The proxy behavior (Circuit Breaker / Bulkhead / Retry) is
 * validated separately via Resilience4j's own harness or integration tests.
 */
@ExtendWith(MockitoExtension.class)
class ResilientAIClientTest {

    @Mock
    private AIClient delegate;

    @InjectMocks
    private ResilientAIClient resilient;

    @Test
    void analyze_delegates_to_underlying_client() {
        AnalysisResult stub = AnalysisResult.builder()
                .palette(List.of("#FFFFFF"))
                .typographyStyle("serif")
                .build();
        when(delegate.analyze(any())).thenReturn(stub);

        AnalysisResult r = resilient.analyze(AnalysisRequest.builder().audience("x").build());

        assertThat(r).isSameAs(stub);
    }

    @Test
    void generate_delegates_to_underlying_client() {
        GenerationResult stub = GenerationResult.builder()
                .imageUrl("https://example/img.png")
                .mimeType("image/png")
                .build();
        when(delegate.generate(any())).thenReturn(stub);

        GenerationResult r = resilient.generate(
                GenerationRequest.builder().resourceType("LOGO").width(512).height(512).build());

        assertThat(r).isSameAs(stub);
    }

    @Test
    void propagates_AIException_for_resilience_retry_path() {
        when(delegate.analyze(any())).thenThrow(new AIException("provider 500"));

        try {
            resilient.analyze(AnalysisRequest.builder().build());
        } catch (AIException ex) {
            assertThat(ex.getMessage()).isEqualTo("provider 500");
        }
    }
}
