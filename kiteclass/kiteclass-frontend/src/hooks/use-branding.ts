/**
 * React Query hooks for branding management.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/hooks/use-toast';
import { brandingApi } from '@/lib/api/branding';
import type { UpdateBrandingRequest } from '@/types/branding';
import type { AxiosError } from 'axios';

const BRANDING_KEY = 'branding';
const BRANDING_VERSIONS_KEY = 'branding-versions';

export function useBranding() {
  return useQuery({
    queryKey: [BRANDING_KEY],
    queryFn: () => brandingApi.get(),
  });
}

export function useUpdateBranding() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: UpdateBrandingRequest) => brandingApi.update(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [BRANDING_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã cập nhật thông tin branding',
      });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể cập nhật branding',
        variant: 'destructive',
      });
    },
  });
}

export function useUploadLogo() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (file: File) => brandingApi.uploadLogo(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [BRANDING_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã tải lên logo',
      });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải lên logo',
        variant: 'destructive',
      });
    },
  });
}

// GAP-1229: favicon upload — mirror useUploadLogo (endpoint POST /settings/branding/favicon
// đã sẵn BE per GAP-1035/1036; brandingApi.uploadFavicon trước đây 0 caller).
export function useUploadFavicon() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (file: File) => brandingApi.uploadFavicon(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [BRANDING_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã tải lên favicon',
      });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải lên favicon',
        variant: 'destructive',
      });
    },
  });
}

// GAP-1446: branding version history — BE BrandingVersionController đã sẵn
// (GET /api/v1/branding/{instanceId}/versions) nhưng trước đây không có FE surface.
// Query disabled khi chưa có instanceId (tenantId) để tránh fetch với path rỗng.
export function useBrandingVersions(instanceId: string | null) {
  return useQuery({
    queryKey: [BRANDING_VERSIONS_KEY, instanceId],
    queryFn: () => brandingApi.listVersions(instanceId as string),
    enabled: !!instanceId,
  });
}

// GAP-1446: rollback branding về 1 version — tạo version mới (append-only) restore snapshot.
// Invalidate cả branding hiện tại + danh sách version sau khi rollback thành công.
export function useRollbackBranding(instanceId: string | null) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (versionNumber: number) =>
      brandingApi.rollback(instanceId as string, versionNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [BRANDING_KEY] });
      queryClient.invalidateQueries({ queryKey: [BRANDING_VERSIONS_KEY, instanceId] });
      toast({
        title: 'Thành công',
        description: 'Đã khôi phục branding về phiên bản đã chọn',
      });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể khôi phục phiên bản',
        variant: 'destructive',
      });
    },
  });
}
