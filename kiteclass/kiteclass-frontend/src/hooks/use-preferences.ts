/**
 * React Query hooks for user preferences management.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/hooks/use-toast';
import { preferencesApi } from '@/lib/api/preferences';
import type { UpdatePreferencesRequest } from '@/types/preferences';
import type { AxiosError } from 'axios';

const PREFERENCES_KEY = 'preferences';

export function usePreferences() {
  return useQuery({
    queryKey: [PREFERENCES_KEY],
    queryFn: () => preferencesApi.get(),
  });
}

export function useUpdatePreferences() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: UpdatePreferencesRequest) => preferencesApi.update(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PREFERENCES_KEY] });
      toast({
        title: 'Thành công',
        description: 'Đã cập nhật tùy chọn',
      });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể cập nhật tùy chọn',
        variant: 'destructive',
      });
    },
  });
}
