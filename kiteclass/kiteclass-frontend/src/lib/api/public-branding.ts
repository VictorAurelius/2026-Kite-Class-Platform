/**
 * Public branding API — anonymous access for auth pages (login, register, reset).
 *
 * Returns a trimmed-down branding payload that's safe to expose without a
 * logged-in user: display name, logo URL, primary/secondary/accent colors.
 * Contact/social/admin-only fields are NOT returned by the backend.
 *
 * @since Wave 4 (GAP-037)
 */

import { apiClient } from '@/lib/api-client';

export interface PublicBranding {
  displayName: string;
  logoUrl: string;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  tagline: string;
}

export const DEFAULT_PUBLIC_BRANDING: PublicBranding = {
  displayName: 'KiteClass',
  logoUrl: '',
  primaryColor: '#3B82F6',
  secondaryColor: '#8B5CF6',
  accentColor: '#10B981',
  tagline: '',
};

export const publicBrandingApi = {
  get: async (tenantId: string | null): Promise<PublicBranding> => {
    if (!tenantId) {
      return DEFAULT_PUBLIC_BRANDING;
    }
    try {
      const { data } = await apiClient.get<PublicBranding>(
        `/api/v1/branding/public`,
        { params: { tenantId } },
      );
      return {
        ...DEFAULT_PUBLIC_BRANDING,
        ...data,
      };
    } catch {
      // Graceful degradation: render auth pages with defaults if the
      // branding service is unreachable or the tenant is unknown.
      return DEFAULT_PUBLIC_BRANDING;
    }
  },
};
