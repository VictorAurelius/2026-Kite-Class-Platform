import { Clock, Shield, Palette, Smartphone, Zap, CreditCard } from 'lucide-react';
import { KiteLogo } from '@/components/brand/KiteLogo';
import { SkipToContent } from '@/components/a11y/SkipToContent';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      {/* Skip to main content (accessibility — WCAG 2.4.1) */}
      <SkipToContent />
      {/* Left: Feature highlights (hidden on mobile) */}
      <div className="hidden lg:flex lg:flex-1 bg-gradient-to-br from-primary/10 via-primary/5 to-accent/10 relative overflow-hidden">
        <div className="flex flex-col justify-center p-12 xl:p-16 relative z-10">
          <div className="mb-10">
            <KiteLogo size="lg" />
            <p className="mt-1 text-sm italic text-muted-foreground tracking-wide">Nền tảng quản lý giáo dục thông minh</p>

            <div className="mt-8 rounded-2xl bg-white/60 dark:bg-white/5 backdrop-blur-sm border border-primary/10 p-6 max-w-md">
              <p className="text-lg font-semibold text-foreground/90 leading-relaxed">
                Giảng dạy là đam mê —
              </p>
              <p className="text-lg text-primary font-bold">
                quản lý để KiteClass lo.
              </p>
            </div>
          </div>

          <div className="space-y-5 max-w-sm">
            {[
              { icon: Zap, text: 'Thiết lập trong 30 giây, dùng ngay không cần cài đặt' },
              { icon: Clock, text: 'Điểm danh, lịch học, nhắc nhở tự động' },
              { icon: CreditCard, text: 'Quản lý học phí, hóa đơn, QR thanh toán' },
              { icon: Palette, text: 'AI tạo website chuyên nghiệp cho trung tâm' },
              { icon: Smartphone, text: 'Dùng được trên mọi thiết bị, không cần cài app' },
              { icon: Shield, text: 'Dữ liệu mã hóa, sao lưu tự động mỗi ngày' },
            ].map((item) => (
              <div key={item.text} className="flex items-start gap-3">
                <div className="shrink-0 rounded-lg bg-primary/10 p-2 text-primary mt-0.5">
                  <item.icon className="h-4 w-4" />
                </div>
                <span className="text-sm text-foreground/70 leading-relaxed">{item.text}</span>
              </div>
            ))}
          </div>

          <p className="mt-10 text-xs text-muted-foreground">
            Dùng thử miễn phí 14 ngày • Không cần thẻ tín dụng
          </p>
        </div>

        {/* Decorative blobs */}
        <div className="absolute -bottom-20 -right-20 h-64 w-64 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute -top-10 -left-10 h-48 w-48 rounded-full bg-accent/10 blur-3xl" />
      </div>

      {/* Right: Form */}
      <main id="main-content" role="main" className="flex flex-1 items-center justify-center p-6 sm:p-8">
        <div className="w-full max-w-md">{children}</div>
      </main>
    </div>
  );
}
