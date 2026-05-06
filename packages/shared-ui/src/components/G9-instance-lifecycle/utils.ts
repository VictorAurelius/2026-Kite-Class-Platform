/**
 * State-machine helpers for G9 Instance Lifecycle.
 *
 * Single source of truth for the transition graph specified by
 * `.claude/rules/ai-branding-guidelines.md` §6:
 *
 *   NOT_STARTED -> INITIALIZING -> GENERATING -> DEPLOYED <-> REGENERATING
 *                     |               |             ^
 *                   FAILED <----- FAILED ---------+ (retry)
 *
 * The graph is encoded as an `Object.freeze`d adjacency map — a lookup table,
 * NOT a status switch/if cascade (per `design-patterns.md` §3.3).  Adding a
 * new state means adding a key + array entry; no code branches change.
 *
 * Why a pure function instead of a class-based state machine: this module is
 * shared between client + server; keeping it dependency-free + side-effect-free
 * means it can be imported from a Next.js server component without pulling
 * any React runtime in.  The full state-machine enforcement
 * (`IllegalStateException`-shaped behaviour) lives in the backend
 * `InstanceLifecycleService` (per AI Branding §6); this helper exists so the
 * frontend can validate optimistic UI hints + render `LifecycleEvent` lists
 * without contradicting the rule.
 */

import type { InstanceLifecycleState } from './types';

/**
 * Adjacency map of legal transitions.  Keys are source states; values list
 * the destination states reachable in one step.
 *
 * Notes per `ai-branding-guidelines.md` §6:
 *  - `NOT_STARTED -> INITIALIZING` (the wizard kicks off provisioning).
 *  - `INITIALIZING` may proceed to `GENERATING` OR jump straight to `FAILED`
 *    (subdomain reservation / DB / TLS errors).
 *  - `GENERATING` may proceed to `DEPLOYED` OR jump to `FAILED`
 *    (quality-gate fail per §5 InstanceQualityReviewer).
 *  - `DEPLOYED <-> REGENERATING` is bidirectional — the owner can request a
 *    refresh, and a successful regen returns to DEPLOYED.
 *  - `REGENERATING -> DEPLOYED` (success) and `REGENERATING -> DEPLOYED` only
 *    happens once the new render passes the quality gate.  A failed regen
 *    drops back to `DEPLOYED` with the previous render still live (per the
 *    HTML proto `regenerating.html`) — the failure case for regen is modelled
 *    as a side-effect, not a state.  If the owner explicitly accepts a
 *    failed regen result we additionally permit `REGENERATING -> FAILED` for
 *    the rare admin-forced fail.
 *  - `FAILED -> GENERATING` is the retry path (per the §6 `+ (retry)` arrow);
 *    a retry kicks off a fresh generation pass with the existing analyser
 *    result.  We disallow `FAILED -> DEPLOYED` directly — the retry MUST go
 *    back through GENERATING so the §5 quality gate runs again.
 */
const TRANSITION_GRAPH: Readonly<
  Record<InstanceLifecycleState, readonly InstanceLifecycleState[]>
> = Object.freeze({
  NOT_STARTED: Object.freeze(['INITIALIZING', 'FAILED'] as const),
  INITIALIZING: Object.freeze(['GENERATING', 'FAILED'] as const),
  GENERATING: Object.freeze(['DEPLOYED', 'FAILED'] as const),
  DEPLOYED: Object.freeze(['REGENERATING'] as const),
  REGENERATING: Object.freeze(['DEPLOYED', 'FAILED'] as const),
  // Retry path: FAILED -> GENERATING (per §6 `+ (retry)` arrow) — regen has
  // to go through generation again so the quality gate fires.
  FAILED: Object.freeze(['GENERATING'] as const),
});

/**
 * Return `true` if `from -> to` is a legal transition per the graph above.
 *
 * Self-transitions (`from === to`) return `false` — there is no idle-tick
 * transition; the backend never publishes a same-to-same event.
 *
 * Unknown states (TypeScript-erased values arriving via JSON) return `false`
 * fail-safe, matching the spirit of the §6 `IllegalStateException` rule.
 */
export function validTransition(
  from: InstanceLifecycleState,
  to: InstanceLifecycleState,
): boolean {
  if (from === to) return false;
  const allowed = TRANSITION_GRAPH[from];
  if (!allowed) return false;
  return allowed.includes(to);
}
