'use client';

// ---------------------------------------------------------------------------
// AssetReusePicker — reuse a previously-uploaded/generated asset (GAP-1143).
//
// Step 7 affordance "chọn banner từ asset cũ": lists the tenant's existing
// BrandingAssets (via useAssets) so the owner can reuse one instead of always
// regenerating. Optionally filtered by `type` (e.g. 'BANNER' | 'LOGO' |
// 'PORTRAIT'). Selecting a thumbnail calls onSelect(url); the "Không dùng"
// tile calls onSelect(null).
//
// Presentational + 1 data hook; no fetching logic beyond useAssets.
// ---------------------------------------------------------------------------

import { ImageOff, Check } from 'lucide-react';

import { useAssets } from '@/hooks/use-branding';
import type { BrandingAsset } from '@/types/branding';
import { cn } from '@/lib/utils';

export interface AssetReusePickerProps {
  /** Instance owning the assets; null/undefined → empty-state. */
  instanceId?: string;
  /** Restrict to one asset type; omit to show all. */
  type?: BrandingAsset['type'];
  /** Currently selected asset URL (null = "không dùng asset cũ"). */
  selectedUrl?: string | null;
  /** Fires with the chosen asset URL, or null when the user clears the choice. */
  onSelect: (url: string | null) => void;
  className?: string;
}

export function AssetReusePicker({
  instanceId,
  type,
  selectedUrl,
  onSelect,
  className,
}: AssetReusePickerProps) {
  const { data: assets, isLoading } = useAssets(instanceId);

  const items = (assets ?? []).filter((a) => (type ? a.type === type : true));

  return (
    <div data-testid="asset-reuse-picker" className={cn('space-y-2', className)}>
      <h4 className="text-sm font-semibold text-foreground">Dùng lại từ thư viện</h4>

      {isLoading ? (
        <p data-testid="asset-reuse-picker-loading" className="text-xs text-muted-foreground">
          Đang tải thư viện…
        </p>
      ) : items.length === 0 ? (
        <div
          data-testid="asset-reuse-picker-empty"
          className="flex flex-col items-center gap-1 rounded-lg border border-dashed border-border py-6 text-center"
        >
          <ImageOff className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
          <p className="text-xs text-muted-foreground">Chưa có asset nào trong thư viện</p>
        </div>
      ) : (
        <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
          {/* "Không dùng asset cũ" tile */}
          <button
            type="button"
            data-testid="asset-reuse-clear"
            data-selected={selectedUrl == null}
            onClick={() => onSelect(null)}
            className={cn(
              'flex aspect-square flex-col items-center justify-center gap-1 rounded-lg border text-[11px] transition',
              selectedUrl == null
                ? 'border-primary ring-2 ring-primary/40'
                : 'border-input hover:bg-muted'
            )}
          >
            <ImageOff className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <span className="px-1 text-center text-muted-foreground">Không dùng</span>
          </button>

          {items.map((asset) => {
            const selected = selectedUrl === asset.url;
            return (
              <button
                key={asset.id}
                type="button"
                data-testid={`asset-reuse-item-${asset.id}`}
                data-selected={selected}
                onClick={() => onSelect(asset.url)}
                title={asset.type}
                className={cn(
                  'relative aspect-square overflow-hidden rounded-lg border transition',
                  selected
                    ? 'border-primary ring-2 ring-primary/40'
                    : 'border-input hover:opacity-90'
                )}
              >
                {/* eslint-disable-next-line @next/next/no-img-element -- thumbnail, dynamic remote URL */}
                <img
                  src={asset.url}
                  alt={`Asset ${asset.type}`}
                  className="h-full w-full object-cover"
                />
                {selected && (
                  <span className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-primary-foreground">
                    <Check className="h-3 w-3" aria-hidden="true" />
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default AssetReusePicker;
