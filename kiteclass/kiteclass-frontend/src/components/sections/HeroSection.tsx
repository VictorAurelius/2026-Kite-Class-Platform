import Image from 'next/image';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ArrowRight, ShieldCheck, GraduationCap } from 'lucide-react';
import type { SlotData } from '@/lib/template/slots';

interface HeroSectionProps {
  slots?: SlotData;
  title?: string;
  subtitle?: string;
  tagline?: string;
}

/**
 * Full-width promo hero. When a hero image (slots.image) is present, renders a
 * tenant-coloured dark gradient banner: oversized slogan + bold orange CTA on
 * the left, framed portrait on the right (aspect 1.9:1 baked banner). Text stays
 * real HTML (crisp Vietnamese, accessible, responsive) — the PNG banner is the
 * portrait only. Falls back to a centred gradient text hero when no image.
 */
export function HeroSection({ slots, title, subtitle, tagline }: HeroSectionProps) {
  const heroTitle = (slots?.title as string) || title || 'Trung tâm giáo dục';
  const heroSubtitle = (slots?.subtitle as string) || subtitle;
  const heroTagline = (slots?.tagline as string) || tagline;
  const heroImage = slots?.image as string | undefined;
  const urgency =
    (slots?.urgency as string) ||
    '🔥 Khai giảng khóa mới — ưu đãi học phí cho 10 học viên đăng ký sớm';

  // Full-width tenant-coloured gradient (navy → lighter) reads as a promo block.
  const bgStyle = {
    background:
      'linear-gradient(125deg, rgb(var(--theme-secondary)) 0%, color-mix(in srgb, rgb(var(--theme-secondary)) 78%, black) 55%, color-mix(in srgb, rgb(var(--theme-secondary)) 60%, black) 100%)',
  };

  const urgencyBadge = (
    <span className="mb-6 inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-1.5 text-sm font-semibold backdrop-blur">
      {urgency}
    </span>
  );

  // Primary CTA is bold orange (theme-cta) so it pops against the navy gradient,
  // UPPERCASE + large. "Học thử miễn phí" repeated at hero + CTASection.
  const ctaButtons = (
    <div className="flex flex-wrap gap-4">
      <Button
        size="lg"
        asChild
        className="h-12 rounded-xl bg-theme-cta px-8 text-base font-bold uppercase tracking-wide text-white shadow-lg shadow-theme-cta/30 transition hover:bg-theme-cta/90 hover:shadow-xl"
      >
        <Link href="/register">
          Học thử miễn phí <ArrowRight className="ml-2 h-5 w-5" />
        </Link>
      </Button>
      <Button
        size="lg"
        variant="outline"
        asChild
        className="h-12 rounded-xl border-white/40 px-8 text-base text-white hover:bg-white/10"
      >
        <Link href="/catalog">Xem khóa học</Link>
      </Button>
    </div>
  );

  // Trust ribbons — qualitative commitments (independent teacher: no inflated
  // headcount) + real social proof (rating, students taught).
  const trustRibbons = (
    <ul className="mt-7 flex flex-wrap items-center gap-3 text-sm">
      <li className="inline-flex items-center gap-1.5 rounded-full bg-theme-cta/20 px-3 py-1.5 font-semibold text-white ring-1 ring-theme-cta/40">
        <ShieldCheck className="h-4 w-4" aria-hidden /> Cam kết đầu ra
      </li>
      <li className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1.5 font-medium text-white/90">
        <GraduationCap className="h-4 w-4" aria-hidden /> Học thử miễn phí
      </li>
      <li className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1.5 font-medium text-white/90">
        <span className="text-amber-300" aria-hidden>★ 4.9/5</span> từ phụ huynh
      </li>
    </ul>
  );

  if (heroImage) {
    // Composed banner đã bake text + CTA + icon chủ đề → dùng làm HERO full-width
    // (giống mẫu mshoa). KHÔNG render text HTML trùng. h1 sr-only cho SEO/a11y;
    // toàn banner clickable → /register (CTA "Học thử miễn phí" baked báo hiệu).
    // Trust ribbon HTML thật đặt ngay dưới (clickable, responsive, text sắc nét).
    return (
      <section className="relative w-full" aria-label={heroTitle}>
        <h1 className="sr-only">{heroTitle}</h1>
        <Link
          href="/register"
          aria-label={`${heroTitle} — Đăng ký học thử miễn phí`}
          className="group relative block w-full overflow-hidden focus:outline-none focus-visible:ring-4 focus-visible:ring-theme-cta"
        >
          <Image
            src={heroImage}
            alt={heroTitle}
            width={2400}
            height={1260}
            unoptimized
            priority
            sizes="100vw"
            className="block h-auto w-full transition-transform duration-300 group-hover:scale-[1.01]"
          />
        </Link>
        {/* Trust ribbon HTML thật dưới banner — CTA phụ click được + cam kết */}
        <div className="border-b bg-theme-secondary/5">
          <div className="container mx-auto flex flex-wrap items-center justify-center gap-3 px-4 py-3 text-sm">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-theme-cta/15 px-3 py-1.5 font-semibold text-theme-cta">
              <ShieldCheck className="h-4 w-4" aria-hidden /> Cam kết đầu ra
            </span>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-muted px-3 py-1.5 font-medium text-muted-foreground">
              <GraduationCap className="h-4 w-4" aria-hidden /> Học thử miễn phí
            </span>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-muted px-3 py-1.5 font-medium text-muted-foreground">
              <span className="text-amber-500" aria-hidden>★ 4.9/5</span> từ phụ huynh
            </span>
            <Button
              size="sm"
              asChild
              className="ml-auto h-9 rounded-lg bg-theme-cta px-5 font-bold uppercase tracking-wide text-white shadow hover:bg-theme-cta/90"
            >
              <Link href="/register">Đăng ký học thử</Link>
            </Button>
          </div>
        </div>
      </section>
    );
  }

  // Fallback: full-width gradient text hero (no portrait configured).
  return (
    <section className="relative overflow-hidden text-white" style={bgStyle}>
      <div
        className="pointer-events-none absolute -right-24 -bottom-32 h-[420px] w-[420px] rounded-full opacity-40 blur-3xl"
        style={{ background: 'radial-gradient(circle, rgb(var(--theme-cta)) 0%, transparent 70%)' }}
      />
      <div className="container relative mx-auto px-4 py-20 text-center md:py-28">
        <div className="mx-auto flex max-w-3xl flex-col items-center">
          {urgencyBadge}
          <h1 className="text-4xl font-extrabold leading-[1.1] tracking-tight md:text-6xl">
            {heroTitle}
          </h1>
          {heroSubtitle && (
            <p className="mx-auto mt-6 max-w-2xl text-xl text-white/85">{heroSubtitle}</p>
          )}
          {heroTagline && (
            <p className="mt-3 text-lg font-medium text-white/70">{heroTagline}</p>
          )}
          <div className="mt-8 flex justify-center">{ctaButtons}</div>
          <div className="flex justify-center">{trustRibbons}</div>
        </div>
      </div>
    </section>
  );
}
