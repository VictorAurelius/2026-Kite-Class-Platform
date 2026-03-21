'use client';

import { useEffect, useState } from 'react';
import { CMSEditor } from '@/components/cms/CMSEditor';
import { landingApi, transformApiResponseToFormData, transformFormDataToApiRequest } from '@/lib/cms/api/landing';
import { useTenantFromUrl } from '@/hooks/useTenantFromUrl';

export default function CMSEditPage() {
  const [initialData, setInitialData] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);
  const tenantId = useTenantFromUrl();

  useEffect(() => {
    if (!tenantId) return;

    const loadLandingData = async () => {
      try {
        const data = await landingApi.getLandingPage(tenantId);
        const formData = transformApiResponseToFormData(data);
        setInitialData(formData);
      } catch (error) {
        console.error('Failed to load landing data:', error);
      } finally {
        setLoading(false);
      }
    };

    loadLandingData();
  }, [tenantId]);

  const handleSave = async (formData: Record<string, any>) => {
    if (!tenantId) throw new Error('Tenant ID not found');

    const apiData = transformFormDataToApiRequest(formData);
    await landingApi.updateLandingPage(tenantId, apiData);
  };

  if (loading) {
    return (
      <div className="container mx-auto py-8">
        <p>Loading...</p>
      </div>
    );
  }

  if (!tenantId) {
    return (
      <div className="container mx-auto py-8">
        <p className="text-destructive">Tenant ID not found in URL</p>
      </div>
    );
  }

  return <CMSEditor tenantId={tenantId} initialData={initialData} onSave={handleSave} />;
}
