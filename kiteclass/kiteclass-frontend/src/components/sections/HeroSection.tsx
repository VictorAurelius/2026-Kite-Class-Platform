import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ArrowRight } from 'lucide-react';

interface HeroSectionProps {
  title?: string;
  subtitle?: string;
  tagline?: string;
}

export function HeroSection({ title, subtitle, tagline }: HeroSectionProps) {
  return (
    <section className="py-20 bg-gradient-to-b from-theme-primary/5 to-background">
      <div className="container mx-auto px-4 text-center">
        <h1 className="text-4xl md:text-6xl font-bold mb-6">
          {title || 'Trung tâm giáo dục'}
          <br />
          <span className="text-theme-primary">Chuyên nghiệp & Hiệu quả</span>
        </h1>
        <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
          {subtitle || 'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm.'}
        </p>
        {tagline && (
          <p className="text-lg font-semibold mb-8 text-muted-foreground">{tagline}</p>
        )}
        <div className="flex gap-4 justify-center">
          <Button size="lg" asChild className="bg-theme-primary hover:bg-theme-primary/90">
            <Link href="/register">
              Dùng thử miễn phí <ArrowRight className="ml-2 h-5 w-5" />
            </Link>
          </Button>
          <Button size="lg" variant="outline" asChild>
            <Link href="/catalog">Xem khóa học</Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
