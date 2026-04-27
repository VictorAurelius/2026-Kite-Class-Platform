import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { setupServer } from 'msw/node';

import { aiBrandingHandlers } from './ai-branding-handlers';
import { aiBrandingState } from './ai-branding-state';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const server = setupServer(...aiBrandingHandlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  aiBrandingState.reset();
});
afterAll(() => server.close());

async function postJson(path: string, body: unknown = {}) {
  return fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

describe('AI Branding MSW handlers — v2 lifecycle', () => {
  describe('POST /api/v1/instances', () => {
    it('creates instance with status NOT_STARTED', async () => {
      const res = await postJson('/api/v1/instances', { tenantId: 'acme', slug: 'acme' });
      expect(res.status).toBe(201);
      const json = await res.json();
      expect(json.data.status).toBe('NOT_STARTED');
      expect(json.data.brandingVersion).toBe(0);
      expect(json.data.id).toBeGreaterThan(0);
    });
  });

  describe('GET /api/v1/instances', () => {
    it('lists seeded demo instance', async () => {
      const res = await fetch(`${BASE_URL}/api/v1/instances`);
      expect(res.status).toBe(200);
      const json = await res.json();
      expect(json.data).toHaveLength(1);
      expect(json.data[0].slug).toBe('thanglong');
      expect(json.data[0].status).toBe('DEPLOYED');
    });

    it('returns 404 for missing id', async () => {
      const res = await fetch(`${BASE_URL}/api/v1/instances/9999`);
      expect(res.status).toBe(404);
    });
  });

  describe('lifecycle transitions', () => {
    it('infrastructure-ready moves NOT_STARTED → INITIALIZING (delayed → GENERATING)', async () => {
      const created = await postJson('/api/v1/instances', { slug: 'flow-test' });
      const { data: instance } = await created.json();

      const transit = await postJson(`/api/v1/instances/${instance.id}/infrastructure-ready`);
      expect(transit.status).toBe(202);
      const after = await transit.json();
      expect(after.data.status).toBe('INITIALIZING');
      expect(after.data.initializingAt).not.toBeNull();
    });

    it('rejects invalid transition (DEPLOYED → INITIALIZING) with 422', async () => {
      // seeded demo instance is already DEPLOYED
      const res = await postJson('/api/v1/instances/1/infrastructure-ready');
      expect(res.status).toBe(422);
      const json = await res.json();
      expect(json.code).toBe('INVALID_TRANSITION');
    });

    it('branding-completed gate passes at score 85 → DEPLOYED', async () => {
      const created = await postJson('/api/v1/instances', { slug: 'gate-pass' });
      const { data: instance } = await created.json();

      // Walk through state machine: NOT_STARTED → INITIALIZING → GENERATING manually
      await postJson(`/api/v1/instances/${instance.id}/infrastructure-ready`);
      // Force GENERATING since the delay is async
      const stateInst = aiBrandingState.get(instance.id)!;
      stateInst.status = 'GENERATING';

      const res = await postJson(`/api/v1/instances/${instance.id}/branding-completed`, { qualityScore: 85 });
      expect(res.status).toBe(200);
      const json = await res.json();
      expect(json.data.status).toBe('DEPLOYED');
      expect(json.data.brandingVersion).toBe(1);
      expect(json.data.deployedAt).not.toBeNull();
    });

    it('branding-completed gate fails at score 50 → FAILED with reason', async () => {
      const created = await postJson('/api/v1/instances', { slug: 'gate-fail' });
      const { data: instance } = await created.json();

      const stateInst = aiBrandingState.get(instance.id)!;
      stateInst.status = 'GENERATING';

      const res = await postJson(`/api/v1/instances/${instance.id}/branding-completed`, { qualityScore: 50 });
      expect(res.status).toBe(200);
      const json = await res.json();
      expect(json.data.status).toBe('FAILED');
      expect(json.data.failureReason).toContain('50');
    });

    it('rebrand DEPLOYED → REGENERATING', async () => {
      const res = await postJson('/api/v1/instances/1/rebrand');
      expect(res.status).toBe(202);
      const json = await res.json();
      expect(json.data.status).toBe('REGENERATING');
      expect(json.data.lastRegenerateAt).not.toBeNull();
    });

    it('failed records reason from body', async () => {
      const created = await postJson('/api/v1/instances', { slug: 'fail-test' });
      const { data: instance } = await created.json();

      const res = await postJson(`/api/v1/instances/${instance.id}/failed`, { reason: 'Saga timeout' });
      expect(res.status).toBe(200);
      const json = await res.json();
      expect(json.data.status).toBe('FAILED');
      expect(json.data.failureReason).toBe('Saga timeout');
    });

    it('retry FAILED → INITIALIZING and increments retryCount on the failed step', async () => {
      const created = await postJson('/api/v1/instances', { slug: 'retry-test' });
      const { data: instance } = await created.json();

      await postJson(`/api/v1/instances/${instance.id}/failed`, { reason: 'first attempt' });
      const retried = await postJson(`/api/v1/instances/${instance.id}/retry`);
      expect(retried.status).toBe(202);
      const json = await retried.json();
      expect(json.data.status).toBe('INITIALIZING');
      expect(json.data.retryCount).toBe(1); // recorded by FAILED transition
      expect(json.data.failureReason).toBeNull();
    });
  });

  describe('branding package endpoints', () => {
    it('GET /api/v1/branding/:id/package returns theme + assets + ETag', async () => {
      const res = await fetch(`${BASE_URL}/api/v1/branding/1/package`);
      expect(res.status).toBe(200);
      expect(res.headers.get('ETag')).toBeTruthy();

      const json = await res.json();
      expect(json.data.theme.primaryColor).toMatch(/^#/);
      expect(Object.keys(json.data.assets)).toEqual(
        expect.arrayContaining(['logo', 'banner', 'hero', 'favicon', 'courseThumbnail', 'socialCover']),
      );
      expect(json.data.brandingVersion).toBe(1);
    });

    it('returns 304 when If-None-Match matches', async () => {
      const first = await fetch(`${BASE_URL}/api/v1/branding/1/package`);
      const etag = first.headers.get('ETag')!;

      const second = await fetch(`${BASE_URL}/api/v1/branding/1/package`, {
        headers: { 'If-None-Match': etag },
      });
      expect(second.status).toBe(304);
    });

    it('GET /api/v1/branding/public returns the seeded DEPLOYED instance package', async () => {
      const res = await fetch(`${BASE_URL}/api/v1/branding/public`);
      expect(res.status).toBe(200);
      const json = await res.json();
      expect(json.data.theme).toBeDefined();
      expect(json.data.assets).toBeDefined();
    });
  });

  describe('internal webhook', () => {
    it('acknowledges instance-deployed notify with 200', async () => {
      const res = await postJson('/internal/notify/instance-deployed', { instanceId: 1 });
      expect(res.status).toBe(200);
      const json = await res.json();
      expect(json.data.acknowledged).toBe(true);
    });

    it('rejects missing instanceId with 400', async () => {
      const res = await postJson('/internal/notify/instance-deployed', {});
      expect(res.status).toBe(400);
    });
  });
});
