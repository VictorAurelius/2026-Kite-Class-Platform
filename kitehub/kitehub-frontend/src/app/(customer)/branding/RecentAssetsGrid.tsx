'use client';

/**
 * Lazy-loaded recent-assets thumbnails for `/branding`.
 *
 * GAP-236 Sub-PR B (Wave GAP-236 Agent D) — splits the AssetPreviewCard grid
 * (Card + Badge + image cards) out of the route's initial chunk. Stats above
 * paint immediately; this only loads if the user actually has assets.
 */

import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

interface AssetPreview {
  id: string;
  type: string;
  url: string;
  createdAt: string;
}

const ASSET_TYPE_LABELS: Record<string, string> = {
  PROFILE: 'Profile',
  HERO: 'Hero',
  LOGO: 'Logo',
  BANNER: 'Banner',
  OG_IMAGE: 'OG Image',
};

interface RecentAssetsGridProps {
  assets: AssetPreview[];
}

export default function RecentAssetsGrid({ assets }: RecentAssetsGridProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {assets.map((asset) => (
        <AssetPreviewCard key={asset.id} asset={asset} />
      ))}
    </div>
  );
}

function AssetPreviewCard({ asset }: { asset: AssetPreview }) {
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
