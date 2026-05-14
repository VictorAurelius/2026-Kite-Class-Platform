/**
 * Unit tests for auth-helpers role compatibility layer.
 *
 * Closes GAP-518 (Wave 78 Bucket D) — codifies the BE seed (`PLATFORM_ADMIN`)
 * vs FE guard (`ADMIN` legacy) reconciliation as a regression-safe check.
 *
 * Backend reference: `kitehub-subscription` ProductionSeedRunner.java line 110
 *   `.role("PLATFORM_ADMIN")`
 *
 * Frontend consumers verified to use this helper:
 *   - `components/layout/AdminLayout.tsx` (route guard)
 *   - `app/(auth)/login/page.tsx` (post-login redirect)
 *   - `app/(auth)/2fa-setup/page.tsx`
 *   - `app/(auth)/2fa-challenge/page.tsx`
 */

import { describe, it, expect } from 'vitest';
import { isPlatformAdmin } from '../auth-helpers';

describe('auth-helpers', () => {
  describe('isPlatformAdmin', () => {
    it('accepts canonical PLATFORM_ADMIN role (BE seed value)', () => {
      expect(isPlatformAdmin('PLATFORM_ADMIN')).toBe(true);
    });

    it('accepts legacy ADMIN alias (backward compatibility)', () => {
      expect(isPlatformAdmin('ADMIN')).toBe(true);
    });

    it('rejects tenant-scoped OWNER role', () => {
      expect(isPlatformAdmin('OWNER')).toBe(false);
    });

    it('rejects empty string', () => {
      expect(isPlatformAdmin('')).toBe(false);
    });

    it('rejects undefined (unauthenticated user)', () => {
      expect(isPlatformAdmin(undefined)).toBe(false);
    });

    it('rejects null', () => {
      expect(isPlatformAdmin(null)).toBe(false);
    });

    it('rejects lowercase admin (case-sensitive)', () => {
      expect(isPlatformAdmin('admin')).toBe(false);
    });

    it('rejects partial-match role like SCHOOL_ADMIN', () => {
      expect(isPlatformAdmin('SCHOOL_ADMIN')).toBe(false);
    });

    it('rejects partial-match role like PLATFORM', () => {
      expect(isPlatformAdmin('PLATFORM')).toBe(false);
    });

    it('rejects unknown role values', () => {
      expect(isPlatformAdmin('STUDENT')).toBe(false);
      expect(isPlatformAdmin('TEACHER')).toBe(false);
      expect(isPlatformAdmin('SUPER_USER')).toBe(false);
    });
  });
});
