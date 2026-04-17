'use client';

import { Card } from '@/components/ui/card';
import { Palette, Type, Maximize2 } from 'lucide-react';
import { ThemeConfig, ColorVariants } from '@/types/theme';

interface ThemePreviewCardProps {
  themeConfig: ThemeConfig;
}

/**
 * Preview card showing AI-generated theme configuration.
 * Displays color palette, typography samples, and spacing/layout info.
 */
export function ThemePreviewCard({ themeConfig }: ThemePreviewCardProps) {
  const { colors, typography, spacing, layout } = themeConfig;

  return (
    <Card className="p-6">
      <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
        <Palette className="w-5 h-5" />
        Theme Preview
      </h3>

      {/* Color Palette */}
      <div className="space-y-4 mb-6">
        <h4 className="font-medium text-sm text-muted-foreground flex items-center gap-2">
          <Palette className="w-4 h-4" />
          Bảng Màu
        </h4>

        <div className="space-y-3">
          {/* Primary Colors */}
          <div>
            <p className="text-xs font-medium mb-1.5 text-muted-foreground">Màu Chính</p>
            <ColorVariantRow variants={colors.primary} label="Primary" />
          </div>

          {/* Secondary Colors */}
          <div>
            <p className="text-xs font-medium mb-1.5 text-muted-foreground">Màu Phụ</p>
            <ColorVariantRow variants={colors.secondary} label="Secondary" />
          </div>

          {/* Accent Colors */}
          <div>
            <p className="text-xs font-medium mb-1.5 text-muted-foreground">Màu Nhấn</p>
            <ColorVariantRow variants={colors.accent} label="Accent" />
          </div>

          {/* Semantic Colors */}
          <div>
            <p className="text-xs font-medium mb-1.5 text-muted-foreground">Màu Hệ Thống</p>
            <div className="flex gap-2 flex-wrap">
              <ColorSwatch color={colors.semantic.success} label="Success" />
              <ColorSwatch color={colors.semantic.warning} label="Warning" />
              <ColorSwatch color={colors.semantic.error} label="Error" />
              <ColorSwatch color={colors.semantic.info} label="Info" />
            </div>
          </div>
        </div>
      </div>

      {/* Typography */}
      <div className="space-y-4 mb-6">
        <h4 className="font-medium text-sm text-muted-foreground flex items-center gap-2">
          <Type className="w-4 h-4" />
          Typography
        </h4>

        <div className="space-y-2">
          <div className="p-3 bg-muted/50 rounded-md">
            <p className="text-xs text-muted-foreground mb-1">Heading Font</p>
            <p className="font-semibold" style={{ fontFamily: typography.fontFamilyHeading }}>
              {typography.fontFamilyHeading}
            </p>
          </div>

          <div className="p-3 bg-muted/50 rounded-md">
            <p className="text-xs text-muted-foreground mb-1">Body Font</p>
            <p style={{ fontFamily: typography.fontFamilyBody }}>
              {typography.fontFamilyBody}
            </p>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="p-2 bg-muted/50 rounded-md text-center">
              <p className="text-xs text-muted-foreground">Base Size</p>
              <p className="font-medium text-sm">{typography.fontSizeBase}</p>
            </div>
            <div className="p-2 bg-muted/50 rounded-md text-center">
              <p className="text-xs text-muted-foreground">Line Height</p>
              <p className="font-medium text-sm">{typography.lineHeights.normal}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Spacing & Layout */}
      <div className="space-y-4">
        <h4 className="font-medium text-sm text-muted-foreground flex items-center gap-2">
          <Maximize2 className="w-4 h-4" />
          Spacing & Layout
        </h4>

        <div className="grid grid-cols-3 gap-2">
          <div className="p-2 bg-muted/50 rounded-md text-center">
            <p className="text-xs text-muted-foreground">Unit</p>
            <p className="font-medium text-sm">{spacing.unit}px</p>
          </div>
          <div className="p-2 bg-muted/50 rounded-md text-center">
            <p className="text-xs text-muted-foreground">Max Width</p>
            <p className="font-medium text-sm">{layout.maxWidth}</p>
          </div>
          <div className="p-2 bg-muted/50 rounded-md text-center">
            <p className="text-xs text-muted-foreground">Border</p>
            <p className="font-medium text-sm">{layout.borderRadius.base}</p>
          </div>
        </div>
      </div>
    </Card>
  );
}

/**
 * Display a row of color variants (50-900 shades).
 */
function ColorVariantRow({ variants, label }: { variants: ColorVariants; label: string }) {
  const shades = [
    { key: 'shade50', value: variants.shade50, label: '50' },
    { key: 'shade100', value: variants.shade100, label: '100' },
    { key: 'shade200', value: variants.shade200, label: '200' },
    { key: 'shade300', value: variants.shade300, label: '300' },
    { key: 'shade400', value: variants.shade400, label: '400' },
    { key: 'shade500', value: variants.shade500, label: '500' },
    { key: 'shade600', value: variants.shade600, label: '600' },
    { key: 'shade700', value: variants.shade700, label: '700' },
    { key: 'shade800', value: variants.shade800, label: '800' },
    { key: 'shade900', value: variants.shade900, label: '900' },
  ];

  return (
    <div className="flex gap-1 overflow-x-auto pb-1">
      {shades.map((shade) => (
        <div key={shade.key} className="flex-shrink-0">
          <div
            className="w-10 h-10 rounded border border-border"
            style={{ backgroundColor: shade.value }}
            title={`${label} ${shade.label}: ${shade.value}`}
          />
          <p className="text-[10px] text-center mt-0.5 text-muted-foreground">{shade.label}</p>
        </div>
      ))}
    </div>
  );
}

/**
 * Display a single color swatch with label.
 */
function ColorSwatch({ color, label }: { color: string; label: string }) {
  return (
    <div className="flex items-center gap-2">
      <div
        className="w-8 h-8 rounded border border-border"
        style={{ backgroundColor: color }}
        title={`${label}: ${color}`}
      />
      <div>
        <p className="text-xs font-medium">{label}</p>
        <p className="text-[10px] text-muted-foreground">{color}</p>
      </div>
    </div>
  );
}
