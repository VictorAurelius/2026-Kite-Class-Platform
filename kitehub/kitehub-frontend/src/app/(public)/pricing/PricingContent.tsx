'use client';

import Link from 'next/link';
import { useState } from 'react';
import { cn } from '@/lib/utils';

const tiers = [
  {
    name: 'FREE',
    monthlyPrice: 0,
    yearlyPrice: 0,
    description: 'Dành cho cá nhân hoặc nhóm nhỏ',
    features: [
      '10 học viên',
      '1 giảng viên',
      '500MB lưu trữ',
      'Báo cáo cơ bản',
      'Hỗ trợ cộng đồng',
    ],
    limits: ['Không có custom domain', 'Không có AI Branding'],
    cta: 'Bắt đầu miễn phí',
    highlighted: false,
  },
  {
    name: 'BASIC',
    monthlyPrice: 500000,
    yearlyPrice: 5400000,
    description: 'Dành cho trung tâm nhỏ',
    features: [
      '50 học viên',
      '5 giảng viên',
      '2GB lưu trữ',
      'Báo cáo nâng cao',
      'AI Branding',
      'Hỗ trợ email',
    ],
    limits: ['Không có custom domain'],
    cta: 'Dùng thử 14 ngày',
    highlighted: false,
  },
  {
    name: 'PREMIUM',
    monthlyPrice: 1500000,
    yearlyPrice: 16200000,
    description: 'Dành cho trung tâm vừa',
    features: [
      '200 học viên',
      '20 giảng viên',
      '10GB lưu trữ',
      'Custom domain',
      'AI Branding',
      'Hỗ trợ ưu tiên',
      'API access',
    ],
    limits: [],
    cta: 'Dùng thử 14 ngày',
    highlighted: true,
  },
  {
    name: 'ENTERPRISE',
    monthlyPrice: -1,
    yearlyPrice: -1,
    description: 'Dành cho tổ chức lớn',
    features: [
      'Không giới hạn học viên',
      'Không giới hạn giảng viên',
      'Không giới hạn lưu trữ',
      'Custom domain',
      'AI Branding',
      'Hỗ trợ chuyên biệt',
      'API access',
      'SLA cam kết',
    ],
    limits: [],
    cta: 'Liên hệ',
    highlighted: false,
  },
];

function formatVND(amount: number): string {
  if (amount === 0) return 'Miễn phí';
  if (amount < 0) return 'Liên hệ';
  return new Intl.NumberFormat('vi-VN').format(amount) + '₫';
}

import { PRICING_FAQS } from './faqs';
// Re-export for backward compat with any server component that still imports from here.
// Prefer importing from './faqs' directly.
export { PRICING_FAQS };

export function PricingContent() {
  const [annual, setAnnual] = useState(false);

  return (
    <div className="container py-20">
      <div className="mx-auto max-w-3xl text-center">
        <h1 className="text-4xl font-bold">Bảng giá</h1>
        <p className="mt-4 text-muted-foreground">
          Chọn gói phù hợp với quy mô trung tâm của bạn. Tiết kiệm 10% khi thanh toán theo năm.
        </p>

        {/* Toggle */}
        <div className="mt-8 flex items-center justify-center gap-3">
          <span className={cn('text-sm', !annual && 'font-semibold')}>Hàng tháng</span>
          <button
            onClick={() => setAnnual(!annual)}
            className={cn(
              'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
              annual ? 'bg-primary' : 'bg-muted-foreground/30'
            )}
            aria-label={annual ? 'Chuyển sang thanh toán hàng tháng' : 'Chuyển sang thanh toán hàng năm'}
          >
            <span
              className={cn(
                'inline-block h-4 w-4 rounded-full bg-white dark:bg-foreground transition-transform',
                annual ? 'translate-x-6' : 'translate-x-1'
              )}
            />
          </button>
          <span className={cn('text-sm', annual && 'font-semibold')}>
            Hàng năm
            <span className="ml-1 rounded bg-green-100 dark:bg-green-950/50 px-1.5 py-0.5 text-xs text-green-700 dark:text-green-400">
              -10%
            </span>
          </span>
        </div>
      </div>

      {/* Tier cards */}
      <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {tiers.map((tier) => {
          const price = annual ? tier.yearlyPrice : tier.monthlyPrice;
          const period = annual ? '/năm' : '/tháng';

          return (
            <div
              key={tier.name}
              className={cn(
                'rounded-lg border p-6 flex flex-col',
                tier.highlighted && 'border-primary shadow-lg ring-1 ring-primary'
              )}
            >
              {tier.highlighted && (
                <span className="mb-4 inline-block self-start rounded-full bg-primary px-3 py-1 text-xs font-medium text-primary-foreground">
                  Phổ biến nhất
                </span>
              )}
              <h3 className="text-lg font-bold">{tier.name}</h3>
              <p className="mt-1 text-sm text-muted-foreground">{tier.description}</p>
              <div className="mt-4">
                <span className="text-3xl font-bold">{formatVND(price)}</span>
                {price > 0 && (
                  <span className="text-sm text-muted-foreground">{period}</span>
                )}
              </div>

              <ul className="mt-6 flex-1 space-y-2">
                {tier.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-sm">
                    <span className="text-green-500">✓</span>
                    {f}
                  </li>
                ))}
                {tier.limits.map((l) => (
                  <li key={l} className="flex items-start gap-2 text-sm text-muted-foreground">
                    <span>✗</span>
                    {l}
                  </li>
                ))}
              </ul>

              <Link
                href={tier.name === 'ENTERPRISE' ? '/contact?plan=enterprise' : '/register'}
                className={cn(
                  'mt-6 block rounded-md px-4 py-2.5 text-center text-sm font-semibold',
                  tier.highlighted
                    ? 'bg-primary text-primary-foreground hover:bg-primary/90'
                    : 'border hover:bg-muted'
                )}
              >
                {tier.cta}
              </Link>
            </div>
          );
        })}
      </div>

      {/* FAQ */}
      <section className="mx-auto mt-20 max-w-2xl">
        <h2 className="text-2xl font-bold text-center">Câu hỏi thường gặp</h2>
        <div className="mt-8 space-y-6">
          {PRICING_FAQS.map((faq) => (
            <div key={faq.q}>
              <h3 className="font-semibold">{faq.q}</h3>
              <p className="mt-1 text-sm text-muted-foreground">{faq.a}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
