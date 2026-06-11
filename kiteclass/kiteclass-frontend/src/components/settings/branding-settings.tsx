/**
 * Branding settings form component.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useBranding, useUpdateBranding, useUploadLogo } from '@/hooks/use-branding';
import { applyBrandColorVars } from '@/providers/BrandingProvider';
import { LandingBannerSettings } from '@/components/settings/landing-banner-settings';
import { Upload, Palette } from 'lucide-react';
import { useState } from 'react';

const schema = z.object({
  displayName: z.string().min(1, 'Tên hiển thị là bắt buộc'),
  tagline: z.string().optional(),
  primaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Màu không hợp lệ'),
  secondaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Màu không hợp lệ'),
  accentColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Màu không hợp lệ'),
  contactEmail: z.string().email('Email không hợp lệ').optional().or(z.literal('')),
  contactPhone: z.string().optional(),
  address: z.string().optional(),
  facebookUrl: z.string().url('URL không hợp lệ').optional().or(z.literal('')),
  zaloUrl: z.string().optional(),
  websiteUrl: z.string().url('URL không hợp lệ').optional().or(z.literal('')),
});

type FormData = z.infer<typeof schema>;

export function BrandingSettings() {
  const { data: branding, isLoading, isError } = useBranding();
  const updateMutation = useUpdateBranding();
  const uploadLogoMutation = useUploadLogo();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    values: branding
      ? {
          displayName: branding.displayName,
          tagline: branding.tagline || '',
          primaryColor: branding.primaryColor,
          secondaryColor: branding.secondaryColor,
          accentColor: branding.accentColor,
          contactEmail: branding.contactEmail || '',
          contactPhone: branding.contactPhone || '',
          address: branding.address || '',
          facebookUrl: branding.facebookUrl || '',
          zaloUrl: branding.zaloUrl || '',
          websiteUrl: branding.websiteUrl || '',
        }
      : undefined,
  });

  const onSubmit = (data: FormData) => {
    updateMutation.mutate(data, {
      onSuccess: () => {
        // Re-apply brand CSS vars immediately so the new colours show without a
        // hard reload — the query invalidation alone wouldn't recolour the DOM
        // (the dashboard applier only re-runs on a fresh fetch). GAP-807.
        applyBrandColorVars({
          primaryColor: data.primaryColor,
          secondaryColor: data.secondaryColor,
          accentColor: data.accentColor,
        });
      },
    });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
    }
  };

  const handleUploadLogo = () => {
    if (selectedFile) {
      uploadLogoMutation.mutate(selectedFile);
      setSelectedFile(null);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Logo</CardTitle>
            <CardDescription>Tải lên logo của tổ chức</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-20 w-20 animate-pulse rounded-lg bg-muted" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Thông tin tổ chức</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="h-10 animate-pulse rounded bg-muted" />
            <div className="h-10 animate-pulse rounded bg-muted" />
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
        Không thể tải cài đặt branding. Vui lòng thử lại.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Logo Upload */}
      <Card>
        <CardHeader>
          <CardTitle>Logo</CardTitle>
          <CardDescription>Tải lên logo của tổ chức</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {branding?.logoUrl && (
            <div className="flex items-center gap-4">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={branding.logoUrl}
                alt="Logo"
                className="h-20 w-20 rounded-lg border object-contain"
              />
              <div className="text-sm text-muted-foreground">Logo hiện tại</div>
            </div>
          )}

          <div className="flex items-center gap-4">
            <Input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={handleFileChange}
              className="flex-1"
            />
            <Button
              onClick={handleUploadLogo}
              disabled={!selectedFile || uploadLogoMutation.isPending}
            >
              <Upload className="mr-2 h-4 w-4" />
              Tải lên
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Landing banner carousel (GAP-826) */}
      <LandingBannerSettings />

      {/* Branding Info */}
      <form onSubmit={handleSubmit(onSubmit)}>
        <Card>
          <CardHeader>
            <CardTitle>Thông tin tổ chức</CardTitle>
            <CardDescription>Tên và slogan của tổ chức</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label htmlFor="displayName">Tên hiển thị *</Label>
              <Input id="displayName" {...register('displayName')} />
              {errors.displayName && (
                <p className="text-sm text-destructive">{errors.displayName.message}</p>
              )}
            </div>

            <div>
              <Label htmlFor="tagline">Slogan</Label>
              <Input id="tagline" {...register('tagline')} />
            </div>
          </CardContent>
        </Card>

        {/* Colors */}
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Palette className="h-5 w-5" />
              Màu sắc
            </CardTitle>
            <CardDescription>Cấu hình màu chủ đạo của giao diện</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 md:grid-cols-3">
              <div>
                <Label htmlFor="primaryColor">Màu chính</Label>
                <div className="flex gap-2">
                  <Input
                    id="primaryColor"
                    type="color"
                    {...register('primaryColor')}
                    className="h-10 w-20"
                  />
                  <Input {...register('primaryColor')} className="flex-1" />
                </div>
                {errors.primaryColor && (
                  <p className="text-sm text-destructive">{errors.primaryColor.message}</p>
                )}
              </div>

              <div>
                <Label htmlFor="secondaryColor">Màu phụ</Label>
                <div className="flex gap-2">
                  <Input
                    id="secondaryColor"
                    type="color"
                    {...register('secondaryColor')}
                    className="h-10 w-20"
                  />
                  <Input {...register('secondaryColor')} className="flex-1" />
                </div>
                {errors.secondaryColor && (
                  <p className="text-sm text-destructive">{errors.secondaryColor.message}</p>
                )}
              </div>

              <div>
                <Label htmlFor="accentColor">Màu nhấn</Label>
                <div className="flex gap-2">
                  <Input
                    id="accentColor"
                    type="color"
                    {...register('accentColor')}
                    className="h-10 w-20"
                  />
                  <Input {...register('accentColor')} className="flex-1" />
                </div>
                {errors.accentColor && (
                  <p className="text-sm text-destructive">{errors.accentColor.message}</p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Contact Info */}
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Thông tin liên hệ</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <Label htmlFor="contactEmail">Email</Label>
                <Input id="contactEmail" type="email" {...register('contactEmail')} />
                {errors.contactEmail && (
                  <p className="text-sm text-destructive">{errors.contactEmail.message}</p>
                )}
              </div>

              <div>
                <Label htmlFor="contactPhone">Số điện thoại</Label>
                <Input id="contactPhone" {...register('contactPhone')} />
              </div>
            </div>

            <div>
              <Label htmlFor="address">Địa chỉ</Label>
              <Textarea id="address" {...register('address')} rows={3} />
            </div>
          </CardContent>
        </Card>

        {/* Social Media */}
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Mạng xã hội</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label htmlFor="facebookUrl">Facebook URL</Label>
              <Input id="facebookUrl" {...register('facebookUrl')} placeholder="https://facebook.com/..." />
              {errors.facebookUrl && (
                <p className="text-sm text-destructive">{errors.facebookUrl.message}</p>
              )}
            </div>

            <div>
              <Label htmlFor="zaloUrl">Zalo ID/URL</Label>
              <Input id="zaloUrl" {...register('zaloUrl')} />
            </div>

            <div>
              <Label htmlFor="websiteUrl">Website URL</Label>
              <Input id="websiteUrl" {...register('websiteUrl')} placeholder="https://..." />
              {errors.websiteUrl && (
                <p className="text-sm text-destructive">{errors.websiteUrl.message}</p>
              )}
            </div>
          </CardContent>
        </Card>

        <div className="mt-6 flex justify-end">
          <Button type="submit" disabled={updateMutation.isPending}>
            {updateMutation.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
          </Button>
        </div>
      </form>
    </div>
  );
}
