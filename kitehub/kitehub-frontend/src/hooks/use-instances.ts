import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { Instance, TrialStatus } from '@/types/instance';

export function useOwnerInstances(ownerId: string | undefined) {
  return useQuery({
    queryKey: ['instances', 'owner', ownerId],
    queryFn: async () => {
      const { data } = await apiClient.get<Instance[]>(
        endpoints.instances.byOwner(ownerId!)
      );
      return data;
    },
    enabled: !!ownerId,
  });
}

export function useInstance(id: string | undefined) {
  return useQuery({
    queryKey: ['instances', id],
    queryFn: async () => {
      const { data } = await apiClient.get<Instance>(
        endpoints.instances.byId(id!)
      );
      return data;
    },
    enabled: !!id,
  });
}

export function useTrialStatus(instanceId: string | undefined) {
  return useQuery({
    queryKey: ['instances', instanceId, 'trial-status'],
    queryFn: async () => {
      const { data } = await apiClient.get<TrialStatus>(
        endpoints.instances.trialStatus(instanceId!)
      );
      return data;
    },
    enabled: !!instanceId,
  });
}
