/**
 * Mock Data Fixtures
 *
 * Reusable mock data for tests.
 *
 * @since PR-Q4
 */

// Mock Instance data
export const mockInstance = {
  id: 'test-instance-1',
  tenantId: 'tenant-123',
  status: 'ACTIVE',
  name: 'Test English Center',
  organizationName: 'Test English Center',
  subdomain: 'test-center',
  createdAt: '2026-01-01T00:00:00Z',
  planId: 'basic',
  tier: 'BASIC',
  isOnTrial: false,
  trialDaysLeft: null,
  trialExpiresAt: null,
};

export const mockInstances = [
  mockInstance,
  {
    id: 'test-instance-2',
    tenantId: 'tenant-456',
    status: 'SUSPENDED',
    name: 'Another Center',
    organizationName: 'Another Center',
    subdomain: 'another-center',
    createdAt: '2026-01-02T00:00:00Z',
    planId: 'pro',
    tier: 'PRO',
    isOnTrial: false,
    trialDaysLeft: null,
    trialExpiresAt: null,
  },
];

// Mock User data
export const mockUser = {
  id: 'user-123',
  email: 'test@example.com',
  name: 'Test User',
  role: 'customer',
};

// Mock Subscription/Plan data
export const mockPlan = {
  id: 'basic',
  name: 'Basic Plan',
  price: 299000,
  features: ['10 students', 'Basic LMS', 'Email support'],
  maxInstances: 1,
};

export const mockSubscription = {
  id: 'sub-123',
  userId: 'user-123',
  planId: 'basic',
  status: 'active',
  currentPeriodEnd: '2026-12-31T23:59:59Z',
  cancelAtPeriodEnd: false,
};

// Mock Payment data
export const mockPayment = {
  id: 'payment-123',
  amount: 299000,
  status: 'paid',
  createdAt: '2026-01-15T00:00:00Z',
  method: 'bank_transfer',
};

// Mock Branding data
export const mockBranding = {
  instanceId: 'test-instance-1',
  logoUrl: 'https://example.com/logo.png',
  primaryColor: '#3B82F6',
  secondaryColor: '#8B5CF6',
  brandName: 'Test English Center',
};
