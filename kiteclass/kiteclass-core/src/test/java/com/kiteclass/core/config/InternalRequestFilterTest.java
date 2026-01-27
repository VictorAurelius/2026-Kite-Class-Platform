package com.kiteclass.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InternalRequestFilter}.
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@ExtendWith(MockitoExtension.class)
class InternalRequestFilterTest {

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
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void doFilterInternal_shouldAllowRequest_whenValidHeaderProvided() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/internal/students/1");
        when(request.getHeader("X-Internal-Request")).thenReturn("true");

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
        when(request.getHeader("X-Internal-Request")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);

        String responseBody = responseWriter.toString();
        assertThat(responseBody).contains("\"success\":false");
        assertThat(responseBody).contains("INTERNAL_API_FORBIDDEN");
    }

    @Test
    void doFilterInternal_shouldRejectRequest_whenHeaderValueIncorrect() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/internal/students/1");
        when(request.getHeader("X-Internal-Request")).thenReturn("false");

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
            when(request.getHeader("X-Internal-Request")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(filterChain, never()).doFilter(request, response);
        }
    }
}
