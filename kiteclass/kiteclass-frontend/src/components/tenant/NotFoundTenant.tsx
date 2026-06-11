/**
 * Friendly "trung tâm không tồn tại" page (GAP-1200).
 *
 * Rendered by the public landing page when the middleware resolved a real
 * subdomain slug but the backend returned 404 (no tenant). This replaces the
 * previous silent fallback to the env/default tenant landing — which showed a
 * DIFFERENT center's brand on a mistyped subdomain (confusing + mild
 * content-leak). Generic KiteClass branding only: no tenant name/logo/contact.
 *
 * @author KiteClass Team
 */

import { SearchX } from 'lucide-react';

interface NotFoundTenantProps {
  /** The subdomain slug the visitor attempted (e.g. "khong-ton-tai"). */
  slug?: string;
}

export function NotFoundTenant({ slug }: NotFoundTenantProps) {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center p-8 text-center">
      <SearchX className="mb-4 h-16 w-16 text-muted-foreground" aria-hidden />
      <h1 className="mb-2 text-2xl font-bold">Không tìm thấy trung tâm</h1>
      <p className="mb-2 max-w-md text-muted-foreground">
        {slug ? (
          <>
            Địa chỉ <span className="font-semibold">{slug}</span> chưa gắn với trung tâm nào trên
            KiteClass.
          </>
        ) : (
          'Địa chỉ này chưa gắn với trung tâm nào trên KiteClass.'
        )}
      </p>
      <p className="max-w-md text-sm text-muted-foreground">
        Vui lòng kiểm tra lại đường dẫn, hoặc liên hệ trung tâm để có địa chỉ chính xác.
      </p>
    </div>
  );
}
