import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type {
  NotificationPreferenceDto,
  NotificationPreferenceListResponse,
  NotificationType,
  UpdateNotificationPreferenceRequest,
} from '@/types/notification-preference';

const QUERY_KEY = ['notification-preferences'] as const;

/**
 * List the current user's notification preferences.
 *
 * Wave 18a Bucket B — GAP-063 Phase 1.
 */
export function useNotificationPreferences() {
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<NotificationPreferenceListResponse>(
        endpoints.notificationPreferences.list
      );
      return data;
    },
  });
}

/**
 * Upsert one preference row. Optimistically updates cache on success.
 */
export function useUpdateNotificationPreference() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      notificationType,
      payload,
    }: {
      notificationType: NotificationType;
      payload: UpdateNotificationPreferenceRequest;
    }) => {
      const { data } = await apiClient.patch<NotificationPreferenceDto>(
        endpoints.notificationPreferences.update(notificationType),
        payload
      );
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData<NotificationPreferenceListResponse>(QUERY_KEY, (prev) => {
        if (!prev) return prev;
        return {
          preferences: prev.preferences.map((p) =>
            p.notificationType === updated.notificationType ? updated : p
          ),
        };
      });
    },
  });
}
