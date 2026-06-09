import type { Metadata } from 'next';

import { DataRightsForm } from './DataRightsForm';

/**
 * DSAR self-service intake page — KiteHub.
 *
 * Phase 2 v1 (Wave 26 Bucket A, GAP-353c) — Vietnamese-first, EN deferred to GAP-182 Phase 2.
 * Backed by `BR-PDPL-DSAR-001..005` in `documents/01-business/kitehub/marketing/rules.md`.
 * 6 PDPL Art 14 rights + 20-day SLA (Decree 13/2023 Art 19).
 */
export const metadata: Metadata = {
  title: 'Yêu cầu quyền dữ liệu cá nhân (DSAR) | KiteHub',
  description:
    'Form tự phục vụ để thực hiện 6 quyền dữ liệu cá nhân theo Điều 14 Nghị định 13/2023/NĐ-CP — quyền truy cập, chỉnh sửa, xoá, chuyển dữ liệu, hạn chế xử lý, phản đối xử lý. SLA 20 ngày.',
};

export default function DataRightsPage() {
  return (
    <div className="container max-w-3xl py-12">
      <article className="space-y-6 text-sm leading-relaxed">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">Yêu cầu quyền dữ liệu cá nhân</h1>
          <p className="text-muted-foreground">
            Theo Điều 14 Nghị định 13/2023/NĐ-CP (PDPL), bạn có 6 quyền đối với dữ liệu cá nhân của mình. Sử dụng
            biểu mẫu bên dưới để gửi yêu cầu. DPO sẽ phản hồi trong tối đa <strong>20 ngày</strong>.
          </p>
        </header>

        <aside
          role="note"
          className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
        >
          <p>
            <strong>v1 — đang chờ legal counsel review.</strong> Quy trình DSAR chính thức sẽ được cập nhật sau khi
            luật sư rà soát (GAP-182 Phase 2). Trong khi chờ, yêu cầu vẫn được tiếp nhận và xử lý theo đúng SLA 20
            ngày của PDPL.
          </p>
        </aside>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold">Bạn cũng có thể liên hệ trực tiếp</h2>
          <p>
            Nếu không muốn dùng form, bạn có thể email DPO trực tiếp tại{' '}
            <a className="underline" href="mailto:dpo@kitehub.me">dpo@kitehub.me</a>. Vui lòng cung cấp đủ thông tin
            xác minh (họ tên, email đăng ký, 4 chữ số cuối CCCD/CMND) để DPO xử lý nhanh.
          </p>
        </section>

        <section className="rounded-md border bg-card p-6 shadow-sm">
          <DataRightsForm />
        </section>

        <footer className="space-y-2 text-xs text-muted-foreground">
          <p>
            Các quyền này không bao gồm dữ liệu thuộc nghĩa vụ tuân thủ pháp lý (kế toán, thuế, lưu trữ pháp định).
            Xem chi tiết tại{' '}
            <a className="underline" href="/legal/privacy">Chính sách quyền riêng tư</a>.
          </p>
          <p>Cập nhật lần cuối: 2026-05-06 · Phiên bản: 1.0</p>
        </footer>
      </article>
    </div>
  );
}
