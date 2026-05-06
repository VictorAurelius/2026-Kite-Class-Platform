package com.kitehub.shared.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PIIScrubberTest {

    @Test
    @DisplayName("null and empty inputs are returned untouched")
    void nullAndEmpty() {
        assertThat(PIIScrubber.scrub(null)).isNull();
        assertThat(PIIScrubber.scrub("")).isEmpty();
    }

    @Test
    @DisplayName("strings with no PII pass through unchanged")
    void noPii() {
        assertThat(PIIScrubber.scrub("subscription renewed for tenant"))
            .isEqualTo("subscription renewed for tenant");
    }

    @ParameterizedTest
    @DisplayName("email addresses are masked, domain preserved")
    @CsvSource({
        "alice@example.com, a***@example.com",
        "bob.smith+tag@kite.com, b***@kite.com",
        "Contact us at sales@kite.io please, Contact us at s***@kite.io please"
    })
    void emailMasked(String input, String expected) {
        assertThat(PIIScrubber.scrub(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Vietnamese phone numbers are masked, first 2 + last 2 preserved")
    @CsvSource({
        "0987654321, 09******21",
        "Phone: 0123456789 OK, 'Phone: 01******89 OK'",
        "01234567890, 01*******90"
    })
    void vnPhoneMasked(String input, String expected) {
        assertThat(PIIScrubber.scrub(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("credit-card-shaped digits are masked, last 4 preserved")
    void creditCardMasked() {
        assertThat(PIIScrubber.scrub("Card 4111111111111111 charged"))
            .isEqualTo("Card ************1111 charged");
    }

    @Test
    @DisplayName("JWT bearer tokens replaced wholesale")
    void jwtMasked() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3IifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        assertThat(PIIScrubber.scrub("Bearer " + jwt)).isEqualTo("Bearer <REDACTED_JWT>");
    }

    @ParameterizedTest
    @DisplayName("password and api-key tokens replaced")
    @CsvSource(value = {
        "password=hunter2| password=<REDACTED>",
        "Login {\"password\":\"secret123\"}| Login {password=<REDACTED>}",
        "apiKey=ABCDEFGHIJKLMNOP| apiKey=<REDACTED>",
        "X-Token: abc123def456ghi789| X-Token=<REDACTED>"
    }, delimiter = '|')
    void credentialsMasked(String input, String expected) {
        assertThat(PIIScrubber.scrub(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("contextual Vietnamese national-id (CCCD) masked")
    void cccdMasked() {
        assertThat(PIIScrubber.scrub("user CCCD: 123456789012 verified"))
            .isEqualTo("user CCCD=<REDACTED_ID> verified");
    }

    @Test
    @DisplayName("multiple PII types in one string all get scrubbed")
    void mixed() {
        String input = "User alice@kite.com phone 0987654321 password=hunter2";
        String out = PIIScrubber.scrub(input);
        assertThat(out)
            .doesNotContain("alice@kite.com")
            .doesNotContain("0987654321")
            .doesNotContain("hunter2")
            .contains("a***@kite.com")
            .contains("09******21")
            .contains("password=<REDACTED>");
    }
}
