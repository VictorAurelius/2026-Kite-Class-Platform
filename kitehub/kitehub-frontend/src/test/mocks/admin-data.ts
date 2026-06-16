import type {
  AdminInstanceSummary,
  AdminPayment,
  DashboardStats,
  RevenueReport,
} from '@/types/admin';

export const mockInstances: AdminInstanceSummary[] = [
  {
    id: '123e4567-e89b-12d3-a456-426614174000',
    organizationName: 'Trung tâm ABC',
    subdomain: 'abc',
    status: 'ACTIVE',
    tier: 'PREMIUM',
    ownerEmail: 'admin@abc.com',
    ownerPhone: '0901234567',
    trialEndDate: null,
    subscriptionEndDate: '2026-12-31T23:59:59',
    databaseUrl: 'jdbc:postgresql://localhost:5432/abc',
    totalUsers: 50,
    totalStudents: 200,
    totalCourses: 15,
    createdAt: '2026-01-15T10:00:00',
    updatedAt: '2026-03-01T14:30:00',
  },
  {
    id: '223e4567-e89b-12d3-a456-426614174001',
    organizationName: 'Học viện XYZ',
    subdomain: 'xyz',
    status: 'TRIAL',
    tier: 'BASIC',
    ownerEmail: 'owner@xyz.edu',
    ownerPhone: '0912345678',
    trialEndDate: '2026-03-30T23:59:59',
    subscriptionEndDate: null,
    databaseUrl: 'jdbc:postgresql://localhost:5432/xyz',
    totalUsers: 10,
    totalStudents: 30,
    totalCourses: 5,
    createdAt: '2026-03-01T08:00:00',
    updatedAt: '2026-03-15T09:00:00',
  },
  {
    id: '323e4567-e89b-12d3-a456-426614174002',
    organizationName: 'Trường DEF',
    subdomain: 'def',
    status: 'SUSPENDED',
    tier: 'FREE',
    ownerEmail: 'contact@def.vn',
    ownerPhone: null,
    trialEndDate: null,
    subscriptionEndDate: null,
    databaseUrl: 'jdbc:postgresql://localhost:5432/def',
    totalUsers: 5,
    totalStudents: 10,
    totalCourses: 2,
    createdAt: '2026-02-01T12:00:00',
    updatedAt: '2026-03-10T16:00:00',
  },
];

export const mockPendingPayments: AdminPayment[] = [
  {
    id: 'pay-001-uuid',
    subscriptionId: 'sub-001-uuid',
    amountVnd: 2990000,
    currency: 'VND',
    paymentMethod: 'VIETQR',
    status: 'PENDING',
    qrCodeUrl: 'https://example.com/qr/001.png',
    transactionId: null,
    bankCode: 'VCB',
    accountNumber: '1234567890',
    accountName: 'KITEHUB JSC',
    paymentContent: 'KITEHUB PAY001',
    paidAt: null,
    createdAt: '2026-03-16T10:00:00',
    updatedAt: '2026-03-16T10:00:00',
  },
  {
    id: 'pay-002-uuid',
    subscriptionId: 'sub-002-uuid',
    amountVnd: 990000,
    currency: 'VND',
    paymentMethod: 'BANK_TRANSFER',
    status: 'PENDING',
    qrCodeUrl: null,
    transactionId: null,
    bankCode: 'TCB',
    accountNumber: '0987654321',
    accountName: 'KITEHUB JSC',
    paymentContent: 'KITEHUB PAY002',
    paidAt: null,
    createdAt: '2026-03-15T14:30:00',
    updatedAt: '2026-03-15T14:30:00',
  },
];

export const mockEmptyInstances: AdminInstanceSummary[] = [];
export const mockEmptyPayments: AdminPayment[] = [];

// GAP-1440: pendingPayments is no longer part of the flat DashboardStats view
// model (the dashboard sources it from the pending-payments list).
export const mockDashboardStats: DashboardStats = {
  totalInstances: 150,
  activeInstances: 120,
  trialInstances: 25,
  suspendedInstances: 5,
  totalRevenue: 450000000, // 450 million VND
  monthlyRevenue: 89000000, // 89 million VND
  newInstancesThisMonth: 12,
};

// GAP-1441: aligned to the backend RevenueReport shape (revenueByTier +
// dailyRevenue + mrr/projectedArr/churnImpact).
export const mockRevenueReport: RevenueReport = {
  period: 'MONTHLY',
  startDate: '2026-03-01',
  endDate: '2026-03-31',
  totalRevenue: 89000000,
  revenueByTier: [
    { tier: 'BASIC', revenue: 30000000, subscriptionCount: 30 },
    { tier: 'PREMIUM', revenue: 59000000, subscriptionCount: 20 },
  ],
  dailyRevenue: [
    { date: '2026-03-01', revenue: 2870000 },
    { date: '2026-03-02', revenue: 2870000 },
    { date: '2026-03-03', revenue: 2870000 },
  ],
  mrr: 89000000,
  projectedArr: 1068000000,
  churnImpact: 5000000,
};
