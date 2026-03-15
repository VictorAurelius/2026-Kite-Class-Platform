import Link from 'next/link';

export default function HomePage() {
  return (
    <div className="container py-20">
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
            className="rounded-md bg-primary px-6 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            Dùng thử miễn phí 14 ngày
          </Link>
          <Link
            href="/pricing"
            className="rounded-md border px-6 py-3 text-sm font-medium hover:bg-muted"
          >
            Xem bảng giá
          </Link>
        </div>
      </div>
    </div>
  );
}
