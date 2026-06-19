import type { Metadata } from 'next';

/**
 * Email unsubscribe landing — KiteHub.
 *
 * Đích đến của link `unsubscribeUrl` trong footer các email giao dịch
 * (welcome / tenant-ready / beta-invite) — `EmailServiceClient`. Reach cả
 * non-user (beta-invitee) nên PHẢI là trang public, không authed.
 *
 * v1 stub (Phase 1 close-2, GAP-1414 follow-up): trang xác nhận + hướng dẫn
 * opt-out. Token-based one-click revoke logic chờ BE wiring — xem follow-up gap.
 * `searchParams` đọc SERVER-side (Next 15) để tránh useSearchParams prerender bailout.
 */
export const metadata: Metadata = {
  title: 'Hủy nhận email | KiteHub',
  description:
    'Trang hủy đăng ký nhận email từ KiteHub. Bạn có thể ngừng nhận email tiếp thị và thông báo không bắt buộc bất cứ lúc nào.',
};

interface UnsubscribePageProps {
  searchParams: Promise<{ email?: string; token?: string }>;
}

export default async function UnsubscribePage({ searchParams }: UnsubscribePageProps) {
  const params = await searchParams;
  const email = params.email;

  return (
    <div className="container max-w-3xl py-12">
      <article className="space-y-6 text-sm leading-relaxed">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">Hủy nhận email</h1>
          <p className="text-muted-foreground">
            Chúng tôi đã ghi nhận yêu cầu ngừng nhận email tiếp thị và thông báo không bắt buộc từ KiteHub
            {email ? (
              <>
                {' '}cho địa chỉ <strong>{email}</strong>
              </>
            ) : null}
            .
          </p>
        </header>

        <aside
          role="note"
          className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
        >
          <p>
            <strong>v1 — đang hoàn thiện.</strong> Cơ chế hủy một chạm bằng token đang được hoàn thiện. Trong khi
            chờ, vui lòng gửi yêu cầu hủy tới{' '}
            <a className="underline" href="mailto:support@kitehub.me">support@kitehub.me</a> kèm địa chỉ email của
            bạn — chúng tôi sẽ xử lý trong tối đa 48 giờ.
          </p>
        </aside>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold">Lưu ý về email bắt buộc</h2>
          <p>
            Một số email giao dịch quan trọng (xác thực tài khoản, cảnh báo bảo mật, thông báo thanh toán, quyền
            dữ liệu cá nhân) vẫn được gửi kể cả khi bạn hủy nhận email tiếp thị — vì đây là các thông báo bắt buộc
            phục vụ tài khoản và tuân thủ pháp luật.
          </p>
        </section>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold">Quản lý tùy chọn thông báo</h2>
          <p>
            Nếu bạn đã có tài khoản KiteHub, bạn có thể tùy chỉnh chi tiết loại email muốn nhận tại trang{' '}
            <a className="underline" href="/settings/notifications">Cài đặt &rsaquo; Thông báo</a> (yêu cầu đăng nhập).
          </p>
        </section>
      </article>
    </div>
  );
}
