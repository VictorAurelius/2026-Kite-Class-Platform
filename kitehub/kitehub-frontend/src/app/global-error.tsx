/**
 * Root-level fatal error boundary (Next.js App Router convention).
 *
 * Activates only when the root layout itself throws — replaces the entire
 * <html>/<body> tree because the regular layout chain is unavailable.
 *
 * Constraints:
 * - MUST render its own <html> and <body> tags (Next.js requirement).
 * - MUST be a client component ('use client').
 * - Cannot rely on root layout providers (ThemeProvider, ReactQueryProvider).
 *   Therefore styles use plain Tailwind utilities + inline-safe colors with
 *   no CSS variable dependency, so the page renders even if globals.css fails.
 * - Vietnamese-first copy.
 * - Sentry-ready hook: console.error so an APM client can subscribe later.
 *
 * @author KiteHub Team
 * @since GAP-136
 */

'use client';

import { useEffect } from 'react';

interface GlobalErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: GlobalErrorProps) {
  useEffect(() => {
    // Sentry-ready hook — fatal-tier so always log even in production.
    console.error('KiteHub fatal error:', error);
  }, [error]);

  return (
    <html lang="vi">
      <body
        style={{
          margin: 0,
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '1rem',
          fontFamily:
            'Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
          background: '#f8fafc',
          color: '#0f172a',
        }}
      >
        <div style={{ maxWidth: '32rem', textAlign: 'center' }}>
          <p
            style={{
              margin: 0,
              fontSize: '0.875rem',
              fontWeight: 600,
              letterSpacing: '0.1em',
              textTransform: 'uppercase',
              color: '#dc2626',
            }}
          >
            Lỗi nghiêm trọng
          </p>
          <h1
            style={{
              margin: '0.5rem 0 0.75rem',
              fontSize: '2rem',
              fontWeight: 700,
              lineHeight: 1.2,
            }}
          >
            Lỗi hệ thống
          </h1>
          <p
            style={{
              margin: '0 0 2rem',
              fontSize: '1rem',
              color: '#475569',
              lineHeight: 1.5,
            }}
          >
            Hệ thống gặp sự cố nghiêm trọng và không thể hiển thị trang. Đội
            ngũ KiteHub đã được thông báo. Vui lòng thử lại sau ít phút.
          </p>
          <div
            style={{
              display: 'flex',
              gap: '0.75rem',
              justifyContent: 'center',
              flexWrap: 'wrap',
            }}
          >
            <button
              type="button"
              onClick={() => reset()}
              style={{
                background: '#0ea5e9',
                color: '#ffffff',
                border: 'none',
                borderRadius: '0.5rem',
                padding: '0.75rem 1.5rem',
                fontSize: '1rem',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Thử lại
            </button>
            {/*
              Intentional plain anchor: global-error.tsx fires when the root
              layout itself crashes, so Next.js Router context is unavailable
              and <Link> would throw. Force a full document navigation instead.
            */}
            {/* eslint-disable-next-line @next/next/no-html-link-for-pages */}
            <a
              href="/"
              style={{
                background: '#ffffff',
                color: '#0f172a',
                border: '1px solid #cbd5e1',
                borderRadius: '0.5rem',
                padding: '0.75rem 1.5rem',
                fontSize: '1rem',
                fontWeight: 600,
                textDecoration: 'none',
                display: 'inline-flex',
                alignItems: 'center',
              }}
            >
              Về trang chủ
            </a>
          </div>
          {process.env.NODE_ENV === 'development' && error.digest && (
            <p
              style={{
                marginTop: '1.5rem',
                fontSize: '0.75rem',
                color: '#94a3b8',
              }}
            >
              Digest: {error.digest}
            </p>
          )}
        </div>
      </body>
    </html>
  );
}
