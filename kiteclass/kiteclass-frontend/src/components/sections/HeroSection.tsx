import Image from 'next/image';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ArrowRight, ShieldCheck, GraduationCap } from 'lucide-react';
import type { SlotData } from '@/lib/template/slots';
import { HeroBannerCarousel } from './HeroBannerCarousel';

interface HeroSectionProps {
  slots?: SlotData;
  title?: string;
  subtitle?: string;
  tagline?: string;
}

/**
 * Two-column promo hero: copy on the LEFT over the tenant gradient, banner
 * asset FRAMED on the RIGHT (rounded card, no scrim over the image).
 *
 * Design parity (GAP-1210, reverts Bucket C full-bleed): the Claude Design
 * source places the banner in a right-side frame — banner assets contain
 * people (faces) and sometimes baked-in text, so a full-bleed background with
 * a text scrim covered faces and dimmed in-image text. The frame keeps the
 * asset fully visible; the HTML copy never overlaps it. Matches the original
 * GAP-810 two-column hero and the marketing-site kit (visual framed right).
 *
 * Falls back to a centred tenant-coloured gradient text hero when no image.
 *
 * All copy (title/subtitle/tagline/urgency/CTA) reads from slot/branding data;
 * fallbacks are generic + honest (no fabricated stats like "500+ học viên").
 */
export function HeroSection({ slots, title, subtitle, tagline }: HeroSectionProps) {
  const heroTitle = (slots?.title as string) || title || 'Trung tâm giáo dục';
  const heroSubtitle = (slots?.subtitle as string) || subtitle;
  const heroTagline = (slots?.tagline as string) || tagline;
  const heroImage = slots?.image as string | undefined;
  const urgency = (slots?.urgency as string) || undefined;

  // Hero banner carousel (GAP-826). `images` is the ordered banner list; fall back to the
  // single `image` slot (legacy heroImageUrl) when the list is empty → backward-compat.
  const rawImages = slots?.images;
  const carouselImages = Array.isArray(rawImages)
    ? (rawImages.filter((x) => typeof x === 'string' && x.length > 0) as string[])
    : [];
  // The frame shows: the carousel when ≥2 slides, else a single static banner (1 slide
  // from the list, or the legacy single `image`). No image at all → text gradient fallback.
  const frameImages = carouselImages.length > 0
    ? carouselImages
    : (heroImage ? [heroImage] : []);
  const hasFrame = frameImages.length > 0;

  // CTA copy is data-driven (slots, Đợt-1 slot C) with generic, non-fabricated
  // fallbacks to the real built-in routes (/register, /catalog).
  const ctaPrimaryLabel = (slots?.ctaPrimaryLabel as string) || 'Học thử miễn phí';
  const ctaPrimaryHref = (slots?.ctaPrimaryHref as string) || '/register';
  const ctaSecondaryLabel = (slots?.ctaSecondaryLabel as string) || 'Xem khóa học';
  const ctaSecondaryHref = (slots?.ctaSecondaryHref as string) || '/catalog';

  // Full-width tenant-coloured gradient (navy → lighter) reads as a promo block.
  const bgStyle = {
    background:
      'linear-gradient(125deg, rgb(var(--theme-secondary)) 0%, color-mix(in srgb, rgb(var(--theme-secondary)) 78%, black) 55%, color-mix(in srgb, rgb(var(--theme-secondary)) 60%, black) 100%)',
  };

  const urgencyBadge = urgency ? (
    <span className="mb-6 inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-1.5 text-sm font-semibold text-white backdrop-blur">
      {urgency}
    </span>
  ) : null;

  // Primary CTA is bold orange (theme-cta) so it pops against the dark scrim,
  // UPPERCASE + large. "Học thử miễn phí" repeated at hero + CTASection.
  const ctaButtons = (
    <div className="flex flex-wrap gap-4">
      <Button
        size="lg"
        asChild
        className="h-12 rounded-xl bg-theme-cta px-8 text-base font-bold uppercase tracking-wide text-white shadow-lg shadow-theme-cta/30 transition hover:bg-theme-cta/90 hover:shadow-xl"
      >
        <Link href={ctaPrimaryHref}>
          {ctaPrimaryLabel} <ArrowRight className="ml-2 h-5 w-5" />
        </Link>
      </Button>
      <Button
        size="lg"
        variant="outline"
        asChild
        className="h-12 rounded-xl border-white/40 bg-white/5 px-8 text-base text-white hover:bg-white/15"
      >
        <Link href={ctaSecondaryHref}>{ctaSecondaryLabel}</Link>
      </Button>
    </div>
  );

  // Trust ribbons — qualitative commitments only (no fabricated headcount/rating).
  const trustRibbons = (
    <ul className="mt-7 flex flex-wrap items-center gap-3 text-sm">
      <li className="inline-flex items-center gap-1.5 rounded-full bg-theme-cta/20 px-3 py-1.5 font-semibold text-white ring-1 ring-theme-cta/40">
        <ShieldCheck className="h-4 w-4" aria-hidden /> Cam kết đầu ra
      </li>
      <li className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1.5 font-medium text-white/90">
        <GraduationCap className="h-4 w-4" aria-hidden /> Học thử miễn phí
      </li>
    </ul>
  );

  if (hasFrame) {
    // Copy left + framed banner right (GAP-1210). The image gets its own frame
    // with NO text/scrim on top of it — faces + any in-image text stay visible.
    // `unoptimized` retained because hero images may be AI-generated data URLs
    // or arbitrary remote URLs not in next.config remotePatterns.
    // GAP-826: ≥2 banners → rotating carousel; exactly 1 → static single banner.
    const single = frameImages.length === 1;
    return (
      <section className="relative w-full overflow-hidden text-white" style={bgStyle} aria-label={heroTitle}>
        <div
          className="pointer-events-none absolute -right-24 -bottom-32 h-[420px] w-[420px] rounded-full opacity-30 blur-3xl"
          style={{ background: 'radial-gradient(circle, rgb(var(--theme-cta)) 0%, transparent 70%)' }}
        />
        <div className="container relative mx-auto px-4 py-16 md:py-24">
          <div className="grid items-center gap-10 md:grid-cols-2">
            {/* Copy column */}
            <div className="flex flex-col items-start">
              <h1 className="sr-only">{heroTitle}</h1>
              {urgencyBadge}
              <p
                aria-hidden
                className="text-balance text-4xl font-extrabold leading-[1.15] tracking-tight md:text-5xl lg:text-[3.25rem]"
              >
                {heroTitle}
              </p>
              {heroSubtitle && (
                <p className="mt-6 max-w-xl text-lg text-white/90 md:text-xl">{heroSubtitle}</p>
              )}
              {heroTagline && (
                <p className="mt-3 text-base font-medium text-white/75 md:text-lg">{heroTagline}</p>
              )}
              <div className="mt-8">{ctaButtons}</div>
              {trustRibbons}
            </div>

            {/* Framed banner column — asset shown whole, never under the copy */}
            <div className="relative">
              <div
                className="pointer-events-none absolute -inset-4 -z-10 rounded-3xl bg-theme-cta/20 blur-2xl"
                aria-hidden
              />
              {single ? (
                <div className="relative aspect-[16/10] w-full overflow-hidden rounded-2xl shadow-2xl ring-1 ring-white/25">
                  <Image
                    src={frameImages[0]!}
                    alt=""
                    fill
                    unoptimized
                    priority
                    sizes="(min-width: 768px) 50vw, 100vw"
                    aria-hidden
                    className="object-cover object-center"
                  />
                </div>
              ) : (
                <HeroBannerCarousel images={frameImages} label={heroTitle} />
              )}
            </div>
          </div>
        </div>
      </section>
    );
  }

  // Fallback: full-width gradient text hero (no AI scene configured).
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
