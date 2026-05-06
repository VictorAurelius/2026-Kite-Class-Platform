/**
 * ThemeProvider tests — verify typed wrapper around next-themes.
 *
 * We mock `next-themes` so the test asserts our wrapper logic, not next-themes.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';

const setThemeMock = vi.fn();
let mockTheme = 'light';

vi.mock('next-themes', () => ({
  useTheme: () => ({
    theme: mockTheme,
    resolvedTheme: mockTheme,
    setTheme: (next: string) => {
      mockTheme = next;
      setThemeMock(next);
    },
  }),
}));

import {
  useDashboardTheme,
  DASHBOARD_THEME_STORAGE_KEY,
} from '../ThemeProvider';

describe('useDashboardTheme', () => {
  beforeEach(() => {
    setThemeMock.mockClear();
    mockTheme = 'light';
    window.localStorage.clear();
  });

  it('exposes typed light/dark mode mapped from next-themes', () => {
    const { result } = renderHook(() => useDashboardTheme());
    expect(result.current.mode).toBe('light');
  });

  it('toggle() flips light → dark and persists to namespaced storage', () => {
    const { result, rerender } = renderHook(() => useDashboardTheme());
    act(() => result.current.toggle());
    rerender();
    expect(setThemeMock).toHaveBeenCalledWith('dark');
    expect(window.localStorage.getItem(DASHBOARD_THEME_STORAGE_KEY)).toBe('dark');
  });

  it('setMode(dark) writes through to next-themes setTheme', () => {
    const { result } = renderHook(() => useDashboardTheme());
    act(() => result.current.setMode('dark'));
    expect(setThemeMock).toHaveBeenCalledWith('dark');
  });
});
