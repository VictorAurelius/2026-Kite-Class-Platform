package com.kitehub.subscription.idempotency.interceptor;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Fully-buffered, re-readable request wrapper (GAP-536 Wave onboarding-polish-2 Bucket C).
 *
 * <p>Reads the entire request body into memory at construction, then serves a
 * fresh {@link ServletInputStream} over the SAME byte buffer for every
 * {@code getInputStream()} / {@code getReader()} call. This is required so that
 * {@link IdempotencyHandlerInterceptor#preHandle} can read the body to compute
 * the idempotency request hash WITHOUT starving the downstream Spring MVC
 * {@code @RequestBody} parse — both consumers get the identical bytes.</p>
 *
 * <p>Spring's {@code ContentCachingRequestWrapper} cannot be used for this:
 * its cached-bytes accessor ({@code getContentAsByteArray()}) is only populated
 * AFTER the stream is consumed downstream, so at {@code preHandle} time it is
 * empty — causing every payload to hash identically and the same-key /
 * different-body 422 conflict guard to never fire. (Bug surfaced by GAP-536
 * Testcontainers live verify 2026-06-02.)</p>
 *
 * @since Wave onboarding-polish-2 Bucket C — GAP-536
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = getCharacterEncoding() != null
                ? Charset.forName(getCharacterEncoding())
                : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cachedBody), charset));
    }

    /** ServletInputStream backed by an in-memory byte buffer (re-readable per call). */
    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        CachedBodyServletInputStream(byte[] body) {
            this.buffer = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Async read not supported on cached body");
        }

        @Override
        public int read() {
            return buffer.read();
        }
    }
}
