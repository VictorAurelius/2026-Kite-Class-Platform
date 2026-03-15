'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useAssets } from '@/hooks/use-branding';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import {
  ArrowLeft,
  Download,
  ExternalLink,
  Search,
  Filter,
  Grid3x3,
  Sparkles,
} from 'lucide-react';
import type { BrandingAsset } from '@/types/branding';

const ASSET_TYPE_LABELS: Record<string, string> = {
  PROFILE: 'Profile',
  HERO: 'Hero',
  LOGO: 'Logo',
  BANNER: 'Banner',
  OG_IMAGE: 'OG Image',
};

const ASSET_TYPES = ['ALL', 'PROFILE', 'HERO', 'LOGO', 'BANNER', 'OG_IMAGE'] as const;

export default function BrandingAssetsPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const { data: instances, isLoading: instancesLoading } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;
  const { data: assets, isLoading: assetsLoading } = useAssets(instanceId);

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<typeof ASSET_TYPES[number]>('ALL');

  if (instancesLoading || assetsLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  // Filter assets
  const filteredAssets = assets?.filter((asset) => {
    const matchesType = selectedType === 'ALL' || asset.type === selectedType;
    const matchesSearch = searchQuery === '' ||
      ASSET_TYPE_LABELS[asset.type]?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      asset.id.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesType && matchesSearch;
  }) || [];

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
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push('/branding')}
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Quay lại
          </Button>
          <div>
            <h1 className="text-3xl font-bold">Tài Nguyên Branding</h1>
            <p className="text-sm text-muted-foreground">
              {filteredAssets.length} tài nguyên
            </p>
          </div>
        </div>
        <Button onClick={() => router.push('/branding/wizard')}>
          <Sparkles className="w-4 h-4 mr-2" />
          Tạo Mới
        </Button>
      </div>

      {assets && assets.length > 0 ? (
        <>
          {/* Filters */}
          <Card className="p-4 mb-6">
            <div className="flex flex-col md:flex-row gap-4">
              {/* Search */}
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="Tìm kiếm tài nguyên..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
              </div>

              {/* Type Filter */}
              <div className="flex gap-2 overflow-x-auto">
                {ASSET_TYPES.map((type) => (
                  <Button
                    key={type}
                    variant={selectedType === type ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => setSelectedType(type)}
                    className="whitespace-nowrap"
                  >
                    {type === 'ALL' ? 'Tất cả' : ASSET_TYPE_LABELS[type]}
                  </Button>
                ))}
              </div>
            </div>
          </Card>

          {/* Assets Grid */}
          {filteredAssets.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredAssets.map((asset) => (
                <AssetCard
                  key={asset.id}
                  asset={asset}
                  onDownload={() => handleDownload(asset)}
                />
              ))}
            </div>
          ) : (
            <EmptyState
              icon={<Filter className="w-12 h-12" />}
              title="Không Tìm Thấy Tài Nguyên"
              description="Thử thay đổi bộ lọc hoặc tìm kiếm"
            />
          )}
        </>
      ) : (
        <EmptyState
          icon={<Grid3x3 className="w-12 h-12" />}
          title="Chưa Có Tài Nguyên Branding"
          description="Bắt đầu tạo bộ nhận diện thương hiệu với AI ngay bây giờ"
          action={
            <Button onClick={() => router.push('/branding/wizard')}>
              <Sparkles className="w-4 h-4 mr-2" />
              Tạo Branding Đầu Tiên
            </Button>
          }
        />
      )}
    </div>
  );
}

interface AssetCardProps {
  asset: BrandingAsset;
  onDownload: () => void;
}

function AssetCard({ asset, onDownload }: AssetCardProps) {
  return (
    <Card className="overflow-hidden group">
      {/* Image Preview */}
      <div className="aspect-video bg-muted flex items-center justify-center relative">
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
        <p className="text-xs text-muted-foreground truncate">
          ID: {asset.id}
        </p>
      </div>
    </Card>
  );
}
