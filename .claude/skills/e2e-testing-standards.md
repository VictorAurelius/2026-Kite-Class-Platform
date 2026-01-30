# E2E Testing Standards

**Version:** 1.0.0
**Last Updated:** 2026-01-30
**Status:** Production Standard

---

## Table of Contents

1. [Overview](#overview)
2. [Testing Philosophy](#testing-philosophy)
3. [Playwright Setup](#playwright-setup)
4. [Guest User Journey Tests](#guest-user-journey-tests)
5. [Trial User Journey Tests](#trial-user-journey-tests)
6. [Payment Journey Tests](#payment-journey-tests)
7. [Feature Upgrade Journey Tests](#feature-upgrade-journey-tests)
8. [AI Branding Journey Tests](#ai-branding-journey-tests)
9. [Multi-Tenant Isolation Tests](#multi-tenant-isolation-tests)
10. [Test Data Management](#test-data-management)
11. [Visual Regression Testing](#visual-regression-testing)
12. [Performance Testing](#performance-testing)

---

## Overview

### Purpose

This document provides comprehensive E2E (End-to-End) testing standards for **KiteClass Platform** using **Playwright**. E2E tests verify complete user journeys from start to finish, ensuring all system components work together correctly.

### Scope

E2E tests cover:
- **Guest user journeys** (B2B owner-centric model - guest cannot self-register)
- **Trial user journeys** (14-day trial → 3-day grace → 90-day retention → deletion)
- **Payment journeys** (QR display → payment → verification → tier upgrade)
- **Feature upgrade journeys** (locked feature → upgrade prompt → payment → access granted)
- **AI branding journeys** (generate logo → preview → approve → apply)
- **Multi-tenant isolation** (data leakage prevention between instances)

### Technology Stack

- **E2E Framework:** Playwright (TypeScript)
- **Test Runner:** Playwright Test
- **Browsers:** Chromium, Firefox, WebKit
- **Visual Testing:** Playwright Screenshots
- **API Mocking:** Playwright Network Interception (for external services)

---

## Testing Philosophy

### Core Principles

1. **Test Real User Flows** - Simulate actual user behavior, not just happy paths
2. **Test Business-Critical Journeys** - Focus on revenue-impacting flows (payments, trial-to-paid conversion)
3. **Test Multi-Tenant Isolation** - Verify data leakage prevention between instances
4. **Test Across Browsers** - Ensure cross-browser compatibility (Chromium, Firefox, WebKit)
5. **Test Responsive Design** - Verify mobile, tablet, desktop layouts
6. **Fail Fast** - Use descriptive assertions and clear error messages
7. **Independent Tests** - Each test should be runnable in isolation
8. **Realistic Test Data** - Use production-like data (Vietnamese names, VietQR banks)

### Test Pyramid for KiteClass

```
        E2E Tests (10%)
      /                \
  Integration Tests (30%)
  /                       \
Unit/Component Tests (60%)
```

E2E tests are:
- **Slowest** - Run in real browsers, interact with real database
- **Most fragile** - Can break due to UI changes, timing issues
- **Most valuable** - Verify entire system works together
- **Fewest in number** - Focus on critical business flows

---

## Playwright Setup

### Configuration

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html'],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['list'],
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 12'] },
    },
  ],

  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
});
```

### Test Utilities

```typescript
// e2e/utils/test-helpers.ts
import { Page, expect } from '@playwright/test';

export class KiteClassTestHelpers {
  constructor(private page: Page) {}

  /**
   * Login as a specific role (OWNER, TEACHER, STUDENT)
   */
  async login(email: string, password: string) {
    await this.page.goto('/login');
    await this.page.fill('input[name="email"]', email);
    await this.page.fill('input[name="password"]', password);
    await this.page.click('button[type="submit"]');
    await this.page.waitForURL(/\/(dashboard|students)/);
  }

  /**
   * Setup test instance with specific tier
   */
  async setupInstance(tier: 'TRIAL' | 'BASIC' | 'STANDARD' | 'PREMIUM') {
    // Call backend setup API
    await this.page.context().request.post('/api/test/setup-instance', {
      data: { tier },
    });
  }

  /**
   * Wait for payment QR code to appear
   */
  async waitForPaymentQR() {
    await this.page.waitForSelector('img[alt*="QR code"]', { timeout: 10000 });
  }

  /**
   * Simulate successful payment (test environment only)
   */
  async simulatePayment(orderId: string) {
    await this.page.context().request.post('/api/test/simulate-payment', {
      data: { orderId, status: 'PAID' },
    });
  }

  /**
   * Verify trial banner is visible with specific message
   */
  async verifyTrialBanner(expectedText: string) {
    const banner = this.page.locator('[role="alert"]');
    await expect(banner).toContainText(expectedText);
  }

  /**
   * Verify feature is locked with upgrade prompt
   */
  async verifyFeatureLocked(featureName: string, requiredTier: string) {
    await expect(
      this.page.locator(`text=/upgrade to ${requiredTier}/i`)
    ).toBeVisible();
  }
}
```

---

## Guest User Journey Tests

### Overview

**B2B Owner-Centric Model:**
- Guest users CANNOT self-register
- Only instance owners can invite/enroll students
- Guest landing page shows instance branding + contact info
- Guest must contact owner to request enrollment

### Test: Guest Visits Instance Landing Page

```typescript
// e2e/guest-user-journey.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Guest User Journey', () => {
  test('should display instance branding on guest landing page', async ({ page }) => {
    // Navigate to guest page (subdomain: abc-learning.kiteclass.com)
    await page.goto('https://abc-learning.kiteclass.local');

    // Should display instance name and logo
    await expect(page.locator('h1')).toContainText('ABC Learning Center');
    await expect(page.locator('img[alt*="logo"]')).toBeVisible();

    // Should display contact information
    await expect(page.locator('text=contact@abc-learning.com')).toBeVisible();
    await expect(page.locator('text=+84 901 234 567')).toBeVisible();
  });

  test('should NOT show registration form (B2B model)', async ({ page }) => {
    await page.goto('https://xyz-school.kiteclass.local');

    // No self-registration form
    await expect(page.locator('button:has-text("Sign Up")')).not.toBeVisible();
    await expect(page.locator('input[name="email"]')).not.toBeVisible();

    // Should show enrollment instructions instead
    await expect(
      page.locator('text=/contact.*office.*to enroll/i')
    ).toBeVisible();
  });

  test('should display trial status badge', async ({ page }) => {
    await page.goto('https://trial-school.kiteclass.local');

    // Should show trial status
    await expect(page.locator('text=/trial.*10 days remaining/i')).toBeVisible();
  });

  test('should display suspended instance message', async ({ page }) => {
    await page.goto('https://suspended-school.kiteclass.local');

    // Should show suspension message
    await expect(
      page.locator('text=/temporarily unavailable/i')
    ).toBeVisible();
    await expect(page.locator('text=/payment overdue/i')).toBeVisible();
  });

  test('should apply custom theme colors from instance config', async ({
    page,
  }) => {
    await page.goto('https://custom-theme.kiteclass.local');

    // Check CSS variables
    const primaryColor = await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue(
        '--color-primary'
      )
    );

    expect(primaryColor.trim()).toBe('#10B981');
  });
});
```

---

## Trial User Journey Tests

### Overview

**Trial System States:**
1. **TRIAL (Days 1-14):** Full access, trial banner with days remaining
2. **GRACE (Days 15-17):** Warning banner, urgent upgrade CTA
3. **SUSPENDED (Days 18+):** System locked, contact admin
4. **DELETED (Day 108):** Data permanently deleted (90 days after suspension)

### Test: Owner Creates Trial Instance

```typescript
// e2e/trial-journey.spec.ts
import { test, expect } from '@playwright/test';
import { KiteClassTestHelpers } from './utils/test-helpers';

test.describe('Trial User Journey', () => {
  test('owner creates trial instance and sees 14-day trial banner', async ({
    page,
  }) => {
    const helpers = new KiteClassTestHelpers(page);

    // Owner registers via KiteHub (external integration)
    await page.goto('/signup');
    await page.fill('input[name="email"]', 'owner@newschool.com');
    await page.fill('input[name="password"]', 'SecurePass123!');
    await page.fill('input[name="instanceName"]', 'New School');
    await page.click('button[type="submit"]');

    // Should redirect to KiteClass dashboard
    await page.waitForURL(/\/dashboard/);

    // Should see trial banner
    await helpers.verifyTrialBanner('14 days remaining');

    // Should have TRIAL tier features
    await expect(page.locator('nav a:has-text("Students")')).toBeVisible();
    await expect(page.locator('nav a:has-text("Classes")')).toBeVisible();

    // Should NOT have PREMIUM features
    await expect(
      page.locator('nav a:has-text("AI Branding")')
    ).not.toBeVisible();
  });

  test('trial countdown updates daily', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    // Setup instance with 10 days remaining
    await helpers.setupInstance('TRIAL');
    await page.context().request.post('/api/test/set-trial-days', {
      data: { daysRemaining: 10 },
    });

    await helpers.login('owner@school.com', 'password');

    await helpers.verifyTrialBanner('10 days remaining');

    // Simulate 1 day passing
    await page.context().request.post('/api/test/advance-days', {
      data: { days: 1 },
    });

    await page.reload();

    await helpers.verifyTrialBanner('9 days remaining');
  });

  test('grace period shows urgent warning (days 15-17)', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('TRIAL');
    await page.context().request.post('/api/test/set-trial-days', {
      data: { daysRemaining: 2, status: 'GRACE' },
    });

    await helpers.login('owner@school.com', 'password');

    // Should show warning banner
    const banner = page.locator('[role="alert"]');
    await expect(banner).toContainText('Grace period: 2 days remaining');
    await expect(banner).toContainText('Upgrade now to avoid suspension');

    // Banner should have warning style
    await expect(banner).toHaveClass(/bg-yellow/);
  });

  test('suspended instance locks all features (day 18+)', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('TRIAL');
    await page.context().request.post('/api/test/set-trial-days', {
      data: { daysRemaining: 0, status: 'SUSPENDED' },
    });

    await helpers.login('owner@school.com', 'password');

    // Should show suspension banner
    const banner = page.locator('[role="alert"]');
    await expect(banner).toContainText('Account suspended');
    await expect(banner).toContainText('Contact support to reactivate');

    // All features should be locked
    await page.click('nav a:has-text("Students")');
    await expect(
      page.locator('text=/account suspended.*cannot access/i')
    ).toBeVisible();
  });

  test('trial-to-paid conversion removes banner', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('TRIAL');
    await page.context().request.post('/api/test/set-trial-days', {
      data: { daysRemaining: 5 },
    });

    await helpers.login('owner@school.com', 'password');

    // Trial banner visible
    await helpers.verifyTrialBanner('5 days remaining');

    // Navigate to pricing and upgrade
    await page.click('button:has-text("Upgrade")');
    await page.waitForURL(/\/settings\/pricing/);

    await page.click('button:has-text("Upgrade to Standard")');

    // Complete payment (tested separately)
    const orderId = await page
      .locator('text=/ORD-/')
      .textContent()
      .then((text) => text?.match(/ORD-[\w-]+/)?.[0]);

    await helpers.simulatePayment(orderId!);

    // Wait for payment success
    await expect(page.locator('text=/payment successful/i')).toBeVisible();

    // Navigate back to dashboard
    await page.goto('/dashboard');

    // Trial banner should be gone
    await expect(
      page.locator('[role="alert"]:has-text("days remaining")')
    ).not.toBeVisible();
  });
});
```

---

## Payment Journey Tests

### Overview

**VietQR Payment Flow:**
1. Owner clicks "Upgrade to [Tier]"
2. System creates payment order (10-minute expiry)
3. QR code displayed (VietQR format: bank + account + amount + message)
4. Owner scans QR with banking app and pays
5. System polls payment status every 5 seconds
6. Payment verified → Tier upgraded → Features unlocked

### Test: Complete Payment Journey

```typescript
// e2e/payment-journey.spec.ts
import { test, expect } from '@playwright/test';
import { KiteClassTestHelpers } from './utils/test-helpers';

test.describe('Payment Journey', () => {
  test('owner upgrades from BASIC to STANDARD via QR payment', async ({
    page,
  }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    // Navigate to pricing page
    await page.click('text=Settings');
    await page.click('text=Pricing');

    // Click upgrade to STANDARD
    await page.click('button:has-text("Upgrade to Standard")');

    // Should display payment QR code
    await helpers.waitForPaymentQR();

    // Should show order details
    const orderId = await page.locator('text=/ORD-/').textContent();
    expect(orderId).toMatch(/ORD-\d{8}-[A-Z0-9]+/);

    await expect(page.locator('text=499,000 VND')).toBeVisible();

    // Should show 10-minute countdown
    await expect(page.locator('text=/9:5[0-9]/')).toBeVisible();

    // Simulate payment
    await helpers.simulatePayment(orderId!);

    // Should show payment success message
    await expect(
      page.locator('text=/payment successful/i'),
      { timeout: 15000 }
    ).toBeVisible();

    await expect(page.locator('text=/upgraded to standard/i')).toBeVisible();

    // Should show new tier in sidebar
    await page.goto('/dashboard');
    await expect(page.locator('text=Standard Plan')).toBeVisible();

    // Previously locked features should now be accessible
    await expect(page.locator('nav a:has-text("Engagement")')).toBeVisible();
  });

  test('payment countdown expires after 10 minutes', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/settings/pricing');
    await page.click('button:has-text("Upgrade to Standard")');

    await helpers.waitForPaymentQR();

    // Fast-forward time to 10 minutes
    await page.context().request.post('/api/test/advance-time', {
      data: { minutes: 10 },
    });

    // Should show expiry message
    await expect(page.locator('text=/expired/i')).toBeVisible();

    // Should show refresh button
    await expect(
      page.locator('button:has-text("Generate New QR Code")')
    ).toBeVisible();
  });

  test('refresh QR code after expiry', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/settings/pricing');
    await page.click('button:has-text("Upgrade to Standard")');

    await helpers.waitForPaymentQR();

    const firstOrderId = await page.locator('text=/ORD-/').textContent();

    // Expire the order
    await page.context().request.post('/api/test/advance-time', {
      data: { minutes: 10 },
    });

    await expect(page.locator('text=/expired/i')).toBeVisible();

    // Refresh QR code
    await page.click('button:has-text("Generate New QR Code")');

    // Should get new order ID
    const newOrderId = await page.locator('text=/ORD-/').textContent();
    expect(newOrderId).not.toBe(firstOrderId);
  });

  test('payment failure shows error and retry option', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/settings/pricing');
    await page.click('button:has-text("Upgrade to Standard")');

    await helpers.waitForPaymentQR();

    const orderId = await page.locator('text=/ORD-/').textContent();

    // Simulate payment failure
    await page.context().request.post('/api/test/simulate-payment', {
      data: { orderId, status: 'FAILED', reason: 'Bank timeout' },
    });

    // Should show error message
    await expect(
      page.locator('text=/payment failed/i'),
      { timeout: 15000 }
    ).toBeVisible();
    await expect(page.locator('text=/bank timeout/i')).toBeVisible();

    // Should show retry button
    await expect(page.locator('button:has-text("Try Again")')).toBeVisible();
  });

  test('prevent double payment for same order', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/settings/pricing');
    await page.click('button:has-text("Upgrade to Standard")');

    await helpers.waitForPaymentQR();

    const orderId = await page.locator('text=/ORD-/').textContent();

    // Simulate first payment
    await helpers.simulatePayment(orderId!);

    await expect(page.locator('text=/payment successful/i')).toBeVisible();

    // Attempt to verify same payment again
    const response = await page.context().request.post('/api/payments/verify', {
      data: {
        orderId,
        transactionId: 'FT999',
        timestamp: new Date().toISOString(),
      },
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.error).toContain('already paid');
  });

  test('upgrade to PREMIUM tier (999k VND)', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('STANDARD');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/settings/pricing');
    await page.click('button:has-text("Upgrade to Premium")');

    await helpers.waitForPaymentQR();

    // Should show PREMIUM price
    await expect(page.locator('text=999,000 VND')).toBeVisible();

    const orderId = await page.locator('text=/ORD-/').textContent();
    await helpers.simulatePayment(orderId!);

    await expect(page.locator('text=/upgraded to premium/i')).toBeVisible();

    // Should unlock AI Branding feature
    await page.goto('/dashboard');
    await expect(page.locator('nav a:has-text("AI Branding")')).toBeVisible();
  });
});
```

---

## Feature Upgrade Journey Tests

### Overview

**Locked Feature Flow:**
1. User tries to access locked feature (e.g., Engagement on BASIC tier)
2. System shows "Upgrade to STANDARD" modal
3. User clicks "Upgrade Now"
4. Redirects to pricing page
5. Completes payment
6. Feature unlocked

### Test: Feature Upgrade Journey

```typescript
// e2e/feature-upgrade-journey.spec.ts
import { test, expect } from '@playwright/test';
import { KiteClassTestHelpers } from './utils/test-helpers';

test.describe('Feature Upgrade Journey', () => {
  test('locked feature shows upgrade prompt', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('teacher@school.com', 'password');

    // Try to access Engagement feature (STANDARD tier required)
    await page.goto('/engagement');

    // Should show upgrade modal
    await expect(page.locator('text=/upgrade to standard/i')).toBeVisible();
    await expect(
      page.locator('text=/engagement tracking.*standard tier/i')
    ).toBeVisible();

    // Should show feature comparison
    await expect(page.locator('text=Current Plan: Basic')).toBeVisible();
    await expect(page.locator('text=Required Plan: Standard')).toBeVisible();
  });

  test('upgrade from feature lock modal', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/engagement');

    // Click upgrade from modal
    await page.click('button:has-text("Upgrade Now")');

    // Should redirect to pricing page
    await page.waitForURL(/\/settings\/pricing/);

    // Proceed with payment
    await page.click('button:has-text("Upgrade to Standard")');
    await helpers.waitForPaymentQR();

    const orderId = await page.locator('text=/ORD-/').textContent();
    await helpers.simulatePayment(orderId!);

    await expect(page.locator('text=/payment successful/i')).toBeVisible();

    // Navigate back to engagement page
    await page.goto('/engagement');

    // Should now have access (no upgrade modal)
    await expect(page.locator('h1:has-text("Engagement")')).toBeVisible();
    await expect(
      page.locator('text=/upgrade to standard/i')
    ).not.toBeVisible();
  });

  test('non-owner cannot upgrade tier', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('BASIC');
    await helpers.login('teacher@school.com', 'password');

    await page.goto('/engagement');

    // Should show locked message but no upgrade button
    await expect(page.locator('text=/contact.*owner.*to upgrade/i')).toBeVisible();
    await expect(
      page.locator('button:has-text("Upgrade Now")')
    ).not.toBeVisible();
  });

  test('sidebar shows locked features with upgrade badge', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('STANDARD');
    await helpers.login('owner@school.com', 'password');

    // AI Branding should show PREMIUM badge
    const aiBrandingLink = page.locator('nav a:has-text("AI Branding")');
    await expect(aiBrandingLink).toBeVisible();

    const badge = aiBrandingLink.locator('text=/premium/i');
    await expect(badge).toBeVisible();

    // Click locked feature
    await aiBrandingLink.click();

    // Should show upgrade modal
    await expect(page.locator('text=/upgrade to premium/i')).toBeVisible();
  });
});
```

---

## AI Branding Journey Tests

### Overview

**AI Branding Flow (PREMIUM tier only):**
1. Owner enters school name
2. System calls GPT-4 Vision to generate concept
3. System calls DALL-E 3 to generate logo
4. Owner previews logo + concept
5. Owner approves OR regenerates
6. Approved logo applied to instance config

### Test: AI Branding Journey

```typescript
// e2e/ai-branding-journey.spec.ts
import { test, expect } from '@playwright/test';
import { KiteClassTestHelpers } from './utils/test-helpers';

test.describe('AI Branding Journey', () => {
  test('generate logo for school name', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('PREMIUM');
    await helpers.login('owner@school.com', 'password');

    // Navigate to AI Branding
    await page.click('nav a:has-text("AI Branding")');

    // Fill school name
    await page.fill('input[name="schoolName"]', 'Sunrise Academy');
    await page.selectOption('select[name="style"]', 'modern');

    // Submit
    await page.click('button:has-text("Generate Logo")');

    // Should show processing status
    await expect(page.locator('text=/generating logo/i')).toBeVisible();

    // Should show progress bar
    await expect(page.locator('[role="progressbar"]')).toBeVisible();

    // Wait for completion (mock AI service responds in 5 seconds)
    await expect(
      page.locator('text=/completed/i'),
      { timeout: 10000 }
    ).toBeVisible();

    // Should display generated logo
    await expect(page.locator('img[alt*="generated logo"]')).toBeVisible();

    // Should display concept
    await expect(
      page.locator('text=/sunrise over mountains/i')
    ).toBeVisible();
  });

  test('approve generated logo', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('PREMIUM');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/ai-branding');

    // Generate logo (simulated)
    await page.context().request.post('/api/test/simulate-ai-job', {
      data: {
        jobId: 'job-123',
        status: 'COMPLETED',
        result: {
          logoUrl: 'https://cdn.example.com/logo-final.png',
          concept: 'Modern education symbol',
        },
      },
    });

    await page.reload();

    // Should show preview
    await expect(page.locator('img[alt*="logo preview"]')).toBeVisible();

    // Approve logo
    await page.click('button:has-text("Approve")');

    // Should show success message
    await expect(page.locator('text=/logo approved/i')).toBeVisible();

    // Logo should now appear in navbar
    await page.goto('/dashboard');
    const navLogo = page.locator('nav img[alt*="logo"]');
    await expect(navLogo).toHaveAttribute(
      'src',
      expect.stringContaining('logo-final.png')
    );
  });

  test('regenerate logo if not satisfied', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('PREMIUM');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/ai-branding');

    // Simulate first generation
    await page.fill('input[name="schoolName"]', 'Test School');
    await page.click('button:has-text("Generate Logo")');

    await expect(
      page.locator('text=/completed/i'),
      { timeout: 10000 }
    ).toBeVisible();

    const firstLogo = await page
      .locator('img[alt*="generated logo"]')
      .getAttribute('src');

    // Regenerate
    await page.click('button:has-text("Regenerate")');

    await expect(page.locator('text=/generating logo/i')).toBeVisible();

    await expect(
      page.locator('text=/completed/i'),
      { timeout: 10000 }
    ).toBeVisible();

    const secondLogo = await page
      .locator('img[alt*="generated logo"]')
      .getAttribute('src');

    // Should be different logo
    expect(secondLogo).not.toBe(firstLogo);
  });

  test('AI job failure shows error and retry', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('PREMIUM');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/ai-branding');

    // Simulate AI failure
    await page.context().request.post('/api/test/simulate-ai-job', {
      data: {
        jobId: 'job-fail',
        status: 'FAILED',
        error: 'OpenAI API quota exceeded',
      },
    });

    await page.fill('input[name="schoolName"]', 'Test School');
    await page.click('button:has-text("Generate Logo")');

    // Should show error
    await expect(
      page.locator('text=/openai api quota exceeded/i')
    ).toBeVisible();

    // Should show retry button
    await expect(page.locator('button:has-text("Retry")')).toBeVisible();
  });

  test('download logo preview before approving', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);

    await helpers.setupInstance('PREMIUM');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/ai-branding');

    // Simulate completed job
    await page.context().request.post('/api/test/simulate-ai-job', {
      data: {
        jobId: 'job-123',
        status: 'COMPLETED',
        result: {
          logoUrl: 'https://cdn.example.com/logo-preview.png',
        },
      },
    });

    await page.reload();

    // Mock download
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("Download")'),
    ]);

    expect(download.suggestedFilename()).toMatch(/logo.*\.png/);
  });
});
```

---

## Multi-Tenant Isolation Tests

### Overview

**Critical Security Tests:**
- Verify users from Tenant A cannot see data from Tenant B
- Verify subdomain routing isolates instances
- Verify JWT tokens are tenant-scoped

### Test: Multi-Tenant Data Isolation

```typescript
// e2e/multi-tenant-isolation.spec.ts
import { test, expect } from '@playwright/test';
import { KiteClassTestHelpers } from './utils/test-helpers';

test.describe('Multi-Tenant Isolation', () => {
  test('users from different instances cannot see each others data', async ({
    browser,
  }) => {
    // Create 2 instances
    const context1 = await browser.newContext();
    const page1 = context1.newPage();
    const helpers1 = new KiteClassTestHelpers(await page1);

    const context2 = await browser.newContext();
    const page2 = context2.newPage();
    const helpers2 = new KiteClassTestHelpers(await page2);

    // Instance 1: abc-school
    await (await page1).goto('https://abc-school.kiteclass.local/login');
    await helpers1.login('owner@abc-school.com', 'password');

    await (await page1).goto('/students');
    await (await page1).click('button:has-text("Add Student")');
    await (await page1).fill('input[name="name"]', 'Alice (ABC School)');
    await (await page1).fill('input[name="email"]', 'alice@abc.com');
    await (await page1).click('button[type="submit"]');

    await expect((await page1).locator('text=Alice (ABC School)')).toBeVisible();

    // Instance 2: xyz-school
    await (await page2).goto('https://xyz-school.kiteclass.local/login');
    await helpers2.login('owner@xyz-school.com', 'password');

    await (await page2).goto('/students');

    // Should NOT see ABC School's student
    await expect(
      (await page2).locator('text=Alice (ABC School)')
    ).not.toBeVisible();

    // Add student to XYZ School
    await (await page2).click('button:has-text("Add Student")');
    await (await page2).fill('input[name="name"]', 'Bob (XYZ School)');
    await (await page2).fill('input[name="email"]', 'bob@xyz.com');
    await (await page2).click('button[type="submit"]');

    await expect((await page2).locator('text=Bob (XYZ School)')).toBeVisible();

    // Verify ABC School still only sees their student
    await (await page1).reload();
    await expect((await page1).locator('text=Alice (ABC School)')).toBeVisible();
    await expect(
      (await page1).locator('text=Bob (XYZ School)')
    ).not.toBeVisible();

    await context1.close();
    await context2.close();
  });

  test('JWT token from instance A cannot access instance B API', async ({
    page,
  }) => {
    const helpers = new KiteClassTestHelpers(page);

    // Login to Instance A
    await page.goto('https://instance-a.kiteclass.local/login');
    await helpers.login('owner@instance-a.com', 'password');

    // Extract JWT token
    const tokenA = await page.evaluate(() => localStorage.getItem('auth-token'));

    // Try to access Instance B API with Instance A token
    const response = await page.context().request.get(
      'https://instance-b.kiteclass.local/api/students',
      {
        headers: {
          Authorization: `Bearer ${tokenA}`,
        },
      }
    );

    // Should be unauthorized
    expect(response.status()).toBe(401);
    const body = await response.json();
    expect(body.error).toContain('Invalid token for this instance');
  });

  test('subdomain routing isolates instances', async ({ page }) => {
    // Navigate to Instance A
    await page.goto('https://school-a.kiteclass.local');

    await expect(page.locator('h1')).toContainText('School A');

    // Navigate to Instance B
    await page.goto('https://school-b.kiteclass.local');

    await expect(page.locator('h1')).toContainText('School B');

    // Should be completely separate instances
    await expect(page.locator('text=School A')).not.toBeVisible();
  });
});
```

---

## Test Data Management

### Seed Data Setup

```typescript
// e2e/utils/seed-data.ts
import { Page } from '@playwright/test';

export class SeedData {
  constructor(private page: Page) {}

  async createTrialInstance(instanceName: string, daysRemaining: number) {
    await this.page.context().request.post('/api/test/seed/instance', {
      data: {
        name: instanceName,
        tier: 'TRIAL',
        trialDaysRemaining: daysRemaining,
        owner: {
          email: `owner@${instanceName}.com`,
          password: 'password123',
        },
      },
    });
  }

  async createPaidInstance(instanceName: string, tier: 'BASIC' | 'STANDARD' | 'PREMIUM') {
    await this.page.context().request.post('/api/test/seed/instance', {
      data: {
        name: instanceName,
        tier,
        status: 'ACTIVE',
        validUntil: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString(),
        owner: {
          email: `owner@${instanceName}.com`,
          password: 'password123',
        },
      },
    });
  }

  async createStudents(instanceId: string, count: number) {
    const students = Array.from({ length: count }, (_, i) => ({
      name: `Student ${i + 1}`,
      email: `student${i + 1}@school.com`,
      instanceId,
    }));

    await this.page.context().request.post('/api/test/seed/students', {
      data: { students },
    });
  }

  async cleanupTestData() {
    await this.page.context().request.post('/api/test/cleanup');
  }
}
```

### Cleanup After Tests

```typescript
// e2e/global-teardown.ts
import { FullConfig } from '@playwright/test';

async function globalTeardown(config: FullConfig) {
  // Cleanup all test instances
  const baseURL = config.projects[0].use.baseURL;

  await fetch(`${baseURL}/api/test/cleanup-all`, {
    method: 'POST',
  });
}

export default globalTeardown;
```

---

## Visual Regression Testing

### Screenshot Comparison

```typescript
// e2e/visual-regression.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Visual Regression', () => {
  test('guest landing page matches baseline', async ({ page }) => {
    await page.goto('https://school.kiteclass.local');

    // Take screenshot and compare to baseline
    await expect(page).toHaveScreenshot('guest-landing-page.png', {
      fullPage: true,
    });
  });

  test('trial banner matches baseline', async ({ page }) => {
    await page.goto('https://trial-school.kiteclass.local/dashboard');

    const banner = page.locator('[role="alert"]');
    await expect(banner).toHaveScreenshot('trial-banner.png');
  });

  test('payment QR code modal matches baseline', async ({ page }) => {
    // Login and navigate to payment
    const helpers = new KiteClassTestHelpers(page);
    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/settings/pricing');
    await page.click('button:has-text("Upgrade to Standard")');

    await helpers.waitForPaymentQR();

    const modal = page.locator('[role="dialog"]');
    await expect(modal).toHaveScreenshot('payment-qr-modal.png');
  });
});
```

---

## Performance Testing

### Load Time Metrics

```typescript
// e2e/performance.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Performance', () => {
  test('guest landing page loads in under 2 seconds', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('https://school.kiteclass.local');

    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    expect(loadTime).toBeLessThan(2000);
  });

  test('dashboard loads in under 3 seconds', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);
    await helpers.setupInstance('STANDARD');
    await helpers.login('owner@school.com', 'password');

    const startTime = Date.now();

    await page.goto('/dashboard');

    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    expect(loadTime).toBeLessThan(3000);
  });

  test('payment QR code generates in under 5 seconds', async ({ page }) => {
    const helpers = new KiteClassTestHelpers(page);
    await helpers.setupInstance('BASIC');
    await helpers.login('owner@school.com', 'password');

    await page.goto('/settings/pricing');

    const startTime = Date.now();

    await page.click('button:has-text("Upgrade to Standard")');

    await helpers.waitForPaymentQR();

    const generationTime = Date.now() - startTime;

    expect(generationTime).toBeLessThan(5000);
  });
});
```

---

## Best Practices Summary

### DO ✅

1. **Test complete user journeys** from start to finish
2. **Test multi-tenant isolation** in every critical flow
3. **Use realistic test data** (Vietnamese names, VietQR banks)
4. **Test across browsers** (Chromium, Firefox, WebKit)
5. **Test payment flows thoroughly** (critical revenue impact)
6. **Test trial-to-paid conversion** (business-critical)
7. **Use visual regression tests** for UI consistency
8. **Clean up test data** after each test run
9. **Use page objects/helpers** to reduce duplication
10. **Test error states** (payment failure, AI failure, etc.)

### DON'T ❌

1. **Don't skip multi-tenant isolation tests** (critical security)
2. **Don't hardcode test data** (use seed functions)
3. **Don't rely on shared state** between tests
4. **Don't skip cross-browser testing**
5. **Don't forget to test mobile viewports**
6. **Don't skip payment edge cases** (expiry, double payment, etc.)
7. **Don't skip trial system edge cases** (grace period, suspension)
8. **Don't test implementation details** (test user behavior)

---

## Next Steps

1. **Read `ci-cd-quality-enforcement.md`** for E2E test automation in CI/CD
2. **Read `performance-testing-standards.md`** for k6 load testing
3. **Implement seed data scripts** for test environment
4. **Set up visual regression baselines** for critical pages
5. **Configure Playwright** in CI/CD pipeline

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-30
**Next Review:** 2026-02-28
