'use client';

/**
 * G9 — Instance Lifecycle Status.
 *
 * Replaces the empty `/instances/[id]` skeleton (33/128) and the matching
 * `/admin/instances/[id]` admin view with a state-aware page covering the 6
 * lifecycle states defined in `.claude/rules/ai-branding-guidelines.md` §6:
 *
 *   NOT_STARTED -> INITIALIZING -> GENERATING -> DEPLOYED <-> REGENERATING
 *                     |               |             ^
 *                   FAILED <----- FAILED ---------+ (retry)
 *
 * Per `dossier/04-component-gaps.md` §G9 + the kit README at
 * `ui_kits/components/G9-instance-lifecycle/README.md` + the 6 spec'd HTML
 * state files under `states/`.
 *
 * Design-patterns compliance (per `.claude/rules/design-patterns.md`):
 *  - State Pattern via lookup map (NO `if (state === 'X') ... else if ...`
 *    cascades — see `STATE_VISUAL`).
 *  - No primitive obsession: the `InstanceLifecycleState` union is the only
 *    state type used; events carry typed `from` + `to`.
 *  - Retry CTA visibility derived from `state` + `onRetry` presence — no
 *    boolean flag prop hack.
 *
 * Vietnamese formatting:
 *  - State labels per kit README §States table:
 *      NOT_STARTED   -> "Chưa khởi tạo"
 *      INITIALIZING  -> "Đang khởi tạo"
 *      GENERATING    -> "Đang tạo"
 *      DEPLOYED      -> "Đã triển khai"
 *      REGENERATING  -> "Đang tạo lại"
 *      FAILED        -> "Lỗi"
 *  - Datetime: `dd/MM/yyyy HH:mm:ss` (UTC accessors so test fixtures + prod
 *    emit the same string regardless of TZ — same trick as G10).
 *
 * Accessibility (WCAG AA — contrast measurements documented in the HTML
 * proto under `states/*.html`):
 *  - Status pill carries `role="status"` + `aria-label` with the state copy.
 *  - FAILED state additionally renders `role="alert"` + `aria-live="polite"`.
 *  - Timeline `<ol aria-label="Timeline trạng thái instance">`.
 *  - Current-step icon carries `aria-current="step"`; other icons are
 *    `aria-hidden` decorative.  Meaning is conveyed by the title text.
 *  - Step icon glyphs (✓ / ⏳ / ✗ / 🔁 / 🚀) are `aria-hidden`.
 *  - Retry CTA is a `<button type="button">` so screen readers announce it
 *    correctly + keyboard tab order is natural.
 */

import type React from 'react';
import { useMemo } from 'react';
import type {
  InstanceLifecycleState,
  InstanceLifecycleStatusProps,
  LifecycleEvent,
} from './types';

const COPY = {
  instanceLabel: 'Instance',
  liveUrlLabel: 'Truy cập trang',
  copyUrlLabel: 'Sao chép địa chỉ',
  retryLabel: 'Thử lại',
  timelineHeading: 'Tiến trình',
  timelineLabel: 'Timeline trạng thái instance',
  failedAlertTitle: 'Có lỗi xảy ra',
  failedAlertHint:
    'Đội ngũ kỹ thuật đã được thông báo. Bạn có thể nhấn "Thử lại" để chạy lại quá trình tạo.',
  state: {
    NOT_STARTED: 'Chưa khởi tạo',
    INITIALIZING: 'Đang khởi tạo',
    GENERATING: 'Đang tạo',
    DEPLOYED: 'Đã triển khai',
    REGENERATING: 'Đang tạo lại',
    FAILED: 'Lỗi',
  } as const satisfies Record<InstanceLifecycleState, string>,
} as const;

/**
 * Per-state visual treatment table.  Pattern: lookup map, NOT switch (per
 * `design-patterns.md` §3.3).  Adding a 7th state means adding a row;
 * no logic branches change.
 */
const STATE_VISUAL: Readonly<
  Record<
    InstanceLifecycleState,
    {
      pillClass: string;
      iconChar: string;
      pulse: boolean;
    }
  >
> = Object.freeze({
  NOT_STARTED: { pillClass: 'bg-muted text-muted-foreground', iconChar: '◯', pulse: false },
  INITIALIZING: {
    pillClass: 'bg-info/10 text-[hsl(var(--info))]',
    iconChar: '⏳',
    pulse: true,
  },
  GENERATING: {
    pillClass: 'bg-warning/10 text-[hsl(var(--warning))]',
    iconChar: '⏳',
    pulse: true,
  },
  DEPLOYED: {
    pillClass: 'bg-success/10 text-[hsl(var(--success))]',
    iconChar: '✓',
    pulse: false,
  },
  REGENERATING: {
    pillClass: 'bg-info/10 text-[hsl(var(--info))]',
    iconChar: '🔁',
    pulse: true,
  },
  FAILED: {
    pillClass: 'bg-destructive/10 text-destructive',
    iconChar: '✗',
    pulse: false,
  },
});

/**
 * Format an ISO-8601 timestamp string as `dd/MM/yyyy HH:mm:ss` (Vietnamese
 * convention per kit README §Vietnamese UX).
 *
 * Defensive: invalid date strings render as the original string so a
 * malformed event timestamp does not crash the render.  UTC accessors keep
 * test fixtures stable across runner TZs.
 */
function formatVNDateTime(isoString: string): string {
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return isoString;
  const dd = String(date.getUTCDate()).padStart(2, '0');
  const mm = String(date.getUTCMonth() + 1).padStart(2, '0');
  const yyyy = date.getUTCFullYear();
  const hh = String(date.getUTCHours()).padStart(2, '0');
  const mn = String(date.getUTCMinutes()).padStart(2, '0');
  const ss = String(date.getUTCSeconds()).padStart(2, '0');
  return `${dd}/${mm}/${yyyy} ${hh}:${mn}:${ss}`;
}

/** Stable copy + sort by `timestamp` ascending — does not mutate caller. */
function sortEventsAsc(events: readonly LifecycleEvent[]): LifecycleEvent[] {
  return [...events].sort((a, b) => {
    const ta = new Date(a.timestamp).getTime();
    const tb = new Date(b.timestamp).getTime();
    if (Number.isNaN(ta) || Number.isNaN(tb)) return 0;
    return ta - tb;
  });
}

export function InstanceLifecycleStatus(
  props: InstanceLifecycleStatusProps,
): React.JSX.Element {
  const {
    state,
    events,
    instanceName,
    instanceId,
    liveUrl,
    onRetry,
    lang = 'vi',
  } = props;

  const visual = STATE_VISUAL[state];
  const stateLabel = COPY.state[state];
  const sortedEvents = useMemo(() => sortEventsAsc(events), [events]);

  const isFailed = state === 'FAILED';
  const showRetry = isFailed && typeof onRetry === 'function';
  const showLiveUrl = state === 'DEPLOYED' && Boolean(liveUrl);

  return (
    <div
      data-testid="instance-lifecycle-root"
      lang={lang}
      data-state={state}
      className="min-h-full bg-muted/30 text-foreground"
    >
      <main className="mx-auto w-full max-w-3xl space-y-5 px-4 py-6 sm:px-6">
        {/* Header — instance name + ID + state pill */}
        <section className="rounded-2xl border bg-card p-6 shadow-soft">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {COPY.instanceLabel}
              </p>
              <h1 className="mt-1 text-xl font-bold md:text-2xl">
                {instanceName ?? instanceId}
              </h1>
              <p
                data-testid="instance-lifecycle-id"
                className="mt-0.5 font-mono text-xs text-muted-foreground"
              >
                {instanceId}
              </p>
            </div>
            <span
              role="status"
              aria-label={stateLabel}
              data-testid="instance-lifecycle-pill"
              className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold ${visual.pillClass}`}
            >
              <span
                aria-hidden="true"
                className={`h-1.5 w-1.5 rounded-full bg-current ${
                  visual.pulse ? 'motion-safe:animate-pulse' : ''
                }`}
              />
              {stateLabel}
            </span>
          </div>

          {/* DEPLOYED — show live URL + actions */}
          {showLiveUrl && (
            <div
              data-testid="instance-lifecycle-live-url"
              className="mt-5 flex flex-wrap items-center gap-3 border-t pt-5"
            >
              <p className="font-mono text-base font-semibold">{liveUrl}</p>
              <a
                href={`https://${liveUrl}`}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 rounded-md border bg-background px-3 py-1.5 text-sm font-medium hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {COPY.liveUrlLabel}
              </a>
            </div>
          )}

          {/* FAILED — alert banner + retry CTA */}
          {isFailed && (
            <div
              role="alert"
              aria-live="polite"
              data-testid="instance-lifecycle-alert"
              className="mt-5 rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive"
            >
              <p className="font-semibold">{COPY.failedAlertTitle}</p>
              <p className="mt-0.5 text-destructive/90">{COPY.failedAlertHint}</p>
              {showRetry && (
                <button
                  type="button"
                  onClick={onRetry}
                  data-testid="instance-lifecycle-retry"
                  className="mt-3 inline-flex items-center gap-1.5 rounded-md bg-destructive px-3 py-1.5 text-sm font-semibold text-destructive-foreground hover:bg-destructive/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {COPY.retryLabel}
                </button>
              )}
            </div>
          )}
        </section>

        {/* Timeline */}
        <section className="rounded-2xl border bg-card p-6 shadow-soft">
          <h2 className="mb-4 text-base font-semibold">{COPY.timelineHeading}</h2>
          {sortedEvents.length === 0 ? (
            <p
              data-testid="instance-lifecycle-empty-timeline"
              className="text-sm text-muted-foreground"
            >
              Chưa có sự kiện nào.
            </p>
          ) : (
            <ol
              aria-label={COPY.timelineLabel}
              className="relative space-y-4"
              data-testid="instance-lifecycle-timeline"
            >
              {sortedEvents.map((event, idx) => {
                const isLast = idx === sortedEvents.length - 1;
                const eventVisual = STATE_VISUAL[event.to];
                return (
                  <li
                    key={`${event.from}-${event.to}-${event.timestamp}`}
                    data-testid={`instance-lifecycle-event-${event.to}`}
                    className="flex gap-4"
                  >
                    <div className="flex flex-col items-center">
                      <span
                        {...(isLast ? { 'aria-current': 'step' as const } : {})}
                        aria-hidden={!isLast}
                        className={`inline-flex h-8 w-8 items-center justify-center rounded-full text-sm ${eventVisual.pillClass}`}
                      >
                        {eventVisual.iconChar}
                      </span>
                      {!isLast && (
                        <span
                          aria-hidden="true"
                          className="mt-1 w-px flex-1 bg-border"
                        />
                      )}
                    </div>

                    <div className="flex-1 pb-2">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <p className="text-sm font-semibold">
                          {COPY.state[event.from]}
                          {' → '}
                          {COPY.state[event.to]}
                        </p>
                        <time
                          dateTime={event.timestamp}
                          className="font-mono text-xs text-muted-foreground"
                        >
                          {formatVNDateTime(event.timestamp)}
                        </time>
                      </div>
                      {event.reason && (
                        <p className="mt-0.5 text-sm text-muted-foreground">
                          {event.reason}
                        </p>
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
          )}
        </section>
      </main>
    </div>
  );
}
