/**
 * Lightweight client-side onboarding telemetry.
 *
 * Records onboarding funnel events (start, step view, skip, complete, replay)
 * to a capped localStorage ring buffer + console.debug. No external analytics
 * service — keeps the telemetry self-contained for Phase 1 BETA (per GAP-288
 * AC "track tour completion rate + skip rate"). A later wave can flush this
 * buffer to a backend analytics endpoint without changing call sites.
 *
 * @author KiteClass Team
 * @since 4.0.0 — GAP-288 first-login onboarding telemetry
 */

const EVENTS_STORAGE_KEY = 'kiteclass-onboarding-events';
const MAX_EVENTS = 50;

export type OnboardingEventType =
  | 'onboarding_start'
  | 'onboarding_step_view'
  | 'onboarding_step_skip'
  | 'onboarding_complete'
  | 'onboarding_replay';

export interface OnboardingEvent {
  type: OnboardingEventType;
  /** 1-based step index where applicable; omitted for start/complete/replay. */
  step?: number;
  /** ISO timestamp of the event. */
  at: string;
}

function readEvents(): OnboardingEvent[] {
  try {
    const raw = localStorage.getItem(EVENTS_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as OnboardingEvent[]) : [];
  } catch {
    return [];
  }
}

/**
 * Record one onboarding funnel event. Best-effort: a storage failure
 * (quota, private mode) must never throw into the UI render path.
 */
export function trackOnboarding(type: OnboardingEventType, step?: number): void {
  const event: OnboardingEvent = {
    type,
    ...(typeof step === 'number' ? { step } : {}),
    at: new Date().toISOString(),
  };

  try {
    const events = readEvents();
    events.push(event);
    // Cap the ring buffer so localStorage never grows unbounded.
    const capped = events.slice(-MAX_EVENTS);
    localStorage.setItem(EVENTS_STORAGE_KEY, JSON.stringify(capped));
  } catch {
    // Ignore persistence failures — telemetry is best-effort.
  }

  // Console breadcrumb for dev/debug; harmless in production.
  if (typeof console !== 'undefined' && typeof console.debug === 'function') {
    // eslint-disable-next-line no-console
    console.debug('[onboarding]', type, step ?? '');
  }
}

/** Return the recorded onboarding events (for tests / future flush). */
export function getOnboardingEvents(): OnboardingEvent[] {
  return readEvents();
}

/** Clear recorded onboarding events. */
export function clearOnboardingEvents(): void {
  try {
    localStorage.removeItem(EVENTS_STORAGE_KEY);
  } catch {
    // Ignore.
  }
}

/**
 * Funnel summary derived from recorded events — completion + skip rates.
 * `completionRate` / `skipRate` are 0..1 fractions of starts that reached
 * complete / had ≥1 skip respectively. Returns zeros when no starts recorded.
 */
export function summarizeOnboardingFunnel(): {
  starts: number;
  completes: number;
  skips: number;
  completionRate: number;
  skipRate: number;
} {
  const events = readEvents();
  const starts = events.filter((e) => e.type === 'onboarding_start').length;
  const completes = events.filter((e) => e.type === 'onboarding_complete').length;
  const skips = events.filter((e) => e.type === 'onboarding_step_skip').length;
  return {
    starts,
    completes,
    skips,
    completionRate: starts > 0 ? completes / starts : 0,
    skipRate: starts > 0 ? Math.min(skips, starts) / starts : 0,
  };
}
