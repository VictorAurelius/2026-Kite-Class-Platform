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

    it('should return false when colors are incomplete', () => {
      const invalid = {
        colors: {
          primary: '#1E40AF',
          // missing secondary, accent, background
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        borderRadius: '8px',
        shadows: { sm: '', md: '', lg: '' },
      };

      expect(isThemeConfig(invalid)).toBe(false);
    });

    it('should return false when fonts are missing', () => {
      const invalid = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        borderRadius: '8px',
        shadows: { sm: '', md: '', lg: '' },
      };

      expect(isThemeConfig(invalid)).toBe(false);
    });

    it('should return false when borderRadius is missing', () => {
      const invalid = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        shadows: { sm: '', md: '', lg: '' },
      };

      expect(isThemeConfig(invalid)).toBe(false);
    });

    it('should return false when shadows are missing', () => {
      const invalid = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        borderRadius: '8px',
      };

      expect(isThemeConfig(invalid)).toBe(false);
    });

    it('should return false when shadows are incomplete', () => {
      const invalid = {
        colors: {
          primary: '#1E40AF',
          secondary: '#3B82F6',
          accent: '#F59E0B',
          background: '#FFFFFF',
        },
        fonts: { heading: 'Inter', body: 'Inter' },
        borderRadius: '8px',
        shadows: {
          sm: '0 1px 2px rgba(0,0,0,0.05)',
          // missing md, lg
        },
      };

      expect(isThemeConfig(invalid)).toBe(false);
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

    it('should return false when theme is invalid', () => {
      const invalid = {
        type: 'APPLY_THEME',
        theme: {
          colors: { primary: '#1E40AF' }, // incomplete
        },
      };

      expect(isThemeMessage(invalid)).toBe(false);
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
