/**
 * Tests for default theme configuration.
 *
 * @since PR-THEME-1
 */

import { describe, it, expect } from 'vitest';
import { DEFAULT_THEME, THEME_CSS_VARS, isValidCSSColor } from '../defaultTheme';
import { isThemeConfig } from '../types';

describe('Default Theme', () => {
  describe('DEFAULT_THEME', () => {
    it('should be a valid ThemeConfig', () => {
      expect(isThemeConfig(DEFAULT_THEME)).toBe(true);
    });

    it('should have all required color properties', () => {
      expect(DEFAULT_THEME.colors).toBeDefined();
      expect(DEFAULT_THEME.colors.primary).toBeDefined();
      expect(DEFAULT_THEME.colors.secondary).toBeDefined();
      expect(DEFAULT_THEME.colors.accent).toBeDefined();
      expect(DEFAULT_THEME.colors.background).toBeDefined();
    });

    it('should have all required font properties', () => {
      expect(DEFAULT_THEME.fonts).toBeDefined();
      expect(DEFAULT_THEME.fonts.heading).toBeDefined();
      expect(DEFAULT_THEME.fonts.body).toBeDefined();
    });

    it('should have borderRadius property', () => {
      expect(DEFAULT_THEME.borderRadius).toBeDefined();
      expect(typeof DEFAULT_THEME.borderRadius).toBe('string');
    });

    it('should have all shadow properties', () => {
      expect(DEFAULT_THEME.shadows).toBeDefined();
      expect(DEFAULT_THEME.shadows.sm).toBeDefined();
      expect(DEFAULT_THEME.shadows.md).toBeDefined();
      expect(DEFAULT_THEME.shadows.lg).toBeDefined();
    });

    it('should use valid CSS hex colors', () => {
      expect(isValidCSSColor(DEFAULT_THEME.colors.primary)).toBe(true);
      expect(isValidCSSColor(DEFAULT_THEME.colors.secondary)).toBe(true);
      expect(isValidCSSColor(DEFAULT_THEME.colors.accent)).toBe(true);
      expect(isValidCSSColor(DEFAULT_THEME.colors.background)).toBe(true);
    });

    it('should use Inter font family', () => {
      expect(DEFAULT_THEME.fonts.heading).toContain('Inter');
      expect(DEFAULT_THEME.fonts.body).toContain('Inter');
    });

    it('should have moderate border radius', () => {
      // Border radius should be a valid CSS value
      expect(DEFAULT_THEME.borderRadius).toMatch(/^\d+px$/);
    });

    it('should have valid box-shadow values', () => {
      // Shadows should contain rgba and px values
      expect(DEFAULT_THEME.shadows.sm).toContain('rgba');
      expect(DEFAULT_THEME.shadows.md).toContain('rgba');
      expect(DEFAULT_THEME.shadows.lg).toContain('rgba');
    });

    it('should use professional, accessible colors', () => {
      // Primary should be blue (Tailwind blue-500)
      expect(DEFAULT_THEME.colors.primary).toBe('#3B82F6');

      // Secondary should be violet (Tailwind violet-500)
      expect(DEFAULT_THEME.colors.secondary).toBe('#8B5CF6');

      // Accent should be amber (Tailwind amber-500)
      expect(DEFAULT_THEME.colors.accent).toBe('#F59E0B');

      // Background should be white
      expect(DEFAULT_THEME.colors.background).toBe('#FFFFFF');
    });
  });

  describe('THEME_CSS_VARS', () => {
    it('should define all CSS variable names', () => {
      expect(THEME_CSS_VARS.PRIMARY).toBe('--theme-primary');
      expect(THEME_CSS_VARS.SECONDARY).toBe('--theme-secondary');
      expect(THEME_CSS_VARS.ACCENT).toBe('--theme-accent');
      expect(THEME_CSS_VARS.BACKGROUND).toBe('--theme-background');
      expect(THEME_CSS_VARS.FONT_HEADING).toBe('--theme-font-heading');
      expect(THEME_CSS_VARS.FONT_BODY).toBe('--theme-font-body');
      expect(THEME_CSS_VARS.BORDER_RADIUS).toBe('--theme-border-radius');
      expect(THEME_CSS_VARS.SHADOW_SM).toBe('--theme-shadow-sm');
      expect(THEME_CSS_VARS.SHADOW_MD).toBe('--theme-shadow-md');
      expect(THEME_CSS_VARS.SHADOW_LG).toBe('--theme-shadow-lg');
    });

    it('should use consistent naming convention', () => {
      const varNames = Object.values(THEME_CSS_VARS);

      varNames.forEach((varName) => {
        // All should start with --theme-
        expect(varName).toMatch(/^--theme-/);
        // All should be kebab-case
        expect(varName).toMatch(/^--theme-[a-z-]+$/);
      });
    });
  });

  describe('isValidCSSColor', () => {
    describe('valid colors', () => {
      it('should validate 6-digit hex colors', () => {
        expect(isValidCSSColor('#3B82F6')).toBe(true);
        expect(isValidCSSColor('#FFFFFF')).toBe(true);
        expect(isValidCSSColor('#000000')).toBe(true);
        expect(isValidCSSColor('#abc123')).toBe(true);
      });

      it('should validate 3-digit hex colors', () => {
        expect(isValidCSSColor('#FFF')).toBe(true);
        expect(isValidCSSColor('#000')).toBe(true);
        expect(isValidCSSColor('#abc')).toBe(true);
      });

      it('should validate 8-digit hex colors with alpha', () => {
        expect(isValidCSSColor('#3B82F6FF')).toBe(true);
        expect(isValidCSSColor('#00000080')).toBe(true);
      });

      it('should validate rgb() colors', () => {
        expect(isValidCSSColor('rgb(59, 130, 246)')).toBe(true);
        expect(isValidCSSColor('rgb(255,255,255)')).toBe(true);
        expect(isValidCSSColor('rgb(0, 0, 0)')).toBe(true);
      });

      it('should validate rgba() colors', () => {
        expect(isValidCSSColor('rgba(59, 130, 246, 1)')).toBe(true);
        expect(isValidCSSColor('rgba(0,0,0,0.5)')).toBe(true);
        expect(isValidCSSColor('rgba(255, 255, 255, 0.8)')).toBe(true);
      });

      it('should validate hsl() colors', () => {
        expect(isValidCSSColor('hsl(217, 91%, 60%)')).toBe(true);
        expect(isValidCSSColor('hsl(0,0%,100%)')).toBe(true);
      });

      it('should validate hsla() colors', () => {
        expect(isValidCSSColor('hsla(217, 91%, 60%, 1)')).toBe(true);
        expect(isValidCSSColor('hsla(0,0%,0%,0.5)')).toBe(true);
      });

      it('should validate named colors', () => {
        expect(isValidCSSColor('red')).toBe(true);
        expect(isValidCSSColor('blue')).toBe(true);
        expect(isValidCSSColor('transparent')).toBe(true);
        expect(isValidCSSColor('white')).toBe(true);
      });
    });

    describe('invalid colors', () => {
      it('should reject invalid hex colors', () => {
        expect(isValidCSSColor('#GGG')).toBe(false);
        expect(isValidCSSColor('#12')).toBe(false);
        expect(isValidCSSColor('3B82F6')).toBe(false); // missing #
        expect(isValidCSSColor('#3B82F')).toBe(false); // wrong length
      });

      it('should reject invalid rgb/rgba', () => {
        expect(isValidCSSColor('rgb(300, 130, 246)')).toBe(false); // out of range
        expect(isValidCSSColor('rgb(59 130 246)')).toBe(false); // missing commas
        expect(isValidCSSColor('rgba(59, 130, 246)')).toBe(false); // missing alpha
      });

      it('should reject non-color strings', () => {
        expect(isValidCSSColor('not-a-color')).toBe(false);
        expect(isValidCSSColor('123px')).toBe(false);
        expect(isValidCSSColor('')).toBe(false);
      });

      it('should reject numbers', () => {
        expect(isValidCSSColor('123' as string)).toBe(false);
      });
    });
  });
});
