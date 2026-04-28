'use client';

import { useState } from 'react';
import dynamic from 'next/dynamic';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useTemplates, useApplyTemplate } from '@/hooks/use-templates';
import type { BrandingTemplate } from '@/types/branding';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { LayoutGrid } from 'lucide-react';
import { toast } from 'sonner';

// GAP-236 Sub-PR B — lazy-load the templates grid. The grid only renders
// after `useTemplates` resolves, so we delay its bundle until the network
// fetch returns. Initial paint = page header + filter chips only.
const TemplatesGrid = dynamic(() => import('./TemplatesGrid'), {
  ssr: false,
  loading: () => (
    <div className="flex items-center justify-center py-12">
      <LoadingSpinner />
    </div>
  ),
});

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
      ) : templates ? (
        <TemplatesGrid
          templates={templates}
          onApply={handleApply}
          isApplying={applyMutation.isPending}
        />
      ) : null}
    </div>
  );
}
