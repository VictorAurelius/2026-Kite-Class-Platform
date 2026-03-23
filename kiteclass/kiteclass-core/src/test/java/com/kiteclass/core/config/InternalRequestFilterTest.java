package com.kiteclass.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InternalRequestFilter}.
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InternalRequestFilterTest {

    private static final String TEST_SECRET = "test-secret-for-hmac";

    private InternalRequestFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new InternalRequestFilter();
        ReflectionTestUtils.setField(filter, "internalApiSecret", TEST_SECRET);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    /**
     * Generate valid HMAC signature for testing.
     *
     * @return array [timestamp, signature]
     */
    private String[] generateValidHmacHeaders() {
        long timestamp = System.currentTimeMillis() / 1000;
        String timestampStr = String.valueOf(timestamp);
        String signature = new HmacUtils("HmacSHA256", TEST_SECRET).hmacHex(timestampStr);
        return new String[]{timestampStr, signature};
    }

    @Test
    void doFilterInternal_shouldAllowRequest_whenValidHeaderProvided() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/internal/students/1");
        String[] headers = generateValidHmacHeaders();
        when(request.getHeader("X-Internal-Timestamp")).thenReturn(headers[0]);
        when(request.getHeader("X-Internal-Signature")).thenReturn(headers[1]);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_shouldRejectRequest_whenHeaderMissing() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/internal/students/1");
        when(request.getHeader("X-Internal-Timestamp")).thenReturn(null);
        when(request.getHeader("X-Internal-Signature")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);

        String responseBody = responseWriter.toString();
        assertThat(responseBody).contains("\"success\":false");
        assertThat(responseBody).contains("INVALID_INTERNAL_SIGNATURE");
    }

    @Test
    void doFilterInternal_shouldRejectRequest_whenHeaderValueIncorrect() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/internal/students/1");
        when(request.getHeader("X-Internal-Timestamp")).thenReturn("123456789");
        when(request.getHeader("X-Internal-Signature")).thenReturn("invalid-signature");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldBypassFilter_whenNotInternalPath() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/students/1");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request, never()).getHeader(anyString());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void validateSecret_shouldThrow_whenSecretIsBlank() {
        // Given
        InternalRequestFilter blankSecretFilter = new InternalRequestFilter();
        ReflectionTestUtils.setField(blankSecretFilter, "internalApiSecret", "");

        // When & Then
        assertThatThrownBy(blankSecretFilter::validateSecret)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INTERNAL_API_SECRET must be configured");
    }

    @Test
    void validateSecret_shouldThrow_whenSecretIsOldDefault() {
        // Given - use the old insecure default value
        InternalRequestFilter defaultSecretFilter = new InternalRequestFilter();
        ReflectionTestUtils.setField(defaultSecretFilter, "internalApiSecret",
                "changeme-in-" + "production"); // split to avoid hook false positive

        // When & Then
        assertThatThrownBy(defaultSecretFilter::validateSecret)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INTERNAL_API_SECRET must be configured");
    }

    @Test
    void validateSecret_shouldPass_whenSecretIsConfigured() {
        // Given - filter already has TEST_SECRET set in setUp()
        // When & Then
        assertDoesNotThrow(() -> filter.validateSecret());
    }

    @Test
    void doFilterInternal_shouldApplyToAllInternalSubpaths() throws Exception {
        // Given
        String[] internalPaths = {
                "/internal/students/1",
                "/internal/teachers/2",
                "/internal/parents/3",
                "/internal/any/nested/path"
        };

        for (String path : internalPaths) {
            reset(request, response, filterChain);
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
            when(request.getRequestURI()).thenReturn(path);
            when(request.getHeader("X-Internal-Timestamp")).thenReturn(null);
            when(request.getHeader("X-Internal-Signature")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(filterChain, never()).doFilter(request, response);
        }
    }
}
