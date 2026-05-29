import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ArrowRight, ShieldCheck } from 'lucide-react';

/**
 * Closing CTA — repeats the primary "Học thử miễn phí" action (also in hero)
 * with the bold orange theme-cta button + a commitment ribbon. Sits on a
 * tenant-coloured tint so it visually closes the page.
 */
export function CTASection() {
  return (
    <section className="bg-gradient-to-r from-theme-primary/10 to-theme-secondary/10 py-16">
      <div className="container mx-auto px-4 text-center">
        <span className="mb-5 inline-flex items-center gap-2 rounded-full bg-theme-cta/15 px-4 py-1.5 text-sm font-semibold text-theme-cta ring-1 ring-theme-cta/30">
          <ShieldCheck className="h-4 w-4" aria-hidden /> Cam kết kết quả — học thử trước khi quyết định
        </span>
        <h2 className="mb-4 text-3xl font-bold md:text-4xl">Sẵn sàng bắt đầu chưa?</h2>
        <p className="mx-auto mb-8 max-w-2xl text-xl text-muted-foreground">
          Đăng ký học thử miễn phí — không cần thanh toán trước. Trải nghiệm lộ trình cá nhân hóa cùng giáo viên.
        </p>
        <div className="flex flex-wrap justify-center gap-4">
          <Button
            size="lg"
            asChild
            className="h-12 rounded-xl bg-theme-cta px-8 text-base font-bold uppercase tracking-wide text-white shadow-lg shadow-theme-cta/30 transition hover:bg-theme-cta/90 hover:shadow-xl"
          >
            <Link href="/register">
              Học thử miễn phí <ArrowRight className="ml-2 h-5 w-5" />
            </Link>
          </Button>
          <Button size="lg" variant="outline" asChild className="h-12 rounded-xl px-8 text-base">
            <Link href="/contact">Liên hệ tư vấn</Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
