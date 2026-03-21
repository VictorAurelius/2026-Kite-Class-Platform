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
  Images,
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
      <div className="flex items-center justify-center py-12">
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
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-purple-500/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-4 mb-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push('/branding')}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại
          </Button>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-purple-500/10 p-3 text-purple-600">
              <Images className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Tài nguyên Branding</h1>
              <p className="text-muted-foreground">
                {filteredAssets.length} tài nguyên
              </p>
            </div>
          </div>
          <Button onClick={() => router.push('/branding/wizard')}>
            <Sparkles className="w-4 h-4 mr-2" />
            Tạo mới
          </Button>
        </div>
      </div>

      {assets && assets.length > 0 ? (
        <>
          {/* Filters */}
          <Card className="p-4 shadow-soft">
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
              title="Không tìm thấy tài nguyên"
              description="Thử thay đổi bộ lọc hoặc tìm kiếm"
            />
          )}
        </>
      ) : (
        <EmptyState
          icon={<Grid3x3 className="w-12 h-12" />}
          title="Chưa có tài nguyên Branding"
          description="Bắt đầu tạo bộ nhận diện thương hiệu với AI ngay bây giờ"
          action={
            <Button onClick={() => router.push('/branding/wizard')}>
              <Sparkles className="w-4 h-4 mr-2" />
              Tạo Branding đầu tiên
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
    <Card className="overflow-hidden group shadow-soft hover:shadow-lg transition-shadow">
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
