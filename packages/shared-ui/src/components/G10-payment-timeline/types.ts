/**
 * Type definitions for G10 Payment Status Timeline.
 *
 * Mirrors `ui_kits/components/G10-payment-timeline/README.md` + 5 state HTML files
 * (`states/{pending,paid,partial-paid,overdue,refunded}.html`) and `dossier/04-component-gaps.md` §G10.
 *
 * Two abstractions live here:
 *  - `PaymentTimelineState` — the high-level lifecycle bucket (5 buckets per spec README).
 *  - `PaymentTimelineStep` — granular event step rendered as one `<li>` in the timeline `<ol>`
 *    (7 step kinds; the timeline can render a subset/superset depending on the actual
 *    event log produced by the backend).
 */

/**
 * High-level payment state — drives the "current state" pill at the top + the
 * coarse colour story for the whole panel.
 *
 * Maps directly to the 5 spec'd HTML state files.
 */
export type PaymentTimelineState =
  | 'pending'
  | 'paid'
  | 'partial-paid'
  | 'overdue'
  | 'refunded';

/**
 * Per-event step kind.  This is the granular lifecycle event the timeline
 * `<ol>` shows — not the page-level "state".  A single timeline can contain
 * any combination of these in the order they actually fired, for example a
 * refunded-state timeline contains: `CREATED → PAYMENT_PENDING → PAYMENT_RECEIVED
 * → CONFIRMED → COMPLETED → REFUNDED`.
 *
 * Order of values below MIRRORS the canonical lifecycle order; `STEP_ORDER`
 * in the component preserves this order for sorting.
 */
export type PaymentTimelineStep =
  | 'CREATED'
  | 'PAYMENT_PENDING'
  | 'PAYMENT_RECEIVED'
  | 'CONFIRMED'
  | 'COMPLETED'
  | 'FAILED'
  | 'REFUNDED';

/**
 * The visual treatment for a step.  Component derives this internally based
 * on the step + whether it is current / past / future, but callers can
 * override per-event if backend semantics demand it.
 */
export type TimelineEventStatus = 'past' | 'current' | 'future' | 'failed';

/**
 * A single event in the payment timeline `<ol>`.
 *
 * Every event has `step` + `at` (timestamp).  `note` is a 1-line caption
 * shown under the step title, `actor` describes who did it (system or human),
 * and `amount` is rendered with `formatVNCurrency` from the G6 utils when
 * present (e.g. paid 1.500.000đ, refunded 1.500.000đ).
 */
export type TimelineEvent = {
  /** Granular step kind; controls icon + default colour. */
  step: PaymentTimelineStep;
  /** Event timestamp.  Rendered as `dd/MM/yyyy HH:mm` (Asia/Ho_Chi_Minh, UTC accessors). */
  at: Date;
  /** Optional 1-line caption shown under the step title. */
  note?: string;
  /** Optional actor — `'Hệ thống tự động'`, `'Cô Lê Thị Hà (Quản lý lớp)'`, etc. */
  actor?: string;
  /** Optional VND amount rendered via `formatVNCurrency` (re-used from G6). */
  amount?: number;
  /**
   * Optional explicit override for visual status.  Typically the component
   * derives `past`/`current`/`future` from event ordering vs the panel's
   * `currentStep`, but a backend may want to mark an event `'failed'`
   * even though chronologically older events succeeded.
   */
  status?: TimelineEventStatus;
};

export type PaymentStatusTimelineProps = {
  /** Invoice number e.g. `KC-2026-10-0042`. */
  invoiceNumber: string;
  /** High-level state bucket. Drives the top pill + page-level colour story. */
  state: PaymentTimelineState;
  /**
   * Granular events to render in the `<ol>`. The component sorts by
   * `at` ascending; the latest event with `status === 'current'` (or, if
   * none, the chronologically last event whose `step` matches the
   * `currentStep` derivation) wins the "current" treatment.
   *
   * Must contain at least one event (`CREATED` is universal — the
   * timeline starts here).
   */
  events: TimelineEvent[];
  /**
   * Optional explicit "current" step override.  When omitted, the
   * component derives it from `state` (see `derivedCurrentStep` in
   * `PaymentStatusTimeline.tsx`).
   */
  currentStep?: PaymentTimelineStep;
  /** Total amount payable in VND. Rendered in the header block. */
  totalAmount: number;
  /**
   * Layout orientation. Defaults to `'auto'`:
   *  - `'auto'` → vertical at all breakpoints (matches HTML proto;
   *    horizontal-on-desktop deferred to a follow-up scope).
   *  - `'vertical'` → forced vertical regardless of viewport.
   *  - `'horizontal'` → forced horizontal (rare; admin dashboards).
   *
   * The HTML proto in `states/*.html` is vertical-only — the
   * `'horizontal'` mode is a structural hook for the admin views and is
   * intentionally minimal in v1.
   */
  orientation?: 'vertical' | 'horizontal' | 'auto';
  /**
   * Override the wrapper `lang` attribute. Defaults to `'vi'`.
   * `'en'` falls back to `'vi'` for v1 (Vietnamese-first per CLAUDE.md).
   */
  lang?: 'vi' | 'en';
  /**
   * Embedded trong dashboard section → bỏ standalone page-chrome
   * (max-width / centering / bg / min-h). Default `false` = standalone full-page
   * (giữ backward-compat cho parent-page design tương lai). Khi nhúng vào dashboard
   * `/billing/[id]` hoặc card section, set `embedded` để component fill full width
   * + bỏ `bg-muted/30 min-h-full` island; dashboard tự lo padding + background.
   */
  embedded?: boolean;
};
