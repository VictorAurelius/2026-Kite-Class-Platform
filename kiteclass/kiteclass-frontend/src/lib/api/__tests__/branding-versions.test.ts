/**
 * Tests for the branding version-history + rollback API client (GAP-1446).
 *
 * Verifies the FE calls the BrandingVersionController endpoints under the
 * tenant-scoped /api/v1/branding/{instanceId} base (NOT the settings/branding
 * base) and reads the raw Spring Page / BrandingVersion bodies directly (no
 * ApiResponse unwrap — the version controller does not wrap its responses).
 *
 * @since GAP-1446
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { apiClient } from '@/lib/api-client';
import { brandingApi } from '../branding';
import type { BrandingVersion, BrandingVersionPage } from '@/types/branding';

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

const INSTANCE_ID = '22222222-2222-2222-2222-222222222222';

const sampleVersion: BrandingVersion = {
  id: 1,
  instanceId: INSTANCE_ID,
  versionNumber: 3,
  snapshotJson: '{"displayName":"Trung tâm Sky Education"}',
  rollbackOf: null,
  active: true,
  createdAt: '2026-06-16T08:00:00Z',
  updatedAt: '2026-06-16T08:00:00Z',
};

const samplePage: BrandingVersionPage = {
  content: [sampleVersion],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe('brandingApi version history (GAP-1446)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('listVersions', () => {
    it('GETs the tenant-scoped versions page with default paging + returns raw Page', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: samplePage });

      const result = await brandingApi.listVersions(INSTANCE_ID);

      expect(apiClient.get).toHaveBeenCalledWith(
        `/api/v1/branding/${INSTANCE_ID}/versions`,
        { params: { page: 0, size: 20 } },
      );
      // Raw Page returned (NOT unwrapped from an ApiResponse envelope).
      expect(result).toEqual(samplePage);
      expect(result.content[0]!.versionNumber).toBe(3);
    });

    it('forwards explicit page + size params', async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: samplePage });

      await brandingApi.listVersions(INSTANCE_ID, 2, 5);

      expect(apiClient.get).toHaveBeenCalledWith(
        `/api/v1/branding/${INSTANCE_ID}/versions`,
        { params: { page: 2, size: 5 } },
      );
    });
  });

  describe('rollback', () => {
    it('POSTs to the version rollback endpoint + returns the raw new version', async () => {
      const restored: BrandingVersion = { ...sampleVersion, id: 2, versionNumber: 4, rollbackOf: 3 };
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: restored });

      const result = await brandingApi.rollback(INSTANCE_ID, 3);

      expect(apiClient.post).toHaveBeenCalledWith(
        `/api/v1/branding/${INSTANCE_ID}/versions/3/rollback`,
      );
      expect(result.rollbackOf).toBe(3);
      expect(result.versionNumber).toBe(4);
    });
  });
});
