// ---------------------------------------------------------------------------
// paletteVariants — multi-variant palette derivation (GAP-1212 kit v3
// "AI tạo 3 biến thể — chọn 1"; design source
// `ui_kits/ai-branding-wizard-v2/v3/screens/generate-ready.html`).
//
// The backend currently resolves ONE brand palette per job (usePreviewBrandColors).
// To honour the kit's multi-variant pick affordance WITHOUT touching the BE
// pipeline (Bucket C closed), we derive 2 sibling palettes client-side from the
// base palette via deterministic HSL transforms.
//
// Variant A = base (the deploy-faithful palette — what the BE job resolved).
// Variant B = analogous hue shift (ngọc/teal feel).
// Variant C = bolder/darker primary (đậm feel).
//
// Picking a variant only re-themes the LIVE PREVIEW + regenerates the preview
// banner — deploy still uses the job's BE-resolved palette (variant A). Wiring
// a non-base selected variant through to deploy needs a BE palette-override
// (Bucket C scope) and is tracked as a follow-up gap.
// ---------------------------------------------------------------------------

export interface BasePalette {
  primary: string;
  secondary: string;
  accent: string;
}

export interface PaletteVariant extends BasePalette {
  /** Stable id used as the React key + selection token. */
  id: string;
  /** Vietnamese label shown on the variant card (kit: "Bản A · Xanh"). */
  label: string;
}

const HEX_RE = /^#?[0-9A-Fa-f]{6}$/;

/** Normalise a hex colour to lowercase `#rrggbb`; returns `fallback` when invalid. */
function normHex(value: string | undefined | null, fallback: string): string {
  if (!value || !HEX_RE.test(value)) return fallback;
  const withHash = value.startsWith('#') ? value : `#${value}`;
  return withHash.toLowerCase();
}

function hexToHsl(hex: string): [number, number, number] {
  const r = parseInt(hex.slice(1, 3), 16) / 255;
  const g = parseInt(hex.slice(3, 5), 16) / 255;
  const b = parseInt(hex.slice(5, 7), 16) / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;
  let h = 0;
  let s = 0;
  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    switch (max) {
      case r:
        h = (g - b) / d + (g < b ? 6 : 0);
        break;
      case g:
        h = (b - r) / d + 2;
        break;
      default:
        h = (r - g) / d + 4;
    }
    h /= 6;
  }
  return [h * 360, s, l];
}

function hslToHex(h: number, s: number, l: number): string {
  const hue = (((h % 360) + 360) % 360) / 360;
  const sat = Math.min(1, Math.max(0, s));
  const lig = Math.min(1, Math.max(0, l));
  const hue2rgb = (p: number, q: number, t: number) => {
    let tt = t;
    if (tt < 0) tt += 1;
    if (tt > 1) tt -= 1;
    if (tt < 1 / 6) return p + (q - p) * 6 * tt;
    if (tt < 1 / 2) return q;
    if (tt < 2 / 3) return p + (q - p) * (2 / 3 - tt) * 6;
    return p;
  };
  let r: number;
  let g: number;
  let b: number;
  if (sat === 0) {
    r = g = b = lig;
  } else {
    const q = lig < 0.5 ? lig * (1 + sat) : lig + sat - lig * sat;
    const p = 2 * lig - q;
    r = hue2rgb(p, q, hue + 1 / 3);
    g = hue2rgb(p, q, hue);
    b = hue2rgb(p, q, hue - 1 / 3);
  }
  const toHex = (x: number) =>
    Math.round(x * 255)
      .toString(16)
      .padStart(2, '0');
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

/** Rotate hue by `deg` degrees, preserving saturation + lightness. */
function shiftHue(hex: string, deg: number): string {
  const [h, s, l] = hexToHsl(hex);
  return hslToHex(h + deg, s, l);
}

/** Adjust lightness by `delta` (clamped), preserving hue + saturation. */
function adjustLightness(hex: string, delta: number): string {
  const [h, s, l] = hexToHsl(hex);
  return hslToHex(h, s, l + delta);
}

const FALLBACK: BasePalette = {
  primary: '#1E40AF',
  secondary: '#F59E0B',
  accent: '#F59E0B',
};

/** Exactly 3 variants — tuple type so `[0]` is non-undefined under strict TS. */
export type PaletteVariantTriple = [PaletteVariant, PaletteVariant, PaletteVariant];

/**
 * Derive 3 palette variants from the BE-resolved base palette.
 *
 * Deterministic: same base → same variants (stable across re-renders + tests).
 */
export function buildPaletteVariants(base: BasePalette): PaletteVariantTriple {
  const primary = normHex(base?.primary, FALLBACK.primary);
  const secondary = normHex(base?.secondary, FALLBACK.secondary);
  const accent = normHex(base?.accent, FALLBACK.accent);

  return [
    {
      id: 'variant-a',
      label: 'Bản A · Gốc',
      primary,
      secondary,
      accent,
    },
    {
      id: 'variant-b',
      label: 'Bản B · Ngọc',
      // Analogous shift toward teal/emerald.
      primary: shiftHue(primary, 28),
      secondary: shiftHue(secondary, 18),
      accent: shiftHue(accent, 24),
    },
    {
      id: 'variant-c',
      label: 'Bản C · Đậm',
      // Bolder/darker primary, keep accent vivid.
      primary: adjustLightness(primary, -0.12),
      secondary: shiftHue(secondary, -16),
      accent,
    },
  ];
}

export default buildPaletteVariants;
