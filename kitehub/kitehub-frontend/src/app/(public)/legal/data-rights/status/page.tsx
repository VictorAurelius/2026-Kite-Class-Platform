import type { Metadata } from 'next';

/**
 * DSAR ticket status check — KiteHub.
 *
 * Đích đến của `statusCheckUrl` trong email `dsar-acknowledgement-requester`
 * (`EmailServiceClient`) — PDPL Điều 14 ticket confirmation. Link dạng
 * `/legal/data-rights/status?id=<ticketUuid>`. Reach cả non-user nên là trang public.
 *
 * v1 stub (Phase 1 close-2, GAP-1414 follow-up): hiển thị mã ticket + hướng dẫn.
 * Truy vấn trạng thái live theo ticket chờ BE endpoint — xem follow-up gap.
 * `searchParams` đọc SERVER-side (Next 15) để tránh useSearchParams prerender bailout.
 */
export const metadata: Metadata = {
  title: 'Tra cứu trạng thái yêu cầu quyền dữ liệu (DSAR) | KiteHub',
  description:
    'Tra cứu trạng thái xử lý yêu cầu quyền dữ liệu cá nhân (DSAR) theo Điều 14 Nghị định 13/2023/NĐ-CP. SLA 20 ngày.',
};

interface DsarStatusPageProps {
  searchParams: Promise<{ id?: string }>;
}

export default async function DsarStatusPage({ searchParams }: DsarStatusPageProps) {
  const params = await searchParams;
  const ticketId = params.id;

  return (
    <div className="container max-w-3xl py-12">
      <article className="space-y-6 text-sm leading-relaxed">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">Trạng thái yêu cầu quyền dữ liệu</h1>
          <p className="text-muted-foreground">
            Tra cứu tiến độ xử lý yêu cầu quyền dữ liệu cá nhân (DSAR) của bạn theo Điều 14 Nghị định
            13/2023/NĐ-CP (PDPL). DPO xử lý trong tối đa <strong>20 ngày</strong>.
          </p>
        </header>

        {ticketId ? (
          <section className="rounded-md border bg-card p-6 shadow-sm">
            <p className="text-muted-foreground">Mã yêu cầu (ticket):</p>
            <p className="mt-1 break-all font-mono text-base font-semibold">{ticketId}</p>
          </section>
        ) : (
          <aside
            role="note"
            className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
          >
            <p>
              Không tìm thấy mã yêu cầu trong đường dẫn. Vui lòng mở lại link trong email xác nhận DSAR của bạn,
              hoặc liên hệ DPO để được hỗ trợ.
            </p>
          </aside>
        )}

        <aside
          role="note"
          className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
        >
          <p>
            <strong>v1 — đang hoàn thiện.</strong> Tra cứu trạng thái trực tuyến theo mã ticket đang được hoàn
            thiện. Trong khi chờ, vui lòng liên hệ DPO tại{' '}
            <a className="underline" href="mailto:dpo@kitehub.me">dpo@kitehub.me</a> (kèm mã ticket ở trên) để biết
            tiến độ xử lý.
          </p>
        </aside>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold">Gửi yêu cầu mới</h2>
          <p>
            Nếu bạn muốn gửi một yêu cầu quyền dữ liệu mới, hãy dùng{' '}
            <a className="underline" href="/legal/data-rights">biểu mẫu DSAR tự phục vụ</a>.
          </p>
        </section>
      </article>
    </div>
  );
}
