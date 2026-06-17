'use client';

/**
 * G10 — Payment Status Timeline.
 *
 * Replaces implicit status chips in production KH `/admin/payments` table + KC
 * `/billing/[id]`, per `dossier/04-component-gaps.md` §G10 +
 * `ui_kits/components/G10-payment-timeline/README.md` + the 5 spec'd HTML
 * state files (`states/pending.html`, `states/paid.html`,
 * `states/partial-paid.html`, `states/overdue.html`, `states/refunded.html`).
 *
 * Cross-component re-use:
 *   `formatVNCurrency` is imported via relative path from the sibling G6
 *   `utils.ts` (NOT via `@kite/shared-ui` — that would create a circular
 *   module reference inside the same package).  This is the proof-of-concept
 *   for sharing utilities across G* components within `@kite/shared-ui`.
 *   We re-export it from this module so test code + downstream consumers can
 *   obtain it via `import { formatVNCurrency } from '...G10-payment-timeline/PaymentStatusTimeline'`
 *   without reaching into the G6 internals — but the IDENTITY is preserved
 *   (`G10.formatVNCurrency === G6.formatVNCurrency`), so there's no
 *   copy-paste drift.
 *
 * Vietnamese formatting:
 *   - Currency: `1.500.000đ` (G6 helper).
 *   - Datetime: `dd/MM/yyyy HH:mm` (UTC accessors so test fixtures + production
 *     emit the same string regardless of TZ).
 *
 * Accessibility (WCAG AA, contrast measurements documented in
 * `states/*.html` HTML proto comments):
 *   - Timeline rendered as `<ol aria-label="Timeline trạng thái thanh toán">`.
 *   - Current step icon carries `aria-current="step"`.
 *   - Step icons are `aria-hidden` decorative; the step title text carries
 *     the meaning, color is not the only signal.
 *   - Status pill carries `role="status"`.
 *   - Overdue banner carries `role="alert"` + `aria-live="polite"`.
 *
 * No new deps — uses Tailwind tokens already shipped with the consuming
 * app's theme.
 */

import type React from 'react';
import { useMemo } from 'react';
import { formatVNCurrency } from '../G6-invoice-detail/utils';
import type {
  PaymentStatusTimelineProps,
  PaymentTimelineState,
  PaymentTimelineStep,
  TimelineEvent,
  TimelineEventStatus,
} from './types';

// Re-export the G6 helper so consumers + tests can verify the identity.
export { formatVNCurrency } from '../G6-invoice-detail/utils';

const COPY = {
  invoiceLabel: 'Hóa đơn',
  total: 'Số tiền cần trả',
  totalPaid: 'Đã thanh toán',
  totalRefunded: 'Đã hoàn',
  timelineHeading: 'Lịch sử trạng thái',
  timelineLabel: 'Timeline trạng thái thanh toán',
  overdueAlertTitle: 'Hóa đơn quá hạn',
  overdueAlertHint:
    'Vui lòng thanh toán sớm để tránh phát sinh thêm phí và đảm bảo quyền học của con.',
  state: {
    pending: 'Chờ thanh toán',
    paid: 'Đã thanh toán',
    'partial-paid': 'Trả góp',
    overdue: 'Quá hạn',
    refunded: 'Đã hoàn tiền',
  } as const satisfies Record<PaymentTimelineState, string>,
  step: {
    CREATED: 'Đã phát hành hóa đơn',
    PAYMENT_PENDING: 'Chờ thanh toán',
    PAYMENT_RECEIVED: 'Đã nhận thanh toán',
    CONFIRMED: 'Đã xác nhận',
    COMPLETED: 'Hoàn thành',
    FAILED: 'Thất bại',
    REFUNDED: 'Đã hoàn tiền',
  } as const satisfies Record<PaymentTimelineStep, string>,
};

/** Canonical lifecycle order — used to sort events when timestamps tie. */
const STEP_ORDER: Record<PaymentTimelineStep, number> = {
  CREATED: 0,
  PAYMENT_PENDING: 1,
  PAYMENT_RECEIVED: 2,
  CONFIRMED: 3,
  COMPLETED: 4,
  FAILED: 5,
  REFUNDED: 6,
};

function deriveCurrentStep(state: PaymentTimelineState): PaymentTimelineStep {
  switch (state) {
    case 'pending':
      return 'PAYMENT_PENDING';
    case 'paid':
      return 'COMPLETED';
    case 'partial-paid':
      return 'PAYMENT_RECEIVED';
    case 'overdue':
      return 'FAILED';
    case 'refunded':
      return 'REFUNDED';
    /* istanbul ignore next */
    default: {
      const _exhaustive: never = state;
      throw new Error(`unhandled state ${_exhaustive as string}`);
    }
  }
}

function statePillClasses(state: PaymentTimelineState): string {
  switch (state) {
    case 'paid':
      return 'bg-success/10 text-[hsl(var(--success))]';
    case 'overdue':
      return 'bg-destructive/10 text-destructive';
    case 'partial-paid':
      return 'bg-info/10 text-[hsl(var(--info))]';
    case 'refunded':
      return 'bg-warning/10 text-[hsl(var(--warning))]';
    case 'pending':
    default:
      return 'bg-warning/10 text-[hsl(var(--warning))]';
  }
}

function stepIconChar(step: PaymentTimelineStep): string {
  switch (step) {
    case 'CREATED':
    case 'PAYMENT_RECEIVED':
    case 'CONFIRMED':
    case 'COMPLETED':
      // Heavy check mark — past / done events.
      return '✓';
    case 'PAYMENT_PENDING':
      // Hourglass — current pending event.
      return '⏳';
    case 'FAILED':
      // Heavy ballot X — failed / overdue.
      return '✗';
    case 'REFUNDED':
      // Money with wings — refund event.
      return '💸';
  }
}

function stepIconClasses(
  status: TimelineEventStatus,
  step: PaymentTimelineStep,
): string {
  if (status === 'failed' || step === 'FAILED') {
    return 'bg-destructive/10 text-destructive';
  }
  if (step === 'REFUNDED') {
    return 'bg-warning/10 text-[hsl(var(--warning))]';
  }
  switch (status) {
    case 'past':
      return 'bg-success/10 text-[hsl(var(--success))]';
    case 'current':
      return 'bg-warning/10 text-[hsl(var(--warning))]';
    case 'future':
      return 'bg-muted text-muted-foreground';
  }
}

function connectorClasses(status: TimelineEventStatus, step: PaymentTimelineStep): string {
  if (status === 'failed' || step === 'FAILED') return 'bg-destructive/40';
  if (step === 'REFUNDED') return 'bg-warning/40';
  if (status === 'past') return 'bg-success/40';
  if (status === 'current') return 'bg-warning/40';
  return 'bg-border';
}

/**
 * Format a Date as `dd/MM/yyyy HH:mm` (Vietnamese convention).
 *
 * UTC accessors so the same fixture renders identically across TZs.
 * `Intl.DateTimeFormat('vi-VN', ...)` would honour the runner's local TZ,
 * which makes tests flaky in CI vs local dev — manual zero-pad keeps
 * the assertion stable.
 */
function formatVNDateTime(date: Date): string {
  const dd = String(date.getUTCDate()).padStart(2, '0');
  const mm = String(date.getUTCMonth() + 1).padStart(2, '0');
  const yyyy = date.getUTCFullYear();
  const hh = String(date.getUTCHours()).padStart(2, '0');
  const mn = String(date.getUTCMinutes()).padStart(2, '0');
  return `${dd}/${mm}/${yyyy} ${hh}:${mn}`;
}

/**
 * Sort events by `at` ascending; tie-breaker is canonical step order.
 *
 * Stable copy — does not mutate the caller's array.
 */
function sortEvents(events: readonly TimelineEvent[]): TimelineEvent[] {
  return [...events].sort((a, b) => {
    const dt = a.at.getTime() - b.at.getTime();
    if (dt !== 0) return dt;
    return STEP_ORDER[a.step] - STEP_ORDER[b.step];
  });
}

/**
 * Derive `past` / `current` / `future` / `failed` for each event.
 *
 * - Caller's explicit `event.status` always wins.
 * - Otherwise: events before the current-step index = `past`,
 *   the current-step event = `current`, after = `future`.
 *   FAILED step is always `failed` regardless of position.
 */
function annotateEvents(
  sorted: readonly TimelineEvent[],
  currentStep: PaymentTimelineStep,
): Array<TimelineEvent & { resolvedStatus: TimelineEventStatus }> {
  const currentIdx = sorted.findIndex((e) => e.step === currentStep);
  return sorted.map((e, i) => {
    if (e.status) return { ...e, resolvedStatus: e.status };
    if (e.step === 'FAILED') return { ...e, resolvedStatus: 'failed' as const };
    if (currentIdx === -1) {
      // Current step not present in the event log — treat the latest event as current.
      return { ...e, resolvedStatus: i === sorted.length - 1 ? 'current' : 'past' };
    }
    if (i < currentIdx) return { ...e, resolvedStatus: 'past' as const };
    if (i === currentIdx) return { ...e, resolvedStatus: 'current' as const };
    return { ...e, resolvedStatus: 'future' as const };
  });
}

export function PaymentStatusTimeline(
  props: PaymentStatusTimelineProps,
): React.JSX.Element {
  const {
    invoiceNumber,
    state,
    events,
    currentStep: explicitCurrent,
    totalAmount,
    orientation = 'auto',
    lang = 'vi',
    embedded = false,
  } = props;

  const currentStep = explicitCurrent ?? deriveCurrentStep(state);
  const annotated = useMemo(
    () => annotateEvents(sortEvents(events), currentStep),
    [events, currentStep],
  );

  const isOverdue = state === 'overdue';
  const isHorizontal = orientation === 'horizontal';

  const headerLabel =
    state === 'paid' || state === 'partial-paid'
      ? COPY.totalPaid
      : state === 'refunded'
        ? COPY.totalRefunded
        : COPY.total;

  return (
    <div
      data-testid="payment-timeline-root"
      lang={lang}
      data-orientation={orientation}
      className={
        embedded ? 'text-foreground' : 'min-h-full bg-muted/30 text-foreground'
      }
    >
      <main
        className={
          embedded
            ? 'w-full space-y-5'
            : 'mx-auto w-full max-w-3xl space-y-5 px-4 py-6 sm:px-6'
        }
      >
        {isOverdue && (
          <div
            role="alert"
            aria-live="polite"
            className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive"
          >
            <p className="font-semibold">{COPY.overdueAlertTitle}</p>
            <p className="mt-0.5 text-destructive/90">{COPY.overdueAlertHint}</p>
          </div>
        )}

        {/* Invoice header — invoice number + status pill + total */}
        <section className="rounded-2xl border bg-card p-6 shadow-soft">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {COPY.invoiceLabel}
              </p>
              <p className="mt-1 font-mono text-xl font-bold md:text-2xl">
                {invoiceNumber}
              </p>
            </div>
            <span
              role="status"
              aria-label={COPY.state[state]}
              className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold ${statePillClasses(state)}`}
            >
              <span
                aria-hidden="true"
                className={`h-1.5 w-1.5 rounded-full ${
                  state === 'pending' || state === 'overdue'
                    ? 'bg-current animate-pulse'
                    : 'bg-current'
                }`}
              />
              {COPY.state[state]}
            </span>
          </div>

          <div className="mt-5 border-t pt-5">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              {headerLabel}
            </p>
            <p
              data-testid="payment-timeline-total"
              className="mt-1 font-mono text-3xl font-bold md:text-4xl"
            >
              {formatVNCurrency(totalAmount)}
            </p>
          </div>
        </section>

        {/* Timeline */}
        <section className="rounded-2xl border bg-card p-6 shadow-soft">
          <h2 className="mb-4 text-base font-semibold">{COPY.timelineHeading}</h2>
          <ol
            aria-label={COPY.timelineLabel}
            className={
              isHorizontal
                ? 'flex flex-row gap-2 overflow-x-auto'
                : 'relative space-y-4'
            }
          >
            {annotated.map((event, idx) => {
              const isLast = idx === annotated.length - 1;
              const opacityClass =
                event.resolvedStatus === 'future' ? 'opacity-50' : '';
              return (
                <li
                  key={`${event.step}-${event.at.getTime()}`}
                  data-testid={`payment-timeline-event-${event.step}`}
                  className={
                    isHorizontal
                      ? `flex min-w-[10rem] flex-col items-center gap-2 ${opacityClass}`
                      : `flex gap-4 ${opacityClass}`
                  }
                >
                  <div
                    className={
                      isHorizontal
                        ? 'flex flex-row items-center'
                        : 'flex flex-col items-center'
                    }
                  >
                    <span
                      {...(event.resolvedStatus === 'current'
                        ? { 'aria-current': 'step' as const }
                        : {})}
                      aria-hidden={event.resolvedStatus !== 'current'}
                      className={`inline-flex h-8 w-8 items-center justify-center rounded-full text-sm ${stepIconClasses(event.resolvedStatus, event.step)}`}
                    >
                      {stepIconChar(event.step)}
                    </span>
                    {!isLast && (
                      <span
                        aria-hidden="true"
                        className={
                          isHorizontal
                            ? `mx-1 h-px w-8 ${connectorClasses(event.resolvedStatus, event.step)}`
                            : `mt-1 w-px flex-1 ${connectorClasses(event.resolvedStatus, event.step)}`
                        }
                      />
                    )}
                  </div>

                  <div className={isHorizontal ? 'text-center' : 'flex-1 pb-2'}>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p
                        className={`text-sm font-semibold ${
                          event.resolvedStatus === 'failed'
                            ? 'text-destructive'
                            : event.resolvedStatus === 'current'
                              ? 'text-[hsl(var(--warning))]'
                              : ''
                        }`}
                      >
                        {COPY.step[event.step]}
                        {typeof event.amount === 'number' && (
                          <>
                            {' — '}
                            <span className="font-mono">
                              {formatVNCurrency(event.amount)}
                            </span>
                          </>
                        )}
                      </p>
                      <time
                        dateTime={event.at.toISOString()}
                        className="font-mono text-xs text-muted-foreground"
                      >
                        {formatVNDateTime(event.at)}
                      </time>
                    </div>
                    {event.note && (
                      <p className="mt-0.5 text-sm text-muted-foreground">{event.note}</p>
                    )}
                    {event.actor && (
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        Bởi: {event.actor}
                      </p>
                    )}
                  </div>
                </li>
              );
            })}
          </ol>
        </section>
      </main>
    </div>
  );
}
