'use client';

/**
 * HelpLink — inline contextual help link beside critical CTAs.
 *
 * Wave beta-prep-1 Bucket G3 — complement of SupportMenu (G1, bottom-right floating).
 * SupportMenu = global "I need help" button; HelpLink = per-CTA inline tooltip + manual link.
 *
 * Usage:
 *   <HelpLink topic="signup" />          // ⓘ + 1-line tooltip "Em hỗ trợ chị/anh đăng ký..."
 *   <HelpLink topic="invoice" inline />  // Compact variant
 *
 * Topics map → user manual route + Vietnamese 1-line context tooltip.
 *
 * VN-localization audit checklist (per .claude/rules/vn-localization-audit-checklist.md):
 *   §2 Vietnamese tooltip + §4 persona-aware tone ("Em hỗ trợ chị/anh ...")
 *
 * Accessibility:
 *   - aria-label tiếng Việt
 *   - Tooltip via title attr (graceful no-JS fallback)
 *   - Tap target ≥ 24×24px (acceptable inline; SupportMenu G1 floating uses 56×56)
 *   - Keyboard-focusable (Link receives tab focus)
 *
 * @since Wave beta-prep-1 — Bucket G3
 */

import Link from 'next/link';
import { HelpCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * HelpLink topic registry — maps slug → user manual route + 1-line VN tooltip.
 *
 * Each topic represents a critical CTA surface where tenants often get stuck.
 * Per `beta-cohort-onboarding-playbook.md` Day 2-7 friction tracking, top
 * stuck-points from cohort survey become new topic entries here.
 */
const HELP_TOPICS: Record<
  string,
  { href: string; tooltip: string; label: string }
> = {
  signup: {
    href: '/help/anonymous/beta-access',
    tooltip:
      'Em hỗ trợ chị/anh các bước đăng ký Beta access. Click để xem hướng dẫn.',
    label: 'Hướng dẫn đăng ký',
  },
  'beta-token': {
    href: '/help/anonymous/beta-access',
    tooltip:
      'Token Beta có hiệu lực 24 giờ. Em hướng dẫn cách hoàn tất đăng ký với token.',
    label: 'Hướng dẫn token Beta',
  },
  'verify-email': {
    href: '/help/anonymous/email-verification',
    tooltip:
      'Nếu email xác thực không tới sau 5 phút, em hỗ trợ chị/anh khắc phục.',
    label: 'Hướng dẫn xác thực email',
  },
  'first-class': {
    href: '/help/p2-owner/first-class-wizard',
    tooltip:
      'Em hướng dẫn chị/anh tạo lớp đầu tiên với học sinh + lịch học mẫu.',
    label: 'Hướng dẫn tạo lớp',
  },
  'first-invoice': {
    href: '/help/p2-owner/billing-overview',
    tooltip:
      'Em hỗ trợ chị/anh tạo hoá đơn đầu tiên (VND + VietQR + chuyển khoản).',
    label: 'Hướng dẫn hoá đơn',
  },
  pricing: {
    href: '/help/anonymous/pricing-explainer',
    tooltip: 'Bảng giá Phase 1 BETA — Beta cohort được hỗ trợ miễn phí.',
    label: 'Hướng dẫn bảng giá',
  },
  consent: {
    href: '/help/anonymous/pdpl-consent',
    tooltip:
      'Theo PDPL 2023, em cần chị/anh đồng ý điều khoản. Click để xem chi tiết.',
    label: 'Hướng dẫn PDPL consent',
  },
  branch: {
    href: '/help/anonymous/multi-branch-policy',
    tooltip:
      'Phase 1 BETA chỉ hỗ trợ 1 chi nhánh. Đa chi nhánh mở Phase 1.5 (Q3 2026).',
    label: 'Hướng dẫn chính sách chi nhánh',
  },
};

export interface HelpLinkProps {
  /** Topic slug; must exist in HELP_TOPICS registry. */
  topic: keyof typeof HELP_TOPICS;
  /** Compact variant — smaller icon, no margin. */
  inline?: boolean;
  /** Additional className for the link wrapper. */
  className?: string;
}

export function HelpLink({ topic, inline = false, className }: HelpLinkProps) {
  const entry = HELP_TOPICS[topic];

  if (!entry) {
    // Defensive — registry miss should never happen in production but harmless skip.
    if (process.env.NODE_ENV !== 'production') {
      // eslint-disable-next-line no-console
      console.warn(`HelpLink: unknown topic "${topic}"`);
    }
    return null;
  }

  return (
    <Link
      href={entry.href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={entry.label}
      title={entry.tooltip}
      data-testid={`help-link-${topic}`}
      className={cn(
        'inline-flex items-center justify-center text-muted-foreground transition-colors hover:text-primary focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1 rounded-full',
        inline ? 'ml-1 h-5 w-5' : 'ml-2 h-6 w-6',
        className,
      )}
    >
      <HelpCircle
        className={inline ? 'h-4 w-4' : 'h-5 w-5'}
        aria-hidden="true"
      />
    </Link>
  );
}

export default HelpLink;
