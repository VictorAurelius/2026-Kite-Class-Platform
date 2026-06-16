/**
 * Tests for OnboardingChecklist (Wave 78 GAP-538).
 *
 * Uses MSW handlers from `src/test/msw/handlers/onboarding.ts` for the happy
 * path; per-test `server.use(...)` overrides cover error states.
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/msw/server';
import { resetOnboardingHandlerState } from '@/test/msw/handlers/onboarding';
import { OnboardingChecklist } from '../OnboardingChecklist';

/** Build a 3-part JWT whose payload carries the given claims (no real signature). */
const makeJwt = (claims: Record<string, unknown>) =>
  `eyJhbGciOiJIUzI1NiJ9.${btoa(JSON.stringify(claims))}.sig`;

// GAP-1445: OnboardingChecklist now skips the fetch when the JWT has no tenantId
// claim (onboarding is per-tenant). Tests of the fetch path need a token that
// carries one.
const TENANT_JWT = makeJwt({ tenantId: '00000000-0000-0000-0000-000000000001' });

beforeEach(() => {
  resetOnboardingHandlerState();
  // Provide a bearer token so MSW handler doesn't 401.
  // GAP-599 Wave 92 Bucket B: api client reads from sessionStorage (per-tab isolation).
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.setItem('accessToken', TENANT_JWT);
  }
});

describe('OnboardingChecklist', () => {
  it('GAP-1445: renders nothing when JWT has no tenantId (tenantless owner skips fetch)', async () => {
    // Onboarding is per-tenant; a platform owner with no tenant context must not
    // hit the tenant-scoped endpoint (would reject TENANT_CONTEXT_MISSING).
    sessionStorage.setItem('accessToken', makeJwt({ sub: 'owner-1' })); // no tenantId

    const { container } = render(<OnboardingChecklist />);

    // No fetch fired → no checklist, no loading spinner, no error alert.
    await waitFor(() => {
      expect(screen.queryByTestId('onboarding-checklist')).not.toBeInTheDocument();
    });
    expect(screen.queryByTestId('onboarding-checklist-error')).not.toBeInTheDocument();
    expect(container).toBeEmptyDOMElement();
  });

  it('renders 5 steps in canonical order after fetch', async () => {
    render(<OnboardingChecklist />);
    await waitFor(() => {
      expect(screen.getByTestId('onboarding-checklist')).toBeInTheDocument();
    });

    const list = screen.getByTestId('onboarding-step-list');
    const items = list.querySelectorAll('li[data-step-id]');
    expect(items).toHaveLength(5);
    expect(items[0]).toHaveAttribute('data-step-id', 'PROFILE_SETUP');
    expect(items[1]).toHaveAttribute('data-step-id', 'INVITE_TEAM');
    expect(items[2]).toHaveAttribute('data-step-id', 'IMPORT_DATA');
    expect(items[3]).toHaveAttribute('data-step-id', 'CREATE_FIRST_CLASS');
    expect(items[4]).toHaveAttribute('data-step-id', 'EXPLORE_FEATURES');
  });

  it('shows 0% progress initially when no steps completed', async () => {
    render(<OnboardingChecklist />);
    await waitFor(() => {
      expect(screen.getByTestId('onboarding-progress-percent')).toHaveTextContent('0%');
    });
  });

  it('updates percent after toggling PROFILE_SETUP', async () => {
    const user = userEvent.setup();
    render(<OnboardingChecklist />);

    await waitFor(() => {
      expect(screen.getByTestId('onboarding-checklist')).toBeInTheDocument();
    });

    const profileItem = document.querySelector('li[data-step-id="PROFILE_SETUP"]');
    expect(profileItem).toBeTruthy();
    const toggleBtn = profileItem!.querySelector('button');
    expect(toggleBtn).toBeTruthy();

    await act(async () => {
      await user.click(toggleBtn!);
    });

    await waitFor(() => {
      expect(screen.getByTestId('onboarding-progress-percent')).toHaveTextContent('20%');
    });
  });

  it('opens demo-data confirmation dialog before IMPORT_DATA opt-in', async () => {
    const user = userEvent.setup();
    render(<OnboardingChecklist />);

    await waitFor(() => {
      expect(screen.getByTestId('onboarding-checklist')).toBeInTheDocument();
    });

    const importItem = document.querySelector('li[data-step-id="IMPORT_DATA"]');
    expect(importItem).toBeTruthy();
    const toggleBtn = importItem!.querySelector('button');

    await act(async () => {
      await user.click(toggleBtn!);
    });

    expect(screen.getByTestId('onboarding-demo-confirm')).toBeInTheDocument();
    expect(screen.getByText(/bật dữ liệu mẫu cho tài khoản/i)).toBeInTheDocument();

    // Confirm opt-in
    await act(async () => {
      await user.click(screen.getByTestId('onboarding-demo-confirm-cta'));
    });

    await waitFor(() => {
      expect(screen.queryByTestId('onboarding-demo-confirm')).not.toBeInTheDocument();
    });
  });

  it('shows error state when GET endpoint returns 500', async () => {
    server.use(
      http.get('*/api/v1/onboarding-progress', () => {
        return HttpResponse.json({ error: 'INTERNAL' }, { status: 500 });
      })
    );

    render(<OnboardingChecklist />);

    await waitFor(() => {
      expect(screen.getByTestId('onboarding-checklist-error')).toBeInTheDocument();
    });
  });

  it('renders disclaimer description (Vietnamese) for IMPORT_DATA opt-in', async () => {
    render(<OnboardingChecklist />);
    await waitFor(() => {
      expect(screen.getByTestId('onboarding-checklist')).toBeInTheDocument();
    });
    expect(screen.getByText(/Tải lên danh sách học viên/i)).toBeInTheDocument();
  });

  // GAP-545 — Wave 79 Bucket D — WCAG 2.1.1 + 2.4.3
  it('closes demo-confirm dialog on Escape key (WCAG 2.1.1)', async () => {
    const user = userEvent.setup();
    render(<OnboardingChecklist />);
    await waitFor(() => {
      expect(screen.getByTestId('onboarding-checklist')).toBeInTheDocument();
    });

    const importItem = document.querySelector('li[data-step-id="IMPORT_DATA"]');
    const toggleBtn = importItem!.querySelector('button');
    await act(async () => {
      await user.click(toggleBtn!);
    });

    expect(screen.getByTestId('onboarding-demo-confirm')).toBeInTheDocument();

    await act(async () => {
      await user.keyboard('{Escape}');
    });

    await waitFor(() => {
      expect(screen.queryByTestId('onboarding-demo-confirm')).not.toBeInTheDocument();
    });
  });

  it('traps focus inside demo-confirm dialog when opened (WCAG 2.4.3)', async () => {
    const user = userEvent.setup();
    render(<OnboardingChecklist />);
    await waitFor(() => {
      expect(screen.getByTestId('onboarding-checklist')).toBeInTheDocument();
    });

    const importItem = document.querySelector('li[data-step-id="IMPORT_DATA"]');
    const toggleBtn = importItem!.querySelector('button');
    await act(async () => {
      await user.click(toggleBtn!);
    });

    await waitFor(() => {
      const dialog = screen.getByTestId('onboarding-demo-confirm');
      expect(dialog.contains(document.activeElement)).toBe(true);
    });
  });
});
