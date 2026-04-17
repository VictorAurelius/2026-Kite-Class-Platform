/**
 * Theme System Type Definitions
 *
 * Defines TypeScript types for KiteClass theme configuration.
 * These types match the backend ThemeConfig JSON structure from the design doc.
 *
 * @see documents/03-planning/kiteclass-theme-system-design.md
 * @since PR-THEME-1
 */

/**
 * Theme color palette.
 * All colors should be valid CSS color values (hex, rgb, hsl, etc.)
 */
export interface ThemeColors {
  /** Primary brand color - used for CTAs, links, primary UI elements */
  primary: string;
  /** Secondary brand color - used for accents, highlights */
  secondary: string;
  /** Accent color - used for special emphasis, warnings, highlights */
  accent: string;
  /** Background color - main page background */
  background: string;
}

/**
 * Theme typography configuration.
 * Font names should match Google Fonts or system font names.
 */
export interface ThemeFonts {
  /** Font family for headings (h1-h6) */
  heading: string;
  /** Font family for body text and UI */
  body: string;
}

/**
 * Theme shadow definitions for elevation.
 * Values should be valid CSS box-shadow values.
 */
export interface ThemeShadows {
  /** Small shadow - subtle elevation */
  sm: string;
  /** Medium shadow - card elevation */
  md: string;
  /** Large shadow - modal/popup elevation */
  lg: string;
}

/**
 * Complete theme configuration.
 * This structure matches the JSON stored in the backend database.
 *
 * @example
 * ```typescript
 * const theme: ThemeConfig = {
 *   colors: {
 *     primary: '#1E40AF',
 *     secondary: '#3B82F6',
 *     accent: '#F59E0B',
 *     background: '#FFFBF5'
 *   },
 *   fonts: {
 *     heading: 'Inter',
 *     body: 'Inter'
 *   },
 *   borderRadius: '12px',
 *   shadows: {
 *     sm: '0 1px 2px rgba(0,0,0,0.05)',
 *     md: '0 4px 6px rgba(0,0,0,0.07)',
 *     lg: '0 10px 15px rgba(0,0,0,0.1)'
 *   }
 * };
 * ```
 */
export interface ThemeConfig {
  /** Theme color palette */
  colors: ThemeColors;
  /** Typography configuration */
  fonts: ThemeFonts;
  /** Border radius for UI elements (e.g., '8px', '12px') */
  borderRadius: string;
  /** Shadow definitions for elevation */
  shadows: ThemeShadows;
}

/**
 * Theme variant metadata.
 * Used when AI generates multiple theme options from a single logo.
 */
export interface ThemeVariant {
  /** Variant name (e.g., "Chuyên nghiệp", "Ấm áp", "Hiện đại") */
  name: string;
  /** Mood/personality of this variant */
  mood: string;
  /** The theme configuration for this variant */
  config: ThemeConfig;
}

/**
 * postMessage payload for theme updates.
 * Sent from KiteHub parent window to KiteClass iframe for live preview.
 */
export interface ThemeMessage {
  /** Message type identifier */
  type: 'APPLY_THEME';
  /** Theme configuration to apply */
  theme: ThemeConfig;
}

/**
 * Type guard to check if a value is a valid ThemeConfig.
 *
 * @param value - Value to check
 * @returns True if value matches ThemeConfig structure
 */
export function isThemeConfig(value: unknown): value is ThemeConfig {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const obj = value as Record<string, unknown>;

  // Only require colors.primary as minimum valid theme
  // Other fields are optional and will use defaults
  if (
    typeof obj.colors !== 'object' ||
    obj.colors === null ||
    typeof (obj.colors as Record<string, unknown>).primary !== 'string'
  ) {
    return false;
  }

  return true;
}

/**
 * Type guard to check if a message is a valid ThemeMessage.
 *
 * @param value - Value to check
 * @returns True if value matches ThemeMessage structure
 */
export function isThemeMessage(value: unknown): value is ThemeMessage {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const obj = value as Record<string, unknown>;

  return obj.type === 'APPLY_THEME' && isThemeConfig(obj.theme);
}
