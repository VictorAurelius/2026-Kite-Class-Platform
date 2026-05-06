/**
 * Type definitions for G9 Instance Lifecycle Status.
 *
 * Mirrors `ui_kits/components/G9-instance-lifecycle/README.md` + 6 state HTML
 * files (`states/{not-started,initializing,generating,deployed,regenerating,failed}.html`)
 * and `dossier/04-component-gaps.md` §G9.
 *
 * State machine — verbatim copy of `.claude/rules/ai-branding-guidelines.md` §6:
 *
 *   NOT_STARTED -> INITIALIZING -> GENERATING -> DEPLOYED <-> REGENERATING
 *                     |               |             ^
 *                   FAILED <----- FAILED ---------+ (retry)
 *
 * Transitions go ONLY through the backend `InstanceLifecycleService` (per the
 * AI Branding rule).  This component is presentational — it renders whichever
 * state the backend reports + a timeline of past `LifecycleEvent`s.  Callers
 * may pass an `onRetry` handler that fires only when state === 'FAILED'.
 */

/**
 * The 6 lifecycle states an AI Branding instance can occupy.
 *
 * Maps directly to the 6 spec'd HTML state files.  The string form (uppercase
 * snake) matches the backend `InstanceLifecycleService` enum so this type can
 * be sourced verbatim from the API package contract per
 * `dossier/04-component-gaps.md` §G9.
 */
export type InstanceLifecycleState =
  | 'NOT_STARTED'
  | 'INITIALIZING'
  | 'GENERATING'
  | 'DEPLOYED'
  | 'REGENERATING'
  | 'FAILED';

/**
 * A single event in the lifecycle timeline `<ol>`.
 *
 * Every event records a state transition with a timestamp and optional reason
 * (e.g. quality-gate failure copy).  The `from` field is the prior state; the
 * `to` field is the state entered.  An `actor` is rarely needed (transitions
 * are system-driven) but is supported for the rare admin-forced override.
 */
export type LifecycleEvent = {
  /** State the instance left. */
  from: InstanceLifecycleState;
  /** State the instance entered. */
  to: InstanceLifecycleState;
  /**
   * ISO-8601 timestamp string (`YYYY-MM-DDTHH:mm:ss[.SSS]Z`).  String form keeps
   * the type wire-compatible with backend JSON without any Date parsing on the
   * caller side; the component renders it as `dd/MM/yyyy HH:mm:ss` for VN UX.
   */
  timestamp: string;
  /**
   * Optional human-readable reason — e.g. `Quality gate điểm 62/100 — chưa đạt
   * ngưỡng 70/100` for FAILED transitions.  Shown beneath the step title.
   */
  reason?: string;
  /** Optional actor — `'Hệ thống tự động'` / admin handle for forced overrides. */
  actor?: string;
};

export type InstanceLifecycleStatusProps = {
  /**
   * The instance's current lifecycle state.  Drives the page-level pill, the
   * primary copy block, and the retry-CTA visibility.
   */
  state: InstanceLifecycleState;
  /**
   * Past transitions, ordered any way (component sorts ascending by
   * `timestamp`).  May be empty in the initial NOT_STARTED state.
   */
  events: readonly LifecycleEvent[];
  /**
   * Tenant / instance display name shown in the header.  Optional — the
   * fallback is the instance ID.
   */
  instanceName?: string;
  /**
   * Instance identifier — `INST-2026-0429-003` style.  Always shown so admins
   * can correlate with logs.
   */
  instanceId: string;
  /**
   * Live URL once the instance reaches DEPLOYED (e.g. `edison.kiteclass.vn`).
   * Ignored for non-DEPLOYED states.  When provided in DEPLOYED state, a
   * `Truy cập` link + copy button render below the status pill.
   */
  liveUrl?: string;
  /**
   * Click handler invoked when the retry CTA is pressed.  Only rendered when
   * `state === 'FAILED'`; falsy in any other state.  Caller is responsible
   * for kicking the backend retry workflow.
   */
  onRetry?: () => void;
  /**
   * Override the wrapper `lang` attribute. Defaults to `'vi'`.  English
   * fallback is identical to Vietnamese for v1 (Vietnamese-first per
   * `CLAUDE.md` §Communication Language).
   */
  lang?: 'vi' | 'en';
};
