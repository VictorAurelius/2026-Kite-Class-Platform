import { GraduationCap, Users, BarChart3, Sparkles } from 'lucide-react';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      {/* Left: Illustration panel (hidden on mobile) */}
      <div className="hidden lg:flex lg:flex-1 bg-gradient-to-br from-primary/10 via-primary/5 to-accent/10 relative overflow-hidden">
        <div className="flex flex-col justify-center p-12 xl:p-16 relative z-10">
          <div className="mb-8">
            <span className="text-3xl font-bold text-primary">KiteClass</span>
            <p className="mt-3 text-lg text-foreground/70 max-w-md leading-relaxed">
              Nền tảng quản lý trung tâm giáo dục thông minh. Tiết kiệm thời gian, tăng hiệu quả vận hành.
            </p>
          </div>

          <div className="space-y-4 max-w-sm">
            {[
              { icon: Users, text: 'Quản lý 500+ trung tâm giáo dục' },
              { icon: GraduationCap, text: '50,000+ học viên đang sử dụng' },
              { icon: BarChart3, text: 'Tiết kiệm 3-5 giờ mỗi ngày' },
              { icon: Sparkles, text: 'AI tạo website tự động trong 5 phút' },
            ].map((item) => (
              <div key={item.text} className="flex items-center gap-3">
                <div className="shrink-0 rounded-lg bg-primary/10 p-2 text-primary">
                  <item.icon className="h-4 w-4" />
                </div>
                <span className="text-sm text-foreground/70">{item.text}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Decorative blobs */}
        <div className="absolute -bottom-20 -right-20 h-64 w-64 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute -top-10 -left-10 h-48 w-48 rounded-full bg-accent/10 blur-3xl" />
      </div>

      {/* Right: Form */}
      <div className="flex flex-1 items-center justify-center p-6 sm:p-8">
        <div className="w-full max-w-md">{children}</div>
      </div>
    </div>
  );
}
