package com.kitehub.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DnsTxtLookupService}.
 *
 * <p>Tests use a subclass override of {@code lookupTxtRecords(String)} to avoid
 * hitting real DNS during CI — covers verification logic deterministically.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("DnsTxtLookupService Unit Tests")
class DnsTxtLookupServiceTest {

    @Test
    @DisplayName("verifyTxtRecord: TXT match at _kitehub-verify subdomain returns true")
    void verifyTxtRecord_matchAtVerifySubdomain_returnsTrue() {
        DnsTxtLookupService service = new DnsTxtLookupService() {
            @Override
            protected List<String> lookupTxtRecords(String host) {
                if (host.equals("_kitehub-verify.example.com")) {
                    return List.of("kitehub-verify=abc123");
                }
                return Collections.emptyList();
            }
        };
        assertThat(service.verifyTxtRecord("example.com", "kitehub-verify=abc123")).isTrue();
    }

    @Test
    @DisplayName("verifyTxtRecord: TXT match at apex (fallback) returns true")
    void verifyTxtRecord_matchAtApex_returnsTrue() {
        DnsTxtLookupService service = new DnsTxtLookupService() {
            @Override
            protected List<String> lookupTxtRecords(String host) {
                if (host.equals("example.com")) {
                    return List.of("v=spf1 -all", "kitehub-verify=xyz789");
                }
                return Collections.emptyList();
            }
        };
        assertThat(service.verifyTxtRecord("example.com", "kitehub-verify=xyz789")).isTrue();
    }

    @Test
    @DisplayName("verifyTxtRecord: no records anywhere returns false")
    void verifyTxtRecord_noRecords_returnsFalse() {
        DnsTxtLookupService service = new DnsTxtLookupService() {
            @Override
            protected List<String> lookupTxtRecords(String host) {
                return Collections.emptyList();
            }
        };
        assertThat(service.verifyTxtRecord("example.com", "kitehub-verify=abc123")).isFalse();
    }

    @Test
    @DisplayName("verifyTxtRecord: records present but token mismatch returns false")
    void verifyTxtRecord_tokenMismatch_returnsFalse() {
        DnsTxtLookupService service = new DnsTxtLookupService() {
            @Override
            protected List<String> lookupTxtRecords(String host) {
                return List.of("kitehub-verify=wrong-token", "v=spf1 -all");
            }
        };
        assertThat(service.verifyTxtRecord("example.com", "kitehub-verify=correct-token")).isFalse();
    }

    @Test
    @DisplayName("verifyTxtRecord: blank domain returns false (no throw)")
    void verifyTxtRecord_blankDomain_returnsFalse() {
        DnsTxtLookupService service = new DnsTxtLookupService();
        assertThat(service.verifyTxtRecord("", "kitehub-verify=abc")).isFalse();
        assertThat(service.verifyTxtRecord(null, "kitehub-verify=abc")).isFalse();
    }

    @Test
    @DisplayName("verifyTxtRecord: blank token returns false (no throw)")
    void verifyTxtRecord_blankToken_returnsFalse() {
        DnsTxtLookupService service = new DnsTxtLookupService();
        assertThat(service.verifyTxtRecord("example.com", "")).isFalse();
        assertThat(service.verifyTxtRecord("example.com", null)).isFalse();
    }
}
