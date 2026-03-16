'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Palette, Sparkles, ArrowLeft } from 'lucide-react';
import type { LogoAnalysis } from '@/types/branding';

interface AnalyzeStepProps {
  analysis: LogoAnalysis;
  onConfirm: (customizedAnalysis: LogoAnalysis) => void;
  onBack: () => void;
}

const THEME_LABELS: Record<string, string> = {
  MODERN: 'Hiện đại',
  CLASSIC: 'Cổ điển',
  PLAYFUL: 'Vui tươi',
  MINIMAL: 'Tối giản',
};

export function AnalyzeStep({ analysis, onConfirm, onBack }: AnalyzeStepProps) {
  const [customizedAnalysis, setCustomizedAnalysis] = useState<LogoAnalysis>(analysis);

  const handleColorChange = (field: keyof Pick<LogoAnalysis, 'primaryColor' | 'secondaryColor' | 'accentColor'>, value: string) => {
    setCustomizedAnalysis((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleThemeChange = (theme: LogoAnalysis['theme']) => {
    setCustomizedAnalysis((prev) => ({
      ...prev,
      theme,
    }));
  };

  const handleConfirm = () => {
    onConfirm(customizedAnalysis);
  };

  return (
    <Card className="p-8">
      <div className="flex items-center gap-3 mb-2">
        <Sparkles className="w-6 h-6 text-primary" />
        <h2 className="text-xl font-semibold">Phân Tích AI</h2>
      </div>
      <p className="text-sm text-muted-foreground mb-6">
        AI đã phân tích logo của bạn. Bạn có thể tùy chỉnh kết quả bên dưới.
      </p>

      {/* Color Palette */}
      <div className="space-y-6 mb-8">
        <div>
          <div className="flex items-center gap-2 mb-4">
            <Palette className="w-5 h-5 text-muted-foreground" />
            <h3 className="font-medium">Bảng Màu</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Primary Color */}
            <div className="space-y-2">
              <Label htmlFor="primaryColor">Màu Chính</Label>
              <div className="flex gap-2">
                <div
                  className="w-12 h-12 rounded-lg border-2 border-border"
                  style={{ backgroundColor: customizedAnalysis.primaryColor }}
                />
                <Input
                  id="primaryColor"
                  type="color"
                  value={customizedAnalysis.primaryColor}
                  onChange={(e) => handleColorChange('primaryColor', e.target.value)}
                  className="flex-1"
                />
              </div>
            </div>

            {/* Secondary Color */}
            <div className="space-y-2">
              <Label htmlFor="secondaryColor">Màu Phụ</Label>
              <div className="flex gap-2">
                <div
                  className="w-12 h-12 rounded-lg border-2 border-border"
                  style={{ backgroundColor: customizedAnalysis.secondaryColor }}
                />
                <Input
                  id="secondaryColor"
                  type="color"
                  value={customizedAnalysis.secondaryColor}
                  onChange={(e) => handleColorChange('secondaryColor', e.target.value)}
                  className="flex-1"
                />
              </div>
            </div>

            {/* Accent Color */}
            <div className="space-y-2">
              <Label htmlFor="accentColor">Màu Nhấn</Label>
              <div className="flex gap-2">
                <div
                  className="w-12 h-12 rounded-lg border-2 border-border"
                  style={{ backgroundColor: customizedAnalysis.accentColor }}
                />
                <Input
                  id="accentColor"
                  type="color"
                  value={customizedAnalysis.accentColor}
                  onChange={(e) => handleColorChange('accentColor', e.target.value)}
                  className="flex-1"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Theme */}
        <div>
          <h3 className="font-medium mb-3">Phong Cách</h3>
          <div className="flex flex-wrap gap-2">
            {(['MODERN', 'CLASSIC', 'PLAYFUL', 'MINIMAL'] as const).map((theme) => (
              <Badge
                key={theme}
                variant={customizedAnalysis.theme === theme ? 'default' : 'outline'}
                className="cursor-pointer px-4 py-2"
                onClick={() => handleThemeChange(theme)}
              >
                {THEME_LABELS[theme]}
              </Badge>
            ))}
          </div>
        </div>

        {/* Brand Personality */}
        <div>
          <h3 className="font-medium mb-3">Tính Cách Thương Hiệu</h3>
          <div className="flex flex-wrap gap-2">
            {customizedAnalysis.brandPersonality.map((trait, idx) => (
              <Badge key={idx} variant="secondary">
                {trait}
              </Badge>
            ))}
          </div>
        </div>
      </div>

      <div className="flex justify-between">
        <Button variant="outline" onClick={onBack}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          Quay lại
        </Button>
        <Button onClick={handleConfirm}>
          Xác nhận và Tạo
        </Button>
      </div>
    </Card>
  );
}
