'use client';

import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useAssets } from '@/hooks/use-branding';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Sparkles, Image as ImageIcon, Grid3x3, ArrowRight } from 'lucide-react';
import { toast } from 'sonner';

export default function BrandingDashboardPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const user = useAuthStore((state) => state.user);
  const { data: instances, isLoading: instancesLoading } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;
  const { data: assets, isLoading: assetsLoading } = useAssets(instanceId);

  // Show success toast if redirected from wizard
  useEffect(() => {
    if (searchParams.get('success') === 'true') {
      toast.success('Branding đã được xuất bản thành công!');
    }
  }, [searchParams]);

  if (instancesLoading || assetsLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  const hasAssets = assets && assets.length > 0;
  const recentAssets = assets?.slice(0, 6) || [];

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold mb-2">AI Branding</h1>
          <p className="text-muted-foreground">
            Quản lý và tạo bộ nhận diện thương hiệu với AI
          </p>
        </div>
        <Button onClick={() => router.push('/branding/wizard')} size="lg">
          <Sparkles className="w-4 h-4 mr-2" />
          Tạo Branding Mới
        </Button>
      </div>

      {/* Status Card */}
      <Card className="mb-8">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ImageIcon className="w-5 h-5" />
            Trạng Thái Branding
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <StatCard
              label="Tổng Tài Nguyên"
              value={assets?.length || 0}
              icon={<ImageIcon className="w-5 h-5 text-muted-foreground" />}
            />
            <StatCard
              label="Profile Images"
              value={assets?.filter((a) => a.type === 'PROFILE').length || 0}
              icon={<ImageIcon className="w-5 h-5 text-muted-foreground" />}
            />
            <StatCard
              label="Hero Images"
              value={assets?.filter((a) => a.type === 'HERO').length || 0}
              icon={<ImageIcon className="w-5 h-5 text-muted-foreground" />}
            />
          </div>
        </CardContent>
      </Card>

      {/* Recent Assets */}
      {hasAssets ? (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Tài Nguyên Gần Đây</CardTitle>
            <Button variant="ghost" onClick={() => router.push('/branding/assets')}>
              Xem Tất Cả
              <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {recentAssets.map((asset) => (
                <AssetPreviewCard key={asset.id} asset={asset} />
              ))}
            </div>
          </CardContent>
        </Card>
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

interface StatCardProps {
  label: string;
  value: number;
  icon: React.ReactNode;
}

function StatCard({ label, value, icon }: StatCardProps) {
  return (
    <div className="flex items-center gap-4">
      <div className="p-3 bg-muted rounded-lg">
        {icon}
      </div>
      <div>
        <p className="text-2xl font-bold">{value}</p>
        <p className="text-sm text-muted-foreground">{label}</p>
      </div>
    </div>
  );
}

interface AssetPreviewCardProps {
  asset: {
    id: string;
    type: string;
    url: string;
    createdAt: string;
  };
}

const ASSET_TYPE_LABELS: Record<string, string> = {
  PROFILE: 'Profile',
  HERO: 'Hero',
  LOGO: 'Logo',
  BANNER: 'Banner',
  OG_IMAGE: 'OG Image',
};

function AssetPreviewCard({ asset }: AssetPreviewCardProps) {
  return (
    <Card className="overflow-hidden cursor-pointer hover:shadow-lg transition-shadow">
      <div className="aspect-video bg-muted">
        <img
          src={asset.url}
          alt={ASSET_TYPE_LABELS[asset.type]}
          className="w-full h-full object-cover"
        />
      </div>
      <div className="p-4">
        <div className="flex items-center justify-between">
          <Badge variant="secondary">{ASSET_TYPE_LABELS[asset.type]}</Badge>
          <span className="text-xs text-muted-foreground">
            {new Date(asset.createdAt).toLocaleDateString('vi-VN')}
          </span>
        </div>
      </div>
    </Card>
  );
}
