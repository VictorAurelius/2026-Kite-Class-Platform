package com.kitehub.branding.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptTokenEstimator")
class PromptTokenEstimatorTest {

    @Test
    @DisplayName("null and empty inputs estimate to 0")
    void nullAndEmpty() {
        assertThat(PromptTokenEstimator.estimate((String) null)).isZero();
        assertThat(PromptTokenEstimator.estimate("")).isZero();
        assertThat(PromptTokenEstimator.estimate((String[]) null)).isZero();
    }

    @Test
    @DisplayName("4 chars round up to 1 token")
    void fourCharsOneToken() {
        assertThat(PromptTokenEstimator.estimate("abcd")).isEqualTo(1);
    }

    @Test
    @DisplayName("ceiling division — 1 char rounds up to 1 token")
    void ceilingForShortInput() {
        assertThat(PromptTokenEstimator.estimate("a")).isEqualTo(1);
        assertThat(PromptTokenEstimator.estimate("ab")).isEqualTo(1);
        assertThat(PromptTokenEstimator.estimate("abc")).isEqualTo(1);
    }

    @Test
    @DisplayName("8000 chars yields 2000 tokens (FREE tier ceiling sample)")
    void tierCeilingSample() {
        String input = "x".repeat(8000);
        assertThat(PromptTokenEstimator.estimate(input)).isEqualTo(2000);
    }

    @Test
    @DisplayName("varargs sum across multiple inputs")
    void varargsSum() {
        // 4 chars = 1 token, 8 chars = 2 tokens, total = 3
        assertThat(PromptTokenEstimator.estimate("abcd", "abcdefgh")).isEqualTo(3);
    }

    @Test
    @DisplayName("varargs tolerate null elements")
    void varargsNullSafe() {
        assertThat(PromptTokenEstimator.estimate("abcd", null, "efgh")).isEqualTo(2);
    }
}
