/**
 * Branding wizard → backend API client.
 *
 * Wraps the endpoints from Wave 3 Sub-PR 3.4: initiate instance, fetch package.
 * Real submit flow triggers the backend saga (Sub-PR 3.6) which runs the full plan.
 *
 * @since Wave 3 Sub-PR 3.7
 */

import type { BrandInputs } from '@/components/branding/wizard/types';

export interface InstanceSnapshot {
  id: number;
  tenantId: string;
  slug: string;
  status:
    | 'NOT_STARTED'
    | 'INITIALIZING'
    | 'GENERATING'
    | 'DEPLOYED'
    | 'REGENERATING'
    | 'FAILED';
  brandingVersion: number;
  frontendUrl?: string;
}

export async function submitWizard(
  tenantId: string,
  slug: string,
  inputs: BrandInputs,
  signal?: AbortSignal,
): Promise<InstanceSnapshot> {
  const res = await fetch('/api/v1/instances', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenantId, slug, inputs }),
    signal,
  });
  if (!res.ok) {
    throw new Error(`submit failed: ${res.status}`);
  }
  const body = (await res.json()) as { data: InstanceSnapshot };
  return body.data;
}

export async function fetchInstance(
  id: number,
  signal?: AbortSignal,
): Promise<InstanceSnapshot> {
  const res = await fetch(`/api/v1/instances/${id}`, { signal });
  if (!res.ok) throw new Error(`fetch instance failed: ${res.status}`);
  const body = (await res.json()) as { data: InstanceSnapshot };
  return body.data;
}
