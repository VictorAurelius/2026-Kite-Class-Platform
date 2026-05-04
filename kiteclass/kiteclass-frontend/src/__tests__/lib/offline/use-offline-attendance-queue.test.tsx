/**
 * Tests for the {@code useOfflineAttendanceQueue} hook + sync badge.
 *
 * The hook owns:
 *   - lazy queue init (per dbName)
 *   - enqueue() helper that writes to IDB
 *   - online-event listener that auto-drains
 *   - exposed `state` summarizing pending/syncing/failed counts
 *
 * @since 4.x.x (Wave 18b3 Bucket A)
 */

import * as React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import 'fake-indexeddb/auto';
import { render, screen, act, waitFor } from '@testing-library/react';
import { useOfflineAttendanceQueue } from '../../../lib/offline/use-offline-attendance-queue';
import type { AttendancePeriodBatchEntry } from '../../../lib/api/attendance-period';

const sampleEntry: AttendancePeriodBatchEntry = {
  studentId: 101,
  classId: 7,
  subjectSectionId: 21,
  periodNo: 3,
  date: '2026-05-04',
  status: 'PRESENT',
};

interface HarnessProps {
  dbName: string;
  upsertFn: () => Promise<void>;
  flushOnMount?: boolean;
}

function Harness({ dbName, upsertFn, flushOnMount }: HarnessProps) {
  const queue = useOfflineAttendanceQueue({ dbName, upsertFn });

  React.useEffect(() => {
    if (flushOnMount) {
      void queue.flush();
    }
  }, [flushOnMount, queue]);

  return (
    <div>
      <div data-testid="pending">{queue.state.pending}</div>
      <div data-testid="failed">{queue.state.failed}</div>
      <div data-testid="synced">{queue.state.synced}</div>
      <button
        type="button"
        data-testid="enqueue-btn"
        onClick={() => {
          void queue.enqueue({
            teacherId: 9,
            classId: 7,
            date: '2026-05-04',
            entries: [sampleEntry],
          });
        }}
      >
        Enqueue
      </button>
      <button
        type="button"
        data-testid="flush-btn"
        onClick={() => {
          void queue.flush();
        }}
      >
        Flush
      </button>
    </div>
  );
}

describe('useOfflineAttendanceQueue', () => {
  beforeEach(() => {
    vi.useRealTimers();
  });

  it('starts with all counts at zero', async () => {
    const upsertFn = vi.fn(async () => undefined);
    render(
      <Harness dbName={`hook-init-${Date.now()}`} upsertFn={upsertFn} />,
    );

    await waitFor(() => {
      expect(screen.getByTestId('pending').textContent).toBe('0');
      expect(screen.getByTestId('failed').textContent).toBe('0');
      expect(screen.getByTestId('synced').textContent).toBe('0');
    });
  });

  it('enqueue bumps pending count', async () => {
    const upsertFn = vi.fn(async () => undefined);
    render(
      <Harness dbName={`hook-enqueue-${Date.now()}`} upsertFn={upsertFn} />,
    );

    const btn = await screen.findByTestId('enqueue-btn');
    await act(async () => {
      btn.click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('pending').textContent).toBe('1');
    });
  });

  it('flush calls upsertFn and moves item from pending → synced', async () => {
    const upsertFn = vi.fn(async () => undefined);
    render(
      <Harness dbName={`hook-flush-${Date.now()}`} upsertFn={upsertFn} />,
    );

    const enqueueBtn = await screen.findByTestId('enqueue-btn');
    await act(async () => {
      enqueueBtn.click();
    });
    await waitFor(() =>
      expect(screen.getByTestId('pending').textContent).toBe('1'),
    );

    const flushBtn = screen.getByTestId('flush-btn');
    await act(async () => {
      flushBtn.click();
    });

    await waitFor(() => {
      expect(upsertFn).toHaveBeenCalledTimes(1);
      expect(screen.getByTestId('pending').textContent).toBe('0');
      expect(screen.getByTestId('synced').textContent).toBe('1');
    });
  });

  it('online event triggers automatic drain', async () => {
    const upsertFn = vi.fn(async () => undefined);
    render(
      <Harness dbName={`hook-online-${Date.now()}`} upsertFn={upsertFn} />,
    );

    const enqueueBtn = await screen.findByTestId('enqueue-btn');
    await act(async () => {
      enqueueBtn.click();
    });
    await waitFor(() =>
      expect(screen.getByTestId('pending').textContent).toBe('1'),
    );

    // Fire a real `online` event on window — the hook attaches a listener.
    await act(async () => {
      window.dispatchEvent(new Event('online'));
    });

    await waitFor(() => {
      expect(upsertFn).toHaveBeenCalled();
      expect(screen.getByTestId('synced').textContent).toBe('1');
    });
  });
});
