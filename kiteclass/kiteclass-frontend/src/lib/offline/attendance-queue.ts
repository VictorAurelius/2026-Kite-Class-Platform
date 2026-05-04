/**
 * IndexedDB-backed offline queue for K-12 per-tiết attendance batches
 * (Wave 18b3 Bucket A, GAP-323b Phase 1B remainder).
 *
 * Design (詳細設計):
 *   - Single object store keyed by auto-increment {@code id}
 *   - Each row carries the full {@link AttendancePeriodBatchEntry}[] payload
 *     plus metadata (status, attempts, timestamps, last error)
 *   - Status state machine:
 *
 *       pending  ──drain (success)──▶  synced
 *           │
 *           └──drain (failure)─────▶  failed ──drain(retry success)──▶ synced
 *
 *   - {@link drain} is idempotent: items already in {@code synced} are skipped.
 *   - {@link clearSynced} prunes successfully-shipped rows so the store does
 *     not grow unbounded.
 *
 * Why IndexedDB instead of localStorage:
 *   - Larger quota (≥50MB typical vs 5MB)
 *   - Async API + structured cloning of entries[] arrays
 *   - Multiple-tab safe (each tab opens its own connection; transactions
 *     serialize automatically)
 *
 * The queue is FE-only — sync requires the caller to inject an
 * {@link AttendanceUpsertFn} pointing at
 * {@code attendancePeriodApi.upsertBatch}, so this module has no direct
 * dependency on the API client (easier to test, easier to swap).
 *
 * @since 4.x.x (Wave 18b3 Bucket A)
 */

import { openDB, type IDBPDatabase } from 'idb';
import type { AttendancePeriodBatchEntry } from '@/lib/api/attendance-period';

const DEFAULT_DB_NAME = 'kc-offline-attendance';
const DEFAULT_STORE_NAME = 'queue';
const DB_VERSION = 1;

/** Lifecycle states for a queued batch. */
export type AttendanceQueueStatus = 'pending' | 'syncing' | 'synced' | 'failed';

/**
 * One queued batch row. Persisted as-is in IndexedDB, so all fields must be
 * structured-cloneable (no functions, no symbols).
 */
export interface AttendanceQueueItem {
  id: number;
  teacherId: number;
  classId: number;
  date: string;
  entries: AttendancePeriodBatchEntry[];
  status: AttendanceQueueStatus;
  queuedAt: number;
  attempts: number;
  lastAttemptAt: number | null;
  lastError: string | null;
}

/** Fields the caller provides when queuing a new batch. */
export interface AttendanceQueueInput {
  teacherId: number;
  classId: number;
  date: string;
  entries: AttendancePeriodBatchEntry[];
}

/**
 * Caller-supplied upsert function (typically wraps
 * {@code attendancePeriodApi.upsertBatch}). Returning a successful result
 * marks the queue item synced; throwing marks it failed and bumps
 * {@code attempts}.
 */
export type AttendanceUpsertFn = (
  item: AttendanceQueueItem,
) => Promise<{ ok: true } | void>;

/** Aggregate result of a single drain pass. */
export interface DrainResult {
  synced: number;
  failed: number;
}

export interface AttendanceQueueOptions {
  dbName?: string;
  storeName?: string;
}

export interface AttendanceQueue {
  init(): Promise<void>;
  enqueue(input: AttendanceQueueInput): Promise<AttendanceQueueItem>;
  list(): Promise<AttendanceQueueItem[]>;
  drain(upsertFn: AttendanceUpsertFn): Promise<DrainResult>;
  clearSynced(): Promise<number>;
  close(): Promise<void>;
}

/**
 * Factory — every call returns an independent queue handle. We expose this
 * as a factory (rather than a singleton) so unit tests can use isolated DB
 * names and so the future PWA service worker can open its own handle if
 * needed.
 */
export function createAttendanceQueue(
  options: AttendanceQueueOptions = {},
): AttendanceQueue {
  const dbName = options.dbName ?? DEFAULT_DB_NAME;
  const storeName = options.storeName ?? DEFAULT_STORE_NAME;
  let dbPromise: Promise<IDBPDatabase> | null = null;

  async function db(): Promise<IDBPDatabase> {
    if (!dbPromise) {
      dbPromise = openDB(dbName, DB_VERSION, {
        upgrade(database) {
          if (!database.objectStoreNames.contains(storeName)) {
            database.createObjectStore(storeName, {
              keyPath: 'id',
              autoIncrement: true,
            });
          }
        },
      });
    }
    return dbPromise;
  }

  async function init(): Promise<void> {
    await db();
  }

  async function enqueue(
    input: AttendanceQueueInput,
  ): Promise<AttendanceQueueItem> {
    const handle = await db();
    const now = Date.now();
    // We persist a placeholder id=0 because IndexedDB will overwrite it via
    // the auto-incrementing keyPath. Reading back guarantees a real id.
    const draft: Omit<AttendanceQueueItem, 'id'> = {
      teacherId: input.teacherId,
      classId: input.classId,
      date: input.date,
      entries: [...input.entries],
      status: 'pending',
      queuedAt: now,
      attempts: 0,
      lastAttemptAt: null,
      lastError: null,
    };
    const id = (await handle.add(storeName, draft as AttendanceQueueItem)) as number;
    const stored = (await handle.get(storeName, id)) as
      | AttendanceQueueItem
      | undefined;
    if (!stored) {
      throw new Error(`Failed to read back queued item id=${id}`);
    }
    return stored;
  }

  async function list(): Promise<AttendanceQueueItem[]> {
    const handle = await db();
    const all = (await handle.getAll(storeName)) as AttendanceQueueItem[];
    return all.sort((a, b) => a.id - b.id);
  }

  async function drain(upsertFn: AttendanceUpsertFn): Promise<DrainResult> {
    const items = await list();
    let synced = 0;
    let failed = 0;

    for (const item of items) {
      // Idempotent: already-synced rows are skipped on subsequent drains.
      if (item.status === 'synced') {
        continue;
      }
      const next: AttendanceQueueItem = { ...item };
      next.attempts = item.attempts + 1;
      next.lastAttemptAt = Date.now();

      try {
        await upsertFn(item);
        next.status = 'synced';
        next.lastError = null;
        synced += 1;
      } catch (err) {
        next.status = 'failed';
        next.lastError =
          err instanceof Error
            ? err.message
            : typeof err === 'string'
              ? err
              : 'Unknown error';
        failed += 1;
      }

      const handle = await db();
      await handle.put(storeName, next);
    }

    return { synced, failed };
  }

  async function clearSynced(): Promise<number> {
    const handle = await db();
    const tx = handle.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    const all = (await store.getAll()) as AttendanceQueueItem[];
    let removed = 0;
    for (const item of all) {
      if (item.status === 'synced') {
        await store.delete(item.id);
        removed += 1;
      }
    }
    await tx.done;
    return removed;
  }

  async function close(): Promise<void> {
    if (dbPromise) {
      const handle = await dbPromise;
      handle.close();
      dbPromise = null;
    }
  }

  return { init, enqueue, list, drain, clearSynced, close };
}
