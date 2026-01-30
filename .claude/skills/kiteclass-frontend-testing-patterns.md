# KiteClass Frontend Testing Patterns

**Version:** 1.0.0
**Last Updated:** 2026-01-30
**Status:** Production Standard

---

## Table of Contents

1. [Overview](#overview)
2. [Testing Philosophy](#testing-philosophy)
3. [Feature Detection UI Testing](#feature-detection-ui-testing)
4. [Payment UI Testing](#payment-ui-testing)
5. [Guest Landing Page Testing](#guest-landing-page-testing)
6. [Trial Banner Testing](#trial-banner-testing)
7. [AI Branding UI Testing](#ai-branding-ui-testing)
8. [Theme System Testing](#theme-system-testing)
9. [React Query Caching Tests](#react-query-caching-tests)
10. [Zustand State Management Tests](#zustand-state-management-tests)
11. [Component Testing Patterns](#component-testing-patterns)
12. [Integration Testing](#integration-testing)
13. [Accessibility Testing](#accessibility-testing)
14. [Performance Testing](#performance-testing)

---

## Overview

### Purpose

This document provides comprehensive frontend testing patterns specifically designed for **KiteClass Platform**, covering:

- Multi-tenant UI behavior
- Feature detection UI (tier-based visibility)
- Payment UI (QR display, countdown, polling)
- Guest landing pages (B2B owner-centric model)
- Trial system UI (banners, countdowns, grace period warnings)
- AI branding UI (preview, generation, approval)
- Theme system (light/dark mode)
- React Query caching strategies
- Zustand state management

### Technology Stack

- **Framework:** Next.js 14+ (App Router)
- **UI Library:** React 18+ with TypeScript
- **Component Library:** shadcn/ui (Radix UI primitives)
- **Testing Framework:** Vitest + React Testing Library
- **E2E Testing:** Playwright (see `e2e-testing-standards.md`)
- **State Management:** Zustand
- **API Client:** React Query (TanStack Query)
- **Styling:** Tailwind CSS

### Testing Levels

1. **Component Tests** - Isolated component behavior (Vitest + RTL)
2. **Integration Tests** - Component interactions with API/state (MSW + RTL)
3. **E2E Tests** - Full user journeys (Playwright - see separate doc)

---

## Testing Philosophy

### Core Principles

1. **Test User Behavior, Not Implementation**
   - Use `getByRole`, `getByLabelText`, `getByText` over `getByTestId`
   - Simulate real user interactions (click, type, submit)
   - Avoid testing internal state or implementation details

2. **Test Multi-Tenant Context**
   - All tests must verify tenant isolation
   - Mock subdomain/instanceId context correctly
   - Verify tenant-specific data rendering

3. **Test Feature Detection UI**
   - Verify tier-based visibility (BASIC/STANDARD/PREMIUM)
   - Test upgrade prompts for locked features
   - Ensure graceful degradation for unavailable features

4. **Test Payment UI Flows**
   - QR code display and refresh
   - Countdown timers (10-minute expiry)
   - Payment status polling (every 5 seconds)
   - Success/failure handling

5. **Test Trial System UI**
   - Trial banners visibility (14-day trial, grace period, suspended)
   - Countdown displays (days remaining)
   - Upgrade prompts

6. **Accessibility First**
   - All interactive elements must be keyboard accessible
   - Screen reader support (ARIA labels, roles, live regions)
   - Color contrast compliance (WCAG AA)

---

## Feature Detection UI Testing

### Overview

Feature detection determines UI visibility based on the instance's pricing tier:

- **BASIC Tier:** Core features only (students, classes, attendance)
- **STANDARD Tier:** + Engagement tracking, basic reports
- **PREMIUM Tier:** + AI branding, advanced analytics, custom themes

### Test Structure

```typescript
// __tests__/components/features/FeatureGate.test.tsx
import { render, screen } from '@testing-library/react';
import { FeatureGate } from '@/components/features/FeatureGate';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('FeatureGate', () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient.clear();
  });

  describe('Basic Tier', () => {
    it('should show content for available features', async () => {
      // Mock instance config API
      server.use(
        http.get('/api/instance/config', () => {
          return HttpResponse.json({
            instanceId: '123e4567-e89b-12d3-a456-426614174000',
            tier: 'BASIC',
            features: {
              STUDENTS: true,
              CLASSES: true,
              ATTENDANCE: true,
              ENGAGEMENT: false,
              AI_BRANDING: false,
            },
          });
        })
      );

      render(
        <FeatureGate feature="STUDENTS">
          <div>Student Management</div>
        </FeatureGate>,
        { wrapper }
      );

      expect(await screen.findByText('Student Management')).toBeInTheDocument();
    });

    it('should show upgrade prompt for unavailable features', async () => {
      server.use(
        http.get('/api/instance/config', () => {
          return HttpResponse.json({
            instanceId: '123e4567-e89b-12d3-a456-426614174000',
            tier: 'BASIC',
            features: {
              STUDENTS: true,
              ENGAGEMENT: false,
            },
          });
        })
      );

      render(
        <FeatureGate feature="ENGAGEMENT">
          <div>Engagement Tracking</div>
        </FeatureGate>,
        { wrapper }
      );

      expect(await screen.findByText(/upgrade to STANDARD/i)).toBeInTheDocument();
      expect(screen.queryByText('Engagement Tracking')).not.toBeInTheDocument();
    });

    it('should render custom fallback for unavailable features', async () => {
      server.use(
        http.get('/api/instance/config', () => {
          return HttpResponse.json({
            instanceId: '123e4567-e89b-12d3-a456-426614174000',
            tier: 'BASIC',
            features: { AI_BRANDING: false },
          });
        })
      );

      render(
        <FeatureGate
          feature="AI_BRANDING"
          fallback={<div>AI Branding requires PREMIUM tier</div>}
        >
          <div>AI Branding Settings</div>
        </FeatureGate>,
        { wrapper }
      );

      expect(
        await screen.findByText('AI Branding requires PREMIUM tier')
      ).toBeInTheDocument();
    });
  });

  describe('Standard Tier', () => {
    it('should show engagement features', async () => {
      server.use(
        http.get('/api/instance/config', () => {
          return HttpResponse.json({
            instanceId: '123e4567-e89b-12d3-a456-426614174000',
            tier: 'STANDARD',
            features: {
              STUDENTS: true,
              ENGAGEMENT: true,
              AI_BRANDING: false,
            },
          });
        })
      );

      render(
        <FeatureGate feature="ENGAGEMENT">
          <div>Engagement Dashboard</div>
        </FeatureGate>,
        { wrapper }
      );

      expect(await screen.findByText('Engagement Dashboard')).toBeInTheDocument();
    });

    it('should hide premium features', async () => {
      server.use(
        http.get('/api/instance/config', () => {
          return HttpResponse.json({
            instanceId: '123e4567-e89b-12d3-a456-426614174000',
            tier: 'STANDARD',
            features: { AI_BRANDING: false },
          });
        })
      );

      render(
        <FeatureGate feature="AI_BRANDING">
          <div>AI Branding</div>
        </FeatureGate>,
        { wrapper }
      );

      expect(await screen.findByText(/upgrade to PREMIUM/i)).toBeInTheDocument();
    });
  });

  describe('Premium Tier', () => {
    it('should show all features', async () => {
      server.use(
        http.get('/api/instance/config', () => {
          return HttpResponse.json({
            instanceId: '123e4567-e89b-12d3-a456-426614174000',
            tier: 'PREMIUM',
            features: {
              STUDENTS: true,
              ENGAGEMENT: true,
              AI_BRANDING: true,
            },
          });
        })
      );

      render(
        <FeatureGate feature="AI_BRANDING">
          <div>AI Branding Settings</div>
        </FeatureGate>,
        { wrapper }
      );

      expect(await screen.findByText('AI Branding Settings')).toBeInTheDocument();
    });
  });

  describe('Caching', () => {
    it('should cache instance config to prevent duplicate API calls', async () => {
      let apiCallCount = 0;

      server.use(
        http.get('/api/instance/config', () => {
          apiCallCount++;
          return HttpResponse.json({
            instanceId: '123e4567-e89b-12d3-a456-426614174000',
            tier: 'BASIC',
            features: { STUDENTS: true },
          });
        })
      );

      const { rerender } = render(
        <FeatureGate feature="STUDENTS">
          <div>Content</div>
        </FeatureGate>,
        { wrapper }
      );

      await screen.findByText('Content');

      // Rerender should use cached data
      rerender(
        <FeatureGate feature="STUDENTS">
          <div>Content 2</div>
        </FeatureGate>
      );

      await screen.findByText('Content 2');

      expect(apiCallCount).toBe(1); // Only 1 API call
    });
  });
});
```

### Navigation Menu Visibility Test

```typescript
// __tests__/components/layout/Sidebar.test.tsx
import { render, screen } from '@testing-library/react';
import { Sidebar } from '@/components/layout/Sidebar';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('Sidebar - Feature-based Navigation', () => {
  it('should show only BASIC tier menu items', async () => {
    server.use(
      http.get('/api/instance/config', () => {
        return HttpResponse.json({
          tier: 'BASIC',
          features: {
            STUDENTS: true,
            CLASSES: true,
            ATTENDANCE: true,
            ENGAGEMENT: false,
            AI_BRANDING: false,
          },
        });
      })
    );

    render(<Sidebar />);

    // Visible items
    expect(await screen.findByRole('link', { name: /students/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /classes/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /attendance/i })).toBeInTheDocument();

    // Hidden items
    expect(screen.queryByRole('link', { name: /engagement/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /ai branding/i })).not.toBeInTheDocument();
  });

  it('should show upgrade badges for locked features in STANDARD tier', async () => {
    server.use(
      http.get('/api/instance/config', () => {
        return HttpResponse.json({
          tier: 'STANDARD',
          features: {
            STUDENTS: true,
            ENGAGEMENT: true,
            AI_BRANDING: false,
          },
        });
      })
    );

    render(<Sidebar />);

    expect(await screen.findByRole('link', { name: /engagement/i })).toBeInTheDocument();

    // AI Branding should show upgrade badge
    const aiBrandingItem = screen.queryByText(/ai branding/i);
    if (aiBrandingItem) {
      expect(aiBrandingItem.closest('div')).toHaveTextContent(/premium/i);
    }
  });

  it('should show all menu items for PREMIUM tier', async () => {
    server.use(
      http.get('/api/instance/config', () => {
        return HttpResponse.json({
          tier: 'PREMIUM',
          features: {
            STUDENTS: true,
            ENGAGEMENT: true,
            AI_BRANDING: true,
          },
        });
      })
    );

    render(<Sidebar />);

    expect(await screen.findByRole('link', { name: /students/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /engagement/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /ai branding/i })).toBeInTheDocument();
  });
});
```

### Upgrade Modal Test

```typescript
// __tests__/components/features/UpgradeModal.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UpgradeModal } from '@/components/features/UpgradeModal';

describe('UpgradeModal', () => {
  it('should display tier comparison table', () => {
    render(<UpgradeModal isOpen={true} requiredTier="STANDARD" currentTier="BASIC" />);

    expect(screen.getByText(/upgrade to standard/i)).toBeInTheDocument();
    expect(screen.getByText(/comparison/i)).toBeInTheDocument();

    // Check tier features
    expect(screen.getByText(/engagement tracking/i)).toBeInTheDocument();
    expect(screen.getByText(/basic reports/i)).toBeInTheDocument();
  });

  it('should navigate to pricing page on CTA click', async () => {
    const user = userEvent.setup();
    const mockRouter = { push: vi.fn() };

    render(
      <UpgradeModal
        isOpen={true}
        requiredTier="PREMIUM"
        currentTier="STANDARD"
        router={mockRouter}
      />
    );

    const upgradeButton = screen.getByRole('button', { name: /upgrade now/i });
    await user.click(upgradeButton);

    await waitFor(() => {
      expect(mockRouter.push).toHaveBeenCalledWith('/settings/pricing');
    });
  });

  it('should close modal when cancel is clicked', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(
      <UpgradeModal
        isOpen={true}
        requiredTier="STANDARD"
        currentTier="BASIC"
        onClose={onClose}
      />
    );

    const cancelButton = screen.getByRole('button', { name: /cancel/i });
    await user.click(cancelButton);

    expect(onClose).toHaveBeenCalled();
  });
});
```

---

## Payment UI Testing

### Overview

Payment UI includes:
- QR code display (VietQR format)
- 10-minute countdown timer
- Payment status polling (every 5 seconds)
- Success/failure feedback
- Retry mechanisms

### QR Code Display Test

```typescript
// __tests__/components/payment/PaymentQRCode.test.tsx
import { render, screen } from '@testing-library/react';
import { PaymentQRCode } from '@/components/payment/PaymentQRCode';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('PaymentQRCode', () => {
  it('should display QR code after order creation', async () => {
    server.use(
      http.post('/api/payments/orders', () => {
        return HttpResponse.json({
          orderId: 'ORD-20260130-ABC123',
          amount: 499000,
          qrCodeUrl: 'https://img.vietqr.io/image/MB-0123456789-compact.png',
          expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
        });
      })
    );

    render(<PaymentQRCode tier="STANDARD" />);

    const qrImage = await screen.findByRole('img', { name: /qr code/i });
    expect(qrImage).toHaveAttribute(
      'src',
      'https://img.vietqr.io/image/MB-0123456789-compact.png'
    );

    expect(screen.getByText(/499,000 VND/i)).toBeInTheDocument();
    expect(screen.getByText(/ORD-20260130-ABC123/i)).toBeInTheDocument();
  });

  it('should show loading state while creating order', () => {
    server.use(
      http.post('/api/payments/orders', async () => {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        return HttpResponse.json({});
      })
    );

    render(<PaymentQRCode tier="STANDARD" />);

    expect(screen.getByText(/generating qr code/i)).toBeInTheDocument();
    expect(screen.getByRole('status')).toBeInTheDocument(); // Loading spinner
  });

  it('should display error message on order creation failure', async () => {
    server.use(
      http.post('/api/payments/orders', () => {
        return HttpResponse.json(
          { error: 'Instance already has active subscription' },
          { status: 400 }
        );
      })
    );

    render(<PaymentQRCode tier="STANDARD" />);

    expect(
      await screen.findByText(/already has active subscription/i)
    ).toBeInTheDocument();
  });

  it('should refresh QR code when expired', async () => {
    const user = userEvent.setup();
    let callCount = 0;

    server.use(
      http.post('/api/payments/orders', () => {
        callCount++;
        return HttpResponse.json({
          orderId: `ORD-${callCount}`,
          qrCodeUrl: `https://img.vietqr.io/image/MB-${callCount}.png`,
          expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
        });
      })
    );

    render(<PaymentQRCode tier="STANDARD" />);

    await screen.findByText(/ORD-1/i);

    const refreshButton = screen.getByRole('button', { name: /refresh/i });
    await user.click(refreshButton);

    expect(await screen.findByText(/ORD-2/i)).toBeInTheDocument();
  });
});
```

### Countdown Timer Test

```typescript
// __tests__/components/payment/PaymentCountdown.test.tsx
import { render, screen, act } from '@testing-library/react';
import { PaymentCountdown } from '@/components/payment/PaymentCountdown';
import { vi } from 'vitest';

describe('PaymentCountdown', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should display countdown from 10 minutes', () => {
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000);

    render(<PaymentCountdown expiresAt={expiresAt} />);

    expect(screen.getByText(/9:59/i)).toBeInTheDocument(); // 10:00 - 1 second
  });

  it('should update countdown every second', () => {
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutes

    render(<PaymentCountdown expiresAt={expiresAt} />);

    expect(screen.getByText(/4:59/i)).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(1000); // 1 second
    });

    expect(screen.getByText(/4:58/i)).toBeInTheDocument();
  });

  it('should show warning style when under 2 minutes', () => {
    const expiresAt = new Date(Date.now() + 90 * 1000); // 1:30

    render(<PaymentCountdown expiresAt={expiresAt} />);

    const countdown = screen.getByText(/1:29/i);
    expect(countdown).toHaveClass('text-yellow-600'); // Warning color
  });

  it('should show danger style when under 1 minute', () => {
    const expiresAt = new Date(Date.now() + 30 * 1000); // 30 seconds

    render(<PaymentCountdown expiresAt={expiresAt} />);

    const countdown = screen.getByText(/0:29/i);
    expect(countdown).toHaveClass('text-red-600'); // Danger color
  });

  it('should call onExpire when countdown reaches 0', () => {
    const onExpire = vi.fn();
    const expiresAt = new Date(Date.now() + 5 * 1000); // 5 seconds

    render(<PaymentCountdown expiresAt={expiresAt} onExpire={onExpire} />);

    act(() => {
      vi.advanceTimersByTime(6000); // 6 seconds (past expiry)
    });

    expect(onExpire).toHaveBeenCalled();
    expect(screen.getByText(/expired/i)).toBeInTheDocument();
  });

  it('should stop countdown when component unmounts', () => {
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000);

    const { unmount } = render(<PaymentCountdown expiresAt={expiresAt} />);

    unmount();

    act(() => {
      vi.advanceTimersByTime(60000); // 1 minute
    });

    // No errors should occur (timer cleanup successful)
  });
});
```

### Payment Status Polling Test

```typescript
// __tests__/components/payment/PaymentStatusPoller.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import { PaymentStatusPoller } from '@/components/payment/PaymentStatusPoller';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { vi } from 'vitest';

describe('PaymentStatusPoller', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should poll payment status every 5 seconds', async () => {
    let pollCount = 0;

    server.use(
      http.get('/api/payments/orders/:orderId/status', () => {
        pollCount++;
        return HttpResponse.json({ status: 'PENDING' });
      })
    );

    render(<PaymentStatusPoller orderId="ORD-123" />);

    await waitFor(() => expect(pollCount).toBe(1));

    act(() => {
      vi.advanceTimersByTime(5000);
    });

    await waitFor(() => expect(pollCount).toBe(2));

    act(() => {
      vi.advanceTimersByTime(5000);
    });

    await waitFor(() => expect(pollCount).toBe(3));
  });

  it('should stop polling when payment is successful', async () => {
    let pollCount = 0;

    server.use(
      http.get('/api/payments/orders/:orderId/status', () => {
        pollCount++;
        if (pollCount === 2) {
          return HttpResponse.json({ status: 'PAID', tier: 'STANDARD' });
        }
        return HttpResponse.json({ status: 'PENDING' });
      })
    );

    render(<PaymentStatusPoller orderId="ORD-123" />);

    await waitFor(() => expect(pollCount).toBe(1));

    act(() => {
      vi.advanceTimersByTime(5000);
    });

    await waitFor(() => {
      expect(screen.getByText(/payment successful/i)).toBeInTheDocument();
    });

    act(() => {
      vi.advanceTimersByTime(10000); // Advance more time
    });

    // Should not poll again
    expect(pollCount).toBe(2);
  });

  it('should display success message with tier upgrade info', async () => {
    server.use(
      http.get('/api/payments/orders/:orderId/status', () => {
        return HttpResponse.json({
          status: 'PAID',
          tier: 'PREMIUM',
          validUntil: '2027-01-30T00:00:00Z',
        });
      })
    );

    render(<PaymentStatusPoller orderId="ORD-123" />);

    expect(
      await screen.findByText(/upgraded to premium/i)
    ).toBeInTheDocument();
    expect(screen.getByText(/valid until/i)).toBeInTheDocument();
  });

  it('should handle failed payment status', async () => {
    server.use(
      http.get('/api/payments/orders/:orderId/status', () => {
        return HttpResponse.json({
          status: 'FAILED',
          reason: 'Bank timeout',
        });
      })
    );

    render(<PaymentStatusPoller orderId="ORD-123" />);

    expect(await screen.findByText(/payment failed/i)).toBeInTheDocument();
    expect(screen.getByText(/bank timeout/i)).toBeInTheDocument();

    // Should show retry button
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });

  it('should stop polling on component unmount', async () => {
    let pollCount = 0;

    server.use(
      http.get('/api/payments/orders/:orderId/status', () => {
        pollCount++;
        return HttpResponse.json({ status: 'PENDING' });
      })
    );

    const { unmount } = render(<PaymentStatusPoller orderId="ORD-123" />);

    await waitFor(() => expect(pollCount).toBe(1));

    unmount();

    act(() => {
      vi.advanceTimersByTime(20000); // 20 seconds
    });

    // Should not continue polling after unmount
    expect(pollCount).toBe(1);
  });
});
```

---

## Guest Landing Page Testing

### Overview

Guest landing page requirements:
- **B2B owner-centric model**: Guest CANNOT self-register
- Must display instance branding (owner-configured)
- Show trial/subscription status
- Display contact information for enrollment

### Guest Page Rendering Test

```typescript
// __tests__/app/guest/page.test.tsx
import { render, screen } from '@testing-library/react';
import GuestPage from '@/app/guest/page';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('Guest Landing Page', () => {
  it('should display instance branding for trial instance', async () => {
    server.use(
      http.get('/api/instance/public-info', () => {
        return HttpResponse.json({
          instanceName: 'ABC Learning Center',
          logo: 'https://cdn.example.com/abc-logo.png',
          primaryColor: '#3B82F6',
          contactEmail: 'contact@abc-learning.com',
          contactPhone: '+84 901 234 567',
          status: 'TRIAL',
          trialDaysRemaining: 10,
        });
      })
    );

    render(<GuestPage />);

    // Branding
    expect(await screen.findByText('ABC Learning Center')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /logo/i })).toHaveAttribute(
      'src',
      expect.stringContaining('abc-logo.png')
    );

    // Trial status
    expect(screen.getByText(/10 days remaining/i)).toBeInTheDocument();

    // Contact info
    expect(screen.getByText('contact@abc-learning.com')).toBeInTheDocument();
    expect(screen.getByText('+84 901 234 567')).toBeInTheDocument();
  });

  it('should NOT show registration form (B2B model)', async () => {
    server.use(
      http.get('/api/instance/public-info', () => {
        return HttpResponse.json({
          instanceName: 'XYZ School',
          status: 'ACTIVE',
        });
      })
    );

    render(<GuestPage />);

    await screen.findByText('XYZ School');

    // No registration form should exist
    expect(screen.queryByRole('button', { name: /sign up/i })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/email/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
  });

  it('should display enrollment instructions', async () => {
    server.use(
      http.get('/api/instance/public-info', () => {
        return HttpResponse.json({
          instanceName: 'DEF Academy',
          status: 'ACTIVE',
          enrollmentInstructions:
            'Please contact our office at contact@def-academy.com to enroll.',
        });
      })
    );

    render(<GuestPage />);

    expect(
      await screen.findByText(/contact our office at contact@def-academy.com/i)
    ).toBeInTheDocument();
  });

  it('should display suspended instance message', async () => {
    server.use(
      http.get('/api/instance/public-info', () => {
        return HttpResponse.json({
          instanceName: 'GHI Institute',
          status: 'SUSPENDED',
          suspensionReason: 'Payment overdue',
        });
      })
    );

    render(<GuestPage />);

    expect(
      await screen.findByText(/temporarily unavailable/i)
    ).toBeInTheDocument();
    expect(screen.getByText(/payment overdue/i)).toBeInTheDocument();
  });

  it('should use custom theme colors from instance config', async () => {
    server.use(
      http.get('/api/instance/public-info', () => {
        return HttpResponse.json({
          instanceName: 'JKL School',
          primaryColor: '#10B981',
          secondaryColor: '#3B82F6',
        });
      })
    );

    render(<GuestPage />);

    await screen.findByText('JKL School');

    const container = screen.getByTestId('guest-container');
    expect(container).toHaveStyle({ '--primary-color': '#10B981' });
  });
});
```

---

## Trial Banner Testing

### Overview

Trial system states:
- **TRIAL (1-14 days)**: Show days remaining, upgrade CTA
- **GRACE (15-17 days)**: Warning banner, urgent upgrade CTA
- **SUSPENDED (18+ days)**: System locked, contact admin message
- **ACTIVE (paid)**: No banner, all features accessible

### Trial Banner Component Test

```typescript
// __tests__/components/trial/TrialBanner.test.tsx
import { render, screen } from '@testing-library/react';
import { TrialBanner } from '@/components/trial/TrialBanner';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('TrialBanner', () => {
  it('should show trial days remaining (early trial period)', async () => {
    server.use(
      http.get('/api/instance/trial-status', () => {
        return HttpResponse.json({
          status: 'TRIAL',
          daysRemaining: 10,
          tier: 'TRIAL',
        });
      })
    );

    render(<TrialBanner />);

    expect(await screen.findByText(/10 days remaining/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /upgrade/i })).toBeInTheDocument();
  });

  it('should show urgent warning in grace period', async () => {
    server.use(
      http.get('/api/instance/trial-status', () => {
        return HttpResponse.json({
          status: 'GRACE',
          daysRemaining: 2,
          tier: 'TRIAL',
        });
      })
    );

    render(<TrialBanner />);

    expect(
      await screen.findByText(/grace period: 2 days remaining/i)
    ).toBeInTheDocument();
    expect(screen.getByText(/upgrade now to avoid suspension/i)).toBeInTheDocument();

    const banner = screen.getByRole('alert');
    expect(banner).toHaveClass('bg-yellow-100'); // Warning style
  });

  it('should show suspension message when suspended', async () => {
    server.use(
      http.get('/api/instance/trial-status', () => {
        return HttpResponse.json({
          status: 'SUSPENDED',
          tier: 'TRIAL',
        });
      })
    );

    render(<TrialBanner />);

    expect(
      await screen.findByText(/account suspended/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/contact support to reactivate/i)
    ).toBeInTheDocument();

    const banner = screen.getByRole('alert');
    expect(banner).toHaveClass('bg-red-100'); // Danger style
  });

  it('should NOT show banner for active paid instances', async () => {
    server.use(
      http.get('/api/instance/trial-status', () => {
        return HttpResponse.json({
          status: 'ACTIVE',
          tier: 'STANDARD',
          validUntil: '2027-01-30T00:00:00Z',
        });
      })
    );

    render(<TrialBanner />);

    // Wait to ensure no banner appears
    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('should navigate to pricing page on upgrade click', async () => {
    const user = userEvent.setup();
    const mockRouter = { push: vi.fn() };

    server.use(
      http.get('/api/instance/trial-status', () => {
        return HttpResponse.json({
          status: 'TRIAL',
          daysRemaining: 5,
        });
      })
    );

    render(<TrialBanner router={mockRouter} />);

    const upgradeButton = await screen.findByRole('button', { name: /upgrade/i });
    await user.click(upgradeButton);

    expect(mockRouter.push).toHaveBeenCalledWith('/settings/pricing');
  });

  it('should dismiss banner temporarily when close is clicked', async () => {
    const user = userEvent.setup();

    server.use(
      http.get('/api/instance/trial-status', () => {
        return HttpResponse.json({
          status: 'TRIAL',
          daysRemaining: 10,
        });
      })
    );

    render(<TrialBanner dismissible={true} />);

    const banner = await screen.findByRole('alert');
    const closeButton = screen.getByRole('button', { name: /close/i });

    await user.click(closeButton);

    expect(banner).not.toBeInTheDocument();

    // Should reappear on next render
    const { rerender } = render(<TrialBanner dismissible={true} />);
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });
});
```

---

## AI Branding UI Testing

### Overview

AI Branding features (PREMIUM tier only):
- Logo generation (GPT-4 Vision + DALL-E 3)
- School name → visual concept → logo image
- Preview → Approve/Regenerate workflow
- Async job processing with status polling

### AI Branding Form Test

```typescript
// __tests__/components/ai-branding/AIBrandingForm.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AIBrandingForm } from '@/components/ai-branding/AIBrandingForm';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('AIBrandingForm', () => {
  it('should submit branding request with school name', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('/api/ai-branding/generate', async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({
          schoolName: 'Sunrise Academy',
          style: 'modern',
        });

        return HttpResponse.json({
          jobId: 'job-123',
          status: 'PROCESSING',
        });
      })
    );

    render(<AIBrandingForm />);

    const input = screen.getByLabelText(/school name/i);
    await user.type(input, 'Sunrise Academy');

    const styleSelect = screen.getByLabelText(/style/i);
    await user.selectOptions(styleSelect, 'modern');

    const submitButton = screen.getByRole('button', { name: /generate logo/i });
    await user.click(submitButton);

    expect(await screen.findByText(/generating logo/i)).toBeInTheDocument();
  });

  it('should show validation error for empty school name', async () => {
    const user = userEvent.setup();

    render(<AIBrandingForm />);

    const submitButton = screen.getByRole('button', { name: /generate logo/i });
    await user.click(submitButton);

    expect(
      await screen.findByText(/school name is required/i)
    ).toBeInTheDocument();
  });

  it('should disable submit button while processing', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('/api/ai-branding/generate', async () => {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        return HttpResponse.json({ jobId: 'job-123' });
      })
    );

    render(<AIBrandingForm />);

    const input = screen.getByLabelText(/school name/i);
    await user.type(input, 'Test School');

    const submitButton = screen.getByRole('button', { name: /generate logo/i });
    await user.click(submitButton);

    expect(submitButton).toBeDisabled();
  });

  it('should display error message on API failure', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('/api/ai-branding/generate', () => {
        return HttpResponse.json(
          { error: 'AI service temporarily unavailable' },
          { status: 503 }
        );
      })
    );

    render(<AIBrandingForm />);

    const input = screen.getByLabelText(/school name/i);
    await user.type(input, 'Test School');

    const submitButton = screen.getByRole('button', { name: /generate logo/i });
    await user.click(submitButton);

    expect(
      await screen.findByText(/ai service temporarily unavailable/i)
    ).toBeInTheDocument();
  });
});
```

### AI Job Status Polling Test

```typescript
// __tests__/components/ai-branding/AIJobStatus.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import { AIJobStatus } from '@/components/ai-branding/AIJobStatus';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { vi } from 'vitest';

describe('AIJobStatus', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should poll job status every 3 seconds until completion', async () => {
    let pollCount = 0;

    server.use(
      http.get('/api/ai-branding/jobs/:jobId', () => {
        pollCount++;
        if (pollCount < 3) {
          return HttpResponse.json({
            jobId: 'job-123',
            status: 'PROCESSING',
            progress: pollCount * 30,
          });
        }
        return HttpResponse.json({
          jobId: 'job-123',
          status: 'COMPLETED',
          result: {
            logoUrl: 'https://cdn.example.com/logo-123.png',
            concept: 'A sunrise over mountains representing growth',
          },
        });
      })
    );

    render(<AIJobStatus jobId="job-123" />);

    await waitFor(() => expect(pollCount).toBe(1));

    act(() => {
      vi.advanceTimersByTime(3000);
    });

    await waitFor(() => expect(pollCount).toBe(2));

    act(() => {
      vi.advanceTimersByTime(3000);
    });

    await waitFor(() => {
      expect(screen.getByText(/completed/i)).toBeInTheDocument();
    });

    // Should stop polling
    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(pollCount).toBe(3);
  });

  it('should display progress bar during processing', async () => {
    server.use(
      http.get('/api/ai-branding/jobs/:jobId', () => {
        return HttpResponse.json({
          status: 'PROCESSING',
          progress: 60,
        });
      })
    );

    render(<AIJobStatus jobId="job-123" />);

    const progressBar = await screen.findByRole('progressbar');
    expect(progressBar).toHaveAttribute('aria-valuenow', '60');
  });

  it('should display generated logo on completion', async () => {
    server.use(
      http.get('/api/ai-branding/jobs/:jobId', () => {
        return HttpResponse.json({
          status: 'COMPLETED',
          result: {
            logoUrl: 'https://cdn.example.com/logo-final.png',
            concept: 'Modern education symbol',
          },
        });
      })
    );

    render(<AIJobStatus jobId="job-123" />);

    const logoImage = await screen.findByRole('img', { name: /generated logo/i });
    expect(logoImage).toHaveAttribute('src', 'https://cdn.example.com/logo-final.png');
    expect(screen.getByText(/modern education symbol/i)).toBeInTheDocument();
  });

  it('should display error message on job failure', async () => {
    server.use(
      http.get('/api/ai-branding/jobs/:jobId', () => {
        return HttpResponse.json({
          status: 'FAILED',
          error: 'OpenAI API quota exceeded',
        });
      })
    );

    render(<AIJobStatus jobId="job-123" />);

    expect(
      await screen.findByText(/openai api quota exceeded/i)
    ).toBeInTheDocument();

    // Should show retry button
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
```

### Logo Preview and Approval Test

```typescript
// __tests__/components/ai-branding/LogoPreview.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LogoPreview } from '@/components/ai-branding/LogoPreview';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('LogoPreview', () => {
  const mockLogo = {
    logoUrl: 'https://cdn.example.com/logo-preview.png',
    concept: 'Sunrise over mountains',
  };

  it('should display logo preview with concept', () => {
    render(<LogoPreview logo={mockLogo} />);

    const image = screen.getByRole('img', { name: /logo preview/i });
    expect(image).toHaveAttribute('src', mockLogo.logoUrl);
    expect(screen.getByText(mockLogo.concept)).toBeInTheDocument();
  });

  it('should approve logo and update instance config', async () => {
    const user = userEvent.setup();
    const onApprove = vi.fn();

    server.use(
      http.post('/api/ai-branding/approve', async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ logoUrl: mockLogo.logoUrl });

        return HttpResponse.json({
          success: true,
          instanceConfig: {
            logo: mockLogo.logoUrl,
          },
        });
      })
    );

    render(<LogoPreview logo={mockLogo} onApprove={onApprove} />);

    const approveButton = screen.getByRole('button', { name: /approve/i });
    await user.click(approveButton);

    await waitFor(() => {
      expect(onApprove).toHaveBeenCalled();
    });

    expect(screen.getByText(/logo approved/i)).toBeInTheDocument();
  });

  it('should regenerate logo on regenerate click', async () => {
    const user = userEvent.setup();
    const onRegenerate = vi.fn();

    render(<LogoPreview logo={mockLogo} onRegenerate={onRegenerate} />);

    const regenerateButton = screen.getByRole('button', { name: /regenerate/i });
    await user.click(regenerateButton);

    expect(onRegenerate).toHaveBeenCalled();
  });

  it('should download logo preview', async () => {
    const user = userEvent.setup();

    // Mock window.open
    const mockOpen = vi.fn();
    window.open = mockOpen;

    render(<LogoPreview logo={mockLogo} />);

    const downloadButton = screen.getByRole('button', { name: /download/i });
    await user.click(downloadButton);

    expect(mockOpen).toHaveBeenCalledWith(mockLogo.logoUrl, '_blank');
  });
});
```

---

## Theme System Testing

### Overview

Theme system features:
- Light/dark mode toggle
- Custom theme colors (primary, secondary, accent)
- Persistent theme preference (localStorage)
- System preference detection

### Theme Toggle Test

```typescript
// __tests__/components/theme/ThemeToggle.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeToggle } from '@/components/theme/ThemeToggle';
import { ThemeProvider } from '@/components/theme/ThemeProvider';

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should toggle between light and dark mode', async () => {
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>
    );

    const toggleButton = screen.getByRole('button', { name: /toggle theme/i });

    // Initial: light mode
    expect(document.documentElement).toHaveClass('light');

    await user.click(toggleButton);

    // After click: dark mode
    await waitFor(() => {
      expect(document.documentElement).toHaveClass('dark');
    });

    await user.click(toggleButton);

    // After second click: light mode again
    await waitFor(() => {
      expect(document.documentElement).toHaveClass('light');
    });
  });

  it('should persist theme preference in localStorage', async () => {
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>
    );

    const toggleButton = screen.getByRole('button', { name: /toggle theme/i });
    await user.click(toggleButton);

    await waitFor(() => {
      expect(localStorage.getItem('theme')).toBe('dark');
    });

    await user.click(toggleButton);

    await waitFor(() => {
      expect(localStorage.getItem('theme')).toBe('light');
    });
  });

  it('should load saved theme preference on mount', () => {
    localStorage.setItem('theme', 'dark');

    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>
    );

    expect(document.documentElement).toHaveClass('dark');
  });

  it('should use system preference when no saved preference exists', () => {
    // Mock system dark mode preference
    Object.defineProperty(window, 'matchMedia', {
      value: vi.fn().mockImplementation((query) => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      })),
    });

    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>
    );

    expect(document.documentElement).toHaveClass('dark');
  });
});
```

### Custom Theme Colors Test

```typescript
// __tests__/components/theme/CustomThemeProvider.test.tsx
import { render, screen } from '@testing-library/react';
import { CustomThemeProvider } from '@/components/theme/CustomThemeProvider';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('CustomThemeProvider', () => {
  it('should apply custom theme colors from instance config', async () => {
    server.use(
      http.get('/api/instance/config', () => {
        return HttpResponse.json({
          theme: {
            primaryColor: '#10B981',
            secondaryColor: '#3B82F6',
            accentColor: '#F59E0B',
          },
        });
      })
    );

    render(
      <CustomThemeProvider>
        <div data-testid="themed-content">Content</div>
      </CustomThemeProvider>
    );

    await screen.findByTestId('themed-content');

    const root = document.documentElement;
    expect(root.style.getPropertyValue('--color-primary')).toBe('#10B981');
    expect(root.style.getPropertyValue('--color-secondary')).toBe('#3B82F6');
    expect(root.style.getPropertyValue('--color-accent')).toBe('#F59E0B');
  });

  it('should use default colors when instance has no custom theme', async () => {
    server.use(
      http.get('/api/instance/config', () => {
        return HttpResponse.json({
          theme: null,
        });
      })
    );

    render(
      <CustomThemeProvider>
        <div data-testid="themed-content">Content</div>
      </CustomThemeProvider>
    );

    await screen.findByTestId('themed-content');

    const root = document.documentElement;
    expect(root.style.getPropertyValue('--color-primary')).toBe('#3B82F6'); // Default
  });
});
```

---

## React Query Caching Tests

### Overview

React Query configuration:
- **staleTime:** 1 hour (3600000ms) for instance config
- **cacheTime:** 5 minutes for most queries
- **refetchOnWindowFocus:** false for static data
- **refetchInterval:** 5 seconds for payment polling

### Query Caching Test

```typescript
// __tests__/lib/react-query.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('React Query Caching', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
  });

  afterEach(() => {
    queryClient.clear();
  });

  it('should cache instance config for 1 hour (staleTime)', async () => {
    let apiCallCount = 0;

    server.use(
      http.get('/api/instance/config', () => {
        apiCallCount++;
        return HttpResponse.json({
          tier: 'STANDARD',
          features: { STUDENTS: true },
        });
      })
    );

    const TestComponent = () => {
      const { data } = useQuery({
        queryKey: ['instance', 'config'],
        queryFn: () =>
          fetch('/api/instance/config').then((res) => res.json()),
        staleTime: 3600000, // 1 hour
      });

      return <div>{data ? data.tier : 'Loading'}</div>;
    };

    const { rerender } = render(
      <QueryClientProvider client={queryClient}>
        <TestComponent />
      </QueryClientProvider>
    );

    expect(await screen.findByText('STANDARD')).toBeInTheDocument();
    expect(apiCallCount).toBe(1);

    // Rerender should use cached data
    rerender(
      <QueryClientProvider client={queryClient}>
        <TestComponent />
      </QueryClientProvider>
    );

    await screen.findByText('STANDARD');
    expect(apiCallCount).toBe(1); // Still 1 (cache hit)
  });

  it('should invalidate cache on mutation', async () => {
    let getCallCount = 0;

    server.use(
      http.get('/api/instance/config', () => {
        getCallCount++;
        return HttpResponse.json({ tier: 'BASIC' });
      }),
      http.post('/api/payments/verify', () => {
        return HttpResponse.json({ success: true, tier: 'STANDARD' });
      })
    );

    const TestComponent = () => {
      const { data } = useQuery({
        queryKey: ['instance', 'config'],
        queryFn: () =>
          fetch('/api/instance/config').then((res) => res.json()),
      });

      const verifyPayment = async () => {
        await fetch('/api/payments/verify', { method: 'POST' });
        queryClient.invalidateQueries({ queryKey: ['instance', 'config'] });
      };

      return (
        <div>
          <div>{data ? data.tier : 'Loading'}</div>
          <button onClick={verifyPayment}>Verify Payment</button>
        </div>
      );
    };

    render(
      <QueryClientProvider client={queryClient}>
        <TestComponent />
      </QueryClientProvider>
    );

    expect(await screen.findByText('BASIC')).toBeInTheDocument();
    expect(getCallCount).toBe(1);

    const button = screen.getByRole('button', { name: /verify payment/i });
    await userEvent.click(button);

    // Should refetch after invalidation
    await waitFor(() => {
      expect(getCallCount).toBe(2);
    });
  });

  it('should use separate cache keys for different tenants', async () => {
    const mockFetch = vi.fn();

    server.use(
      http.get('/api/students', ({ request }) => {
        const instanceId = new URL(request.url).searchParams.get('instanceId');
        mockFetch(instanceId);

        return HttpResponse.json({
          students: instanceId === 'tenant1' ? ['Alice'] : ['Bob'],
        });
      })
    );

    const TestComponent = ({ instanceId }: { instanceId: string }) => {
      const { data } = useQuery({
        queryKey: ['students', instanceId],
        queryFn: () =>
          fetch(`/api/students?instanceId=${instanceId}`).then((res) =>
            res.json()
          ),
      });

      return <div>{data ? data.students[0] : 'Loading'}</div>;
    };

    const { rerender } = render(
      <QueryClientProvider client={queryClient}>
        <TestComponent instanceId="tenant1" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('Alice')).toBeInTheDocument();

    rerender(
      <QueryClientProvider client={queryClient}>
        <TestComponent instanceId="tenant2" />
      </QueryClientProvider>
    );

    expect(await screen.findByText('Bob')).toBeInTheDocument();

    // Should have made 2 separate API calls
    expect(mockFetch).toHaveBeenCalledTimes(2);
    expect(mockFetch).toHaveBeenCalledWith('tenant1');
    expect(mockFetch).toHaveBeenCalledWith('tenant2');
  });
});
```

---

## Zustand State Management Tests

### Overview

Zustand stores:
- **Auth Store:** User session, JWT tokens, logout
- **Instance Store:** Current instance config, tier, features
- **Trial Store:** Trial status, days remaining, warnings

### Auth Store Test

```typescript
// __tests__/stores/authStore.test.ts
import { renderHook, act } from '@testing-library/react';
import { useAuthStore } from '@/stores/authStore';

describe('Auth Store', () => {
  beforeEach(() => {
    useAuthStore.getState().reset();
  });

  it('should initialize with null user', () => {
    const { result } = renderHook(() => useAuthStore());

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('should set user on login', () => {
    const { result } = renderHook(() => useAuthStore());

    act(() => {
      result.current.setUser({
        id: 1,
        email: 'teacher@school.com',
        role: 'TEACHER',
        instanceId: '123e4567-e89b-12d3-a456-426614174000',
      });
    });

    expect(result.current.user).toEqual({
      id: 1,
      email: 'teacher@school.com',
      role: 'TEACHER',
      instanceId: '123e4567-e89b-12d3-a456-426614174000',
    });
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('should clear user on logout', () => {
    const { result } = renderHook(() => useAuthStore());

    act(() => {
      result.current.setUser({
        id: 1,
        email: 'teacher@school.com',
        role: 'TEACHER',
        instanceId: '123e4567-e89b-12d3-a456-426614174000',
      });
    });

    expect(result.current.isAuthenticated).toBe(true);

    act(() => {
      result.current.logout();
    });

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('should persist user in localStorage', () => {
    const { result } = renderHook(() => useAuthStore());

    act(() => {
      result.current.setUser({
        id: 1,
        email: 'teacher@school.com',
        role: 'TEACHER',
        instanceId: '123e4567-e89b-12d3-a456-426614174000',
      });
    });

    const stored = localStorage.getItem('auth-storage');
    expect(stored).toBeTruthy();

    const parsed = JSON.parse(stored!);
    expect(parsed.state.user.email).toBe('teacher@school.com');
  });
});
```

### Instance Store Test

```typescript
// __tests__/stores/instanceStore.test.ts
import { renderHook, act } from '@testing-library/react';
import { useInstanceStore } from '@/stores/instanceStore';

describe('Instance Store', () => {
  beforeEach(() => {
    useInstanceStore.getState().reset();
  });

  it('should initialize with null config', () => {
    const { result } = renderHook(() => useInstanceStore());

    expect(result.current.config).toBeNull();
  });

  it('should set instance config', () => {
    const { result } = renderHook(() => useInstanceStore());

    const mockConfig = {
      instanceId: '123e4567-e89b-12d3-a456-426614174000',
      tier: 'STANDARD',
      features: {
        STUDENTS: true,
        ENGAGEMENT: true,
        AI_BRANDING: false,
      },
    };

    act(() => {
      result.current.setConfig(mockConfig);
    });

    expect(result.current.config).toEqual(mockConfig);
  });

  it('should check feature availability', () => {
    const { result } = renderHook(() => useInstanceStore());

    act(() => {
      result.current.setConfig({
        instanceId: '123e4567-e89b-12d3-a456-426614174000',
        tier: 'BASIC',
        features: {
          STUDENTS: true,
          ENGAGEMENT: false,
        },
      });
    });

    expect(result.current.hasFeature('STUDENTS')).toBe(true);
    expect(result.current.hasFeature('ENGAGEMENT')).toBe(false);
  });

  it('should get required tier for locked feature', () => {
    const { result } = renderHook(() => useInstanceStore());

    act(() => {
      result.current.setConfig({
        instanceId: '123e4567-e89b-12d3-a456-426614174000',
        tier: 'BASIC',
        features: {
          ENGAGEMENT: false,
        },
      });
    });

    expect(result.current.getRequiredTier('ENGAGEMENT')).toBe('STANDARD');
  });
});
```

---

## Component Testing Patterns

### Form Validation Test

```typescript
// __tests__/components/forms/StudentForm.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StudentForm } from '@/components/forms/StudentForm';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('StudentForm', () => {
  it('should validate required fields', async () => {
    const user = userEvent.setup();

    render(<StudentForm />);

    const submitButton = screen.getByRole('button', { name: /add student/i });
    await user.click(submitButton);

    expect(await screen.findByText(/name is required/i)).toBeInTheDocument();
    expect(screen.getByText(/email is required/i)).toBeInTheDocument();
  });

  it('should validate email format', async () => {
    const user = userEvent.setup();

    render(<StudentForm />);

    const emailInput = screen.getByLabelText(/email/i);
    await user.type(emailInput, 'invalid-email');

    const submitButton = screen.getByRole('button', { name: /add student/i });
    await user.click(submitButton);

    expect(await screen.findByText(/invalid email format/i)).toBeInTheDocument();
  });

  it('should submit valid form data', async () => {
    const user = userEvent.setup();
    const onSuccess = vi.fn();

    server.use(
      http.post('/api/students', async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({
          name: 'John Doe',
          email: 'john@student.com',
        });

        return HttpResponse.json({
          id: 1,
          name: 'John Doe',
          email: 'john@student.com',
        });
      })
    );

    render(<StudentForm onSuccess={onSuccess} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@student.com');

    const submitButton = screen.getByRole('button', { name: /add student/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalled();
    });
  });
});
```

### Data Table Test

```typescript
// __tests__/components/tables/StudentTable.test.tsx
import { render, screen, within } from '@testing-library/react';
import { StudentTable } from '@/components/tables/StudentTable';

describe('StudentTable', () => {
  const mockStudents = [
    { id: 1, name: 'Alice', email: 'alice@student.com' },
    { id: 2, name: 'Bob', email: 'bob@student.com' },
  ];

  it('should render student rows', () => {
    render(<StudentTable students={mockStudents} />);

    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('alice@student.com')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('bob@student.com')).toBeInTheDocument();
  });

  it('should display empty state when no students', () => {
    render(<StudentTable students={[]} />);

    expect(screen.getByText(/no students found/i)).toBeInTheDocument();
  });

  it('should show action buttons for each row', () => {
    render(<StudentTable students={mockStudents} />);

    const rows = screen.getAllByRole('row').slice(1); // Skip header row

    rows.forEach((row) => {
      const editButton = within(row).getByRole('button', { name: /edit/i });
      const deleteButton = within(row).getByRole('button', { name: /delete/i });

      expect(editButton).toBeInTheDocument();
      expect(deleteButton).toBeInTheDocument();
    });
  });

  it('should call onEdit when edit button is clicked', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();

    render(<StudentTable students={mockStudents} onEdit={onEdit} />);

    const firstRow = screen.getAllByRole('row')[1];
    const editButton = within(firstRow).getByRole('button', { name: /edit/i });

    await user.click(editButton);

    expect(onEdit).toHaveBeenCalledWith(mockStudents[0]);
  });
});
```

---

## Integration Testing

### API Integration Test

```typescript
// __tests__/integration/studentAPI.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StudentList } from '@/components/students/StudentList';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('Student API Integration', () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient.clear();
  });

  it('should fetch and display students', async () => {
    server.use(
      http.get('/api/students', () => {
        return HttpResponse.json({
          content: [
            { id: 1, name: 'Alice', email: 'alice@student.com' },
            { id: 2, name: 'Bob', email: 'bob@student.com' },
          ],
          totalElements: 2,
        });
      })
    );

    render(<StudentList />, { wrapper });

    expect(await screen.findByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
  });

  it('should create new student and update list', async () => {
    const user = userEvent.setup();

    server.use(
      http.get('/api/students', () => {
        return HttpResponse.json({
          content: [],
          totalElements: 0,
        });
      }),
      http.post('/api/students', async ({ request }) => {
        const body = await request.json();
        return HttpResponse.json({
          id: 1,
          ...body,
        });
      })
    );

    render(<StudentList />, { wrapper });

    await screen.findByText(/no students/i);

    const addButton = screen.getByRole('button', { name: /add student/i });
    await user.click(addButton);

    // Fill form
    await user.type(screen.getByLabelText(/name/i), 'Charlie');
    await user.type(screen.getByLabelText(/email/i), 'charlie@student.com');

    const submitButton = screen.getByRole('button', { name: /save/i });
    await user.click(submitButton);

    // Should invalidate and refetch
    await waitFor(() => {
      expect(screen.getByText('Charlie')).toBeInTheDocument();
    });
  });

  it('should delete student and update list', async () => {
    const user = userEvent.setup();

    server.use(
      http.get('/api/students', () => {
        return HttpResponse.json({
          content: [
            { id: 1, name: 'Alice', email: 'alice@student.com' },
          ],
          totalElements: 1,
        });
      }),
      http.delete('/api/students/:id', () => {
        return HttpResponse.json({ success: true });
      })
    );

    render(<StudentList />, { wrapper });

    expect(await screen.findByText('Alice')).toBeInTheDocument();

    const deleteButton = screen.getByRole('button', { name: /delete/i });
    await user.click(deleteButton);

    // Confirm deletion
    const confirmButton = screen.getByRole('button', { name: /confirm/i });
    await user.click(confirmButton);

    await waitFor(() => {
      expect(screen.queryByText('Alice')).not.toBeInTheDocument();
    });
  });
});
```

---

## Accessibility Testing

### Keyboard Navigation Test

```typescript
// __tests__/accessibility/keyboard-navigation.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Sidebar } from '@/components/layout/Sidebar';

describe('Keyboard Navigation', () => {
  it('should navigate sidebar menu items with Tab key', async () => {
    const user = userEvent.setup();

    render(<Sidebar />);

    const links = screen.getAllByRole('link');

    // Focus first link
    await user.tab();
    expect(links[0]).toHaveFocus();

    // Tab to next link
    await user.tab();
    expect(links[1]).toHaveFocus();

    // Shift+Tab to previous link
    await user.tab({ shift: true });
    expect(links[0]).toHaveFocus();
  });

  it('should activate links with Enter key', async () => {
    const user = userEvent.setup();
    const mockRouter = { push: vi.fn() };

    render(<Sidebar router={mockRouter} />);

    const studentsLink = screen.getByRole('link', { name: /students/i });
    studentsLink.focus();

    await user.keyboard('{Enter}');

    expect(mockRouter.push).toHaveBeenCalledWith('/students');
  });

  it('should close modal with Escape key', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(<UpgradeModal isOpen={true} onClose={onClose} />);

    await user.keyboard('{Escape}');

    expect(onClose).toHaveBeenCalled();
  });
});
```

### Screen Reader Test

```typescript
// __tests__/accessibility/screen-reader.test.tsx
import { render, screen } from '@testing-library/react';
import { PaymentQRCode } from '@/components/payment/PaymentQRCode';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('Screen Reader Accessibility', () => {
  it('should have proper ARIA labels for QR code', async () => {
    server.use(
      http.post('/api/payments/orders', () => {
        return HttpResponse.json({
          orderId: 'ORD-123',
          qrCodeUrl: 'https://example.com/qr.png',
          amount: 499000,
        });
      })
    );

    render(<PaymentQRCode tier="STANDARD" />);

    const qrImage = await screen.findByRole('img', {
      name: /qr code for payment/i,
    });
    expect(qrImage).toHaveAttribute('alt', expect.stringContaining('ORD-123'));
  });

  it('should announce countdown updates to screen readers', async () => {
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000);

    render(<PaymentCountdown expiresAt={expiresAt} />);

    const liveRegion = screen.getByRole('timer');
    expect(liveRegion).toHaveAttribute('aria-live', 'polite');
    expect(liveRegion).toHaveAttribute('aria-atomic', 'true');
  });

  it('should have descriptive button labels', () => {
    render(<AIBrandingForm />);

    const submitButton = screen.getByRole('button', {
      name: /generate logo using ai/i,
    });
    expect(submitButton).toHaveAccessibleName();
  });
});
```

---

## Performance Testing

### Render Performance Test

```typescript
// __tests__/performance/render-performance.test.tsx
import { render } from '@testing-library/react';
import { StudentTable } from '@/components/tables/StudentTable';

describe('Render Performance', () => {
  it('should render large student list efficiently', () => {
    const largeDataset = Array.from({ length: 1000 }, (_, i) => ({
      id: i,
      name: `Student ${i}`,
      email: `student${i}@example.com`,
    }));

    const start = performance.now();
    render(<StudentTable students={largeDataset} />);
    const end = performance.now();

    const renderTime = end - start;
    expect(renderTime).toBeLessThan(1000); // Should render in under 1 second
  });

  it('should use React.memo to prevent unnecessary re-renders', () => {
    const renderSpy = vi.fn();

    const MemoizedComponent = React.memo(({ value }: { value: string }) => {
      renderSpy();
      return <div>{value}</div>;
    });

    const { rerender } = render(<MemoizedComponent value="test" />);

    expect(renderSpy).toHaveBeenCalledTimes(1);

    // Rerender with same props
    rerender(<MemoizedComponent value="test" />);

    // Should NOT re-render (memo optimization)
    expect(renderSpy).toHaveBeenCalledTimes(1);

    // Rerender with different props
    rerender(<MemoizedComponent value="new" />);

    // Should re-render
    expect(renderSpy).toHaveBeenCalledTimes(2);
  });
});
```

---

## Best Practices Summary

### DO ✅

1. **Test user-visible behavior**, not implementation details
2. **Use semantic queries** (`getByRole`, `getByLabelText`, `getByText`)
3. **Mock API calls** with MSW for predictable tests
4. **Test multi-tenant isolation** in every component
5. **Test feature detection** for tier-based visibility
6. **Test accessibility** (keyboard nav, screen readers, ARIA)
7. **Use React Testing Library** for component tests
8. **Use Playwright** for E2E tests (see separate doc)
9. **Cache instance config** to reduce API calls
10. **Test error states** and loading states

### DON'T ❌

1. **Don't test internal state** or component implementation
2. **Don't use `getByTestId`** unless absolutely necessary
3. **Don't mock React Query** - test the full integration
4. **Don't skip multi-tenant security tests**
5. **Don't forget to test countdown timers** and polling
6. **Don't skip theme/accessibility tests**
7. **Don't test without considering B2B model** (guest cannot register)
8. **Don't forget to clean up timers** in tests

---

## Next Steps

1. **Read `e2e-testing-standards.md`** for full E2E journey tests (Playwright)
2. **Read `ci-cd-quality-enforcement.md`** for test coverage gates (80% minimum)
3. **Implement frontend components** with tests from this document
4. **Set up MSW** for API mocking in development
5. **Configure Vitest** with coverage reporting

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-30
**Next Review:** 2026-02-28
