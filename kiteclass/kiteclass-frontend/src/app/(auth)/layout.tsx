/**
 * Auth pages layout.
 *
 * Wrapping auth pages (login, register, reset) with BrandingProvider so each
 * tenant's logo + primary color paint the auth UI before the user logs in
 * (GAP-037, Wave 4).
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { Suspense, type ReactNode } from 'react';
import { BrandingProvider } from '@/providers/BrandingProvider';

// Next.js 15: useSearchParams() inside BrandingProvider requires a Suspense
// boundary for static prerendering of auth pages. Default placeholder is null
// because BrandingProvider applies a safe default palette synchronously.
export default function AuthPagesLayout({ children }: { children: ReactNode }) {
  return (
    <Suspense fallback={null}>
      <BrandingProvider>{children}</BrandingProvider>
    </Suspense>
  );
}
