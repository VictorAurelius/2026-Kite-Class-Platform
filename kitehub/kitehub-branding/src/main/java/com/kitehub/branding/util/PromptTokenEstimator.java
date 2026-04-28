package com.kitehub.branding.util;

/**
 * Heuristic token estimator for AI prompt input cost control (GAP-258).
 *
 * <p>v1 uses a simple chars/4 approximation, which mirrors OpenAI's published
 * rule of thumb (cl100k_base averages ~4 chars/token for English; Vietnamese
 * runs ~3.5 chars/token, so this errs on the over-estimate side and is safe
 * for cost-attack rejection). Real BPE tokenization (tiktoken-java) is tracked
 * out-of-scope in GAP-258 §Out-of-scope.</p>
 *
 * <p>Pure utility — no Spring wiring. Stateless and thread-safe.</p>
 */
public final class PromptTokenEstimator {

    /** Approximate characters per token (cl100k_base average). */
    private static final int CHARS_PER_TOKEN = 4;

    private PromptTokenEstimator() {
    }

    /**
     * Estimate the token count for a single string. Null and empty count as 0.
     */
    public static int estimate(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        return (input.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN;
    }

    /**
     * Estimate the combined token count for multiple inputs (sums each).
     * Useful when a request DTO has several user-controlled string fields.
     */
    public static int estimate(String... inputs) {
        if (inputs == null) {
            return 0;
        }
        int total = 0;
        for (String s : inputs) {
            total += estimate(s);
        }
        return total;
    }
}
