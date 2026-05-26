/**
 * Waitlist page — Phase 1.5+ deferral landing.
 *
 * Wave beta-prep-1 Bucket F7 — multi-branch filter per ADR-036.
 * Tenants with > 1 branch submitting beta access form redirect here instead of
 * sending request. Phase 1 BETA scope limits to single-branch tenants only.
 *
 * URL params:
 *   ?reason=multi-branch&branches=N
 *
 * VN-localization audit checklist (per .claude/rules/vn-localization-audit-checklist.md):
 *   §2 Vietnamese label, §4 niên khóa 9-5 + Phase 1.5 timeline disclosure.
 */

import Link from 'next/link';

export const metadata = {
  title: 'Waitlist Phase 1.5 — KiteHub',
  description:
    'Đăng ký waitlist cho trung tâm nhiều chi nhánh. KiteHub Phase 1 BETA chỉ hỗ trợ 1 chi nhánh; Phase 1.5 mở rộng đa chi nhánh dự kiến Q3 2026.',
};

interface WaitlistPageProps {
  searchParams: Promise<{ reason?: string; branches?: string }>;
}

export default async function WaitlistPage({ searchParams }: WaitlistPageProps) {
  const params = await searchParams;
  const reason = params.reason ?? 'unknown';
  const branches = params.branches ? parseInt(params.branches, 10) : null;

  const isMultiBranch = reason === 'multi-branch';

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b">
        <nav className="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
          <Link href="/" className="text-lg font-bold">
            KiteHub
          </Link>
          <Link href="/" className="text-sm hover:underline">
            ← Về trang chủ
          </Link>
        </nav>
      </header>

      <main className="mx-auto max-w-3xl px-6 py-16">
        <div className="rounded-2xl border bg-card p-8 shadow-sm">
          <p className="text-sm font-medium text-primary">Phase 1.5 Waitlist</p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">
            Cảm ơn chị/anh đã quan tâm KiteHub!
          </h1>

          {isMultiBranch && (
            <div className="mt-6 rounded-xl border-l-4 border-amber-500 bg-amber-50 p-4 text-sm text-amber-900">
              <p className="font-semibold">
                Trung tâm của chị/anh có {branches ?? 'nhiều'} chi nhánh
              </p>
              <p className="mt-1">
                Phase 1 BETA của KiteHub đang ưu tiên hoàn thiện trải nghiệm cho trung
                tâm 1 chi nhánh trước (cohort 5 tenant đầu tiên). Tính năng đồng bộ
                dữ liệu + phân quyền theo chi nhánh sẽ được mở trong{' '}
                <strong>Phase 1.5 — dự kiến Q3 2026</strong>.
              </p>
            </div>
          )}

          <div className="mt-6 space-y-4 text-base">
            <h2 className="text-lg font-semibold">Em sẽ liên hệ chị/anh khi nào?</h2>
            <ul className="ml-6 list-disc space-y-2 text-muted-foreground">
              <li>
                <strong>Đầu Phase 1.5</strong> (dự kiến tháng 7-8/2026) — em gửi email
                + Zalo OA invite Beta dành riêng cho đa chi nhánh
              </li>
              <li>
                <strong>Phụ thuộc</strong>: hoàn thiện feature multi-branch (phân
                quyền, đồng bộ học sinh giữa chi nhánh, báo cáo tổng hợp) trong Wave
                BETA-3
              </li>
              <li>
                <strong>Không tính phí</strong> waitlist — đăng ký miễn phí, không
                spam
              </li>
            </ul>
          </div>

          <div className="mt-8 space-y-4">
            <h2 className="text-lg font-semibold">Trong lúc chờ Phase 1.5</h2>
            <p className="text-base text-muted-foreground">
              Nếu chị/anh muốn thử KiteHub ngay với <strong>1 chi nhánh</strong> đại
              diện (vd chi nhánh chính + tự quản tay các chi nhánh phụ tạm thời), em
              vẫn hỗ trợ được. Vui lòng email{' '}
              <a
                href="mailto:support@kitehub.me?subject=Phase%201%20BETA%20-%20single-branch%20test"
                className="text-primary underline hover:opacity-80"
              >
                support@kitehub.me
              </a>{' '}
              để em hướng dẫn riêng nhé.
            </p>
          </div>

          <div className="mt-8 grid gap-4 sm:grid-cols-2">
            <a
              href="mailto:waitlist@kitehub.me?subject=Phase%201.5%20Waitlist%20-%20Multi-branch"
              className="rounded-xl bg-primary px-6 py-3 text-center text-sm font-semibold text-primary-foreground hover:opacity-90"
            >
              Đăng ký waitlist qua email
            </a>
            <a
              href="https://zalo.me/kitehub"
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-xl border-2 px-6 py-3 text-center text-sm font-semibold hover:border-primary hover:text-primary"
            >
              Nhắn em qua Zalo OA
            </a>
          </div>

          <p className="mt-8 text-xs italic text-muted-foreground">
            [Phase 1.5 timeline dự kiến — có thể thay đổi dựa trên feedback Phase 1
            BETA cohort. Em sẽ cập nhật sớm nhất qua email + Zalo OA. Cảm ơn chị/anh
            đã thông cảm.]
          </p>
        </div>
      </main>
    </div>
  );
}
