import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { DomainVerifyResponse } from '@/types/instance';

/**
 * Hook to get current domain status for an instance.
 */
export function useDomainStatus(instanceId: string | undefined) {
  return useQuery({
    queryKey: ['instances', instanceId, 'domain'],
    queryFn: async () => {
      const { data } = await apiClient.get<DomainVerifyResponse>(
        endpoints.instances.domain(instanceId!)
      );
      return data;
    },
    enabled: !!instanceId,
    retry: false,
  });
}

/**
 * Hook to initiate custom domain setup.
 */
export function useInitiateDomain(instanceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (customDomain: string) => {
      const { data } = await apiClient.post<DomainVerifyResponse>(
        endpoints.instances.domain(instanceId),
        { customDomain }
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['instances', instanceId, 'domain'] });
    },
  });
}

/**
 * Hook to trigger DNS verification.
 */
export function useVerifyDomain(instanceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const { data } = await apiClient.post<DomainVerifyResponse>(
        endpoints.instances.domainVerify(instanceId)
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['instances', instanceId, 'domain'] });
    },
  });
}

/**
 * Hook to remove custom domain.
 */
export function useRemoveDomain(instanceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      await apiClient.delete(endpoints.instances.domain(instanceId));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['instances', instanceId, 'domain'] });
    },
  });
}
