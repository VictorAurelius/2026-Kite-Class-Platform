/**
 * Friendly status page for tenants currently SUSPENDED / ARCHIVED / DELETED
 * (GAP-811).
 *
 * Reached via middleware 307 redirect when `resolveTenant` raises
 * `TenantSuspendedError` (BE 410 GONE). Query params:
 *
 * - `slug=<subdomain>` — the tenant slug the user attempted to access
 * - `status=<lowercase>` — one of `suspended` / `archived` / `deleted`
 *
 * Phase 1 BETA scope: static informational page. Designed content per
 * `documents/01-business/kitehub/marketing/` will replace this in a follow-up.
 *
 * Ported per GAP-1077 từ kitehub-frontend — host→tenant middleware thuộc về
 * kiteclass-frontend.
 *
 * @author KiteClass Team
 */

import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Trang tạm ngưng — KiteClass',
  description: 'Trang của trung tâm này đang tạm ngưng phục vụ.',
};

interface SuspendedPageProps {
  searchParams: Promise<{ slug?: string; status?: string }>;
}

const STATUS_COPY: Record<string, { title: string; body: string }> = {
  suspended: {
    title: 'Trang tạm ngưng phục vụ',
    body: 'Trung tâm này đang tạm ngưng hoạt động trên KiteClass. Vui lòng liên hệ trung tâm để biết thêm chi tiết.',
  },
  archived: {
    title: 'Trang đã lưu trữ',
    body: 'Trung tâm này đã ngừng sử dụng KiteClass. Vui lòng liên hệ trung tâm để được hỗ trợ.',
  },
  deleted: {
    title: 'Trang không còn tồn tại',
    body: 'Trang của trung tâm này đã được gỡ bỏ khỏi KiteClass.',
  },
};

const DEFAULT_COPY = STATUS_COPY.suspended!;

export default async function SuspendedPage({ searchParams }: SuspendedPageProps) {
  const params = await searchParams;
  const slug = params.slug ?? '';
  const statusKey = (params.status ?? 'suspended').toLowerCase();
  const copy = STATUS_COPY[statusKey] ?? DEFAULT_COPY;

  return (
    <main className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center px-6 py-16 text-center">
      <h1 className="mb-4 text-3xl font-semibold text-foreground">{copy.title}</h1>
      <p className="mb-6 text-muted-foreground">{copy.body}</p>
      {slug ? (
        <p className="mb-4 text-sm text-muted-foreground">
          Trang yêu cầu: <code className="rounded bg-muted px-2 py-1">{slug}</code>
        </p>
      ) : null}
      <a
        href="https://kitehub.me"
        className="rounded-lg bg-primary px-6 py-3 text-primary-foreground hover:bg-primary/90"
      >
        Về trang chủ KiteClass
      </a>
    </main>
  );
}
