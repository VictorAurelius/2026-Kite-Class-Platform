/**
 * /signup/mobile — mobile-first phone + OTP signup (GAP-286, AC-ONBOARD-001).
 *
 * KiteHub SaaS-lifecycle signup entry: a prospective center owner / solo tutor
 * verifies a VN phone number via OTP before the (Phase 2) create-tenant step.
 * Served by kitehub-subscription via the KiteHub gateway. PRE-AUTH — no tenant
 * context yet.
 *
 * @since Wave OTP — GAP-286
 */
import type { Metadata } from 'next';
import Link from 'next/link';

import { KiteLogo } from '@/components/brand/KiteLogo';
import MobileSignupOtpForm from '@/components/auth/MobileSignupOtpForm';

export const metadata: Metadata = {
  title: 'Đăng ký bằng số điện thoại',
  description: 'Tạo tài khoản KiteHub nhanh chóng bằng số điện thoại và mã OTP.',
};

export default function MobileSignupPage() {
  return (
    <div>
      <div className="mb-8">
        <Link href="/">
          <KiteLogo size="md" />
        </Link>
        <h1 className="mt-6 text-2xl font-bold tracking-tight">
          Đăng ký bằng số điện thoại
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Nhập số điện thoại để nhận mã xác thực và bắt đầu dùng thử miễn phí.
        </p>
      </div>

      <MobileSignupOtpForm />

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link href="/login" className="font-medium text-primary hover:underline">
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
