/**
 * Tests for classesApi.generateSessionsFromRecurrence wire contract (KC-3 walk fix).
 *
 * BE RecurrenceRuleDto binds snake_case keys via @JsonProperty (by_day / start_time
 * / end_time / exclude_dates). The FE RecurrenceRule type is camelCase, so the API
 * client MUST map to snake_case — otherwise BE sees null and rejects with 400
 * (NotEmpty/NotNull on by_day/start_time/end_time). This pins the mapping.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { apiClient } from '@/lib/api-client';
import { classesApi } from '../classes';
import type { RecurrenceRule } from '@/types/class';

vi.mock('@/lib/api-client', () => ({
  apiClient: { post: vi.fn() },
}));

describe('classesApi.generateSessionsFromRecurrence — snake_case wire contract', () => {
  beforeEach(() => vi.clearAllMocks());

  it('maps camelCase RecurrenceRule to BE snake_case keys', async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { data: [] } });

    const rule: RecurrenceRule = {
      freq: 'WEEKLY',
      byDay: ['MO', 'WE'],
      startTime: '18:00',
      endTime: '20:00',
      until: '2026-08-01',
    };
    await classesApi.generateSessionsFromRecurrence(22, rule);

    expect(apiClient.post).toHaveBeenCalledTimes(1);
    const [url, body] = vi.mocked(apiClient.post).mock.calls[0] as [string, Record<string, unknown>];
    expect(url).toBe('/api/v1/classes/22/sessions/generate-from-recurrence');
    expect(body).toMatchObject({
      freq: 'WEEKLY',
      by_day: ['MO', 'WE'],
      start_time: '18:00',
      end_time: '20:00',
      until: '2026-08-01',
    });
    // camelCase keys must NOT leak (they would arrive null at the BE).
    expect(body).not.toHaveProperty('byDay');
    expect(body).not.toHaveProperty('startTime');
    expect(body).not.toHaveProperty('endTime');
  });

  it('includes exclude_dates only when present', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: [] } });

    const base: RecurrenceRule = {
      freq: 'WEEKLY',
      byDay: ['MO'],
      startTime: '18:00',
      endTime: '20:00',
      until: '2026-08-01',
    };
    await classesApi.generateSessionsFromRecurrence(22, base);
    expect(apiClient.post).toHaveBeenNthCalledWith(
      1,
      expect.any(String),
      expect.not.objectContaining({ exclude_dates: expect.anything() }),
    );

    await classesApi.generateSessionsFromRecurrence(22, { ...base, excludeDates: ['2026-07-01'] });
    expect(apiClient.post).toHaveBeenNthCalledWith(
      2,
      expect.any(String),
      expect.objectContaining({ exclude_dates: ['2026-07-01'] }),
    );
  });
});
