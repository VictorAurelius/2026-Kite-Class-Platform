/**
 * G9 Instance Lifecycle Status — RTL coverage of all 6 spec'd states +
 * retry CTA visibility + event timeline rendering.
 *
 * Spec source: `ui_kits/components/G9-instance-lifecycle/README.md` + 6 HTML
 * state files under `states/`.  Vietnamese-only labels per CLAUDE.md.
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { InstanceLifecycleStatus } from '../InstanceLifecycleStatus';
import type {
  InstanceLifecycleState,
  LifecycleEvent,
} from '../types';

const T0 = '2026-04-29T07:00:00Z';
const T1 = '2026-04-29T07:01:00Z';
const T2 = '2026-04-29T07:02:00Z';
const T3 = '2026-04-29T07:03:00Z';

function fullEvents(): LifecycleEvent[] {
  return [
    { from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: T0 },
    { from: 'INITIALIZING', to: 'GENERATING', timestamp: T1 },
    { from: 'GENERATING', to: 'DEPLOYED', timestamp: T2 },
  ];
}

function renderG9(
  state: InstanceLifecycleState,
  options: {
    events?: readonly LifecycleEvent[];
    liveUrl?: string;
    onRetry?: () => void;
  } = {},
) {
  const { events = [], liveUrl, onRetry } = options;
  return render(
    <InstanceLifecycleStatus
      instanceId="INST-2026-0429-003"
      instanceName="Trung tâm Hoa Mặt Trời"
      state={state}
      events={events}
      liveUrl={liveUrl}
      onRetry={onRetry}
    />,
  );
}

describe('<InstanceLifecycleStatus> — 6 state renders', () => {
  it('renders NOT_STARTED with Chưa khởi tạo pill + empty timeline copy', () => {
    renderG9('NOT_STARTED');

    const pill = screen.getByTestId('instance-lifecycle-pill');
    expect(pill).toHaveTextContent('Chưa khởi tạo');
    expect(pill).toHaveAttribute('aria-label', 'Chưa khởi tạo');

    expect(
      screen.getByTestId('instance-lifecycle-empty-timeline'),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId('instance-lifecycle-alert'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId('instance-lifecycle-retry'),
    ).not.toBeInTheDocument();
  });

  it('renders INITIALIZING with Đang khởi tạo pill (info colour)', () => {
    renderG9('INITIALIZING', {
      events: [{ from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: T0 }],
    });

    const pill = screen.getByTestId('instance-lifecycle-pill');
    expect(pill).toHaveTextContent('Đang khởi tạo');
  });

  it('renders GENERATING with Đang tạo pill (warning colour)', () => {
    renderG9('GENERATING', {
      events: [
        { from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: T0 },
        { from: 'INITIALIZING', to: 'GENERATING', timestamp: T1 },
      ],
    });

    const pill = screen.getByTestId('instance-lifecycle-pill');
    expect(pill).toHaveTextContent('Đang tạo');
  });

  it('renders DEPLOYED with Đã triển khai pill + live URL link', () => {
    renderG9('DEPLOYED', {
      events: fullEvents(),
      liveUrl: 'edison.kiteclass.vn',
    });

    const pill = screen.getByTestId('instance-lifecycle-pill');
    expect(pill).toHaveTextContent('Đã triển khai');

    const live = screen.getByTestId('instance-lifecycle-live-url');
    expect(within(live).getByText('edison.kiteclass.vn')).toBeInTheDocument();
    const visit = within(live).getByRole('link', { name: /truy cập/i });
    expect(visit).toHaveAttribute('href', 'https://edison.kiteclass.vn');
    expect(visit).toHaveAttribute('target', '_blank');
    expect(visit).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('renders REGENERATING with Đang tạo lại pill', () => {
    renderG9('REGENERATING', {
      events: [
        ...fullEvents(),
        { from: 'DEPLOYED', to: 'REGENERATING', timestamp: T3 },
      ],
    });

    const pill = screen.getByTestId('instance-lifecycle-pill');
    expect(pill).toHaveTextContent('Đang tạo lại');
  });

  it('renders FAILED with Lỗi pill + role="alert" banner', () => {
    renderG9('FAILED', {
      events: [
        { from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: T0 },
        { from: 'INITIALIZING', to: 'GENERATING', timestamp: T1 },
        {
          from: 'GENERATING',
          to: 'FAILED',
          timestamp: T2,
          reason: 'Quality gate điểm 62/100 — chưa đạt ngưỡng 70/100',
        },
      ],
    });

    expect(screen.getByTestId('instance-lifecycle-pill')).toHaveTextContent(
      'Lỗi',
    );

    const alert = screen.getByRole('alert');
    expect(alert).toHaveAttribute('aria-live', 'polite');
    expect(alert).toHaveTextContent('Có lỗi xảy ra');

    // Reason copy renders in the timeline
    const timeline = screen.getByTestId('instance-lifecycle-timeline');
    expect(
      within(timeline).getByText(/quality gate điểm 62\/100/i),
    ).toBeInTheDocument();
  });
});

describe('<InstanceLifecycleStatus> — retry CTA visibility', () => {
  it('renders retry CTA only when state=FAILED AND onRetry provided', async () => {
    const onRetry = vi.fn();
    renderG9('FAILED', {
      events: [
        { from: 'GENERATING', to: 'FAILED', timestamp: T2, reason: 'qa fail' },
      ],
      onRetry,
    });

    const retry = screen.getByTestId('instance-lifecycle-retry');
    expect(retry).toHaveTextContent('Thử lại');

    await userEvent.click(retry);
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('does NOT render retry CTA when state=FAILED but onRetry is omitted', () => {
    renderG9('FAILED', {
      events: [{ from: 'GENERATING', to: 'FAILED', timestamp: T2 }],
      // no onRetry
    });

    expect(
      screen.queryByTestId('instance-lifecycle-retry'),
    ).not.toBeInTheDocument();
    // The role="alert" banner still renders even without retry handler.
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('does NOT render retry CTA when state≠FAILED even if onRetry provided', () => {
    const onRetry = vi.fn();
    renderG9('GENERATING', { events: fullEvents().slice(0, 2), onRetry });

    expect(
      screen.queryByTestId('instance-lifecycle-retry'),
    ).not.toBeInTheDocument();
    expect(onRetry).not.toHaveBeenCalled();
  });
});

describe('<InstanceLifecycleStatus> — event timeline rendering', () => {
  it('renders one <li> per LifecycleEvent in chronological order', () => {
    renderG9('DEPLOYED', { events: fullEvents() });

    const items = screen.getAllByTestId(/^instance-lifecycle-event-/);
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveAttribute(
      'data-testid',
      'instance-lifecycle-event-INITIALIZING',
    );
    expect(items[1]).toHaveAttribute(
      'data-testid',
      'instance-lifecycle-event-GENERATING',
    );
    expect(items[2]).toHaveAttribute(
      'data-testid',
      'instance-lifecycle-event-DEPLOYED',
    );
  });

  it('marks the last event with aria-current="step" (current event in lifecycle)', () => {
    renderG9('DEPLOYED', { events: fullEvents() });

    const last = screen.getByTestId('instance-lifecycle-event-DEPLOYED');
    expect(last.querySelector('[aria-current="step"]')).not.toBeNull();

    const earlier = screen.getByTestId('instance-lifecycle-event-INITIALIZING');
    expect(earlier.querySelector('[aria-current="step"]')).toBeNull();
  });

  it('formats event timestamps as dd/MM/yyyy HH:mm:ss (Vietnamese convention)', () => {
    renderG9('GENERATING', {
      events: [{ from: 'INITIALIZING', to: 'GENERATING', timestamp: T1 }],
    });

    const row = screen.getByTestId('instance-lifecycle-event-GENERATING');
    // T1 = 2026-04-29T07:01:00Z -> "29/04/2026 07:01:00"
    expect(within(row).getByText(/29\/04\/2026 07:01:00/)).toBeInTheDocument();
  });

  it('uses lang="vi" by default on the wrapper', () => {
    renderG9('NOT_STARTED');
    const root = screen.getByTestId('instance-lifecycle-root');
    expect(root).toHaveAttribute('lang', 'vi');
  });
});
