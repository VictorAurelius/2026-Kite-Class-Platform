/**
 * Tests for theme type definitions and type guards.
 *
 * @since PR-THEME-1
 */

import { describe, it, expect } from 'vitest';
import type { ThemeConfig, ThemeMessage } from '../types';
import { isThemeConfig, isThemeMessage } from '../types';

describe('Theme Types', () => {
  describe('isThemeConfig', () => {
    it('should return true for valid ThemeConfig', () => {
      const validTheme: ThemeConfig = {
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
        borderRadius: '8px',
        shadows: {
          sm: '0 1px 2px rgba(0,0,0,0.05)',
          md: '0 4px 6px rgba(0,0,0,0.07)',
          lg: '0 10px 15px rgba(0,0,0,0.1)',
        },
      };

      expect(isThemeConfig(validTheme)).toBe(true);
    });

    it('should return false for null', () => {
      expect(isThemeConfig(null)).toBe(false);
    });

    it('should return false for undefined', () => {
      expect(isThemeConfig(undefined)).toBe(false);
    });

    it('should return false for non-object', () => {
      expect(isThemeConfig('not an object')).toBe(false);
      expect(isThemeConfig(123)).toBe(false);
      expect(isThemeConfig(true)).toBe(false);
    });

    it('should return false when colors are missing', () => {
      const invalid = {
        fonts: { heading: 'Inter', body: 'Inter' },
        borderRadius: '8px',
        shadows: { sm: '', md: '', lg: '' },
      };

      expect(isThemeConfig(invalid)).toBe(false);
    });

    it('should return true when colors are partial (only primary required)', () => {
      const partial = {
        colors: {
          primary: '#1E40AF',
          // missing secondary, accent, background → OK, merged with defaults
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        borderRadius: '8px',
        shadows: { sm: '', md: '', lg: '' },
      };

      expect(isThemeConfig(partial)).toBe(true);
    });

    it('should return true when fonts are missing (partial config)', () => {
      const partial = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        borderRadius: '8px',
        shadows: { sm: '', md: '', lg: '' },
      };

      // Only colors.primary required, fonts optional
      expect(isThemeConfig(partial)).toBe(true);
    });

    it('should return true for partial config (only colors.primary required)', () => {
      const partial = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        shadows: { sm: '', md: '', lg: '' },
      };

      // borderRadius missing but colors.primary present → valid
      expect(isThemeConfig(partial)).toBe(true);
    });

    it('should return true when shadows are missing (partial config)', () => {
      const partial = {
        colors: {
          primary: '#1E40AF',
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        borderRadius: '8px',
      };

      // Only colors.primary required
      expect(isThemeConfig(partial)).toBe(true);
    });

    it('should return true when shadows are incomplete (partial config)', () => {
      const partial = {
        colors: {
          primary: '#1E40AF',
        },
      };

      // Minimal valid config
      expect(isThemeConfig(partial)).toBe(true);
    });

    it('should return false when color values are not strings', () => {
      const invalid = {
        colors: {
          primary: 123, // number instead of string
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        borderRadius: '8px',
        shadows: { sm: '', md: '', lg: '' },
      };

      expect(isThemeConfig(invalid)).toBe(false);
    });
  });

  describe('isThemeMessage', () => {
    it('should return true for valid ThemeMessage', () => {
      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: {
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
          borderRadius: '8px',
          shadows: {
            sm: '0 1px 2px rgba(0,0,0,0.05)',
            md: '0 4px 6px rgba(0,0,0,0.07)',
            lg: '0 10px 15px rgba(0,0,0,0.1)',
          },
        },
      };

      expect(isThemeMessage(validMessage)).toBe(true);
    });

    it('should return false for null', () => {
      expect(isThemeMessage(null)).toBe(false);
    });

    it('should return false when type is not APPLY_THEME', () => {
      const invalid = {
        type: 'WRONG_TYPE',
        theme: {
          colors: {
            primary: '#1E40AF',
            secondary: '#3B82F6',
            accent: '#F59E0B',
            background: '#FFFFFF',
          },
          fonts: { heading: 'Inter', body: 'Inter' },
          borderRadius: '8px',
          shadows: { sm: '', md: '', lg: '' },
        },
      };

      expect(isThemeMessage(invalid)).toBe(false);
    });

    it('should return true for partial theme (only colors.primary)', () => {
      const partial = {
        type: 'APPLY_THEME',
        theme: {
          colors: { primary: '#1E40AF' }, // minimal valid
        },
      };

      // colors.primary is sufficient for valid theme
      expect(isThemeMessage(partial)).toBe(true);
    });

    it('should return false when theme is missing', () => {
      const invalid = {
        type: 'APPLY_THEME',
        // no theme property
      };

      expect(isThemeMessage(invalid)).toBe(false);
    });
  });

  describe('TypeScript compilation', () => {
    it('should allow valid ThemeConfig assignment', () => {
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

      expect(theme).toBeDefined();
      expect(theme.colors.primary).toBe('#1E40AF');
    });

    it('should allow valid ThemeMessage assignment', () => {
      const message: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: {
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
        },
      };

      expect(message).toBeDefined();
      expect(message.type).toBe('APPLY_THEME');
    });
  });
});
