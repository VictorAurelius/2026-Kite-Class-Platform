package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.ai.client.AIClient;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Enqueues an AI generation job for the requested resource.
 *
 * <p>Uses the {@code ResilientAIClient} decorator (Primary bean from Sub-PR 3.2) — so
 * Circuit Breaker / Bulkhead / Retry / fallback all apply for free. When the underlying
 * fallback fires ({@code templateFallback=true}), this handler returns
 * {@link HandlerResult#fallback} so the caller can route to the template path.
 *
 * <p>Async enqueueing to the RabbitMQ {@code ai.generate.{tier}} queue lands in Sub-PR
 * 3.5 (AI agent workflow). Here the handler invokes the client synchronously — acceptable
 * because ResilientAIClient enforces bulkhead + timeout.
 *
 * @since 3.19.0 (Wave 3 Sub-PR 3.3)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIResourceHandler implements ResourceHandler {

    private final AIClient aiClient;

    @Override
    public ResourceCategory supports() {
        return ResourceCategory.FULL_AI;
    }

    @Override
    public HandlerResult handle(ResourceRequest request, ClassificationContext context) {
        GenerationRequest gen = GenerationRequest.builder()
                .prompt(composePrompt(request))
                .resourceType(request.getType().name())
                .width(defaultWidth(request))
                .height(defaultHeight(request))
                .build();

        GenerationResult result = aiClient.generate(gen);
        if (result.isTemplateFallback()) {
            log.warn("[ai] generate returned template fallback for type={}", request.getType());
            return HandlerResult.fallback("AI fallback triggered — routing to template");
        }
        log.debug("[ai] generated type={} url={}", request.getType(), result.getImageUrl());
        return HandlerResult.pending(ResourceCategory.FULL_AI, "ai-job-pending");
    }

    private String composePrompt(ResourceRequest request) {
        return String.format("Generate %s for %s tone",
                request.getType().name().toLowerCase(),
                request.isCustomRequested() ? "custom requested" : "template matched");
    }

    private int defaultWidth(ResourceRequest request) {
        return switch (request.getType()) {
            case LOGO, FAVICON -> 512;
            case BANNER, HERO, SOCIAL_COVER, EMAIL_HEADER -> 1920;
            case COURSE_THUMBNAIL -> 1280;
        };
    }

    private int defaultHeight(ResourceRequest request) {
        return switch (request.getType()) {
            case LOGO, FAVICON -> 512;
            case BANNER -> 600;
            case HERO -> 1080;
            case SOCIAL_COVER -> 1080;
            case EMAIL_HEADER -> 400;
            case COURSE_THUMBNAIL -> 720;
        };
    }
}
