/**
 * E2E test helper utilities.
 *
 * @since PR 5.10
 */

import { Page } from '@playwright/test';

/**
 * Generate a unique subdomain for testing.
 */
export function generateTestSubdomain(): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 6);
  return `test-${timestamp}-${random}`;
}

/**
 * Generate a unique email for testing.
 */
export function generateTestEmail(): string {
  const timestamp = Date.now();
  return `test-${timestamp}@example.com`;
}

/**
 * Wait for navigation to complete.
 */
export async function waitForNavigation(page: Page, url: string): Promise<void> {
  await page.waitForURL(url, { timeout: 10000 });
}

/**
 * Clear localStorage and sessionStorage.
 * Must navigate to a page first to access storage.
 */
export async function clearBrowserStorage(page: Page): Promise<void> {
  // Navigate to base URL first if not already on a page
  const currentUrl = page.url();
  if (currentUrl === 'about:blank' || !currentUrl.startsWith('http')) {
    await page.goto('/');
  }

  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

/**
 * Get auth token from localStorage.
 */
export async function getAuthToken(page: Page): Promise<string | null> {
  return page.evaluate(() => localStorage.getItem('accessToken'));
}

/**
 * Check if user is authenticated.
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  const token = await getAuthToken(page);
  return token !== null && token.length > 0;
}

/**
 * Set up mock auth state in localStorage (skips registration flow).
 * Useful for testing pages that need auth but don't need real API data.
 *
 * GAP-760 fix (2026-05-27): switched from `page.evaluate()` post-navigation
 * to `page.addInitScript()` (Option B per GAP-760 §"Suspected fix scope").
 *
 * Problem solved: zustand `persist` middleware async rehydrates from
 * localStorage after page mount. Layout's useEffect can read
 * `isAuthenticated: false` BEFORE zustand reads the seeded state → bounce
 * to /login instead of expected /dashboard. 7/20 systematic failures
 * observed in `gap-758-school-admin-phase-1-restrict.spec.ts` 2026-05-27.
 *
 * `addInitScript` registers a script that runs BEFORE any page JS on every
 * subsequent navigation. localStorage is populated before Next.js loads →
 * zustand reads it synchronously on first import → race eliminated.
 *
 * @param page - Playwright Page
 * @param role - Persona role for seeded user (default OWNER)
 *
 * Note: caller does NOT need to navigate before calling setupMockAuth.
 * Subsequent `page.goto(...)` triggers the init script automatically.
 */
export async function setupMockAuth(
  page: Page,
  // GAP-562b (Wave 80 Bucket C): STAFF added for tenant-manager role separation.
  // PLATFORM_ADMIN retained alias for platform-admin scope.
  role: 'OWNER' | 'STAFF' | 'ADMIN' | 'PLATFORM_ADMIN' = 'OWNER',
): Promise<void> {
  const emailByRole: Record<typeof role, string> = {
    OWNER: 'mock@kitehub.com',
    STAFF: 'staff@kitehub.com',
    ADMIN: 'admin@kitehub.com',
    PLATFORM_ADMIN: 'admin@kitehub.com',
  };
  const nameByRole: Record<typeof role, string> = {
    OWNER: 'Mock User',
    STAFF: 'Staff User',
    ADMIN: 'Admin User',
    PLATFORM_ADMIN: 'Admin User',
  };

  const authState = {
    state: {
      user: {
        id: '00000000-0000-0000-0000-000000000099',
        email: emailByRole[role],
        name: nameByRole[role],
        role,
      },
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      isAuthenticated: true,
    },
    version: 0,
  };

  // GAP-760: Seed BEFORE page JS runs (eliminates zustand persist async
  // rehydrate race). Each subsequent page.goto() will re-execute this
  // script in the fresh document context.
  await page.addInitScript((state) => {
    localStorage.setItem('kitehub-auth', JSON.stringify(state));
  }, authState);
}

/**
 * Register a new user and return to specified page.
 * Returns the instance ID from the redirect URL.
 */
export async function registerAndNavigate(page: Page, targetUrl: string): Promise<void> {
  await clearBrowserStorage(page);
  await page.goto('/register');

  const data = {
    organizationName: 'Test Organization',
    subdomain: generateTestSubdomain(),
    email: generateTestEmail(),
    password: 'TestPassword123!',
  };

  await page.getByPlaceholder('Trung tâm Anh ngữ ABC').fill(data.organizationName);
  await page.getByPlaceholder('abc-center').fill(data.subdomain);
  await page.getByPlaceholder('email@example.com').fill(data.email);

  const passwordFields = page.locator('input[type="password"]');
  await passwordFields.first().fill(data.password);
  await passwordFields.nth(1).fill(data.password);

  await page.getByRole('button', { name: /tạo tài khoản/i }).click();
  await expect(page).toHaveURL('/dashboard', { timeout: 10000 });

  if (targetUrl !== '/dashboard') {
    await page.goto(targetUrl);
  }
}

/**
 * Mock admin dashboard API with sample data.
 */
export async function mockAdminDashboardAPI(page: Page): Promise<void> {
  await page.route('**/api/platform/admin/dashboard', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalInstances: 25,
        activeInstances: 18,
        trialInstances: 5,
        suspendedInstances: 2,
        totalRevenue: 25000000,
        monthlyRevenue: 5000000,
        pendingPayments: 3,
        newInstancesThisMonth: 4,
      }),
    });
  });
}

/**
 * Mock admin instances list API.
 */
export async function mockAdminInstancesAPI(page: Page): Promise<void> {
  await page.route('**/api/platform/admin/instances', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: '1',
          organizationName: 'Test Org 1',
          subdomain: 'test1',
          status: 'ACTIVE',
          tier: 'BASIC',
          createdAt: new Date().toISOString(),
        },
        {
          id: '2',
          organizationName: 'Test Org 2',
          subdomain: 'test2',
          status: 'TRIAL',
          tier: 'BASIC',
          createdAt: new Date().toISOString(),
        },
      ]),
    });
  });
}

/**
 * Mock admin payments API.
 */
export async function mockAdminPaymentsAPI(page: Page): Promise<void> {
  await page.route('**/api/platform/admin/payments/pending', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });
}

/**
 * Mock all admin APIs for testing.
 */
export async function mockAllAdminAPIs(page: Page): Promise<void> {
  await mockAdminDashboardAPI(page);
  await mockAdminInstancesAPI(page);
  await mockAdminPaymentsAPI(page);
}

/**
 * Mock settings APIs for user settings page.
 * Includes owner instances endpoint and auth profile.
 *
 * @param page - Playwright page
 * @since PR 5.12
 */
export async function mockSettingsAPIs(page: Page): Promise<void> {
  // Mock instances by owner endpoint
  await page.route('**/api/platform/instances/owner/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: '00000000-0000-0000-0000-000000000001',
          organizationName: 'Test Organization',
          subdomain: 'test-org',
          ownerId: '00000000-0000-0000-0000-000000000099',
          contactEmail: 'test@example.com',
          status: 'TRIAL',
          tier: 'BASIC',
          trialStartedAt: new Date().toISOString(),
          trialExpiresAt: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString(), // 14 days from now
          trialDaysLeft: 14,
          subscriptionId: null,
          subscriptionExpiresAt: null,
          isActive: true,
          isOnTrial: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          customDomain: null,
          customDomainVerified: false,
        },
      ]),
    });
  });

  // Mock auth profile endpoint (for account tab)
  await page.route('**/api/auth/profile', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: '00000000-0000-0000-0000-000000000099',
        email: 'test@example.com',
        name: 'Test User',
        role: 'OWNER',
      }),
    });
  });
}

/**
 * Mock instance detail API with realistic data.
 *
 * @param page - Playwright page
 * @param instanceId - Optional specific instance ID (defaults to UUID)
 * @param status - Instance status (TRIAL, ACTIVE, etc.)
 * @since PR 5.12
 */
export async function mockInstanceDetailAPI(
  page: Page,
  instanceId: string = '00000000-0000-0000-0000-000000000001',
  status: 'TRIAL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' = 'TRIAL'
): Promise<void> {
  const now = new Date();
  const trialEndDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days from now

  // Mock instance detail endpoint
  await page.route(`**/api/platform/instances/${instanceId}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: instanceId,
        organizationName: 'Test Organization',
        subdomain: 'test-org',
        ownerId: '00000000-0000-0000-0000-000000000099',
        contactEmail: 'owner@test-org.com',
        status,
        tier: 'FREE',
        trialStartedAt: now.toISOString(),
        trialExpiresAt: status === 'TRIAL' ? trialEndDate.toISOString() : null,
        trialDaysLeft: status === 'TRIAL' ? 7 : null,
        subscriptionId: status === 'ACTIVE' ? '00000000-0000-0000-0000-000000000101' : null,
        subscriptionExpiresAt: status === 'ACTIVE' ? trialEndDate.toISOString() : null,
        isActive: status === 'ACTIVE' || status === 'TRIAL',
        isOnTrial: status === 'TRIAL',
        createdAt: now.toISOString(),
        updatedAt: now.toISOString(),
        customDomain: null,
        customDomainVerified: false,
      }),
    });
  });

  // Mock trial status endpoint (only for TRIAL instances)
  if (status === 'TRIAL') {
    await page.route(`**/api/platform/instances/${instanceId}/trial-status`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          instanceId,
          trialEndDate: trialEndDate.toISOString(),
          daysRemaining: 7,
          warningLevel: 'NONE',
          expired: false,
        }),
      });
    });
  }
}

/**
 * Mock instances API for owner (for branding/dashboard pages).
 * @param page - Playwright page
 * @param instanceId - Optional instance ID (defaults to standard test UUID)
 * @since PR 5.12
 */
export async function mockInstancesAPI(
  page: Page,
  instanceId: string = '00000000-0000-0000-0000-000000000001'
): Promise<void> {
  await page.route('**/api/platform/instances/owner/**', async (route) => {
    const now = new Date();
    const trialEndDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: instanceId,
          organizationName: 'Test Organization',
          subdomain: 'test-org',
          status: 'TRIAL',
          tier: 'FREE',
          trialStartedAt: now.toISOString(),
          trialExpiresAt: trialEndDate.toISOString(),
          trialDaysLeft: 7,
          subscriptionId: null,
          subscriptionExpiresAt: null,
          isActive: true,
          isOnTrial: true,
          createdAt: now.toISOString(),
          updatedAt: now.toISOString(),
          ownerId: '00000000-0000-0000-0000-000000000099',
          contactEmail: 'owner@test-org.com',
          customDomain: null,
          customDomainVerified: false,
        },
      ]),
    });
  });
}

/**
 * Mock branding APIs.
 * @param page - Playwright page
 * @param hasAssets - If true, return sample assets; if false, return empty array
 * @since PR 5.12
 */
export async function mockBrandingAPIs(page: Page, hasAssets: boolean = false): Promise<void> {
  await page.route('**/api/platform/branding/assets/**', async (route) => {
    const assets = hasAssets
      ? [
          {
            id: 'asset-001',
            type: 'PROFILE',
            url: 'https://placehold.co/400x400/6366f1/ffffff?text=Profile',
            createdAt: new Date().toISOString(),
            instanceId: 'test-instance-001',
          },
          {
            id: 'asset-002',
            type: 'HERO',
            url: 'https://placehold.co/1200x600/8b5cf6/ffffff?text=Hero',
            createdAt: new Date().toISOString(),
            instanceId: 'test-instance-001',
          },
          {
            id: 'asset-003',
            type: 'LOGO',
            url: 'https://placehold.co/200x200/a855f7/ffffff?text=Logo',
            createdAt: new Date().toISOString(),
            instanceId: 'test-instance-001',
          },
        ]
      : [];

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: assets,
      }),
    });
  });
}

/**
 * Mock auth registration API.
 * @param options - Configure mock behavior (success, duplicate error)
 * @since PR 5.12
 */
export async function mockAuthRegisterAPI(
  page: Page,
  options?: { shouldFail?: boolean; duplicateSubdomain?: boolean }
): Promise<void> {
  // Track registered subdomains for duplicate detection
  const registeredSubdomains = new Set<string>();

  await page.route('**/api/auth/register', async (route) => {
    const requestBody = route.request().postDataJSON();
    const { subdomain } = requestBody;

    // Check for duplicate subdomain
    if (registeredSubdomains.has(subdomain) || options?.duplicateSubdomain) {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Subdomain đã được sử dụng',
          message: 'Subdomain này đã tồn tại trong hệ thống',
        }),
      });
      return;
    }

    // Check for explicit failure
    if (options?.shouldFail) {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Đăng ký thất bại',
          message: 'Có lỗi xảy ra trong quá trình đăng ký',
        }),
      });
      return;
    }

    // Success case
    registeredSubdomains.add(subdomain);
    const userId = `${Date.now()}-${Math.random().toString(36).substring(7)}`;

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        user: {
          id: userId,
          email: requestBody.ownerEmail,
          name: requestBody.organizationName,
          role: 'OWNER',
        },
        accessToken: `mock-access-token-${userId}`,
        refreshToken: `mock-refresh-token-${userId}`,
      }),
    });
  });
}

/**
 * Mock auth login API.
 * @param options - Configure mock behavior (valid/invalid credentials)
 * @since PR 5.12
 */
export async function mockAuthLoginAPI(
  page: Page,
  options?: { shouldFail?: boolean; validCredentials?: { email: string; password: string } }
): Promise<void> {
  await page.route('**/api/auth/login', async (route) => {
    const requestBody = route.request().postDataJSON();
    const { email, password } = requestBody;

    // Check for invalid credentials (default fail case)
    const isValidCredentials = options?.validCredentials
      ? email === options.validCredentials.email && password === options.validCredentials.password
      : !options?.shouldFail; // Default: accept any credentials unless shouldFail is true

    if (!isValidCredentials || options?.shouldFail) {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Email hoặc mật khẩu không đúng',
          message: 'Thông tin đăng nhập không chính xác',
        }),
      });
      return;
    }

    // Success case
    const userId = `${Date.now()}-${Math.random().toString(36).substring(7)}`;

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        user: {
          id: userId,
          email,
          name: 'Test User',
          role: 'OWNER',
        },
        accessToken: `mock-access-token-${userId}`,
        refreshToken: `mock-refresh-token-${userId}`,
      }),
    });
  });
}

/**
 * Mock auth logout - just return success (logout is frontend-only).
 * @since PR 5.12
 */
export async function mockAuthLogoutAPI(page: Page): Promise<void> {
  // Logout is actually frontend-only (clearAuth + localStorage clear)
  // But we can mock in case backend endpoint is called
  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true }),
    });
  });
}

/**
 * Mock hCaptcha verification (for cases where NEXT_PUBLIC_HCAPTCHA_SITE_KEY is set).
 * In normal E2E tests, captcha is disabled (no site key), so this is not called.
 * But if someone enables captcha in test env, this will prevent test failures.
 *
 * @since PR-SEC-3
 */
export async function mockHCaptchaAPI(page: Page): Promise<void> {
  // Mock hCaptcha siteverify API (not actually called in tests since captcha.enabled=false)
  // But intercept it just in case to avoid external API calls
  await page.route('**/hcaptcha.com/siteverify', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        challenge_ts: new Date().toISOString(),
        hostname: 'localhost',
      }),
    });
  });

  // Mock hCaptcha JS API loading (prevent external script load)
  await page.route('**/hcaptcha.com/**', async (route) => {
    await route.fulfill({
      status: 200,
      body: '',
    });
  });
}

/**
 * Mock all auth APIs for testing.
 * By default, allows successful registration and login.
 * @since PR 5.12
 */
export async function mockAllAuthAPIs(page: Page): Promise<void> {
  await mockAuthRegisterAPI(page);
  await mockAuthLoginAPI(page);
  await mockAuthLogoutAPI(page);
  await mockHCaptchaAPI(page); // PR-SEC-3: Mock captcha (no-op if captcha disabled)
}

/**
 * Mock active subscription API with sample data.
 * Returns a BASIC tier subscription.
 *
 * @since PR 5.12 (billing tests)
 */
export async function mockActiveSubscriptionAPI(
  page: Page,
  instanceId: string = 'test-instance-001'
): Promise<void> {
  await page.route(`**/api/platform/subscriptions/instance/${instanceId}/active`, async (route) => {
    const now = new Date();
    const expiresAt = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000); // 30 days from now

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        data: {
          id: 'test-subscription-001',
          instanceId,
          tier: 'BASIC',
          status: 'ACTIVE',
          billingCycle: 'MONTHLY',
          priceVnd: 299000,
          startedAt: now.toISOString(),
          expiresAt: expiresAt.toISOString(),
          autoRenew: true,
          isActive: true,
          isExpired: false,
          createdAt: now.toISOString(),
          updatedAt: now.toISOString(),
        },
      }),
    });
  });
}

/**
 * Mock payment history API with sample data.
 * Returns array of completed payments.
 *
 * @since PR 5.12 (billing tests)
 */
export async function mockPaymentHistoryAPI(
  page: Page,
  subscriptionId: string = 'test-subscription-001'
): Promise<void> {
  await page.route(`**/api/platform/payments/subscription/${subscriptionId}`, async (route) => {
    const now = new Date();

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        data: [
          {
            id: 'payment-001',
            subscriptionId,
            amountVnd: 299000,
            currency: 'VND',
            paymentMethod: 'VIETQR',
            status: 'COMPLETED',
            qrCodeUrl: 'https://example.com/qr-code.png',
            transactionId: 'TXN001',
            bankCode: 'VCB',
            accountNumber: '1234567890',
            accountName: 'NGUYEN VAN A',
            paymentContent: 'KITEHUB-payment-001',
            paidAt: new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000).toISOString(),
            expiresAt: null,
            createdAt: new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000).toISOString(),
            updatedAt: new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000).toISOString(),
          },
          {
            id: 'payment-002',
            subscriptionId,
            amountVnd: 299000,
            currency: 'VND',
            paymentMethod: 'VIETQR',
            status: 'COMPLETED',
            qrCodeUrl: 'https://example.com/qr-code-2.png',
            transactionId: 'TXN002',
            bankCode: 'VCB',
            accountNumber: '1234567890',
            accountName: 'NGUYEN VAN A',
            paymentContent: 'KITEHUB-payment-002',
            paidAt: new Date(now.getTime() - 35 * 24 * 60 * 60 * 1000).toISOString(),
            expiresAt: null,
            createdAt: new Date(now.getTime() - 36 * 24 * 60 * 60 * 1000).toISOString(),
            updatedAt: new Date(now.getTime() - 35 * 24 * 60 * 60 * 1000).toISOString(),
          },
        ],
      }),
    });
  });
}

/**
 * Mock all billing-related APIs for testing.
 * Includes instances, subscriptions, and payment history.
 *
 * @since PR 5.12 (billing tests)
 */
export async function mockBillingAPIs(page: Page): Promise<void> {
  // Mock instances for the owner - with ACTIVE status and subscription
  await page.route('**/api/platform/instances/owner/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'test-instance-001',
          organizationName: 'Test Organization',
          subdomain: 'test-org',
          ownerId: '00000000-0000-0000-0000-000000000099',
          contactEmail: 'test@example.com',
          status: 'ACTIVE',
          tier: 'BASIC',
          trialStartedAt: null,
          trialExpiresAt: null,
          trialDaysLeft: null,
          subscriptionId: 'test-subscription-001',
          subscriptionExpiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
          isActive: true,
          isOnTrial: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          customDomain: null,
          customDomainVerified: false,
        },
      ]),
    });
  });

  // Mock active subscription
  await mockActiveSubscriptionAPI(page);

  // Mock payment history
  await mockPaymentHistoryAPI(page);
}

// Re-export expect for use in helpers
import { expect } from '@playwright/test';
