/**
 * /auth/request-beta-access — Phase 1 BETA invite request landing page (GAP-372).
 *
 * Replaces the public signup form during Phase 1 BETA. Visitors submit a beta
 * access request; coordinator manually approves and emails the signup token.
 *
 * @since Wave 33 — GAP-372
 */
import Link from 'next/link';
import { KiteLogo } from '@/components/brand/KiteLogo';
import BetaRequestForm from '@/components/auth/BetaRequestForm';

export const metadata = {
  title: 'Đăng ký dùng thử KiteClass — Beta',
};

export default function RequestBetaAccessPage() {
  return (
    <div>
      <div className="mb-8">
        <Link href="/">
          <KiteLogo size="md" />
        </Link>
        <h1 className="mt-6 text-2xl font-bold tracking-tight">
          Đăng ký dùng thử Beta
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          KiteClass đang trong giai đoạn Beta giới hạn. Hãy gửi yêu cầu — đội ngũ
          sẽ liên hệ và gửi liên kết kích hoạt khi tài khoản của bạn được duyệt.
        </p>
      </div>
      <BetaRequestForm />
      <div className="mt-6 text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link href="/login" className="text-primary underline">
          Đăng nhập
        </Link>
      </div>
    </div>
  );
}
