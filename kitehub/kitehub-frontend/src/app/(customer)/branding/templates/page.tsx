'use client';

import { useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useTemplates, useApplyTemplate } from '@/hooks/use-templates';
import type { BrandingTemplate, ThemeConfig } from '@/types/branding';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { Palette, Check, LayoutGrid } from 'lucide-react';
import { toast } from 'sonner';

const CATEGORY_LABELS: Record<string, string> = {
  all: 'Tất cả',
  education: 'Giáo dục',
  business: 'Doanh nghiệp',
  general: 'Tổng hợp',
};

export default function TemplateGalleryPage() {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const user = useAuthStore((state) => state.user);
  const { data: instances } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;

  const category = selectedCategory === 'all' ? undefined : selectedCategory;
  const { data: templates, isLoading } = useTemplates(category);
  const applyMutation = useApplyTemplate();

  const handleApply = async (template: BrandingTemplate) => {
    if (!instanceId) {
      toast.error('Không tìm thấy instance');
      return;
    }

    try {
      await applyMutation.mutateAsync({
        templateId: template.id,
        instanceId,
      });
      toast.success(`Đã áp dụng template "${template.name}" thành công!`);
    } catch {
      toast.error('Không thể áp dụng template. Vui lòng thử lại.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-blue-500/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-blue-500/10 p-3 text-blue-600">
            <LayoutGrid className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Template Gallery</h1>
            <p className="text-muted-foreground">
              Chọn template để tạo branding ngay lập tức — không cần AI
            </p>
          </div>
        </div>
      </div>

      {/* Category Filter */}
      <div className="flex gap-2 flex-wrap" role="group" aria-label="Lọc template theo danh mục">
        {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
          <Button
            key={key}
            variant={selectedCategory === key ? 'default' : 'outline'}
            size="sm"
            onClick={() => setSelectedCategory(key)}
            aria-label={`Lọc template: ${label}`}
            aria-pressed={selectedCategory === key}
          >
            {label}
          </Button>
        ))}
      </div>

      {/* Templates Grid */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <LoadingSpinner />
        </div>
      ) : templates && templates.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {templates.map((template) => (
            <TemplateCard
              key={template.id}
              template={template}
              onApply={handleApply}
              isApplying={applyMutation.isPending}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-12">
          <LayoutGrid className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
          <p className="text-muted-foreground">Không có template nào trong danh mục này</p>
        </div>
      )}
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
