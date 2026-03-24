import { useQuery, useMutation } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { BrandingTemplate } from '@/types/branding';

/**
 * List all active branding templates, optionally filtered by category.
 */
export function useTemplates(category?: string) {
  return useQuery({
    queryKey: ['branding', 'templates', category],
    queryFn: async () => {
      const params = category ? { category } : {};
      const { data } = await apiClient.get<BrandingTemplate[]>(
        endpoints.branding.templates,
        { params }
      );
      return data;
    },
  });
}

/**
 * Get a single template by ID.
 */
export function useTemplate(id: string | undefined) {
  return useQuery({
    queryKey: ['branding', 'templates', id],
    queryFn: async () => {
      const { data } = await apiClient.get<BrandingTemplate>(
        endpoints.branding.templateById(id!)
      );
      return data;
    },
    enabled: !!id,
  });
}

/**
 * Apply a template to an instance for instant branding.
 */
export function useApplyTemplate() {
  return useMutation({
    mutationFn: async ({
      templateId,
      instanceId,
    }: {
      templateId: string;
      instanceId: string;
    }) => {
      const { data } = await apiClient.post<{ themeConfig: string; status: string }>(
        endpoints.branding.applyTemplate(templateId),
        null,
        { headers: { 'X-Instance-Id': instanceId } }
      );
      return data;
    },
  });
}
