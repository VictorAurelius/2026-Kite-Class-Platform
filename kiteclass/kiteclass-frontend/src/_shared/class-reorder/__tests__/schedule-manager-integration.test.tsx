/**
 * Smoke test for `@kite/shared-ui` ClassScheduleManager integration (G4, Wave 28).
 *
 * Verifies Bucket B can import + render G4 with a minimal sample slot list.
 * Real wiring lives in `/classes/[id]/schedule` page (out-of-scope this PR —
 * the page exists as `attendance/` only today; schedule sub-route arrives in
 * a follow-up). This test guards the import path + types.
 *
 * @since Wave 30 (2026-05-06)
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ClassScheduleManager } from '@kite/shared-ui';
import type { ScheduleSlot } from '@kite/shared-ui';

describe('@kite/shared-ui ClassScheduleManager integration', () => {
  it('renders the empty state with the className header', () => {
    render(
      <ClassScheduleManager
        className="Lớp Toán 6A"
        slots={[]}
        state="empty"
      />,
    );

    // Empty-state rendering should at least surface the className somewhere.
    expect(screen.getByText(/Lớp Toán 6A/)).toBeInTheDocument();
  });

  it('renders the saved state with one one-time slot', () => {
    const slot: ScheduleSlot = {
      id: 'slot-1',
      className: 'Lớp Toán 6A',
      teacherName: 'Cô Lan',
      date: '2026-05-11',
      startTime: '08:00',
      endTime: '09:30',
      recurrence: 'CUSTOM',
      daysOfWeek: ['MON'],
    };

    render(
      <ClassScheduleManager
        className="Lớp Toán 6A"
        slots={[slot]}
        state="saved"
      />,
    );

    // Smoke — component mounted without throwing; className surfaces.
    expect(screen.getAllByText(/Lớp Toán 6A/).length).toBeGreaterThan(0);
  });
});
