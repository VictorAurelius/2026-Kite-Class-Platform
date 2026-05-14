/**
 * Public Beta Status page (Wave 78 GAP-539).
 *
 * Server-rendered; fetches BE markdown at request time (5-min cache).
 * Schema source-of-truth:
 * `documents/01-business/kitehub/beta-status/api-contract.md`.
 *
 * @since Wave 78 — GAP-539
 */

import { remark } from 'remark';
import html from 'remark-html';
import { getBetaStatus } from '@/lib/api/beta-status';

export const revalidate = 300; // 5 minutes

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
      <main className="mx-auto max-w-3xl px-4 py-12">
        <h1 className="text-3xl font-bold tracking-tight">Trạng thái Beta KiteHub</h1>
        <p className="mt-4 text-muted-foreground">
          Không tải được nội dung trạng thái. Vui lòng thử lại sau hoặc liên hệ{' '}
          <a href="mailto:support@kitehub.me" className="underline">
            support@kitehub.me
          </a>
          .
        </p>
      </main>
    );
  }

  const statusInfo = state.currentStatus ? STATUS_VI[state.currentStatus] : undefined;

  return (
    <main className="mx-auto max-w-3xl px-4 py-12">
      <header className="mb-8 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Trạng thái Beta KiteHub</h1>
          {state.lastUpdatedAt && (
            <p className="mt-1 text-sm text-muted-foreground">
              Cập nhật lần cuối: {new Date(state.lastUpdatedAt).toLocaleString('vi-VN')} (phiên bản{' '}
              {state.version})
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
        dangerouslySetInnerHTML={{ __html: state.contentHtml ?? '' }}
      />
    </main>
  );
}
