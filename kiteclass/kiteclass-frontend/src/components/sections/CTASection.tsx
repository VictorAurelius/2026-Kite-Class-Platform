import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ArrowRight } from 'lucide-react';

export function CTASection() {
  return (
    <section className="py-16 bg-gradient-to-r from-theme-primary/10 to-theme-secondary/10">
      <div className="container mx-auto px-4 text-center">
        <h2 className="text-3xl font-bold mb-4">Sẵn sàng bắt đầu chưa?</h2>
        <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
          Dùng thử miễn phí 14 ngày, không cần thẻ tín dụng. Nâng cấp quản lý trung tâm ngay hôm nay.
        </p>
        <div className="flex gap-4 justify-center">
          <Button size="lg" asChild className="bg-theme-primary hover:bg-theme-primary/90">
            <Link href="/register">
              Đăng ký ngay <ArrowRight className="ml-2 h-5 w-5" />
            </Link>
          </Button>
          <Button size="lg" variant="outline" asChild>
            <Link href="/contact">Liên hệ tư vấn</Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
