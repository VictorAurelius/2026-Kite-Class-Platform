/**
 * G9 Instance Lifecycle — state machine validator tests.
 *
 * Asserts the transition graph in `utils.ts` matches verbatim the spec in
 * `.claude/rules/ai-branding-guidelines.md` §6:
 *
 *   NOT_STARTED -> INITIALIZING -> GENERATING -> DEPLOYED <-> REGENERATING
 *                     |               |             ^
 *                   FAILED <----- FAILED ---------+ (retry)
 */

import { describe, expect, it } from 'vitest';
import { validTransition } from '../utils';
import type { InstanceLifecycleState } from '../types';

describe('validTransition — happy path (legal transitions per §6)', () => {
  it('NOT_STARTED -> INITIALIZING is legal', () => {
    expect(validTransition('NOT_STARTED', 'INITIALIZING')).toBe(true);
  });

  it('INITIALIZING -> GENERATING is legal', () => {
    expect(validTransition('INITIALIZING', 'GENERATING')).toBe(true);
  });

  it('GENERATING -> DEPLOYED is legal (quality gate passed)', () => {
    expect(validTransition('GENERATING', 'DEPLOYED')).toBe(true);
  });

  it('DEPLOYED -> REGENERATING is legal (owner kicks refresh)', () => {
    expect(validTransition('DEPLOYED', 'REGENERATING')).toBe(true);
  });

  it('REGENERATING -> DEPLOYED is legal (regen succeeded, returns live)', () => {
    expect(validTransition('REGENERATING', 'DEPLOYED')).toBe(true);
  });

  it('any state -> FAILED is legal where the §6 graph allows it', () => {
    expect(validTransition('NOT_STARTED', 'FAILED')).toBe(true);
    expect(validTransition('INITIALIZING', 'FAILED')).toBe(true);
    expect(validTransition('GENERATING', 'FAILED')).toBe(true);
    expect(validTransition('REGENERATING', 'FAILED')).toBe(true);
  });

  it('FAILED -> GENERATING is legal (retry path per `+ (retry)` arrow)', () => {
    expect(validTransition('FAILED', 'GENERATING')).toBe(true);
  });
});

describe('validTransition — illegal transitions', () => {
  it('NOT_STARTED -> DEPLOYED is illegal (must go through INITIALIZING + GENERATING)', () => {
    expect(validTransition('NOT_STARTED', 'DEPLOYED')).toBe(false);
  });

  it('FAILED -> DEPLOYED is illegal (retry MUST re-run quality gate via GENERATING)', () => {
    expect(validTransition('FAILED', 'DEPLOYED')).toBe(false);
  });

  it('DEPLOYED -> NOT_STARTED is illegal (no rollback to virgin state)', () => {
    expect(validTransition('DEPLOYED', 'NOT_STARTED')).toBe(false);
  });

  it('DEPLOYED -> FAILED is illegal (DEPLOYED is terminal except via REGENERATING)', () => {
    // Per §6: DEPLOYED only transitions to REGENERATING.  A failure post-deploy
    // would land a new event with state=REGENERATING, not flip DEPLOYED itself.
    expect(validTransition('DEPLOYED', 'FAILED')).toBe(false);
  });

  it('GENERATING -> NOT_STARTED is illegal (no rewind)', () => {
    expect(validTransition('GENERATING', 'NOT_STARTED')).toBe(false);
  });

  it('FAILED -> INITIALIZING is illegal (retry skips INITIALIZING — analyser result reused)', () => {
    // Per §6 retry arrow: FAILED -> GENERATING (NOT INITIALIZING).  Re-running
    // INITIALIZING would re-provision the subdomain/DB/TLS which is wasteful.
    expect(validTransition('FAILED', 'INITIALIZING')).toBe(false);
  });

  it('self-transitions are illegal (no idle-tick events)', () => {
    const states: readonly InstanceLifecycleState[] = [
      'NOT_STARTED',
      'INITIALIZING',
      'GENERATING',
      'DEPLOYED',
      'REGENERATING',
      'FAILED',
    ];
    for (const s of states) {
      expect(validTransition(s, s)).toBe(false);
    }
  });

  it('unknown source state returns false fail-safe (TypeScript-erased values)', () => {
    // Cast-around so we can simulate JSON arriving with an unexpected enum
    // value without disabling the runtime check.
    const unknown = 'UNKNOWN_STATE' as unknown as InstanceLifecycleState;
    expect(validTransition(unknown, 'GENERATING')).toBe(false);
  });
});
