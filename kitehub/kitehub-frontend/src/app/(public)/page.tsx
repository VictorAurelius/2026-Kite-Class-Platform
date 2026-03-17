import Link from 'next/link';
import {
  Users,
  GraduationCap,
  BookOpen,
  CheckCircle,
  CreditCard,
  Palette,
  ArrowRight,
  Star,
  Sparkles,
} from 'lucide-react';

const features = [
  {
    icon: Users,
    title: 'Quản lý học viên',
    desc: 'Hồ sơ chi tiết, theo dõi tiến độ, bảng điểm tự động cập nhật.',
  },
  {
    icon: GraduationCap,
    title: 'Quản lý giảng viên',
    desc: 'Phân công lịch dạy, theo dõi chuyên môn, đánh giá hiệu suất.',
  },
  {
    icon: BookOpen,
    title: 'Khóa học & Lớp học',
    desc: 'Tạo khóa học, quản lý lịch học, chia sẻ tài liệu LMS.',
  },
  {
    icon: CheckCircle,
    title: 'Điểm danh thông minh',
    desc: 'Check-in/out tự động, báo cáo chuyên cần real-time.',
  },
  {
    icon: CreditCard,
    title: 'Thanh toán & Hóa đơn',
    desc: 'VietQR, trả góp linh hoạt, xuất hóa đơn điện tử.',
  },
  {
    icon: Palette,
    title: 'Branding AI',
    desc: 'Tạo logo, landing page tự động với AI trong vài phút.',
  },
];

const stats = [
  { value: '500+', label: 'Trung tâm tin dùng' },
  { value: '50,000+', label: 'Học viên đang học' },
  { value: '99.9%', label: 'Uptime cam kết' },
];

const testimonials = [
  {
    name: 'Nguyễn Văn A',
    role: 'Giám đốc Trung tâm Anh ngữ ABC',
    content: 'KiteHub giúp chúng tôi tiết kiệm 10 giờ mỗi tuần cho công việc quản lý. Giao diện đơn giản, dễ dùng.',
    avatar: 'NA',
  },
  {
    name: 'Trần Thị B',
    role: 'Quản lý Trung tâm Toán học XYZ',
    content: 'Tính năng điểm danh tự động là tuyệt vời. Phụ huynh rất hài lòng khi nhận thông báo real-time.',
    avatar: 'TB',
  },
  {
    name: 'Lê Văn C',
    role: 'Founder Music Academy',
    content: 'Từ khi dùng KiteHub, số lượng học viên đăng ký mới tăng 30% nhờ landing page chuyên nghiệp.',
    avatar: 'LC',
  },
];

export default function HomePage() {
  return (
    <>
      {/* Hero Section */}
      <section className="relative overflow-hidden">
        {/* Background decoration */}
        <div className="absolute inset-0 -z-10">
          <div className="absolute top-20 left-10 h-72 w-72 rounded-full bg-primary/10 blur-3xl" />
          <div className="absolute bottom-20 right-10 h-72 w-72 rounded-full bg-accent/10 blur-3xl" />
        </div>

        <div className="container py-20 md:py-32">
          <div className="mx-auto max-w-4xl text-center">
            {/* Badge */}
            <div className="mb-6 inline-flex items-center gap-2 rounded-full border bg-background/80 px-4 py-1.5 text-sm backdrop-blur">
              <Sparkles className="h-4 w-4 text-accent" />
              <span>Dùng thử miễn phí 14 ngày</span>
            </div>

            {/* Headline */}
            <h1 className="text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl">
              Để học viên của bạn
              <span className="relative mx-2">
                <span className="relative z-10 text-primary">bay cao</span>
                <svg
                  className="absolute -bottom-2 left-0 h-3 w-full text-primary/30"
                  viewBox="0 0 200 12"
                  preserveAspectRatio="none"
                >
                  <path
                    d="M0 10 Q50 0 100 10 T200 10"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="4"
                  />
                </svg>
              </span>
              hơn mỗi ngày
            </h1>

            {/* Subheadline */}
            <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground md:text-xl">
              KiteHub là nền tảng quản lý trung tâm giáo dục toàn diện.
              Quản lý học viên, khóa học, điểm danh, thanh toán — tất cả trong một.
            </p>

            {/* CTA Buttons */}
            <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Link
                href="/register"
                className="group inline-flex items-center gap-2 rounded-lg bg-primary px-8 py-3.5 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-all hover:bg-primary/90 hover:shadow-xl hover:shadow-primary/30"
              >
                Bắt đầu miễn phí
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </Link>
              <Link
                href="/pricing"
                className="inline-flex items-center gap-2 rounded-lg border bg-background px-8 py-3.5 text-sm font-semibold transition-colors hover:bg-muted"
              >
                Xem bảng giá
              </Link>
            </div>

            {/* Trust indicators */}
            <div className="mt-12 flex items-center justify-center gap-8 text-sm text-muted-foreground">
              <div className="flex items-center gap-1">
                <CheckCircle className="h-4 w-4 text-green-500" />
                <span>Không cần thẻ tín dụng</span>
              </div>
              <div className="flex items-center gap-1">
                <CheckCircle className="h-4 w-4 text-green-500" />
                <span>Setup trong 5 phút</span>
              </div>
              <div className="flex items-center gap-1">
                <CheckCircle className="h-4 w-4 text-green-500" />
                <span>Hỗ trợ 24/7</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="border-y bg-muted/30 py-20">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Mọi thứ bạn cần để vận hành hiệu quả
            </h2>
            <p className="mt-4 text-lg text-muted-foreground">
              Một nền tảng duy nhất thay thế hàng chục công cụ riêng lẻ
            </p>
          </div>

          <div className="mt-16 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((feature) => (
              <div
                key={feature.title}
                className="group relative rounded-2xl border bg-card p-6 shadow-sm transition-all hover:shadow-md hover:border-primary/50"
              >
                <div className="mb-4 inline-flex rounded-xl bg-primary/10 p-3">
                  <feature.icon className="h-6 w-6 text-primary" />
                </div>
                <h3 className="text-lg font-semibold">{feature.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground leading-relaxed">
                  {feature.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-20">
        <div className="container">
          <div className="mx-auto grid max-w-4xl grid-cols-1 gap-8 sm:grid-cols-3">
            {stats.map((stat) => (
              <div key={stat.label} className="text-center">
                <div className="text-4xl font-bold text-primary md:text-5xl">
                  {stat.value}
                </div>
                <div className="mt-2 text-sm text-muted-foreground">
                  {stat.label}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section className="border-t bg-muted/30 py-20">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Được tin dùng bởi hàng trăm trung tâm
            </h2>
            <p className="mt-4 text-lg text-muted-foreground">
              Xem những gì khách hàng nói về KiteHub
            </p>
          </div>

          <div className="mt-16 grid gap-8 md:grid-cols-3">
            {testimonials.map((testimonial) => (
              <div
                key={testimonial.name}
                className="rounded-2xl border bg-card p-6 shadow-sm"
              >
                <div className="flex items-center gap-1 text-yellow-500">
                  {[...Array(5)].map((_, i) => (
                    <Star key={i} className="h-4 w-4 fill-current" />
                  ))}
                </div>
                <p className="mt-4 text-sm text-muted-foreground leading-relaxed">
                  &ldquo;{testimonial.content}&rdquo;
                </p>
                <div className="mt-6 flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
                    {testimonial.avatar}
                  </div>
                  <div>
                    <div className="text-sm font-semibold">{testimonial.name}</div>
                    <div className="text-xs text-muted-foreground">
                      {testimonial.role}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20">
        <div className="container">
          <div className="relative overflow-hidden rounded-3xl bg-primary px-6 py-16 text-center text-primary-foreground shadow-2xl sm:px-16">
            {/* Background decoration */}
            <div className="absolute inset-0 -z-10">
              <div className="absolute -top-20 -right-20 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
              <div className="absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
            </div>

            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Sẵn sàng nâng tầm trung tâm của bạn?
            </h2>
            <p className="mx-auto mt-4 max-w-xl text-lg text-primary-foreground/80">
              Bắt đầu dùng thử miễn phí ngay hôm nay. Không cần thẻ tín dụng,
              không ràng buộc.
            </p>
            <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Link
                href="/register"
                className="inline-flex items-center gap-2 rounded-lg bg-white px-8 py-3.5 text-sm font-semibold text-primary shadow-lg transition-all hover:bg-white/90"
              >
                Đăng ký miễn phí
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                href="/pricing"
                className="inline-flex items-center gap-2 rounded-lg border border-white/30 px-8 py-3.5 text-sm font-semibold text-white transition-colors hover:bg-white/10"
              >
                Xem bảng giá chi tiết
              </Link>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
