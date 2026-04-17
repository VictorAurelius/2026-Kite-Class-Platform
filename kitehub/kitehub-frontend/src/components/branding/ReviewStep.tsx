'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Download, CheckCircle2, ExternalLink, Loader2 } from 'lucide-react';
import { useJobAssets } from '@/hooks/use-branding';
import { useThemeGeneration } from '@/hooks/use-theme-generation';
import { ThemePreviewCard } from './ThemePreviewCard';
import type { BrandingAsset, LogoAnalysis } from '@/types/branding';

interface ReviewStepProps {
  jobId: string;
  analysis: LogoAnalysis | null;
  onPublish: () => void;
}

const ASSET_TYPE_LABELS: Record<string, string> = {
  PROFILE: 'Ảnh Profile',
  HERO: 'Ảnh Hero',
  LOGO: 'Logo',
  BANNER: 'Banner',
  OG_IMAGE: 'OG Image',
};

const ASSET_TYPE_DESCRIPTIONS: Record<string, string> = {
  PROFILE: 'Ảnh đại diện cho profile, kích thước tối ưu cho mạng xã hội',
  HERO: 'Ảnh hero cho trang chủ, thu hút sự chú ý',
  LOGO: 'Logo chính của thương hiệu',
  BANNER: 'Banner cho header hoặc quảng cáo',
  OG_IMAGE: 'Ảnh hiển thị khi chia sẻ link trên mạng xã hội',
};

export function ReviewStep({ jobId, analysis, onPublish }: ReviewStepProps) {
  const { data: assets, isLoading } = useJobAssets(jobId);
  const { themeConfig, isLoading: isGeneratingTheme, generateTheme } = useThemeGeneration();

  // Auto-generate theme when analysis is available
  useEffect(() => {
    if (analysis && !themeConfig && !isGeneratingTheme) {
      generateTheme(analysis);
    }
  }, [analysis, themeConfig, isGeneratingTheme, generateTheme]);

  if (isLoading) {
    return (
      <Card className="p-8">
        <div className="text-center">
          <p className="text-muted-foreground">Đang tải tài nguyên...</p>
        </div>
      </Card>
    );
  }

  if (!assets || assets.length === 0) {
    return (
      <Card className="p-8">
        <div className="text-center">
          <p className="text-muted-foreground">Không có tài nguyên nào được tạo</p>
        </div>
      </Card>
    );
  }

  const handleDownload = async (asset: BrandingAsset) => {
    try {
      const response = await fetch(asset.url);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${asset.type.toLowerCase()}-${asset.id}.png`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      console.error('Download failed:', error);
    }
  };

  return (
    <div className="space-y-6">
      {/* Theme Preview */}
      {isGeneratingTheme && (
        <Card className="p-8">
          <div className="flex items-center justify-center gap-3">
            <Loader2 className="w-5 h-5 animate-spin" />
            <p className="text-muted-foreground">Đang tạo theme configuration...</p>
          </div>
        </Card>
      )}

      {themeConfig && (
        <ThemePreviewCard themeConfig={themeConfig} />
      )}

      {/* Assets Grid */}
      <Card className="p-8">
        <div className="flex items-center gap-3 mb-4">
          <CheckCircle2 className="w-6 h-6 text-green-500" />
          <h2 className="text-xl font-semibold">Xem Trước Tài Nguyên</h2>
        </div>
        <p className="text-sm text-muted-foreground mb-6">
          Xem trước các tài nguyên branding đã được tạo. Bạn có thể tải về từng tài nguyên hoặc xuất bản toàn bộ.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          {assets.map((asset) => (
            <AssetCard
              key={asset.id}
              asset={asset}
              onDownload={() => handleDownload(asset)}
            />
          ))}
        </div>

        <div className="flex justify-end gap-3">
          <Button variant="outline" asChild>
            <a href="/branding" className="flex items-center gap-2">
              Quay lại Dashboard
            </a>
          </Button>
          <Button onClick={onPublish} className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" />
            Xuất Bản
          </Button>
        </div>
      </Card>
    </div>
  );
}

interface AssetCardProps {
  asset: BrandingAsset;
  onDownload: () => void;
}

function AssetCard({ asset, onDownload }: AssetCardProps) {
  return (
    <Card className="overflow-hidden">
      {/* Image Preview */}
      <div className="aspect-video bg-muted flex items-center justify-center relative group">
        <img
          src={asset.url}
          alt={ASSET_TYPE_LABELS[asset.type]}
          className="w-full h-full object-cover"
        />
        {/* Hover Overlay */}
        <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
          <Button
            size="sm"
            variant="secondary"
            onClick={onDownload}
          >
            <Download className="w-4 h-4 mr-2" />
            Tải về
          </Button>
          <Button
            size="sm"
            variant="secondary"
            asChild
          >
            <a href={asset.url} target="_blank" rel="noopener noreferrer">
              <ExternalLink className="w-4 h-4 mr-2" />
              Xem
            </a>
          </Button>
        </div>
      </div>

      {/* Card Content */}
      <div className="p-4">
        <div className="flex items-center justify-between mb-2">
          <Badge variant="secondary">{ASSET_TYPE_LABELS[asset.type]}</Badge>
          <span className="text-xs text-muted-foreground">
            {new Date(asset.createdAt).toLocaleDateString('vi-VN')}
          </span>
        </div>
        <p className="text-sm text-muted-foreground">
          {ASSET_TYPE_DESCRIPTIONS[asset.type]}
        </p>
      </div>
    </Card>
  );
}
