import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useThemeGeneration } from '../use-theme-generation';
import type { LogoAnalysis } from '@/types/branding';
import type { ThemeConfig } from '@/types/theme';

// Mock fetch globally.
// Wave 34 Bucket D activated MSW (`server.listen()` patches global.fetch
// during `beforeAll` in `src/test/setup.ts`). Re-assign `global.fetch`
// to a fresh `vi.fn()` per test below in `beforeEach` so this test's
// fine-grained `mockResolvedValueOnce` / `mockRejectedValueOnce` calls
// keep working independently of MSW.
global.fetch = vi.fn();

describe('useThemeGeneration', () => {
  const mockLogoAnalysis: LogoAnalysis = {
    primaryColor: '#2196F3',
    secondaryColor: '#FF5722',
    accentColor: '#4CAF50',
    theme: 'MODERN',
    brandPersonality: ['Professional', 'Friendly'],
  };

  const mockThemeConfig: ThemeConfig = {
    colors: {
      primary: {
        shade50: '#E3F2FD',
        shade100: '#BBDEFB',
        shade200: '#90CAF9',
        shade300: '#64B5F6',
        shade400: '#42A5F5',
        shade500: '#2196F3',
        shade600: '#1E88E5',
        shade700: '#1976D2',
        shade800: '#1565C0',
        shade900: '#0D47A1',
      },
      secondary: {
        shade50: '#FBE9E7',
        shade100: '#FFCCBC',
        shade200: '#FFAB91',
        shade300: '#FF8A65',
        shade400: '#FF7043',
        shade500: '#FF5722',
        shade600: '#F4511E',
        shade700: '#E64A19',
        shade800: '#D84315',
        shade900: '#BF360C',
      },
      accent: {
        shade50: '#E8F5E9',
        shade100: '#C8E6C9',
        shade200: '#A5D6A7',
        shade300: '#81C784',
        shade400: '#66BB6A',
        shade500: '#4CAF50',
        shade600: '#43A047',
        shade700: '#388E3C',
        shade800: '#2E7D32',
        shade900: '#1B5E20',
      },
      neutral: {
        shade50: '#FAFAFA',
        shade100: '#F5F5F5',
        shade200: '#E5E5E5',
        shade300: '#D4D4D4',
        shade400: '#A3A3A3',
        shade500: '#737373',
        shade600: '#525252',
        shade700: '#404040',
        shade800: '#262626',
        shade900: '#171717',
      },
      semantic: {
        success: '#10B981',
        warning: '#F59E0B',
        error: '#EF4444',
        info: '#3B82F6',
      },
    },
    typography: {
      fontFamilyHeading: 'Inter',
      fontFamilyBody: 'Inter',
      fontSizeBase: '16px',
      fontSizes: {
        xs: '0.75rem',
        sm: '0.875rem',
        base: '1rem',
        lg: '1.125rem',
        xl: '1.25rem',
        xl2: '1.5rem',
        xl3: '1.875rem',
        xl4: '2.25rem',
      },
      fontWeights: {
        normal: 400,
        medium: 500,
        semibold: 600,
        bold: 700,
      },
      lineHeights: {
        tight: '1.25',
        normal: '1.5',
        relaxed: '1.75',
      },
    },
    spacing: {
      unit: 4,
      sectionSpacing: '4rem',
      componentSpacing: '1.5rem',
      elementSpacing: '0.75rem',
    },
    layout: {
      maxWidth: '1280px',
      borderRadius: {
        sm: '0.25rem',
        base: '0.5rem',
        lg: '0.75rem',
        full: '9999px',
      },
      shadow: {
        sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        base: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
        lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
      },
    },
  };

  beforeEach(() => {
    // Re-install vi.fn() each test so MSW's interceptor (installed once
    // in `beforeAll` via setup.ts) does not steal the fetch slot for
    // unhandled routes. MSW's `onUnhandledRequest: 'bypass'` would
    // otherwise let the request hit the real network.
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('should initialize with null theme config', () => {
    const { result } = renderHook(() => useThemeGeneration());

    expect(result.current.themeConfig).toBeNull();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('should generate theme successfully', async () => {
    // Mock successful API response
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockThemeConfig,
    });

    const { result } = renderHook(() => useThemeGeneration());

    // Call generateTheme
    await result.current.generateTheme(mockLogoAnalysis);

    // Wait for completion
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.themeConfig).toEqual(mockThemeConfig);
    });

    // Should have theme config and no error
    expect(result.current.error).toBeNull();

    // Verify fetch was called correctly
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/platform/branding/ai/generate-theme',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(mockLogoAnalysis),
      }
    );
  });

  it('should handle API error', async () => {
    // Mock failed API response
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      statusText: 'Internal Server Error',
    });

    const { result } = renderHook(() => useThemeGeneration());

    // Call generateTheme and wait
    await result.current.generateTheme(mockLogoAnalysis);

    // Wait for state update
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toContain('Failed to generate theme');
    });

    // Should have error
    expect(result.current.themeConfig).toBeNull();
  });

  it('should handle network error', async () => {
    // Mock network error
    (global.fetch as any).mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useThemeGeneration());

    // Call generateTheme and wait
    await result.current.generateTheme(mockLogoAnalysis);

    // Wait for state update
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBe('Network error');
    });

    // Should have error
    expect(result.current.themeConfig).toBeNull();
  });

  it('should clear error on new generation attempt', async () => {
    // First call fails
    (global.fetch as any).mockRejectedValueOnce(new Error('First error'));

    const { result } = renderHook(() => useThemeGeneration());

    result.current.generateTheme(mockLogoAnalysis);

    await waitFor(() => {
      expect(result.current.error).toBe('First error');
    });

    // Second call succeeds
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockThemeConfig,
    });

    result.current.generateTheme(mockLogoAnalysis);

    await waitFor(() => {
      expect(result.current.error).toBeNull();
      expect(result.current.themeConfig).toEqual(mockThemeConfig);
    });
  });
});
