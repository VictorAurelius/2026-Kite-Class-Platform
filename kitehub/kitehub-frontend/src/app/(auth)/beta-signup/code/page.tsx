/**
 * /beta-signup/code — alternate path khi user có claim code (GAP-609).
 *
 * Khi email link không tới (sự cố email infra, bị spam filter) hoặc user
 * nhận claim code qua kênh khác (Zalo / support handoff), trang này cho phép
 * user nhập 6-digit code và tiếp tục flow signup.
 *
 * @since Wave 91 — GAP-609
 */
'use client';

import Link from 'next/link';
import { KiteLogo } from '@/components/brand/KiteLogo';
import BetaClaimCodeForm from '@/components/auth/BetaClaimCodeForm';

export default function BetaClaimCodePage() {
  return (
    <div>
      <div className="mb-8">
        <Link href="/">
          <KiteLogo size="md" />
        </Link>
        <h1 className="mt-6 text-2xl font-bold tracking-tight">
          Nhập mã invite
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Nhập mã 6 chữ số trong email mời để tiếp tục đăng ký Beta.
        </p>
      </div>
      <BetaClaimCodeForm />

      <div className="mt-8 text-sm text-muted-foreground">
        <p>
          Chưa có mã?{' '}
          <Link
            href="/request-beta-access"
            className="font-medium text-primary hover:underline"
          >
            Yêu cầu truy cập Beta
          </Link>
        </p>
      </div>
    </div>
  );
}
