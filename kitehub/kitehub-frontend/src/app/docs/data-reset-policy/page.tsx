import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Chính sách reset dữ liệu Beta — KiteHub',
  description:
    'Chính sách reset dữ liệu trong giai đoạn Beta của KiteHub: thông báo trước tối thiểu 7 ngày, backup 30 ngày, dữ liệu audit/thanh toán/đăng ký không bị reset.',
};

/**
 * GAP-1460: trang đích cho link /docs/data-reset-policy của BetaDisclaimerBanner.
 * Trước đây link 404 (chưa có route). Nội dung bám đúng cam kết trên banner.
 */
export default function DataResetPolicyPage() {
  return (
    <main className="mx-auto max-w-3xl px-4 py-10 text-foreground">
      <h1 className="text-2xl font-bold tracking-tight">Chính sách reset dữ liệu Beta</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        Áp dụng trong giai đoạn KiteHub Beta · Cập nhật 2026-06-16
      </p>

      <section className="mt-6 space-y-4 text-sm leading-relaxed">
        <p>
          KiteHub đang trong giai đoạn <strong>Beta</strong>. Chúng tôi cam kết{' '}
          <strong>không tự ý reset</strong> dữ liệu tenant của trung tâm. Trang này mô tả rõ khi nào
          dữ liệu có thể bị ảnh hưởng và các biện pháp bảo vệ đi kèm.
        </p>

        <h2 className="pt-2 text-lg font-semibold">1. Khi nào có thể reset</h2>
        <p>
          Việc reset chỉ xảy ra trong hai trường hợp kỹ thuật bắt buộc:
        </p>
        <ul className="list-disc space-y-1 pl-6">
          <li>
            <strong>Migration breaking:</strong> thay đổi cấu trúc dữ liệu không tương thích ngược
            (hiếm, chỉ trong giai đoạn Beta sớm).
          </li>
          <li>
            <strong>Exit-BETA cutover:</strong> chuyển từ hạ tầng Beta sang hạ tầng phát hành chính
            thức.
          </li>
        </ul>

        <h2 className="pt-2 text-lg font-semibold">2. Thông báo trước tối thiểu 7 ngày</h2>
        <p>
          Mọi reset đều được thông báo trước <strong>tối thiểu 7 ngày</strong> qua email đăng ký và
          banner trên dashboard, kèm hướng dẫn sao lưu/khôi phục.
        </p>

        <h2 className="pt-2 text-lg font-semibold">3. Backup 30 ngày</h2>
        <p>
          Hệ thống giữ <strong>bản sao lưu 30 ngày</strong>. Nếu cần khôi phục sau reset, vui lòng
          liên hệ trong cửa sổ 30 ngày kể từ thời điểm reset.
        </p>

        <h2 className="pt-2 text-lg font-semibold">4. Dữ liệu KHÔNG bao giờ bị reset</h2>
        <p>Các nhóm dữ liệu sau được bảo toàn xuyên suốt, không bị reset trong bất kỳ trường hợp nào:</p>
        <ul className="list-disc space-y-1 pl-6">
          <li>Nhật ký kiểm toán (audit log)</li>
          <li>Lịch sử thanh toán (payment)</li>
          <li>Thông tin đăng ký dịch vụ (subscription)</li>
        </ul>

        <h2 className="pt-2 text-lg font-semibold">5. Hỗ trợ</h2>
        <p>
          Gặp vấn đề hoặc cần khôi phục dữ liệu? Xem{' '}
          <Link href="/beta-status" className="font-medium underline underline-offset-2">
            trạng thái Beta
          </Link>{' '}
          hoặc email{' '}
          <a
            href="mailto:support@kitehub.me"
            className="font-medium underline underline-offset-2"
          >
            support@kitehub.me
          </a>
          .
        </p>
      </section>

      <div className="mt-8">
        <Link href="/dashboard" className="text-sm font-medium underline underline-offset-2">
          ← Về Dashboard
        </Link>
      </div>
    </main>
  );
}
