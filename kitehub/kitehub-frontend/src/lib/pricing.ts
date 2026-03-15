import type { PricingTier, PlanDetails, BillingCycle } from '@/types/subscription';

/**
 * Plan details for all pricing tiers
 */
export const PLAN_DETAILS: Record<PricingTier, PlanDetails> = {
  FREE: {
    tier: 'FREE',
    name: 'Miễn phí',
    monthlyPrice: 0,
    yearlyPrice: 0,
    maxStudents: 10,
    maxTeachers: 1,
    storageMB: 500,
    features: [
      '10 học viên',
      '1 giảng viên',
      '500MB lưu trữ',
      'Điểm danh cơ bản',
      'Quản lý khóa học',
    ],
    limits: [
      'Không custom domain',
      'Không AI Branding',
      'Hỗ trợ community',
    ],
  },
  BASIC: {
    tier: 'BASIC',
    name: 'Cơ bản',
    monthlyPrice: 500000,      // ₫500,000/tháng
    yearlyPrice: 5400000,      // ₫5,400,000/năm (-10%)
    maxStudents: 50,
    maxTeachers: 5,
    storageMB: 2048,
    features: [
      '50 học viên',
      '5 giảng viên',
      '2GB lưu trữ',
      'Điểm danh tự động',
      'Quản lý thanh toán',
      'AI Branding',
      'Hỗ trợ email',
    ],
    limits: [
      'Không custom domain',
    ],
  },
  PREMIUM: {
    tier: 'PREMIUM',
    name: 'Cao cấp',
    monthlyPrice: 1500000,     // ₫1,500,000/tháng
    yearlyPrice: 16200000,     // ₫16,200,000/năm (-10%)
    maxStudents: 200,
    maxTeachers: 20,
    storageMB: 10240,
    features: [
      '200 học viên',
      '20 giảng viên',
      '10GB lưu trữ',
      'Custom domain',
      'AI Branding nâng cao',
      'Landing page tự động',
      'Báo cáo chi tiết',
      'Hỗ trợ ưu tiên',
    ],
    limits: [],
  },
  ENTERPRISE: {
    tier: 'ENTERPRISE',
    name: 'Doanh nghiệp',
    monthlyPrice: -1,          // Custom pricing
    yearlyPrice: -1,
    maxStudents: -1,           // Unlimited
    maxTeachers: -1,
    storageMB: -1,
    features: [
      'Unlimited học viên & giảng viên',
      'Unlimited lưu trữ',
      'Custom features',
      'Dedicated support',
      'SLA 99.9%',
      'Training & onboarding',
    ],
    limits: [],
  },
};

/**
 * Get numeric rank for tier comparison
 */
export function getTierRank(tier: PricingTier): number {
  const ranks: Record<PricingTier, number> = {
    FREE: 0,
    BASIC: 1,
    PREMIUM: 2,
    ENTERPRISE: 3,
  };
  return ranks[tier];
}

/**
 * Check if tier change is an upgrade
 */
export function isUpgrade(fromTier: PricingTier, toTier: PricingTier): boolean {
  return getTierRank(toTier) > getTierRank(fromTier);
}

/**
 * Check if tier change is a downgrade
 */
export function isDowngrade(fromTier: PricingTier, toTier: PricingTier): boolean {
  return getTierRank(toTier) < getTierRank(fromTier);
}

/**
 * Calculate prorated charge for upgrade
 *
 * Formula: (newPrice - currentPrice) / daysInCycle * daysRemaining
 */
export function calculateProration(
  currentTier: PricingTier,
  newTier: PricingTier,
  daysRemaining: number,
  billingCycle: BillingCycle = 'MONTHLY'
): number {
  const currentPlan = PLAN_DETAILS[currentTier];
  const newPlan = PLAN_DETAILS[newTier];

  // Get price based on billing cycle
  const currentPrice = billingCycle === 'MONTHLY'
    ? currentPlan.monthlyPrice
    : currentPlan.yearlyPrice;

  const newPrice = billingCycle === 'MONTHLY'
    ? newPlan.monthlyPrice
    : newPlan.yearlyPrice;

  // Days in current cycle
  const daysInCycle = billingCycle === 'MONTHLY' ? 30 : 365;

  // Calculate daily price difference
  const dailyDiff = (newPrice - currentPrice) / daysInCycle;

  // Prorated amount for remaining days
  return Math.max(0, Math.round(dailyDiff * daysRemaining));
}

/**
 * Get days remaining until subscription expires
 */
export function getDaysRemaining(expiresAt: string): number {
  const expiry = new Date(expiresAt);
  const now = new Date();
  const diff = expiry.getTime() - now.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

/**
 * Format price for display
 */
export function formatPrice(price: number, cycle: BillingCycle = 'MONTHLY'): string {
  if (price === -1) return 'Liên hệ';
  if (price === 0) return 'Miễn phí';

  const formatted = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(price);

  return cycle === 'MONTHLY' ? `${formatted}/tháng` : `${formatted}/năm`;
}
