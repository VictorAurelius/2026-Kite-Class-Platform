import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { Instance, TrialStatus } from '@/types/instance';
import type { ApiResponse } from '@/types/api';

export function useOwnerInstances(ownerId: number | undefined) {
  return useQuery({
    queryKey: ['instances', 'owner', ownerId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<Instance[]>>(
        endpoints.instances.byOwner(ownerId!)
      );
      return data.data;
    },
    enabled: !!ownerId,
  });
}

export function useInstance(id: number | undefined) {
  return useQuery({
    queryKey: ['instances', id],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<Instance>>(
        endpoints.instances.byId(id!)
      );
      return data.data;
    },
    enabled: !!id,
  });
}

export function useTrialStatus(instanceId: number | undefined) {
  return useQuery({
    queryKey: ['instances', instanceId, 'trial-status'],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<TrialStatus>>(
        endpoints.instances.trialStatus(instanceId!)
      );
      return data.data;
    },
    enabled: !!instanceId,
  });
}
