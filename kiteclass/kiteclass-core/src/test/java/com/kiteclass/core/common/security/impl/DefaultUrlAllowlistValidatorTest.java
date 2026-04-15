package com.kiteclass.core.common.security.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultUrlAllowlistValidatorTest {

    private DefaultUrlAllowlistValidator validator(String defaultCsv, String publicApiCsv, MockEnvironment env) {
        return new DefaultUrlAllowlistValidator(env, defaultCsv, publicApiCsv);
    }

    @Test
    void rejectsNullAndBlank() {
        DefaultUrlAllowlistValidator v = validator("", "", new MockEnvironment());
        assertThat(v.isAllowed(null, "t1")).isFalse();
        assertThat(v.isAllowed("", "t1")).isFalse();
        assertThat(v.isAllowed("   ", "t1")).isFalse();
    }

    @Test
    void rejectsNonHttpSchemes() {
        DefaultUrlAllowlistValidator v = validator("example.com", "", new MockEnvironment());
        assertThat(v.isAllowed("file:///etc/passwd", "t1")).isFalse();
        assertThat(v.isAllowed("ftp://example.com/file", "t1")).isFalse();
        assertThat(v.isAllowed("gopher://example.com/", "t1")).isFalse();
    }

    @Test
    void rejectsUserinfo() {
        DefaultUrlAllowlistValidator v = validator("example.com", "", new MockEnvironment());
        assertThat(v.isAllowed("http://user:pass@example.com/x", "t1")).isFalse();
    }

    @Test
    void rejectsLoopback() {
        DefaultUrlAllowlistValidator v = validator("", "", new MockEnvironment());
        assertThat(v.isAllowed("http://localhost:8080/", "t1")).isFalse();
        assertThat(v.isAllowed("http://127.0.0.1/", "t1")).isFalse();
        assertThat(v.isAllowed("http://127.5.5.5/", "t1")).isFalse();
    }

    @Test
    void rejectsPrivateIpv4Ranges() {
        DefaultUrlAllowlistValidator v = validator("", "", new MockEnvironment());
        assertThat(v.isAllowed("http://10.0.0.1/", "t1")).isFalse();
        assertThat(v.isAllowed("http://192.168.1.1/", "t1")).isFalse();
        assertThat(v.isAllowed("http://172.16.0.1/", "t1")).isFalse();
        assertThat(v.isAllowed("http://172.31.255.255/", "t1")).isFalse();
    }

    @Test
    void rejectsLinkLocal() {
        DefaultUrlAllowlistValidator v = validator("", "", new MockEnvironment());
        assertThat(v.isAllowed("http://169.254.169.254/latest/meta-data/", "t1")).isFalse();
    }

    @Test
    void rejectsIpv6Loopback() {
        DefaultUrlAllowlistValidator v = validator("", "", new MockEnvironment());
        assertThat(v.isAllowed("http://[::1]/x", "t1")).isFalse();
    }

    @Test
    void allowsTenantListedHost() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("security.url.allowlist.t1", "api.partner.com");
        DefaultUrlAllowlistValidator v = validator("", "", env);
        assertThat(v.isAllowed("https://api.partner.com/webhook", "t1")).isTrue();
    }

    @Test
    void rejectsTenantUnlistedHost() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("security.url.allowlist.t1", "api.partner.com");
        DefaultUrlAllowlistValidator v = validator("", "", env);
        assertThat(v.isAllowed("https://unknown.example.com/", "t1")).isFalse();
    }

    @Test
    void allowsDefaultListedHostForAnyTenant() {
        DefaultUrlAllowlistValidator v = validator("api.shared.example", "", new MockEnvironment());
        assertThat(v.isAllowed("https://api.shared.example/x", "t99")).isTrue();
    }

    @Test
    void wildcardDomainMatchesSubdomain() {
        DefaultUrlAllowlistValidator v = validator("*.trusted.org", "", new MockEnvironment());
        assertThat(v.isAllowed("https://foo.trusted.org/y", "t1")).isTrue();
        assertThat(v.isAllowed("https://nested.bar.trusted.org/y", "t1")).isTrue();
    }

    @Test
    void nullTenantRequiresPublicApiPatternMatch() {
        DefaultUrlAllowlistValidator v = validator("",
                "^https://api\\.ollama\\.com/.*", new MockEnvironment());
        assertThat(v.isAllowed("https://api.ollama.com/v1/generate", null)).isTrue();
        assertThat(v.isAllowed("https://api.unknown.com/v1", null)).isFalse();
    }

    @Test
    void nullTenantAndNoPatternsDenied() {
        DefaultUrlAllowlistValidator v = validator("", "", new MockEnvironment());
        assertThat(v.isAllowed("https://api.example.com/", null)).isFalse();
    }

    @Test
    void malformedUrlRejected() {
        DefaultUrlAllowlistValidator v = validator("example.com", "", new MockEnvironment());
        assertThat(v.isAllowed("not-a-url", "t1")).isFalse();
        assertThat(v.isAllowed("http://", "t1")).isFalse();
    }
}
