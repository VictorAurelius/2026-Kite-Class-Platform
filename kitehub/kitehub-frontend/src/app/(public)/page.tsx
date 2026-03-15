import Link from 'next/link';

const features = [
  { icon: '👨‍🎓', title: 'Quản lý học viên', desc: 'Hồ sơ, điểm danh, bảng điểm tự động' },
  { icon: '👨‍🏫', title: 'Quản lý giảng viên', desc: 'Chuyên môn, lịch dạy, đánh giá' },
  { icon: '📚', title: 'Khóa học & Lớp học', desc: 'Lịch học, buổi học, tài liệu LMS' },
  { icon: '✅', title: 'Điểm danh tự động', desc: 'Check-in/out, báo cáo chuyên cần' },
  { icon: '💳', title: 'Thanh toán & Hóa đơn', desc: 'VietQR, trả góp, xuất hóa đơn' },
  { icon: '🎨', title: 'Landing page AI', desc: 'Logo → branding assets tự động bằng AI' },
];

const stats = [
  { value: '500+', label: 'Trung tâm' },
  { value: '50,000+', label: 'Học viên' },
  { value: '2,000+', label: 'Khóa học' },
];

export default function HomePage() {
  return (
    <>
      {/* Hero */}
      <section className="container py-20 md:py-32">
        <div className="mx-auto max-w-3xl text-center">
          <h1 className="text-4xl font-bold tracking-tight sm:text-6xl">
            Quản lý trung tâm giáo dục{' '}
            <span className="text-primary">thông minh hơn</span>
          </h1>
          <p className="mt-6 text-lg text-muted-foreground">
            KiteHub giúp bạn tạo và quản lý hệ thống KiteClass cho trung tâm/trường học.
            Quản lý học viên, khóa học, điểm danh, thanh toán — tất cả trong một nền tảng.
          </p>
          <div className="mt-10 flex items-center justify-center gap-4">
            <Link
              href="/register"
              className="rounded-md bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow hover:bg-primary/90"
            >
              Dùng thử miễn phí 14 ngày
            </Link>
            <Link
              href="/pricing"
              className="rounded-md border px-6 py-3 text-sm font-semibold hover:bg-muted"
            >
              Xem bảng giá
            </Link>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="border-t bg-muted/30 py-20">
        <div className="container">
          <h2 className="text-center text-3xl font-bold">Tính năng nổi bật</h2>
          <p className="mt-2 text-center text-muted-foreground">
            Mọi thứ bạn cần để vận hành trung tâm giáo dục hiệu quả
          </p>
          <div className="mt-12 grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => (
              <div key={f.title} className="rounded-lg border bg-card p-6 shadow-sm">
                <div className="text-3xl">{f.icon}</div>
                <h3 className="mt-4 text-lg font-semibold">{f.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats */}
      <section className="py-20">
        <div className="container">
          <div className="mx-auto grid max-w-2xl grid-cols-3 gap-8 text-center">
            {stats.map((s) => (
              <div key={s.label}>
                <div className="text-3xl font-bold text-primary">{s.value}</div>
                <div className="mt-1 text-sm text-muted-foreground">{s.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="border-t bg-primary/5 py-20">
        <div className="container text-center">
          <h2 className="text-3xl font-bold">Sẵn sàng bắt đầu?</h2>
          <p className="mt-4 text-muted-foreground">
            Tạo tài khoản miễn phí và trải nghiệm KiteClass trong 14 ngày.
            Không cần thẻ tín dụng.
          </p>
          <Link
            href="/register"
            className="mt-8 inline-block rounded-md bg-primary px-8 py-3 text-sm font-semibold text-primary-foreground shadow hover:bg-primary/90"
          >
            Đăng ký ngay
          </Link>
        </div>
      </section>
    </>
  );
}
