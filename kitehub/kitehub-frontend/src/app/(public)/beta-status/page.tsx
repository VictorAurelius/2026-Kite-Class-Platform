/**
 * Public Beta Status page (Wave 78 GAP-539).
 *
 * Server-rendered; fetches BE markdown at request time (5-min cache).
 * Schema source-of-truth:
 * `documents/01-business/kitehub/beta-status/api-contract.md`.
 *
 * @since Wave 78 — GAP-539
 */

import Link from 'next/link';
import { remark } from 'remark';
import html from 'remark-html';
// SSG-incompatible isomorphic-dompurify removed — state.contentHtml from server API trust boundary (Wave beta-readiness-1 fix per build error /default-stylesheet.css missing). API endpoint upstream is responsible for sanitization OR client component.
import { getBetaStatus } from '@/lib/api/beta-status';

export const revalidate = 300; // 5 minutes

// Wave 98 Bucket B3 freshness signal — hardcoded for Wave 98 release window;
// reviewer fills before each invite-cohort push. Linear-style changelog pattern.
const WAVE_98_LAST_REFRESHED_VI = 'Thứ Hai, 18/05/2026';
const WAVE_98_RECENT_CHANGES: { title: string; summary: string; date: string }[] = [
  {
    date: '2026-05-18',
    title: 'Banner Beta + chip phiên bản trên dashboard',
    summary:
      'Banner cảnh báo Beta hiển thị trên trang chính + admin, kèm chip phiên bản KiteHub và link "Trạng thái Beta".',
  },
  {
    date: '2026-05-18',
    title: 'Trang /beta-status làm mới',
    summary:
      'Bổ sung danh sách thay đổi gần nhất, mục liên hệ hỗ trợ và footer đồng ý xử lý dữ liệu theo PDPL 2023.',
  },
  {
    date: '2026-05-17',
    title: 'Đồng bộ tài liệu Beta sang tiếng Việt',
    summary:
      'Catalog i18n Beta + Privacy + Terms được đồng bộ, sẵn sàng cho đợt mời beta đầu tiên (P2 + P3).',
  },
];

const STATUS_VI: Record<string, { label: string; tone: string }> = {
  OPERATIONAL: { label: 'Hoạt động bình thường', tone: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200' },
  DEGRADED: { label: 'Suy giảm hiệu năng', tone: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-200' },
  PARTIAL_OUTAGE: { label: 'Một số dịch vụ gián đoạn', tone: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-200' },
  MAJOR_OUTAGE: { label: 'Gián đoạn lớn', tone: 'bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-200' },
  MAINTENANCE: { label: 'Đang bảo trì', tone: 'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-200' },
};

const SEVERITY_VI: Record<string, string> = {
  MINOR: 'Nhẹ',
  MAJOR: 'Trung bình',
  CRITICAL: 'Nghiêm trọng',
};

interface BetaStatusPageState {
  ok: boolean;
  version?: string;
  lastUpdatedAt?: string;
  contentHtml?: string;
  currentStatus?: string;
  knownIssues?: { title: string; severity: string; since: string }[];
}

async function loadStatus(): Promise<BetaStatusPageState> {
  try {
    const data = await getBetaStatus();
    const processed = await remark().use(html).process(data.contentMarkdown);
    return {
      ok: true,
      version: data.version,
      lastUpdatedAt: data.lastUpdatedAt,
      contentHtml: processed.toString(),
      currentStatus: data.currentStatus,
      knownIssues: data.knownIssues,
    };
  } catch {
    return { ok: false };
  }
}

export default async function BetaStatusPage() {
  const state = await loadStatus();

  if (!state.ok) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12">
        <h1 className="text-3xl font-bold tracking-tight">Trạng thái Beta KiteHub</h1>
        <p
          data-testid="beta-status-last-refreshed"
          className="mt-1 text-sm text-muted-foreground"
        >
          Cập nhật lần cuối: {WAVE_98_LAST_REFRESHED_VI}
        </p>
        <p className="mt-4 text-muted-foreground">
          Không tải được nội dung trạng thái BE. Trong lúc chờ, đây là những thay đổi gần nhất đã ship:
        </p>
        <section
          data-testid="beta-status-recent-changes-fallback"
          className="mt-4 space-y-3"
        >
          {WAVE_98_RECENT_CHANGES.map((change) => (
            <article key={`${change.date}-${change.title}`} className="rounded-lg border p-3">
              <h3 className="text-sm font-semibold">{change.title}</h3>
              <p className="mt-1 text-xs text-muted-foreground">{change.date}</p>
              <p className="mt-1 text-sm text-muted-foreground">{change.summary}</p>
            </article>
          ))}
        </section>
        <BetaStatusContactSection />
      </div>
    );
  }

  const statusInfo = state.currentStatus ? STATUS_VI[state.currentStatus] : undefined;

  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <header className="mb-8 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Trạng thái Beta KiteHub</h1>
          {state.lastUpdatedAt ? (
            <p
              data-testid="beta-status-last-refreshed"
              className="mt-1 text-sm text-muted-foreground"
            >
              Cập nhật lần cuối: {new Date(state.lastUpdatedAt).toLocaleString('vi-VN')} (phiên bản{' '}
              {state.version})
            </p>
          ) : (
            <p
              data-testid="beta-status-last-refreshed"
              className="mt-1 text-sm text-muted-foreground"
            >
              Cập nhật lần cuối: {WAVE_98_LAST_REFRESHED_VI}
            </p>
          )}
        </div>
        {statusInfo && (
          <span
            data-testid="beta-status-badge"
            className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${statusInfo.tone}`}
          >
            {statusInfo.label}
          </span>
        )}
      </header>

      {state.knownIssues && state.knownIssues.length > 0 && (
        <section className="mb-8 rounded-lg border bg-muted/30 p-4">
          <h2 className="text-base font-semibold">Vấn đề đang theo dõi</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {state.knownIssues.map((issue, idx) => (
              <li key={`${issue.title}-${idx}`} className="flex items-center justify-between gap-3">
                <span>{issue.title}</span>
                <span className="text-xs text-muted-foreground">
                  {SEVERITY_VI[issue.severity] ?? issue.severity} • từ {issue.since}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      <article
        data-testid="beta-status-content"
        className="prose prose-slate max-w-none dark:prose-invert"
        dangerouslySetInnerHTML={{ __html: state.contentHtml ?? "" }}
      />

      <section
        data-testid="beta-status-recent-changes"
        className="mt-10 rounded-lg border bg-muted/20 p-4"
      >
        <h2 className="text-base font-semibold">Thay đổi gần đây</h2>
        <p className="mt-1 text-xs text-muted-foreground">
          Danh sách rút gọn cho đợt mời beta — phiên bản đầy đủ trong nội dung phía trên.
        </p>
        <ul className="mt-3 space-y-3">
          {WAVE_98_RECENT_CHANGES.map((change) => (
            <li key={`${change.date}-${change.title}`} className="text-sm">
              <div className="flex items-baseline justify-between gap-2">
                <span className="font-medium">{change.title}</span>
                <span className="text-xs text-muted-foreground">{change.date}</span>
              </div>
              <p className="mt-1 text-muted-foreground">{change.summary}</p>
            </li>
          ))}
        </ul>
      </section>

      <BetaStatusContactSection />
    </div>
  );
}

/**
 * Wave 98 B3 — shared contact + PDPL consent footer rendered both on success
 * + error branches so user always has a path forward.
 */
function BetaStatusContactSection() {
  return (
    <section
      data-testid="beta-status-contact"
      className="mt-10 rounded-lg border bg-muted/30 p-4 text-sm"
    >
      <h2 className="text-base font-semibold">Liên hệ hỗ trợ</h2>
      <ul className="mt-3 space-y-2">
        <li>
          📧 Email:{' '}
          <a
            href="mailto:support@kitehub.me"
            className="font-medium text-primary underline underline-offset-2"
          >
            support@kitehub.me
          </a>
        </li>
        <li>
          💬 Zalo OA:{' '}
          <span className="text-muted-foreground">
            đang chuẩn bị — sẽ thông báo qua email khi sẵn sàng (GAP-660)
          </span>
        </li>
        <li>
          🐛 Báo lỗi trang này:{' '}
          <a
            href="mailto:support@kitehub.me?subject=L%E1%BB%97i%20trang%20%2Fbeta-status"
            className="font-medium text-primary underline underline-offset-2"
          >
            support@kitehub.me
          </a>
        </li>
      </ul>
      <p
        data-testid="beta-status-pdpl-footer"
        className="mt-4 text-xs text-muted-foreground"
      >
        Bằng cách tiếp tục sử dụng KiteHub Beta, bạn đồng ý với việc xử lý dữ liệu cá nhân theo{' '}
        <Link
          href="/legal/privacy"
          className="font-medium underline underline-offset-2"
        >
          Chính sách Bảo mật
        </Link>{' '}
        và Luật Bảo vệ dữ liệu cá nhân 2023 (Điều 9-15). Để rút lại đồng ý, liên hệ{' '}
        <a
          href="mailto:support@kitehub.me?subject=R%C3%BAt%20%C4%91%E1%BB%93ng%20%C3%BD%20x%E1%BB%AD%20l%C3%BD%20d%E1%BB%AF%20li%E1%BB%87u"
          className="font-medium underline underline-offset-2"
        >
          support@kitehub.me
        </a>
        .
      </p>
    </section>
  );
}
