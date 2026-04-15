package com.kiteclass.core.module.ai.workflow;

import com.kiteclass.core.module.ai.client.AIClient;
import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Layer 1 of the agent orchestration (ADR-006): extracts brand signals from tenant input.
 *
 * <p>Delegates to {@link AIClient} (auto-wired {@code resilientAIClient} bean — Circuit
 * Breaker + Bulkhead + Retry already applied). On fallback (templateOnly=true) the
 * downstream {@link PlannerService} still produces a plan but biases toward TEMPLATE
 * routing.
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, ADR-006)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzerService {

    private final AIClient aiClient;

    public AnalysisResult analyze(AnalysisRequest request) {
        log.debug("[analyzer] audience={} tone={}", request.getAudience(), request.getTone());
        return aiClient.analyze(request);
    }
}
