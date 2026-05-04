/**
 * Tests for the IndexedDB-backed offline attendance queue
 * (Wave 18b3 Bucket A, GAP-323b Phase 1B remainder).
 *
 * Uses fake-indexeddb so tests run in jsdom without a real browser.
 *
 * @since 4.x.x (Wave 18b3 Bucket A)
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import 'fake-indexeddb/auto';
import {
  createAttendanceQueue,
  type AttendanceQueueItem,
} from '../../../lib/offline/attendance-queue';
import type { AttendancePeriodBatchEntry } from '../../../lib/api/attendance-period';

const sampleEntries: AttendancePeriodBatchEntry[] = [
  {
    studentId: 101,
    classId: 7,
    subjectSectionId: 21,
    periodNo: 3,
    date: '2026-05-04',
    status: 'PRESENT',
  },
  {
    studentId: 102,
    classId: 7,
    subjectSectionId: 21,
    periodNo: 3,
    date: '2026-05-04',
    status: 'ABSENT',
  },
];

async function freshQueue(dbName?: string) {
  // Each test gets its own DB name so state never leaks between tests.
  const queue = createAttendanceQueue({
    dbName: dbName ?? `kc-offline-test-${Math.random().toString(36).slice(2)}`,
  });
  await queue.init();
  return queue;
}

describe('attendance offline queue', () => {
  beforeEach(() => {
    vi.useRealTimers();
  });

  it('enqueue persists a batch and assigns id + queuedAt + status=pending', async () => {
    const queue = await freshQueue();
    const item = await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: sampleEntries,
    });

    expect(item.id).toBeGreaterThan(0);
    expect(item.status).toBe('pending');
    expect(item.queuedAt).toBeGreaterThan(0);
    expect(item.entries).toHaveLength(2);

    const all = await queue.list();
    expect(all).toHaveLength(1);
    expect(all[0]?.id).toBe(item.id);
  });

  it('persists across queue re-open (same dbName)', async () => {
    const dbName = `kc-offline-persist-${Date.now()}`;
    const q1 = await freshQueue(dbName);
    await q1.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: sampleEntries,
    });
    await q1.close();

    // Re-open with same dbName — emulates a page reload.
    const q2 = await freshQueue(dbName);
    const all = await q2.list();
    expect(all).toHaveLength(1);
    expect(all[0]?.entries).toHaveLength(2);
  });

  it('drain calls upsertFn for each pending item and marks synced on success', async () => {
    const queue = await freshQueue();
    await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: sampleEntries,
    });
    await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: [sampleEntries[0]!],
    });

    const upsertFn = vi.fn(async () => ({ ok: true as const }));
    const result = await queue.drain(upsertFn);

    expect(upsertFn).toHaveBeenCalledTimes(2);
    expect(result.synced).toBe(2);
    expect(result.failed).toBe(0);

    const all = await queue.list();
    expect(all.every((it: AttendanceQueueItem) => it.status === 'synced')).toBe(true);
  });

  it('drain marks an item failed + records last error when upsertFn throws', async () => {
    const queue = await freshQueue();
    await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: sampleEntries,
    });

    const upsertFn = vi.fn(async () => {
      throw new Error('Network unavailable');
    });
    const result = await queue.drain(upsertFn);

    expect(result.synced).toBe(0);
    expect(result.failed).toBe(1);

    const all = await queue.list();
    expect(all[0]?.status).toBe('failed');
    expect(all[0]?.lastError).toContain('Network unavailable');
    expect(all[0]?.attempts).toBe(1);
  });

  it('retry re-runs failed items and flips them to synced when upsert succeeds', async () => {
    const queue = await freshQueue();
    await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: sampleEntries,
    });

    // First pass: fail.
    const failingFn = vi.fn(async () => {
      throw new Error('boom');
    });
    await queue.drain(failingFn);
    expect((await queue.list())[0]?.status).toBe('failed');

    // Second pass: succeed. drain() must pick up failed items too.
    const succeedingFn = vi.fn(async () => ({ ok: true as const }));
    const result = await queue.drain(succeedingFn);

    expect(succeedingFn).toHaveBeenCalledTimes(1);
    expect(result.synced).toBe(1);
    const all = await queue.list();
    expect(all[0]?.status).toBe('synced');
    expect(all[0]?.attempts).toBe(2);
  });

  it('drain skips items already marked synced (idempotent against repeated drains)', async () => {
    const queue = await freshQueue();
    await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: sampleEntries,
    });

    const upsertFn = vi.fn(async () => ({ ok: true as const }));
    await queue.drain(upsertFn);
    await queue.drain(upsertFn); // second drain should be a no-op
    expect(upsertFn).toHaveBeenCalledTimes(1);
  });

  it('clearSynced removes synced items but leaves pending and failed', async () => {
    const queue = await freshQueue();
    await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: sampleEntries,
    });
    const upsertFn = vi.fn(async () => ({ ok: true as const }));
    await queue.drain(upsertFn);

    await queue.enqueue({
      teacherId: 9,
      classId: 7,
      date: '2026-05-04',
      entries: [sampleEntries[0]!],
    });

    const removed = await queue.clearSynced();
    expect(removed).toBe(1);

    const remaining = await queue.list();
    expect(remaining).toHaveLength(1);
    expect(remaining[0]?.status).toBe('pending');
  });
});
