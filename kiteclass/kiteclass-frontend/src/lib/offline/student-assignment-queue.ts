/**
 * Student assignment offline submit queue (Wave 49 Bucket C — GAP-269).
 *
 * When a student submits an assignment but the network is down (mobile signal
 * loss is the dominant offline scenario for the S. Student persona), we keep
 * their draft in `localStorage` and replay it once the browser fires
 * `online`. This is the additive companion to {@link AttendanceQueue} for
 * teacher offline-roster — same shape, separate namespace + payload.
 *
 * IMPORTANT: this module does NOT touch `public/sw.js`. The service worker
 * handles offline navigation fallback only; the submit-queue lives in the
 * page context because it needs auth headers (the SW does not yet know how
 * to inject the JWT). When the kc-core push endpoint lands, this module is
 * the seam where Background Sync (Periodic Sync API) can be added.
 *
 * Storage layout: a single key `kc.student.offline-submits` containing a
 * JSON array of {@link QueuedSubmit}. Items are kept until the server
 * acknowledges (HTTP 2xx); failures stay queued and retry on next
 * `flush()` call.
 *
 * Design pattern: Command + Outbox (per design-patterns.md §2 — events with
 * DB txn). Each queued submit IS a command; flush replays them in order.
 */

const STORAGE_KEY = 'kc.student.offline-submits';

export interface QueuedSubmit {
  /** Stable client-generated ID — used for idempotency on the server. */
  id: string;
  assignmentId: string;
  /** Free-form student response (text, possibly base64-encoded attachment ref). */
  body: string;
  /** Epoch ms — when the student first hit "Submit" while offline. */
  queuedAt: number;
  /** Number of times we have attempted to flush this item. */
  attempts: number;
}

export interface FlushResult {
  flushed: number;
  remaining: number;
  failures: Array<{ id: string; error: string }>;
}

/**
 * Read the current queue from `localStorage`. Tolerates SSR + corrupt
 * storage by returning `[]` rather than throwing — the queue is best-effort
 * by design.
 */
export function readQueue(): QueuedSubmit[] {
  if (typeof window === 'undefined') return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as QueuedSubmit[]) : [];
  } catch {
    return [];
  }
}

function writeQueue(queue: QueuedSubmit[]): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
  } catch {
    // Quota exceeded or storage disabled — drop silently. The submit will
    // be re-queued from the form on next attempt.
  }
}

/**
 * Append a submit to the queue. Returns the queued item (with generated
 * id + queuedAt) so the caller can show "Đã lưu nháp — sẽ gửi khi online"
 * referencing the id.
 */
export function enqueue(input: {
  assignmentId: string;
  body: string;
}): QueuedSubmit {
  const item: QueuedSubmit = {
    id: generateId(),
    assignmentId: input.assignmentId,
    body: input.body,
    queuedAt: Date.now(),
    attempts: 0,
  };
  const queue = readQueue();
  queue.push(item);
  writeQueue(queue);
  return item;
}

/**
 * Remove a queued item by id. Used when the server confirms or the user
 * explicitly discards a draft.
 */
export function remove(id: string): void {
  const queue = readQueue().filter((item) => item.id !== id);
  writeQueue(queue);
}

/**
 * Replay the queue against the given submitter. Each item is sent in order;
 * 2xx responses are removed from the queue, 4xx/5xx and network errors are
 * left in place with `attempts` incremented for backoff display.
 *
 * The submitter signature accepts the raw payload + idempotency id so the
 * caller can wire it to whatever HTTP client they like (fetch, axios, etc).
 */
export async function flush(
  submit: (item: QueuedSubmit) => Promise<{ ok: boolean; error?: string }>,
): Promise<FlushResult> {
  const queue = readQueue();
  if (queue.length === 0) {
    return { flushed: 0, remaining: 0, failures: [] };
  }

  const failures: FlushResult['failures'] = [];
  const remaining: QueuedSubmit[] = [];
  let flushed = 0;

  for (const item of queue) {
    try {
      const result = await submit({ ...item, attempts: item.attempts + 1 });
      if (result.ok) {
        flushed += 1;
        continue;
      }
      remaining.push({ ...item, attempts: item.attempts + 1 });
      failures.push({ id: item.id, error: result.error ?? 'submit failed' });
    } catch (err) {
      remaining.push({ ...item, attempts: item.attempts + 1 });
      failures.push({
        id: item.id,
        error: err instanceof Error ? err.message : String(err),
      });
    }
  }

  writeQueue(remaining);
  return { flushed, remaining: remaining.length, failures };
}

/** Best-effort UUID-ish — uses `crypto.randomUUID` when available. */
function generateId(): string {
  if (
    typeof globalThis.crypto !== 'undefined' &&
    typeof globalThis.crypto.randomUUID === 'function'
  ) {
    return globalThis.crypto.randomUUID();
  }
  // Fallback for old test runners: time + random suffix.
  return `q-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

/** Test/recovery helper — never call from production UI. */
export function __resetQueue(): void {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(STORAGE_KEY);
  }
}
