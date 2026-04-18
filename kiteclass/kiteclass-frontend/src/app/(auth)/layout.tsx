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

import type { ReactNode } from 'react';
import { BrandingProvider } from '@/providers/BrandingProvider';

export default function AuthPagesLayout({ children }: { children: ReactNode }) {
  return <BrandingProvider>{children}</BrandingProvider>;
}
