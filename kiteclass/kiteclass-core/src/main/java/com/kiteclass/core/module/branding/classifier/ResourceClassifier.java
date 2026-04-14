package com.kiteclass.core.module.branding.classifier;

import com.kiteclass.core.module.branding.entity.ResourceCategory;

import java.util.Optional;

/**
 * Chain of Responsibility link.
 *
 * <p>Each classifier returns a category if it applies, or {@code Optional.empty()} to
 * delegate to the next link. Classifiers MUST NOT perform I/O — all signals come from
 * {@link ClassificationContext}.
 *
 * <p>Implementations ordered by {@link #order()} ascending:
 * <ol>
 *   <li>StaticAssetClassifier — user uploaded a file → STATIC</li>
 *   <li>CustomAIRequestClassifier — user explicitly asked custom + has quota → FULL_AI</li>
 *   <li>TemplateMatchClassifier — template exists → TEMPLATE</li>
 *   <li>AIFallbackClassifier — quota available → FULL_AI</li>
 *   <li>DefaultTemplateClassifier — last resort → TEMPLATE</li>
 * </ol>
 *
 * @since 3.16.0 (GAP-007, ADR-005)
 */
public interface ResourceClassifier {

    Optional<ResourceCategory> classify(ResourceRequest request, ClassificationContext context);

    int order();
}
