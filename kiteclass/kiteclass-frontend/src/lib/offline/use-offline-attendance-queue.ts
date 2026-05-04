/**
 * React hook wiring the IndexedDB attendance queue into the GVCN per-tiết
 * route (Wave 18b3 Bucket A, GAP-323b Phase 1B remainder).
 *
 * Caller injects an {@code upsertFn} that does the network round-trip
 * (typically wrapping {@code attendancePeriodApi.upsertBatch}). The hook:
 *
 *   1. Lazy-initialises one queue per {@code dbName} on mount
 *   2. Exposes {@code enqueue} for batches the user saved while offline
 *   3. Listens for the browser {@code online} event and automatically drains
 *   4. Exposes {@code flush} for the user's "thử lại" button
 *   5. Refreshes counts after every state-changing operation
 *
 * The hook is FE-only and synchronous-API; the IDB calls happen inside its
 * effects. State is reflected through {@link OfflineQueueState}.
 *
 * @since 4.x.x (Wave 18b3 Bucket A)
 */

'use client';

import * as React from 'react';
import {
  createAttendanceQueue,
  type AttendanceQueue,
  type AttendanceQueueInput,
  type AttendanceQueueItem,
} from './attendance-queue';

export interface OfflineQueueState {
  pending: number;
  failed: number;
  synced: number;
  ready: boolean;
}

export interface UseOfflineAttendanceQueueOptions {
  /** IndexedDB database name. Override in tests for isolation. */
  dbName?: string;
  /**
   * Caller-supplied upsert. Returning normally → marks the item synced;
   * throwing → marks it failed.
   */
  upsertFn: (item: AttendanceQueueItem) => Promise<void>;
}

export interface UseOfflineAttendanceQueueResult {
  state: OfflineQueueState;
  enqueue: (input: AttendanceQueueInput) => Promise<void>;
  flush: () => Promise<void>;
}

const initialState: OfflineQueueState = {
  pending: 0,
  failed: 0,
  synced: 0,
  ready: false,
};

function summarize(items: AttendanceQueueItem[]): OfflineQueueState {
  let pending = 0;
  let failed = 0;
  let synced = 0;
  for (const item of items) {
    if (item.status === 'pending' || item.status === 'syncing') pending += 1;
    else if (item.status === 'failed') failed += 1;
    else if (item.status === 'synced') synced += 1;
  }
  return { pending, failed, synced, ready: true };
}

export function useOfflineAttendanceQueue(
  options: UseOfflineAttendanceQueueOptions,
): UseOfflineAttendanceQueueResult {
  const { dbName, upsertFn } = options;

  // Stash the upsertFn in a ref so the online listener never closes over a
  // stale callback when the parent re-renders.
  const upsertRef = React.useRef(upsertFn);
  React.useEffect(() => {
    upsertRef.current = upsertFn;
  }, [upsertFn]);

  const [state, setState] = React.useState<OfflineQueueState>(initialState);
  const queueRef = React.useRef<AttendanceQueue | null>(null);

  const refreshState = React.useCallback(async () => {
    const queue = queueRef.current;
    if (!queue) return;
    const items = await queue.list();
    setState(summarize(items));
  }, []);

  // Wrap drain so both the online listener AND the user-triggered flush use
  // the same code path.
  const drain = React.useCallback(async () => {
    const queue = queueRef.current;
    if (!queue) return;
    await queue.drain((item) => upsertRef.current(item));
    await refreshState();
  }, [refreshState]);

  // Initialise the queue exactly once per dbName.
  React.useEffect(() => {
    let cancelled = false;
    const queue = createAttendanceQueue(dbName ? { dbName } : undefined);
    queueRef.current = queue;

    void (async () => {
      await queue.init();
      if (cancelled) return;
      await refreshState();
    })();

    return () => {
      cancelled = true;
      // We deliberately do not close the queue here — IDB connections close
      // automatically on tab unload, and a hot-reload re-mount can recover
      // the previous handle.
    };
  }, [dbName, refreshState]);

  // online → automatic drain.
  React.useEffect(() => {
    function handleOnline() {
      void drain();
    }
    window.addEventListener('online', handleOnline);
    return () => {
      window.removeEventListener('online', handleOnline);
    };
  }, [drain]);

  const enqueue = React.useCallback(
    async (input: AttendanceQueueInput) => {
      const queue = queueRef.current;
      if (!queue) return;
      await queue.enqueue(input);
      await refreshState();
    },
    [refreshState],
  );

  const flush = React.useCallback(async () => {
    await drain();
  }, [drain]);

  return { state, enqueue, flush };
}
