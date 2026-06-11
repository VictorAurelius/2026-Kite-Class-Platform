/**
 * React Query hooks for the landing-page admin surface (GAP-826).
 *
 * Resolves the active tenant id from the JWT session (getCurrentTenantId) so the
 * admin manages their OWN landing page. Used by the branding settings banner-carousel card.
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/hooks/use-toast';
import { landingApi, type LandingPageData, type UpdateLandingRequest } from '@/lib/api/landing';
import { getCurrentTenantId } from '@/lib/auth/jwt-storage';
import type { AxiosError } from 'axios';

const LANDING_KEY = 'landing-admin';

export function useLanding() {
  const tenantId = getCurrentTenantId();
  return useQuery({
    queryKey: [LANDING_KEY, tenantId],
    queryFn: () => landingApi.get(tenantId!),
    enabled: !!tenantId,
  });
}

export function useUpdateLanding() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const tenantId = getCurrentTenantId();

  return useMutation({
    mutationFn: (request: UpdateLandingRequest): Promise<LandingPageData> => {
      if (!tenantId) {
        return Promise.reject(new Error('Không xác định được trung tâm hiện tại (chưa đăng nhập?)'));
      }
      return landingApi.update(tenantId, request);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [LANDING_KEY] });
      toast({ title: 'Thành công', description: 'Đã cập nhật banner landing' });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || (error as Error).message || 'Không thể cập nhật banner',
        variant: 'destructive',
      });
    },
  });
}
