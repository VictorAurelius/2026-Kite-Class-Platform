'use client';

/**
 * Lazy-loaded templates grid for `/branding/templates`.
 *
 * GAP-236 Sub-PR B (Wave GAP-236 Agent D) — splits the JSON-parsing color
 * preview cards out of the route's initial chunk. The page header + filter
 * chips paint immediately; this grid + its TemplateCard children stream in
 * once the data fetch resolves.
 */

import type { BrandingTemplate, ThemeConfig } from '@/types/branding';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { Palette, Check, LayoutGrid } from 'lucide-react';

const CATEGORY_LABELS: Record<string, string> = {
  all: 'Tất cả',
  education: 'Giáo dục',
  business: 'Doanh nghiệp',
  general: 'Tổng hợp',
};

interface TemplatesGridProps {
  templates: BrandingTemplate[];
  onApply: (template: BrandingTemplate) => void;
  isApplying: boolean;
}

export default function TemplatesGrid({ templates, onApply, isApplying }: TemplatesGridProps) {
  if (templates.length === 0) {
    return (
      <div className="text-center py-12">
        <LayoutGrid className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
        <p className="text-muted-foreground">Không có template nào trong danh mục này</p>
      </div>
    );
  }
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {templates.map((template) => (
        <TemplateCard
          key={template.id}
          template={template}
          onApply={onApply}
          isApplying={isApplying}
        />
      ))}
    </div>
  );
}

interface TemplateCardProps {
  template: BrandingTemplate;
  onApply: (template: BrandingTemplate) => void;
  isApplying: boolean;
}

function TemplateCard({ template, onApply, isApplying }: TemplateCardProps) {
  let themeConfig: ThemeConfig | null = null;
  try {
    themeConfig = JSON.parse(template.themeConfig) as ThemeConfig;
  } catch {
    // Invalid JSON, skip color preview
  }

  return (
    <Card className="overflow-hidden hover:shadow-lg transition-shadow">
      {/* Color Preview */}
      {themeConfig && (
        <div className="h-24 flex">
          <div
            className="flex-1"
            style={{ backgroundColor: themeConfig.colors.primary }}
          />
          <div
            className="flex-1"
            style={{ backgroundColor: themeConfig.colors.secondary }}
          />
          <div
            className="flex-1"
            style={{ backgroundColor: themeConfig.colors.accent }}
          />
        </div>
      )}

      <CardContent className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-lg">{template.name}</h3>
          <Badge variant="secondary">
            {CATEGORY_LABELS[template.category] || template.category}
          </Badge>
        </div>

        {themeConfig && (
          <div className="space-y-2 text-sm text-muted-foreground">
            <div className="flex items-center gap-2">
              <Palette className="h-4 w-4" />
              <span>Style: {themeConfig.style}</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="font-medium">Font:</span>
              <span>{themeConfig.fonts.heading}</span>
            </div>
            {/* Color swatches */}
            <div className="flex gap-1">
              {Object.entries(themeConfig.colors).map(([name, color]) => (
                <div
                  key={name}
                  className="w-6 h-6 rounded-full border border-border"
                  style={{ backgroundColor: color }}
                  title={`${name}: ${color}`}
                />
              ))}
            </div>
          </div>
        )}

        <Button
          className="w-full"
          onClick={() => onApply(template)}
          disabled={isApplying}
          aria-label={`Áp dụng template ${template.name}`}
        >
          {isApplying ? (
            <LoadingSpinner />
          ) : (
            <>
              <Check className="w-4 h-4 mr-2" />
              Áp dụng Template
            </>
          )}
        </Button>
      </CardContent>
    </Card>
  );
}
