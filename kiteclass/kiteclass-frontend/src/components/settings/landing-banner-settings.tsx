/**
 * Landing banner carousel management (GAP-826).
 *
 * Lets an admin/teacher manage the ordered hero banner list (heroImages) shown on the
 * public landing: add a banner by URL, remove, and reorder (up/down). Saved via the
 * landing admin PUT endpoint.
 *
 * Upload note (PARTIAL — GAP-826 Lớp 3): there is no dedicated banner-upload endpoint
 * yet. The only asset-upload endpoints (`/settings/branding/logo`, `/favicon`) overwrite
 * the branding logo/favicon as a side effect, so reusing them for a banner would clobber
 * the logo. Until a banner-upload endpoint exists (follow-up), banners are managed by URL
 * (e.g. paste an uploaded asset URL). The backend validates each URL's scheme + host
 * allowlist on save (https + allowed host) — off-allowlist or relative URLs are rejected.
 *
 * @since wave landing (GAP-826)
 */

'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useLanding, useUpdateLanding } from '@/hooks/use-landing';
import { ArrowUp, ArrowDown, Trash2, Plus, Images } from 'lucide-react';

const MAX_BANNERS = 20;

export function LandingBannerSettings() {
  const { data: landing, isLoading } = useLanding();
  const updateMutation = useUpdateLanding();

  const [images, setImages] = useState<string[]>([]);
  const [newUrl, setNewUrl] = useState('');
  // Track whether the user has edited locally so we don't clobber edits when the query
  // refetches; seed from server data only on first successful load.
  const [seeded, setSeeded] = useState(false);

  useEffect(() => {
    if (!seeded && landing) {
      setImages(Array.isArray(landing.heroImages) ? landing.heroImages.filter(Boolean) : []);
      setSeeded(true);
    }
  }, [landing, seeded]);

  const addUrl = () => {
    const url = newUrl.trim();
    if (!url || images.length >= MAX_BANNERS || images.includes(url)) return;
    setImages([...images, url]);
    setNewUrl('');
  };

  const removeAt = (idx: number) => setImages(images.filter((_, i) => i !== idx));

  const move = (idx: number, delta: number) => {
    const target = idx + delta;
    if (target < 0 || target >= images.length) return;
    const next = [...images];
    [next[idx], next[target]] = [next[target]!, next[idx]!];
    setImages(next);
  };

  const save = () => updateMutation.mutate({ heroImages: images });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Images className="h-5 w-5" />
          Banner landing
        </CardTitle>
        <CardDescription>
          Quản lý các ảnh banner hiển thị trên trang chủ. Có từ 2 ảnh trở lên sẽ hiển thị dạng
          carousel (tự động chuyển, có nút điều hướng). Chỉ 1 ảnh sẽ hiển thị tĩnh.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoading ? (
          <div className="h-24 animate-pulse rounded bg-muted" />
        ) : (
          <>
            {images.length === 0 && (
              <p className="text-sm text-muted-foreground">
                Chưa có banner nào. Thêm URL ảnh bên dưới (ảnh đã tải lên hoặc đường dẫn https hợp lệ).
              </p>
            )}

            <ul className="space-y-3">
              {images.map((url, idx) => (
                <li
                  key={`${url}-${idx}`}
                  className="flex items-center gap-3 rounded-md border p-2"
                >
                  <span className="w-6 text-center text-sm font-semibold text-muted-foreground">
                    {idx + 1}
                  </span>
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={url}
                    alt={`Banner ${idx + 1}`}
                    className="h-12 w-20 shrink-0 rounded border object-cover"
                  />
                  <span className="flex-1 truncate text-sm" title={url}>
                    {url}
                  </span>
                  <div className="flex items-center gap-1">
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      aria-label={`Di chuyển banner ${idx + 1} lên`}
                      disabled={idx === 0}
                      onClick={() => move(idx, -1)}
                    >
                      <ArrowUp className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      aria-label={`Di chuyển banner ${idx + 1} xuống`}
                      disabled={idx === images.length - 1}
                      onClick={() => move(idx, 1)}
                    >
                      <ArrowDown className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      aria-label={`Xóa banner ${idx + 1}`}
                      onClick={() => removeAt(idx)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>

            <div className="flex items-center gap-2">
              <Input
                value={newUrl}
                onChange={(e) => setNewUrl(e.target.value)}
                placeholder="https://.../banner.webp"
                aria-label="URL banner mới"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addUrl();
                  }
                }}
              />
              <Button
                type="button"
                variant="outline"
                onClick={addUrl}
                disabled={!newUrl.trim() || images.length >= MAX_BANNERS}
              >
                <Plus className="mr-2 h-4 w-4" />
                Thêm
              </Button>
            </div>

            <p className="text-xs text-muted-foreground">
              Tải ảnh lên trực tiếp tại đây sẽ được bổ sung sau (hiện chưa có endpoint upload
              banner riêng). Tạm thời dán URL ảnh https hợp lệ. Tối đa {MAX_BANNERS} ảnh.
            </p>

            <div className="flex justify-end">
              <Button type="button" onClick={save} disabled={updateMutation.isPending}>
                {updateMutation.isPending ? 'Đang lưu...' : 'Lưu banner'}
              </Button>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
