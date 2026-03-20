/**
 * Tests for theme utility functions.
 *
 * @since PR-THEME-1
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  applyThemeVariables,
  removeThemeVariables,
  getThemeVariable,
  themeToCSS,
} from '../utils';
import type { ThemeConfig } from '../types';
import { DEFAULT_THEME } from '../defaultTheme';

// Mock document.documentElement for SSR-safe testing
const mockDocumentElement = {
  style: {
    setProperty: vi.fn(),
    removeProperty: vi.fn(),
    getPropertyValue: vi.fn(),
  },
};

describe('Theme Utils', () => {
  beforeEach(() => {
    // Reset mocks before each test
    vi.clearAllMocks();

    // Mock document.documentElement
    Object.defineProperty(global, 'document', {
      writable: true,
      value: {
        documentElement: mockDocumentElement,
      },
    });
  });

  describe('themeToCSS', () => {
    it('should convert ThemeConfig to CSS variable object', () => {
      const theme: ThemeConfig = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: {
          heading: 'Inter',
          body: 'Inter',
        },
        borderRadius: '12px',
        shadows: {
          sm: '0 1px 2px rgba(0,0,0,0.05)',
          md: '0 4px 6px rgba(0,0,0,0.07)',
          lg: '0 10px 15px rgba(0,0,0,0.1)',
        },
      };

      const cssVars = themeToCSS(theme);

      expect(cssVars).toEqual({
        '--theme-primary': '30 64 175',
        '--theme-secondary': '59 130 246',
        '--theme-accent': '245 158 11',
        '--theme-background': '255 255 255',
        '--theme-font-heading': 'Inter',
        '--theme-font-body': 'Inter',
        '--theme-border-radius': '12px',
        '--theme-shadow-sm': '0 1px 2px rgba(0,0,0,0.05)',
        '--theme-shadow-md': '0 4px 6px rgba(0,0,0,0.07)',
        '--theme-shadow-lg': '0 10px 15px rgba(0,0,0,0.1)',
      });
    });

    it('should handle default theme correctly', () => {
      const cssVars = themeToCSS(DEFAULT_THEME);

      expect(cssVars['--theme-primary']).toBe('59 130 246');
      expect(cssVars['--theme-secondary']).toBe('139 92 246');
      expect(cssVars['--theme-accent']).toBe('245 158 11');
      expect(cssVars['--theme-background']).toBe('255 255 255');
    });
  });

  describe('applyThemeVariables', () => {
    it('should apply theme to document root', () => {
      const theme: ThemeConfig = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: {
          heading: 'Inter',
          body: 'Inter',
        },
        borderRadius: '12px',
        shadows: {
          sm: '0 1px 2px rgba(0,0,0,0.05)',
          md: '0 4px 6px rgba(0,0,0,0.07)',
          lg: '0 10px 15px rgba(0,0,0,0.1)',
        },
      };

      applyThemeVariables(theme);

      // Check that setProperty was called for each variable
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-primary',
        '30 64 175'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-secondary',
        '59 130 246'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-accent',
        '245 158 11'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-background',
        '255 255 255'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-font-heading',
        'Inter'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-font-body',
        'Inter'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-border-radius',
        '12px'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-shadow-sm',
        '0 1px 2px rgba(0,0,0,0.05)'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-shadow-md',
        '0 4px 6px rgba(0,0,0,0.07)'
      );
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-shadow-lg',
        '0 10px 15px rgba(0,0,0,0.1)'
      );

      // Should be called exactly 10 times (10 CSS variables)
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledTimes(10);
    });

    it('should handle SSR gracefully (no document)', () => {
      // Remove document
      Object.defineProperty(global, 'document', {
        writable: true,
        value: undefined,
      });

      const theme = DEFAULT_THEME;

      // Should not throw error
      expect(() => applyThemeVariables(theme)).not.toThrow();
    });

    it('should apply default theme', () => {
      applyThemeVariables(DEFAULT_THEME);

      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-primary',
        '59 130 246'
      );
    });
  });

  describe('removeThemeVariables', () => {
    it('should remove all theme variables', () => {
      removeThemeVariables();

      // Check that removeProperty was called for each variable
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-primary'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-secondary'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-accent'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-background'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-font-heading'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-font-body'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-border-radius'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-shadow-sm'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-shadow-md'
      );
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledWith(
        '--theme-shadow-lg'
      );

      // Should be called exactly 10 times
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledTimes(10);
    });

    it('should handle SSR gracefully (no document)', () => {
      // Remove document
      Object.defineProperty(global, 'document', {
        writable: true,
        value: undefined,
      });

      // Should not throw error
      expect(() => removeThemeVariables()).not.toThrow();
    });
  });

  describe('getThemeVariable', () => {
    it('should get theme variable value', () => {
      mockDocumentElement.style.getPropertyValue.mockReturnValue('59 130 246');

      const value = getThemeVariable('--theme-primary');

      expect(mockDocumentElement.style.getPropertyValue).toHaveBeenCalledWith(
        '--theme-primary'
      );
      expect(value).toBe('59 130 246');
    });

    it('should return empty string if variable not set', () => {
      mockDocumentElement.style.getPropertyValue.mockReturnValue('');

      const value = getThemeVariable('--theme-nonexistent');

      expect(value).toBe('');
    });

    it('should handle SSR gracefully (no document)', () => {
      // Remove document
      Object.defineProperty(global, 'document', {
        writable: true,
        value: undefined,
      });

      // Should return empty string
      const value = getThemeVariable('--theme-primary');
      expect(value).toBe('');
    });

    it('should trim whitespace from returned value', () => {
      mockDocumentElement.style.getPropertyValue.mockReturnValue('  59 130 246  ');

      const value = getThemeVariable('--theme-primary');

      expect(value).toBe('59 130 246');
    });
  });

  describe('Integration scenarios', () => {
    it('should apply, read, and remove theme correctly', () => {
      const theme: ThemeConfig = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: {
          heading: 'Inter',
          body: 'Inter',
        },
        borderRadius: '12px',
        shadows: {
          sm: '0 1px 2px rgba(0,0,0,0.05)',
          md: '0 4px 6px rgba(0,0,0,0.07)',
          lg: '0 10px 15px rgba(0,0,0,0.1)',
        },
      };

      // Apply theme
      applyThemeVariables(theme);
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledTimes(10);

      // Simulate reading a variable
      mockDocumentElement.style.getPropertyValue.mockReturnValue('30 64 175');
      const primary = getThemeVariable('--theme-primary');
      expect(primary).toBe('30 64 175');

      // Remove theme
      removeThemeVariables();
      expect(mockDocumentElement.style.removeProperty).toHaveBeenCalledTimes(10);
    });

    it('should handle theme updates (apply twice)', () => {
      const theme1 = DEFAULT_THEME;
      const theme2: ThemeConfig = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626', // red
        },
      };

      // Apply first theme
      applyThemeVariables(theme1);
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-primary',
        '59 130 246'
      );

      // Clear mock
      vi.clearAllMocks();

      // Apply second theme (should overwrite)
      applyThemeVariables(theme2);
      expect(mockDocumentElement.style.setProperty).toHaveBeenCalledWith(
        '--theme-primary',
        '220 38 38'
      );
    });
  });
});
