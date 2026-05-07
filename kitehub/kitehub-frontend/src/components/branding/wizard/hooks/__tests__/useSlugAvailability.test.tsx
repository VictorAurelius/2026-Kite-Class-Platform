/**
 * Wave 34 Bucket D — useSlugAvailability hook smoke tests.
 *
 * Validates the slug-availability hook against the MSW handler shipped
 * in `src/test/msw/handlers/branding.ts`. Covers happy-path (available
 * + conflict) plus the 400 INVALID_SLUG_FORMAT graceful fallback.
 */

import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useSlugAvailability } from '../useSlugAvailability';

describe('useSlugAvailability', () => {
  it('returns available=true for a fresh slug', async () => {
    const { result } = renderHook(() => useSlugAvailability());

    let response: Awaited<ReturnType<typeof result.current.checkSlug>> | undefined;
    await act(async () => {
      response = await result.current.checkSlug('fresh-tenant-name');
    });

    expect(response).toEqual({ available: true, suggestions: [] });
  });

  it('returns available=false with suggestions for known taken slug', async () => {
    const { result } = renderHook(() => useSlugAvailability());

    let response: Awaited<ReturnType<typeof result.current.checkSlug>> | undefined;
    await act(async () => {
      response = await result.current.checkSlug('toan-master');
    });

    expect(response?.available).toBe(false);
    expect(response?.suggestions.length).toBeGreaterThan(0);
    expect(response?.suggestions).toContain('toan-master-2026');
  });

  it('treats 400 INVALID_SLUG_FORMAT as not-available with empty suggestions', async () => {
    const { result } = renderHook(() => useSlugAvailability());

    let response: Awaited<ReturnType<typeof result.current.checkSlug>> | undefined;
    await act(async () => {
      response = await result.current.checkSlug('A');
    });

    expect(response).toEqual({ available: false, suggestions: [] });
  });
});
