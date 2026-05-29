/**
 * Student offline-submit queue tests — Wave 49 Bucket C (GAP-269).
 *
 * Validates the four contracts the UI relies on:
 *   1. enqueue/readQueue round-trip
 *   2. remove() drops only the matching id
 *   3. flush() with successful submitter empties queue
 *   4. flush() with failing submitter keeps items + bumps attempts
 */

import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  __resetQueue,
  enqueue,
  flush,
  readQueue,
  remove,
} from '../student-assignment-queue';

describe('student-assignment-queue', () => {
  beforeEach(() => {
    __resetQueue();
  });
  afterEach(() => {
    __resetQueue();
  });

  it('enqueue() persists items readable by readQueue()', () => {
    const item = enqueue({ assignmentId: 'asg-1', body: 'Câu 1: a' });
    expect(item.id).toBeTruthy();
    expect(item.attempts).toBe(0);
    expect(item.queuedAt).toBeGreaterThan(0);

    const queue = readQueue();
    expect(queue).toHaveLength(1);
    expect(queue[0]).toMatchObject({
      id: item.id,
      assignmentId: 'asg-1',
      body: 'Câu 1: a',
    });
  });

  it('remove() drops only the matching id', () => {
    const a = enqueue({ assignmentId: 'asg-1', body: 'one' });
    const b = enqueue({ assignmentId: 'asg-2', body: 'two' });
    remove(a.id);
    const queue = readQueue();
    expect(queue.map((q) => q.id)).toEqual([b.id]);
  });

  it('flush() empties queue when submitter returns ok', async () => {
    enqueue({ assignmentId: 'asg-1', body: 'one' });
    enqueue({ assignmentId: 'asg-2', body: 'two' });

    const result = await flush(async () => ({ ok: true }));

    expect(result.flushed).toBe(2);
    expect(result.remaining).toBe(0);
    expect(result.failures).toEqual([]);
    expect(readQueue()).toEqual([]);
  });

  it('flush() keeps failed items with incremented attempts', async () => {
    enqueue({ assignmentId: 'asg-1', body: 'one' });
    enqueue({ assignmentId: 'asg-2', body: 'two' });

    const result = await flush(async (item) => {
      if (item.assignmentId === 'asg-2') {
        return { ok: false, error: 'http 500' };
      }
      return { ok: true };
    });

    expect(result.flushed).toBe(1);
    expect(result.remaining).toBe(1);
    expect(result.failures).toEqual([
      expect.objectContaining({ error: 'http 500' }),
    ]);

    const remaining = readQueue();
    expect(remaining).toHaveLength(1);
    expect(remaining[0]!.assignmentId).toBe('asg-2');
    expect(remaining[0]!.attempts).toBe(1);
  });

  it('flush() catches thrown errors from the submitter', async () => {
    enqueue({ assignmentId: 'asg-1', body: 'one' });

    const result = await flush(async () => {
      throw new Error('network down');
    });

    expect(result.flushed).toBe(0);
    expect(result.remaining).toBe(1);
    expect(result.failures[0]!.error).toBe('network down');
    expect(readQueue()[0]!.attempts).toBe(1);
  });

  it('readQueue() returns [] for fresh storage / corrupted JSON', () => {
    expect(readQueue()).toEqual([]);
    window.localStorage.setItem('kc.student.offline-submits', 'not-json{');
    expect(readQueue()).toEqual([]);
  });
});
