package com.kiteclass.core.module.legal;

import com.kiteclass.core.module.legal.config.TrademarkProperties;
import com.kiteclass.core.module.legal.service.TrademarkCheckResult;
import com.kiteclass.core.module.legal.service.TrademarkCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrademarkCheckServiceTest {

    private TrademarkProperties properties;
    private TrademarkCheckService service;

    @BeforeEach
    void setup() {
        properties = new TrademarkProperties();
        properties.setBannedKeywords(List.of("Nike", "Adidas", "Apple Inc"));
        service = new TrademarkCheckService(properties);
    }

    @Test
    void clear_returned_when_text_is_null_or_blank() {
        assertThat(service.checkTextKeywords(null).isClear()).isTrue();
        assertThat(service.checkTextKeywords("").isClear()).isTrue();
        assertThat(service.checkTextKeywords("   ").isClear()).isTrue();
    }

    @Test
    void clear_when_no_keyword_in_text() {
        TrademarkCheckResult result = service.checkTextKeywords("Quality learning experience");
        assertThat(result.isClear()).isTrue();
        assertThat(result.getHits()).isEmpty();
    }

    @Test
    void flagged_on_exact_match() {
        TrademarkCheckResult result = service.checkTextKeywords("Nike tutoring center");
        assertThat(result.isClear()).isFalse();
        assertThat(result.getHits()).containsExactly("Nike");
    }

    @Test
    void case_insensitive_match() {
        TrademarkCheckResult result = service.checkTextKeywords("Welcome to NIKE academy");
        assertThat(result.isClear()).isFalse();
        assertThat(result.getHits()).containsExactly("Nike");
    }

    @Test
    void multiple_hits_reported() {
        TrademarkCheckResult result = service.checkTextKeywords("Nike and Adidas partnership");
        assertThat(result.isClear()).isFalse();
        assertThat(result.getHits()).containsExactlyInAnyOrder("Nike", "Adidas");
    }

    @Test
    void multi_word_keyword_matched() {
        TrademarkCheckResult result = service.checkTextKeywords(
                "Partnered with apple inc since 2020");
        assertThat(result.isClear()).isFalse();
        assertThat(result.getHits()).containsExactly("Apple Inc");
    }

    @Test
    void empty_banned_list_always_clear() {
        properties.setBannedKeywords(List.of());
        TrademarkCheckResult result = service.checkTextKeywords("Nike or Adidas");
        assertThat(result.isClear()).isTrue();
    }

    @Test
    void blank_keyword_entries_are_ignored() {
        properties.setBannedKeywords(java.util.Arrays.asList("Nike", "", "  ", null));
        TrademarkCheckResult result = service.checkTextKeywords("Some neutral tagline");
        assertThat(result.isClear()).isTrue();
    }
}
