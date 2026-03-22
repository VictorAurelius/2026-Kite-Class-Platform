/**
 * Complete theme configuration from AI branding service.
 * Matches backend ThemeConfig DTO structure.
 */
export interface ThemeConfig {
  colors: ColorScheme;
  typography: Typography;
  spacing: Spacing;
  layout: Layout;
}

/**
 * Color scheme with variants for each color family.
 */
export interface ColorScheme {
  primary: ColorVariants;
  secondary: ColorVariants;
  accent: ColorVariants;
  neutral: ColorVariants;
  semantic: SemanticColors;
}

/**
 * Color variants (50-900 shades) for a single color family.
 */
export interface ColorVariants {
  shade50: string;
  shade100: string;
  shade200: string;
  shade300: string;
  shade400: string;
  shade500: string;  // Base color
  shade600: string;
  shade700: string;
  shade800: string;
  shade900: string;
}

/**
 * Semantic colors for UI states.
 */
export interface SemanticColors {
  success: string;
  warning: string;
  error: string;
  info: string;
}

/**
 * Typography configuration.
 */
export interface Typography {
  fontFamilyHeading: string;
  fontFamilyBody: string;
  fontSizeBase: string;
  fontSizes: FontSizes;
  fontWeights: FontWeights;
  lineHeights: LineHeights;
}

/**
 * Font size scale.
 */
export interface FontSizes {
  xs: string;
  sm: string;
  base: string;
  lg: string;
  xl: string;
  xl2: string;
  xl3: string;
  xl4: string;
}

/**
 * Font weight scale.
 */
export interface FontWeights {
  normal: number;
  medium: number;
  semibold: number;
  bold: number;
}

/**
 * Line height scale.
 */
export interface LineHeights {
  tight: string;
  normal: string;
  relaxed: string;
}

/**
 * Spacing configuration.
 */
export interface Spacing {
  unit: number;
  sectionSpacing: string;
  componentSpacing: string;
  elementSpacing: string;
}

/**
 * Layout configuration.
 */
export interface Layout {
  maxWidth: string;
  borderRadius: BorderRadius;
  shadow: Shadow;
}

/**
 * Border radius scale.
 */
export interface BorderRadius {
  sm: string;
  base: string;
  lg: string;
  full: string;
}

/**
 * Shadow scale.
 */
export interface Shadow {
  sm: string;
  base: string;
  lg: string;
}
