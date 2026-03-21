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
import { Sparkles, Image as ImageIcon, Grid3x3, ArrowRight, Palette } from 'lucide-react';
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
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  const hasAssets = assets && assets.length > 0;
  const recentAssets = assets?.slice(0, 6) || [];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-purple-500/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-purple-500/10 p-3 text-purple-600">
              <Palette className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">AI Branding</h1>
              <p className="text-muted-foreground">
                Quản lý và tạo bộ nhận diện thương hiệu với AI
              </p>
            </div>
          </div>
          <Button onClick={() => router.push('/branding/wizard')}>
            <Sparkles className="w-4 h-4 mr-2" />
            Tạo Branding mới
          </Button>
        </div>
      </div>

      {/* Status Card */}
      <Card className="shadow-soft">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="rounded-lg bg-purple-500/10 p-2 text-purple-600">
              <ImageIcon className="h-4 w-4" />
            </div>
            Trạng thái Branding
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <StatCard
              label="Tổng tài nguyên"
              value={assets?.length || 0}
              icon={<ImageIcon className="w-5 h-5 text-purple-600" />}
            />
            <StatCard
              label="Profile Images"
              value={assets?.filter((a) => a.type === 'PROFILE').length || 0}
              icon={<ImageIcon className="w-5 h-5 text-blue-600" />}
            />
            <StatCard
              label="Hero Images"
              value={assets?.filter((a) => a.type === 'HERO').length || 0}
              icon={<ImageIcon className="w-5 h-5 text-green-600" />}
            />
          </div>
        </CardContent>
      </Card>

      {/* Recent Assets */}
      {hasAssets ? (
        <Card className="shadow-soft">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Tài nguyên gần đây</CardTitle>
            <Button variant="ghost" onClick={() => router.push('/branding/assets')}>
              Xem tất cả
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

interface StatCardProps {
  label: string;
  value: number;
  icon: React.ReactNode;
}

function StatCard({ label, value, icon }: StatCardProps) {
  return (
    <div className="flex items-center gap-4">
      <div className="p-3 bg-muted/50 rounded-xl">
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
