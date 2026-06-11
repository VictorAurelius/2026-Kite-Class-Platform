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
