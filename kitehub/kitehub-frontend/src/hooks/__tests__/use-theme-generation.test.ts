import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useThemeGeneration } from '../use-theme-generation';
import { apiClient } from '@/lib/api/client';
import type { LogoAnalysis } from '@/types/branding';
import type { ThemeConfig } from '@/types/theme';

// GAP-1336: the hook now calls `apiClient.post` (shared axios client) instead of
// raw `fetch`, so the request carries Authorization + X-Tenant-Id and is a real
// POST. Mock the apiClient module here (was: global.fetch) so each test drives
// `apiClient.post` resolution/rejection directly.
vi.mock('@/lib/api/client', () => ({
  apiClient: { post: vi.fn() },
}));

const mockPost = vi.mocked(apiClient.post);

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
    mockPost.mockReset();
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
    // apiClient.post resolves with an axios-shaped response ({ data }).
    mockPost.mockResolvedValueOnce({ data: mockThemeConfig } as any);

    const { result } = renderHook(() => useThemeGeneration());

    await result.current.generateTheme(mockLogoAnalysis);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.themeConfig).toEqual(mockThemeConfig);
    });

    expect(result.current.error).toBeNull();

    // POST to the AI branding endpoint with the analysis as the body; apiClient
    // attaches Authorization + X-Tenant-Id headers automatically.
    expect(mockPost).toHaveBeenCalledWith(
      '/api/platform/branding/ai/generate-theme',
      mockLogoAnalysis
    );
  });

  it('should handle API error', async () => {
    // axios rejects on non-2xx; the hook surfaces err.message.
    mockPost.mockRejectedValueOnce(new Error('Request failed with status code 500'));

    const { result } = renderHook(() => useThemeGeneration());

    await result.current.generateTheme(mockLogoAnalysis);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBe('Request failed with status code 500');
    });

    expect(result.current.themeConfig).toBeNull();
  });

  it('should handle network error', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useThemeGeneration());

    await result.current.generateTheme(mockLogoAnalysis);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBe('Network error');
    });

    expect(result.current.themeConfig).toBeNull();
  });

  it('should clear error on new generation attempt', async () => {
    // First call fails
    mockPost.mockRejectedValueOnce(new Error('First error'));

    const { result } = renderHook(() => useThemeGeneration());

    result.current.generateTheme(mockLogoAnalysis);

    await waitFor(() => {
      expect(result.current.error).toBe('First error');
    });

    // Second call succeeds
    mockPost.mockResolvedValueOnce({ data: mockThemeConfig } as any);

    result.current.generateTheme(mockLogoAnalysis);

    await waitFor(() => {
      expect(result.current.error).toBeNull();
      expect(result.current.themeConfig).toEqual(mockThemeConfig);
    });
  });
});
