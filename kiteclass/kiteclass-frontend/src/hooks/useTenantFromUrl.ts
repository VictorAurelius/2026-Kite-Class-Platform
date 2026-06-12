/**
 * Hook to detect tenant from URL.
 *
 * Priority:
 * 1. Query param: ?tenant=abc-center
 * 2. Subdomain: abc-center.kitehub.me
 * 3. localStorage: tenantId (from previous session)
 *
 * Local dev uses query param, production uses subdomain.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useSearchParams } from 'next/navigation';
import { useEffect } from 'react';

export function useTenantFromUrl(): string | null {
  const searchParams = useSearchParams();

  // 1. Query param: ?tenant=abc-center
  const tenantParam = searchParams.get('tenant');

  // 2. Subdomain detection (production)
  const subdomain = typeof window !== 'undefined'
    ? getSubdomain(window.location.hostname)
    : null;

  const tenant = tenantParam || subdomain;

  // Store for API client to use
  useEffect(() => {
    if (tenant) {
      localStorage.setItem('tenantSubdomain', tenant);
    }
  }, [tenant]);

  return tenant;
}

function getSubdomain(hostname: string): string | null {
  // Skip for localhost, IP addresses
  if (hostname === 'localhost' || /^\d+\.\d+\.\d+\.\d+$/.test(hostname)) {
    return null;
  }

  // abc-center.kitehub.me → abc-center
  const parts = hostname.split('.');
  if (parts.length >= 3) {
    const sub = parts[0] ?? '';
    // Skip www, api, etc.
    if (['www', 'api', 'admin', 'staging'].includes(sub) || sub === '') {
      return null;
    }
    return sub;
  }

  return null;
}
