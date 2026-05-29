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

/**
 * Banner-style hero. When a hero image (slots.image) is present, renders a
 * 2-column promo banner (slogan + CTA left, framed portrait right) on a
 * tenant-coloured dark gradient. Text stays real HTML (crisp Vietnamese,
 * accessible, responsive) — the baked PNG variant is reserved for OG/social.
 * Falls back to a centred text hero when no image is configured.
 */
export function HeroSection({ slots, title, subtitle, tagline }: HeroSectionProps) {
  const heroTitle = (slots?.title as string) || title || 'Trung tâm giáo dục';
  const heroSubtitle = (slots?.subtitle as string) || subtitle;
  const heroTagline = (slots?.tagline as string) || tagline;
  const heroImage = slots?.image as string | undefined;

  // Dark, tenant-coloured gradient (navy for Sky) so the banner reads as a promo block.
  const bgStyle = {
    background:
      'linear-gradient(125deg, rgb(var(--theme-secondary)) 0%, color-mix(in srgb, rgb(var(--theme-secondary)) 80%, black) 100%)',
  };

  const ctaButtons = (
    <div className="flex flex-wrap gap-4">
      <Button size="lg" asChild className="bg-theme-primary hover:bg-theme-primary/90 text-white">
        <Link href="/register">
          Đăng ký học thử <ArrowRight className="ml-2 h-5 w-5" />
        </Link>
      </Button>
      <Button size="lg" variant="outline" asChild className="border-white/40 text-white hover:bg-white/10">
        <Link href="/catalog">Xem khóa học</Link>
      </Button>
    </div>
  );

  if (heroImage) {
    return (
      <section className="relative overflow-hidden text-white" style={bgStyle}>
        {/* accent glow */}
        <div
          className="pointer-events-none absolute -right-24 -bottom-32 h-[420px] w-[420px] rounded-full opacity-40 blur-2xl"
          style={{ background: 'radial-gradient(circle, rgb(var(--theme-accent)) 0%, transparent 70%)' }}
        />
        <div className="container mx-auto grid items-center gap-10 px-4 py-16 md:grid-cols-2 md:py-24">
          <div>
            <h1 className="text-4xl font-extrabold leading-tight md:text-5xl">{heroTitle}</h1>
            {heroSubtitle && <p className="mt-5 max-w-xl text-lg text-white/80">{heroSubtitle}</p>}
            {heroTagline && <p className="mt-3 text-base font-medium text-white/70">{heroTagline}</p>}
            <div className="mt-8">{ctaButtons}</div>
          </div>
          <div className="flex justify-center md:justify-end">
            <div className="relative">
              <div
                className="absolute -inset-2 rounded-full opacity-90 blur-[2px]"
                style={{ background: 'conic-gradient(from 200deg, rgb(var(--theme-accent)), rgb(var(--theme-primary)), rgb(var(--theme-accent)))' }}
              />
              <div className="relative h-64 w-64 overflow-hidden rounded-full border-[6px] border-white/90 shadow-2xl md:h-80 md:w-80">
                <Image src={heroImage} alt={heroTitle} fill unoptimized sizes="(max-width: 768px) 256px, 320px" className="object-cover object-top" />
              </div>
            </div>
          </div>
        </div>
      </section>
    );
  }

  // Fallback: centred text hero (no image configured).
  return (
    <section className="bg-gradient-to-b from-theme-primary/5 to-background py-20">
      <div className="container mx-auto px-4 text-center">
        <h1 className="mb-6 text-4xl font-bold md:text-6xl">{heroTitle}</h1>
        {heroSubtitle && (
          <p className="mx-auto mb-8 max-w-2xl text-xl text-muted-foreground">{heroSubtitle}</p>
        )}
        {heroTagline && <p className="mb-8 text-lg font-semibold text-muted-foreground">{heroTagline}</p>}
        <div className="flex justify-center">{ctaButtons}</div>
      </div>
    </section>
  );
}
