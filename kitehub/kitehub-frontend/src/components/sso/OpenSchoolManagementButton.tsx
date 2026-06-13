'use client';

/**
 * "Mở quản lý trường" — cross-product SSO entry (KiteHub → KiteClass).
 *
 * ADR-040 Option A / GAP-1138. Requests a one-time SSO code from
 * kitehub-subscription then redirects the browser to the KiteClass owner-shell
 * callback (`:3000/sso/callback?code=...`) carrying ONLY the opaque code. The
 * owner/staff lands in KiteClass without re-entering their password.
 */

import { useState } from 'react';
import { ExternalLink, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { issueSsoCode, buildKiteClassSsoCallbackUrl } from '@/lib/api/sso';

interface OpenSchoolManagementButtonProps {
  className?: string;
  /** Optional Button variant (defaults to the primary action style). */
  variant?: 'default' | 'outline' | 'secondary';
}

export function OpenSchoolManagementButton({
  className,
  variant = 'default',
}: OpenSchoolManagementButtonProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleClick = async () => {
    setLoading(true);
    setError(null);
    try {
      const { code } = await issueSsoCode();
      // Hard navigation cross-origin: only the opaque code travels in the URL.
      window.location.href = buildKiteClassSsoCallbackUrl(code);
    } catch {
      setError('Không thể mở trang quản lý trường. Vui lòng đăng nhập lại rồi thử lại.');
      setLoading(false);
    }
  };

  return (
    <div className={className}>
      <Button onClick={handleClick} disabled={loading} variant={variant}>
        {loading ? (
          <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden />
        ) : (
          <ExternalLink className="mr-2 h-4 w-4" aria-hidden />
        )}
        Mở quản lý trường
      </Button>
      {error && (
        <p className="mt-2 text-sm text-destructive" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

export default OpenSchoolManagementButton;
