import type { Metadata } from 'next';
import Link from 'next/link';

/**
 * Help / Support page — KiteClass.
 *
 * Phase 1 BETA stub (GAP-1069 dashboard footer cleanup) — provides a real
 * destination for the footer "Trợ giúp" link (was 404). Vietnamese-first per
 * `dev-readable-doc-language.md` + `vn-localization-audit-checklist.md`.
 */
export const metadata: Metadata = {
  title: 'Trợ giúp | KiteClass',
  description:
    'Trung tâm trợ giúp KiteClass — hướng dẫn sử dụng, câu hỏi thường gặp và kênh hỗ trợ cho trung tâm giáo dục.',
};

export default function HelpPage() {
  return (
    <main className="container mx-auto max-w-3xl px-4 py-12">
      <article className="space-y-6 text-sm leading-relaxed">
        <header className="space-y-2">
          <h1 className="text-2xl font-semibold">Trung tâm trợ giúp</h1>
          <p className="text-muted-foreground">
            Chào bạn! Trang này tập hợp các nguồn hỗ trợ khi bạn sử dụng KiteClass.
          </p>
        </header>

        <section className="space-y-3">
          <h2 className="text-lg font-medium">Câu hỏi thường gặp</h2>
          <ul className="list-disc space-y-2 pl-5 text-muted-foreground">
            <li>Đăng nhập &amp; quản lý tài khoản trung tâm</li>
            <li>Tạo khóa học, lớp học và xếp lịch dạy</li>
            <li>Quản lý học viên, điểm danh và bảng điểm</li>
            <li>Lập hóa đơn và ghi nhận thanh toán học phí</li>
          </ul>
        </section>

        <section className="space-y-2">
          <h2 className="text-lg font-medium">Liên hệ hỗ trợ</h2>
          <p className="text-muted-foreground">
            Cần hỗ trợ thêm? Gửi email tới{' '}
            <a href="mailto:support@kitehub.me" className="text-primary hover:underline">
              support@kitehub.me
            </a>{' '}
            — đội ngũ KiteClass sẽ phản hồi trong giờ làm việc.
          </p>
        </section>

        <section className="space-y-1">
          <h2 className="text-lg font-medium">Tài liệu pháp lý</h2>
          <p className="text-muted-foreground">
            Xem{' '}
            <Link href="/legal/privacy" className="text-primary hover:underline">
              Chính sách quyền riêng tư
            </Link>{' '}
            và{' '}
            <Link href="/legal/terms" className="text-primary hover:underline">
              Điều khoản dịch vụ
            </Link>
            .
          </p>
        </section>
      </article>
    </main>
  );
}
