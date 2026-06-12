/**
 * Playwright mock API routes for KiteHub screenshot capture.
 * Intercepts all /api/* requests to localhost:9000 (gateway) and returns
 * realistic mock data so dashboard/admin pages render fully without backend.
 *
 * Modeled after kiteclass-frontend/scripts/mock-api-routes.ts.
 */

import type { Page, Route } from '@playwright/test';

const API_BASE = 'http://localhost:9000';

// Helper: wrap response in ApiResponse envelope (matches types/api.ts)
function apiResponse<T>(data: T) {
  return { success: true, data, timestamp: new Date().toISOString() };
}

// ── Mock IDs (matching capture script SAMPLE_ID & mock auth user IDs) ──

const OWNER_ID = 'audit-00000000-0000-0000-0000-000000000001';
const INSTANCE_ID = 'inst-00000000-0000-0000-0000-000000000001';
const SUBSCRIPTION_ID = 'sub-00000000-0000-0000-0000-000000000001';
const PAYMENT_ID_1 = 'pay-00000000-0000-0000-0000-000000000001';
const PAYMENT_ID_2 = 'pay-00000000-0000-0000-0000-000000000002';

// ── Mock Data ──

const MOCK_INSTANCES = [
  {
    id: INSTANCE_ID,
    organizationName: 'Trung Tâm Anh Ngữ Hướng Dương',
    subdomain: 'huongduong',
    ownerId: OWNER_ID,
    contactEmail: 'info@huongduong.edu.vn',
    status: 'TRIAL' as const,
    tier: 'FREE' as const,
    trialStartedAt: '2026-04-01T00:00:00Z',
    trialExpiresAt: '2026-04-15T00:00:00Z',
    trialDaysLeft: 5,
    subscriptionId: SUBSCRIPTION_ID,
    subscriptionExpiresAt: null,
    isActive: true,
    isOnTrial: true,
    createdAt: '2026-04-01T00:00:00Z',
    updatedAt: '2026-04-10T08:00:00Z',
    customDomain: null,
    customDomainVerified: false,
  },
];

const MOCK_SUBSCRIPTION = {
  id: SUBSCRIPTION_ID,
  instanceId: INSTANCE_ID,
  tier: 'BASIC' as const,
  status: 'ACTIVE' as const,
  billingCycle: 'MONTHLY' as const,
  priceVnd: 199000,
  startedAt: '2026-04-01T00:00:00Z',
  expiresAt: '2026-05-01T00:00:00Z',
  autoRenew: true,
  isActive: true,
  isExpired: false,
  createdAt: '2026-04-01T00:00:00Z',
  updatedAt: '2026-04-01T00:00:00Z',
};

const MOCK_PAYMENTS = [
  {
    id: PAYMENT_ID_1,
    subscriptionId: SUBSCRIPTION_ID,
    amountVnd: 199000,
    currency: 'VND',
    paymentMethod: 'VIETQR' as const,
    status: 'COMPLETED' as const,
    qrCodeUrl: null,
    transactionId: 'VCB-20260401-001',
    bankCode: 'VCB',
    accountNumber: '1234567890',
    accountName: 'KITEHUB JSC',
    paymentContent: 'KITEHUB BASIC 04/2026',
    paidAt: '2026-04-01T14:30:00Z',
    expiresAt: null,
    createdAt: '2026-04-01T10:00:00Z',
    updatedAt: '2026-04-01T14:30:00Z',
  },
  {
    id: PAYMENT_ID_2,
    subscriptionId: SUBSCRIPTION_ID,
    amountVnd: 199000,
    currency: 'VND',
    paymentMethod: 'VIETQR' as const,
    status: 'PENDING' as const,
    qrCodeUrl: 'https://img.vietqr.io/image/VCB-1234567890-compact.png',
    transactionId: null,
    bankCode: 'VCB',
    accountNumber: '1234567890',
    accountName: 'KITEHUB JSC',
    paymentContent: 'KITEHUB BASIC 05/2026',
    paidAt: null,
    expiresAt: '2026-05-02T10:00:00Z',
    createdAt: '2026-05-01T10:00:00Z',
    updatedAt: '2026-05-01T10:00:00Z',
  },
];

const MOCK_ADMIN_DASHBOARD = {
  totalInstances: 42,
  activeInstances: 28,
  trialInstances: 10,
  suspendedInstances: 4,
  totalRevenue: 25800000,
  monthlyRevenue: 4200000,
  pendingPayments: 3,
  newInstancesThisMonth: 7,
};

const MOCK_ADMIN_INSTANCES = [
  {
    id: INSTANCE_ID,
    organizationName: 'Trung Tâm Anh Ngữ Hướng Dương',
    subdomain: 'huongduong',
    status: 'TRIAL' as const,
    tier: 'FREE' as const,
    ownerEmail: 'owner@huongduong.edu.vn',
    ownerPhone: '0901234567',
    trialEndDate: '2026-04-15T00:00:00Z',
    subscriptionEndDate: null,
    databaseUrl: null,
    totalUsers: 3,
    totalStudents: 45,
    totalCourses: 5,
    createdAt: '2026-04-01T00:00:00Z',
    updatedAt: '2026-04-10T08:00:00Z',
  },
  {
    id: 'inst-00000000-0000-0000-0000-000000000002',
    organizationName: 'Trung Tâm Toán Tư Duy Bright',
    subdomain: 'bright-math',
    status: 'ACTIVE' as const,
    tier: 'PREMIUM' as const,
    ownerEmail: 'admin@bright-math.vn',
    ownerPhone: '0912345678',
    trialEndDate: null,
    subscriptionEndDate: '2026-06-01T00:00:00Z',
    databaseUrl: null,
    totalUsers: 8,
    totalStudents: 120,
    totalCourses: 12,
    createdAt: '2026-02-15T00:00:00Z',
    updatedAt: '2026-04-05T14:00:00Z',
  },
  {
    id: 'inst-00000000-0000-0000-0000-000000000003',
    organizationName: 'Trung Tâm Tin Học Thành Công',
    subdomain: 'thanhcong-it',
    status: 'SUSPENDED' as const,
    tier: 'BASIC' as const,
    ownerEmail: 'contact@thanhcong.edu.vn',
    ownerPhone: '0923456789',
    trialEndDate: null,
    subscriptionEndDate: '2026-03-01T00:00:00Z',
    databaseUrl: null,
    totalUsers: 2,
    totalStudents: 30,
    totalCourses: 3,
    createdAt: '2026-01-10T00:00:00Z',
    updatedAt: '2026-03-01T00:00:00Z',
  },
];

const MOCK_ADMIN_PENDING_PAYMENTS = [
  {
    id: PAYMENT_ID_2,
    subscriptionId: SUBSCRIPTION_ID,
    amountVnd: 199000,
    currency: 'VND',
    paymentMethod: 'VIETQR' as const,
    status: 'PENDING' as const,
    qrCodeUrl: null,
    transactionId: null,
    bankCode: 'VCB',
    accountNumber: '1234567890',
    accountName: 'KITEHUB JSC',
    paymentContent: 'KITEHUB BASIC 05/2026',
    paidAt: null,
    createdAt: '2026-05-01T10:00:00Z',
    updatedAt: '2026-05-01T10:00:00Z',
    instanceName: 'Trung Tâm Anh Ngữ Hướng Dương',
  },
];

const MOCK_REVENUE = {
  items: [
    { period: '2026-01', revenue: 3500000, paymentCount: 12 },
    { period: '2026-02', revenue: 4200000, paymentCount: 15 },
    { period: '2026-03', revenue: 5100000, paymentCount: 18 },
    { period: '2026-04', revenue: 4200000, paymentCount: 14 },
  ],
  totalRevenue: 17000000,
  period: 'MONTHLY' as const,
  startDate: '2026-01-01',
  endDate: '2026-04-30',
};

const MOCK_BRANDING_ASSETS = [
  {
    id: 'asset-001',
    instanceId: INSTANCE_ID,
    type: 'LOGO',
    url: 'https://placehold.co/200x200/2563eb/white?text=Logo',
    s3Key: 'branding/logo.png',
    createdAt: '2026-04-02T10:00:00Z',
  },
  {
    id: 'asset-002',
    instanceId: INSTANCE_ID,
    type: 'HERO',
    url: 'https://placehold.co/1200x400/1e40af/white?text=Hero+Banner',
    s3Key: 'branding/hero.png',
    createdAt: '2026-04-02T11:00:00Z',
  },
  {
    id: 'asset-003',
    instanceId: INSTANCE_ID,
    type: 'PROFILE',
    url: 'https://placehold.co/400x400/f59e0b/white?text=Profile',
    s3Key: 'branding/profile.png',
    createdAt: '2026-04-02T12:00:00Z',
  },
  {
    id: 'asset-004',
    instanceId: INSTANCE_ID,
    type: 'BANNER',
    url: 'https://placehold.co/800x200/10b981/white?text=Banner',
    s3Key: 'branding/banner.png',
    createdAt: '2026-04-03T09:00:00Z',
  },
  {
    id: 'asset-005',
    instanceId: INSTANCE_ID,
    type: 'OG_IMAGE',
    url: 'https://placehold.co/1200x630/8b5cf6/white?text=OG+Image',
    s3Key: 'branding/og.png',
    createdAt: '2026-04-03T10:00:00Z',
  },
];

const MOCK_TEMPLATES = [
  {
    id: 'tpl-001',
    name: 'Giáo Dục Hiện Đại',
    category: 'education',
    thumbnailUrl: 'https://placehold.co/400x300/2563eb/white?text=Modern+Education',
    themeConfig: JSON.stringify({
      colors: { primary: '#2563eb', secondary: '#1e40af', accent: '#f59e0b' },
      fonts: { heading: 'Inter', body: 'Inter' },
      style: 'modern',
    }),
    active: true,
    createdAt: '2026-03-01T00:00:00Z',
  },
  {
    id: 'tpl-002',
    name: 'Truyền Thống Sang Trọng',
    category: 'education',
    thumbnailUrl: 'https://placehold.co/400x300/7c3aed/white?text=Classic+Elegant',
    themeConfig: JSON.stringify({
      colors: { primary: '#7c3aed', secondary: '#5b21b6', accent: '#a78bfa' },
      fonts: { heading: 'Playfair Display', body: 'Source Sans Pro' },
      style: 'classic',
    }),
    active: true,
    createdAt: '2026-03-01T00:00:00Z',
  },
  {
    id: 'tpl-003',
    name: 'Năng Động Sáng Tạo',
    category: 'education',
    thumbnailUrl: 'https://placehold.co/400x300/f97316/white?text=Playful+Creative',
    themeConfig: JSON.stringify({
      colors: { primary: '#f97316', secondary: '#ea580c', accent: '#fbbf24' },
      fonts: { heading: 'Poppins', body: 'Nunito' },
      style: 'playful',
    }),
    active: true,
    createdAt: '2026-03-01T00:00:00Z',
  },
  {
    id: 'tpl-004',
    name: 'Doanh Nghiệp Chuyên Nghiệp',
    category: 'business',
    thumbnailUrl: 'https://placehold.co/400x300/0f172a/white?text=Corporate+Pro',
    themeConfig: JSON.stringify({
      colors: { primary: '#0f172a', secondary: '#334155', accent: '#3b82f6' },
      fonts: { heading: 'Montserrat', body: 'Open Sans' },
      style: 'corporate',
    }),
    active: true,
    createdAt: '2026-03-01T00:00:00Z',
  },
  {
    id: 'tpl-005',
    name: 'Tối Giản Thanh Lịch',
    category: 'general',
    thumbnailUrl: 'https://placehold.co/400x300/64748b/white?text=Minimal+Clean',
    themeConfig: JSON.stringify({
      colors: { primary: '#64748b', secondary: '#475569', accent: '#94a3b8' },
      fonts: { heading: 'DM Sans', body: 'DM Sans' },
      style: 'minimal',
    }),
    active: true,
    createdAt: '2026-03-01T00:00:00Z',
  },
  {
    id: 'tpl-006',
    name: 'Xanh Thiên Nhiên',
    category: 'general',
    thumbnailUrl: 'https://placehold.co/400x300/059669/white?text=Nature+Green',
    themeConfig: JSON.stringify({
      colors: { primary: '#059669', secondary: '#047857', accent: '#34d399' },
      fonts: { heading: 'Quicksand', body: 'Lato' },
      style: 'nature',
    }),
    active: true,
    createdAt: '2026-03-01T00:00:00Z',
  },
];

const MOCK_TRIAL_STATUS = {
  instanceId: INSTANCE_ID,
  trialEndDate: '2026-04-15T00:00:00Z',
  daysRemaining: 5,
  warningLevel: 'MEDIUM' as const,
  expired: false,
};

// ── Route Setup ──

export async function setupMockApi(page: Page) {
  // Intercept all requests to the API gateway
  await page.route(`${API_BASE}/**`, async (route: Route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();

    // ── Auth endpoints ──

    if (path === '/api/auth/refresh') {
      return route.fulfill({ json: { accessToken: 'mock-refreshed-token' } });
    }
    if (path === '/api/auth/login') {
      return route.fulfill({
        json: apiResponse({
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          user: { id: OWNER_ID, email: 'owner@kitehub.local', name: 'Audit Owner', role: 'OWNER' },
        }),
      });
    }
    if (path === '/api/auth/profile') {
      return route.fulfill({
        json: apiResponse({
          id: OWNER_ID,
          email: 'owner@kitehub.local',
          name: 'Audit Owner',
          role: 'OWNER',
        }),
      });
    }

    // ── Instance endpoints ──

    // GET /api/platform/instances/owner/{ownerId}
    if (method === 'GET' && /^\/api\/platform\/instances\/owner\//.test(path)) {
      return route.fulfill({ json: MOCK_INSTANCES });
    }

    // GET /api/platform/instances/{id}/trial-status
    if (method === 'GET' && /^\/api\/platform\/instances\/[^/]+\/trial-status$/.test(path)) {
      return route.fulfill({ json: MOCK_TRIAL_STATUS });
    }

    // GET /api/platform/instances/{id}
    if (method === 'GET' && /^\/api\/platform\/instances\/[^/]+$/.test(path)) {
      return route.fulfill({ json: MOCK_INSTANCES[0] });
    }

    // GET /api/platform/instances
    if (method === 'GET' && path === '/api/platform/instances') {
      return route.fulfill({ json: MOCK_INSTANCES });
    }

    // ── Subscription endpoints ──

    // GET /api/platform/subscriptions/instance/{instanceId}/active
    if (method === 'GET' && /\/subscriptions\/instance\/[^/]+\/active$/.test(path)) {
      return route.fulfill({ json: apiResponse(MOCK_SUBSCRIPTION) });
    }

    // GET /api/platform/subscriptions/instance/{instanceId}
    if (method === 'GET' && /\/subscriptions\/instance\/[^/]+$/.test(path)) {
      return route.fulfill({ json: apiResponse([MOCK_SUBSCRIPTION]) });
    }

    // ── Payment endpoints ──

    // GET /api/platform/payments/{id}/qr-code
    if (method === 'GET' && /\/payments\/[^/]+\/qr-code$/.test(path)) {
      return route.fulfill({
        json: {
          qrCodeUrl: 'https://placehold.co/300x300/000/white?text=QR+Code',
          expiresAt: '2026-05-02T10:00:00Z',
        },
      });
    }

    // GET /api/platform/payments/subscription/{subId}
    if (method === 'GET' && /\/payments\/subscription\//.test(path)) {
      return route.fulfill({ json: apiResponse(MOCK_PAYMENTS) });
    }

    // GET /api/platform/payments/{id}
    if (method === 'GET' && /\/payments\/[^/]+$/.test(path)) {
      return route.fulfill({ json: apiResponse(MOCK_PAYMENTS[0]) });
    }

    // ── Branding endpoints ──

    // GET /api/platform/branding/templates/{id}
    if (method === 'GET' && /\/branding\/templates\/[^/]+$/.test(path)) {
      return route.fulfill({ json: MOCK_TEMPLATES[0] });
    }

    // GET /api/platform/branding/templates
    if (method === 'GET' && /\/branding\/templates$/.test(path)) {
      return route.fulfill({ json: MOCK_TEMPLATES });
    }

    // GET /api/platform/branding/assets/{instanceId}
    if (method === 'GET' && /\/branding\/assets\//.test(path)) {
      return route.fulfill({ json: apiResponse(MOCK_BRANDING_ASSETS) });
    }

    // GET /api/platform/branding/jobs/{id}/assets
    if (method === 'GET' && /\/branding\/jobs\/[^/]+\/assets$/.test(path)) {
      return route.fulfill({ json: apiResponse(MOCK_BRANDING_ASSETS) });
    }

    // GET /api/platform/branding/jobs/{id}
    if (method === 'GET' && /\/branding\/jobs\/[^/]+$/.test(path)) {
      return route.fulfill({
        json: apiResponse({
          id: 'job-001',
          instanceId: INSTANCE_ID,
          status: 'COMPLETED',
          progress: 100,
          currentStep: 'Done',
          analysis: {
            primaryColor: '#2563eb',
            secondaryColor: '#1e40af',
            accentColor: '#f59e0b',
            theme: 'MODERN',
            brandPersonality: ['professional', 'friendly'],
          },
          createdAt: '2026-04-02T09:00:00Z',
          completedAt: '2026-04-02T09:05:00Z',
        }),
      });
    }

    // ── Admin endpoints ──

    // GET /api/platform/admin/dashboard
    if (method === 'GET' && path === '/api/platform/admin/dashboard') {
      return route.fulfill({ json: MOCK_ADMIN_DASHBOARD });
    }

    // GET /api/platform/admin/revenue
    if (method === 'GET' && /\/admin\/revenue/.test(path)) {
      return route.fulfill({ json: MOCK_REVENUE });
    }

    // GET /api/platform/admin/instances/{id}
    if (method === 'GET' && /\/admin\/instances\/[^/]+$/.test(path)) {
      return route.fulfill({ json: MOCK_ADMIN_INSTANCES[0] });
    }

    // GET /api/platform/admin/instances
    if (method === 'GET' && /\/admin\/instances$/.test(path)) {
      return route.fulfill({ json: MOCK_ADMIN_INSTANCES });
    }

    // GET /api/platform/admin/payments/pending
    if (method === 'GET' && /\/admin\/payments\/pending$/.test(path)) {
      return route.fulfill({ json: MOCK_ADMIN_PENDING_PAYMENTS });
    }

    // ── Domain endpoints ──

    // GET /api/instances/{id}/domain
    if (method === 'GET' && /\/api\/instances\/[^/]+\/domain$/.test(path)) {
      return route.fulfill({
        json: {
          customDomain: null,
          verifyToken: null,
          verifyRecord: null,
          status: 'NONE',
          verifiedAt: null,
          backupUrl: 'https://huongduong.kitehub.me',
        },
      });
    }

    // ── Fallback ──

    // Unhandled GET: return empty success
    if (method === 'GET') {
      return route.fulfill({ json: apiResponse(null) });
    }

    // POST/PUT/PATCH/DELETE: return success
    return route.fulfill({ json: apiResponse({ id: 'mock-id-001' }) });
  });
}
