'use client';

/**
 * Lazy-loaded assets grid for `/branding/assets`.
 *
 * GAP-236 Sub-PR B (Wave GAP-236 Agent D) — defers the per-card UI bundle
 * (Card + Badge + Button + Lucide icons) until after the page header +
 * filter bar paint. Empty-state path stays inline so the no-assets case
 * doesn't pay for a chunk it won't use.
 */

import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Download, ExternalLink } from 'lucide-react';
import type { BrandingAsset } from '@/types/branding';

const ASSET_TYPE_LABELS: Record<string, string> = {
  PROFILE: 'Profile',
  HERO: 'Hero',
  LOGO: 'Logo',
  BANNER: 'Banner',
  OG_IMAGE: 'OG Image',
};

interface AssetsGridProps {
  assets: BrandingAsset[];
  onDownload: (asset: BrandingAsset) => void;
}

export default function AssetsGrid({ assets, onDownload }: AssetsGridProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {assets.map((asset) => (
        <AssetCard key={asset.id} asset={asset} onDownload={() => onDownload(asset)} />
      ))}
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
