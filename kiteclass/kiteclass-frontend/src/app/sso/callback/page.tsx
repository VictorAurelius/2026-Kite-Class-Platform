'use client';

/**
 * Cross-product SSO callback (KiteHub → KiteClass) — ADR-040 Option A, GAP-1138.
 *
 * KiteHub redirects owner/staff here with a one-time opaque code
 * (`/sso/callback?code=...`). We exchange the code for a KiteHub-minted session,
 * establish the KC session (tenant-scoped localStorage + auth-store), then land
 * the user on their role-home — no re-login.
 *
 * `useSearchParams()` is wrapped in <Suspense> per the Next.js prerender contract
 * (avoids the static-bailout build error — see `fe-build-local-verify.md`).
 */

import { Suspense, useEffect, useRef, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { exchangeSsoCode } from '@/lib/api/sso';
import { setTokens } from '@/lib/auth/jwt-storage';
import { useAuthStore } from '@/stores/auth-store';
import { normalizeRole, roleHome } from '@/lib/auth/roles';
import { UserType } from '@/types/auth';

/** Fallback tenant for tokens that somehow lack a tenantId claim. */
const DEFAULT_TENANT_ID = '11111111-1111-1111-1111-111111111111';

/** Read the `tenantId` claim from a JWT (read-only; BE verifies the signature). */
function tenantIdFromToken(token: string): string {
  try {
    const part = token.split('.')[1];
    if (!part) return DEFAULT_TENANT_ID;
    const payload = part.replace(/-/g, '+').replace(/_/g, '/');
    const padded = payload + '='.repeat((4 - (payload.length % 4)) % 4);
    const json = JSON.parse(atob(padded));
    return typeof json.tenantId === 'string' ? json.tenantId : DEFAULT_TENANT_ID;
  } catch {
    return DEFAULT_TENANT_ID;
  }
}

function SsoCallbackInner() {
  const router = useRouter();
  const params = useSearchParams();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [error, setError] = useState<string | null>(null);
  // The code is single-use — guard against React StrictMode double-invoking the
  // effect (which would replay the code and get a 401 on the second attempt).
  const ranRef = useRef(false);

  useEffect(() => {
    if (ranRef.current) return;
    ranRef.current = true;

    const code = params.get('code');
    if (!code) {
      setError('Thiếu mã đăng nhập SSO. Vui lòng quay lại KiteHub và thử lại.');
      return;
    }

    exchangeSsoCode(code)
      .then((data) => {
        const rawRole = data.user?.role;
        const role = normalizeRole(rawRole);
        const tenantId = tenantIdFromToken(data.accessToken);
        const user = {
          // KH returns a UUID string for user.id; the KC User type expects number.
          // Cast keeps the store happy (same shim as useAuth.onSuccess).
          id: data.user.id as unknown as number,
          email: data.user.email,
          name: data.user.name,
          userType: role ?? (rawRole as UserType) ?? UserType.OWNER,
          referenceId: undefined,
        };

        // Persist tokens FIRST so the auth-store persist blob lands in this
        // tenant's namespace (GAP-1074 tenant-scoped localStorage).
        setTokens(data.accessToken, data.refreshToken, tenantId);
        setAuth(user, data.accessToken, data.refreshToken, tenantId);

        router.replace(role ? roleHome(role) : '/dashboard');
      })
      .catch(() => {
        setError(
          'Mã đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng quay lại KiteHub và thử lại.',
        );
      });
  }, [params, router, setAuth]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      {error ? (
        <>
          <h1 className="text-xl font-semibold text-destructive">Đăng nhập SSO thất bại</h1>
          <p className="max-w-md text-sm text-muted-foreground">{error}</p>
          <button
            type="button"
            onClick={() => router.replace('/login')}
            className="rounded-lg border px-4 py-2 text-sm font-medium hover:bg-muted"
          >
            Đến trang đăng nhập
          </button>
        </>
      ) : (
        <>
          <div
            className="h-8 w-8 animate-spin rounded-full border-2 border-muted border-t-primary"
            aria-hidden
          />
          <p className="text-sm text-muted-foreground">Đang đăng nhập vào trang quản lý trường...</p>
        </>
      )}
    </div>
  );
}

export default function SsoCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center p-6">
          <p className="text-sm text-muted-foreground">Đang tải...</p>
        </div>
      }
    >
      <SsoCallbackInner />
    </Suspense>
  );
}
