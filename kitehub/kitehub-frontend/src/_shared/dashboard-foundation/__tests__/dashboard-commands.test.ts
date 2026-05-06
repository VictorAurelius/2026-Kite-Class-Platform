/**
 * KH_DASHBOARD_COMMANDS dataset tests — Wave 31 Bucket A.
 *
 * Guards the structural contract Bucket B/C/D consume.
 */

import { describe, it, expect } from 'vitest';
import { KH_DASHBOARD_COMMANDS } from '../dashboard-commands';

describe('KH_DASHBOARD_COMMANDS', () => {
  it('contains the three sidebar sections from kitehub-pro v2 spec', () => {
    const sections = new Set(KH_DASHBOARD_COMMANDS.map((c) => c.section));
    expect(sections.has('Tổng quan')).toBe(true);
    expect(sections.has('Tài chính')).toBe(true);
    expect(sections.has('Cài đặt')).toBe(true);
  });

  it('every command has a stable id + label + section', () => {
    for (const command of KH_DASHBOARD_COMMANDS) {
      expect(command.id).toBeTruthy();
      expect(command.label).toBeTruthy();
      expect(command.section).toBeTruthy();
    }
  });

  it('each command has either an href or onSelect (no orphan commands)', () => {
    for (const command of KH_DASHBOARD_COMMANDS) {
      const hasAction =
        typeof command.href === 'string' || typeof command.onSelect === 'function';
      expect(hasAction).toBe(true);
    }
  });

  it('command ids are unique', () => {
    const ids = KH_DASHBOARD_COMMANDS.map((c) => c.id);
    const unique = new Set(ids);
    expect(unique.size).toBe(ids.length);
  });
});
