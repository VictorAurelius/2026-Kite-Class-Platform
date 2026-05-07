/**
 * /auth/beta-signup?token=XXX — invitee redeems token + completes signup (GAP-372).
 *
 * Reads `?token=` from the URL, validates it via the backend, and renders the
 * signup form pre-filled with the approved request's email/org/persona.
 *
 * @since Wave 33 — GAP-372
 */
'use client';

import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import { KiteLogo } from '@/components/brand/KiteLogo';
import BetaSignupForm from '@/components/auth/BetaSignupForm';

function BetaSignupContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  return <BetaSignupForm token={token} />;
}

export default function BetaSignupPage() {
  return (
    <div>
      <div className="mb-8">
        <Link href="/">
          <KiteLogo size="md" />
        </Link>
        <h1 className="mt-6 text-2xl font-bold tracking-tight">
          Hoàn tất đăng ký Beta
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Liên kết của bạn có hiệu lực trong 24 giờ kể từ khi được duyệt.
        </p>
      </div>
      <Suspense fallback={<p className="text-sm">Đang tải...</p>}>
        <BetaSignupContent />
      </Suspense>
    </div>
  );
}
