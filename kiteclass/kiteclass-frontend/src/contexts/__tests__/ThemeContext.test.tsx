/**
 * Tests for ThemeContext and ThemeProvider.
 *
 * @since PR-THEME-1
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderHook, act } from '@testing-library/react';
import { ThemeProvider, useTheme } from '../ThemeContext';
import type { ThemeConfig } from '@/lib/theme/types';
import { DEFAULT_THEME } from '@/lib/theme/defaultTheme';

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(global, 'localStorage', {
  value: localStorageMock,
});

// Mock theme utils
vi.mock('@/lib/theme/utils', () => ({
  applyThemeVariables: vi.fn(),
  removeThemeVariables: vi.fn(),
}));

describe('ThemeContext', () => {
  beforeEach(() => {
    localStorageMock.clear();
    vi.clearAllMocks();
  });

  describe('ThemeProvider', () => {
    it('should render children', () => {
      render(
        <ThemeProvider>
          <div>Test Content</div>
        </ThemeProvider>
      );

      expect(screen.getByText('Test Content')).toBeInTheDocument();
    });

    it('should provide default theme initially', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      expect(result.current.theme).toEqual(DEFAULT_THEME);
    });

    it('should load theme from localStorage if available', () => {
      const customTheme: ThemeConfig = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626', // red
        },
      };

      localStorageMock.setItem('kiteclass_theme', JSON.stringify(customTheme));

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      expect(result.current.theme.colors.primary).toBe('#DC2626');
    });

    it('should use default theme if localStorage has invalid data', () => {
      localStorageMock.setItem('kiteclass_theme', 'invalid json');

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      expect(result.current.theme).toEqual(DEFAULT_THEME);
    });

    it('should load partial theme from localStorage (only colors.primary needed)', () => {
      const partialTheme = {
        colors: {
          primary: '#DC2626',
        },
      };

      localStorageMock.setItem('kiteclass_theme', JSON.stringify(partialTheme));

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Partial theme is valid, should be loaded
      expect(result.current.theme.colors.primary).toBe('#DC2626');
    });
  });

  describe('useTheme hook', () => {
    it('should provide theme state', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      expect(result.current.theme).toBeDefined();
      expect(result.current.setTheme).toBeDefined();
      expect(result.current.resetTheme).toBeDefined();
      expect(result.current.isDefaultTheme).toBeDefined();
    });

    it('should update theme when setTheme is called', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      const newTheme: ThemeConfig = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626',
        },
      };

      act(() => {
        result.current.setTheme(newTheme);
      });

      expect(result.current.theme.colors.primary).toBe('#DC2626');
    });

    it('should persist theme to localStorage when setTheme is called', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      const newTheme: ThemeConfig = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626',
        },
      };

      act(() => {
        result.current.setTheme(newTheme);
      });

      const stored = localStorageMock.getItem('kiteclass_theme');
      expect(stored).toBeDefined();

      const parsed = JSON.parse(stored!);
      expect(parsed.colors.primary).toBe('#DC2626');
    });

    it('should reset to default theme when resetTheme is called', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Set custom theme
      const customTheme: ThemeConfig = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626',
        },
      };

      act(() => {
        result.current.setTheme(customTheme);
      });

      expect(result.current.theme.colors.primary).toBe('#DC2626');

      // Reset to default
      act(() => {
        result.current.resetTheme();
      });

      expect(result.current.theme).toEqual(DEFAULT_THEME);
    });

    it('should remove theme from localStorage when resetTheme is called', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Set custom theme
      const customTheme: ThemeConfig = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626',
        },
      };

      act(() => {
        result.current.setTheme(customTheme);
      });

      expect(localStorageMock.getItem('kiteclass_theme')).toBeDefined();

      // Reset
      act(() => {
        result.current.resetTheme();
      });

      expect(localStorageMock.getItem('kiteclass_theme')).toBeNull();
    });

    it('should correctly identify default theme', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Initially should be default
      expect(result.current.isDefaultTheme).toBe(true);

      // Set custom theme
      const customTheme: ThemeConfig = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626',
        },
      };

      act(() => {
        result.current.setTheme(customTheme);
      });

      expect(result.current.isDefaultTheme).toBe(false);

      // Reset to default
      act(() => {
        result.current.resetTheme();
      });

      expect(result.current.isDefaultTheme).toBe(true);
    });

    it('should throw error when used outside ThemeProvider', () => {
      // Suppress console.error for this test
      const originalError = console.error;
      console.error = vi.fn();

      expect(() => {
        renderHook(() => useTheme());
      }).toThrow('useTheme must be used within a ThemeProvider');

      console.error = originalError;
    });
  });

  describe('SSR compatibility', () => {
    it('should handle missing localStorage gracefully', () => {
      // Remove localStorage
      const originalLocalStorage = global.localStorage;
      // @ts-expect-error - intentionally removing localStorage for test
      delete global.localStorage;

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Should still provide default theme
      expect(result.current.theme).toEqual(DEFAULT_THEME);

      // Restore localStorage
      Object.defineProperty(global, 'localStorage', {
        value: originalLocalStorage,
        writable: true,
      });
    });
  });
});
