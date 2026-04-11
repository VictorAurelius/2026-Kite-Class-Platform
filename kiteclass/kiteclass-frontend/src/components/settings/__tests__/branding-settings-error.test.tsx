/**
 * Tests for BrandingSettings error state.
 * Verifies proper error UI when API fails instead of infinite loading.
 *
 * @since 2026-04-11
 */

import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { BrandingSettings } from '../branding-settings';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

describe('BrandingSettings error state', () => {
  it('shows error message when branding API fails', async () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/settings/branding`, () => {
        return HttpResponse.json({ message: 'Internal Server Error' }, { status: 500 });
      })
    );

    render(<BrandingSettings />);

    await waitFor(() => {
      expect(screen.getByText(/Không thể tải cài đặt branding/i)).toBeInTheDocument();
    });
  });

  it('shows loading text initially', () => {
    server.use(
      http.get(`${BASE_URL}/api/v1/settings/branding`, async () => {
        await new Promise(() => {}); // never resolves
        return HttpResponse.json({});
      })
    );

    render(<BrandingSettings />);
    expect(screen.getByText(/Đang tải cài đặt/i)).toBeInTheDocument();
  });
});
