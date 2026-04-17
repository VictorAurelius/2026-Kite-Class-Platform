import Image from 'next/image';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ArrowRight } from 'lucide-react';
import type { SlotData } from '@/lib/template/slots';

interface HeroSectionProps {
  slots?: SlotData;
  title?: string;
  subtitle?: string;
  tagline?: string;
}

export function HeroSection({ slots, title, subtitle, tagline }: HeroSectionProps) {
  const heroTitle = (slots?.title as string) || title;
  const heroSubtitle = (slots?.subtitle as string) || subtitle;
  const heroTagline = (slots?.tagline as string) || tagline;
  const heroImage = slots?.image as string | undefined;

  return (
    <section className="py-20 bg-gradient-to-b from-theme-primary/5 to-background">
      <div className="container mx-auto px-4 text-center">
        {heroImage && (
          <Image src={heroImage} alt="" width={640} height={256} unoptimized className="mx-auto mb-8 max-h-64 rounded-2xl shadow-lg object-cover" />
        )}
        <h1 className="text-4xl md:text-6xl font-bold mb-6">
          {heroTitle || 'Trung tâm giáo dục'}
          <br />
          <span className="text-theme-primary">Chuyên nghiệp & Hiệu quả</span>
        </h1>
        <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
          {heroSubtitle || 'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm.'}
        </p>
        {heroTagline && (
          <p className="text-lg font-semibold mb-8 text-muted-foreground">{heroTagline}</p>
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
