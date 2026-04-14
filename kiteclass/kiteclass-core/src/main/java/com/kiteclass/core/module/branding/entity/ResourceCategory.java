package com.kiteclass.core.module.branding.entity;

/**
 * Resource category — drives routing in {@code ResourceRoutingService}.
 *
 * <p>Per ADR-005 / AI branding v2 redesign:
 * <ul>
 *   <li>{@link #STATIC}: user-uploaded or system default (no compute, long cache)</li>
 *   <li>{@link #TEMPLATE}: SVG/HTML template + brand params (fast compose, ~0 cost)</li>
 *   <li>{@link #FULL_AI}: AI-generated (heavy, async, expensive — ~80% of requests should NOT hit this)</li>
 * </ul>
 *
 * @since 3.16.0 (GAP-007, ADR-005)
 */
public enum ResourceCategory {
    STATIC,
    TEMPLATE,
    FULL_AI
}
