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
 * Full-width promo hero.
 *
 * Design (Wave landing-100 Bucket C): the AI scene/portrait is used as an
 * OPTIMIZED BACKGROUND IMAGE (next/image fill + object-cover) with the slogan,
 * subtitle and CTA rendered as REAL HTML OVERLAY on top — NOT baked into the PNG.
 * This keeps Vietnamese text crisp (no compression blur), lets it reflow on
 * mobile, and stays accessible/SEO-friendly. A gradient scrim layer sits between
 * the image and the text for legible contrast on any AI scene.
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

  if (heroImage) {
    // AI scene as optimized background; slogan + CTA as real HTML overlay.
    // `unoptimized` retained because hero images may be AI-generated data URLs
    // or arbitrary remote URLs not in next.config remotePatterns; `fill` +
    // object-cover still gives responsive cover behaviour.
    return (
      <section className="relative w-full overflow-hidden text-white" aria-label={heroTitle}>
        {/* Background AI scene */}
        <div className="absolute inset-0">
          <Image
            src={heroImage}
            alt=""
            fill
            unoptimized
            priority
            sizes="100vw"
            aria-hidden
            className="object-cover object-center"
          />
          {/* Gradient scrim for text contrast: darker on the left where copy sits,
              fading right so the AI scene stays visible. */}
          <div
            className="absolute inset-0"
            style={{
              background:
                'linear-gradient(100deg, color-mix(in srgb, rgb(var(--theme-secondary)) 70%, black) 0%, color-mix(in srgb, rgb(var(--theme-secondary)) 55%, black) 38%, rgba(0,0,0,0.45) 65%, rgba(0,0,0,0.15) 100%)',
            }}
          />
          {/* Extra bottom-up scrim for mobile (text stacks over whole image) */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/10 to-transparent md:hidden" />
        </div>

        {/* HTML overlay content */}
        <div className="container relative mx-auto px-4 py-20 md:py-28">
          <div className="flex max-w-2xl flex-col items-start">
            <h1 className="sr-only">{heroTitle}</h1>
            {urgencyBadge}
            <p
              aria-hidden
              className="text-4xl font-extrabold leading-[1.1] tracking-tight drop-shadow-sm md:text-6xl"
            >
              {heroTitle}
            </p>
            {heroSubtitle && (
              <p className="mt-6 max-w-xl text-lg text-white/90 drop-shadow-sm md:text-xl">
                {heroSubtitle}
              </p>
            )}
            {heroTagline && (
              <p className="mt-3 text-base font-medium text-white/75 md:text-lg">{heroTagline}</p>
            )}
            <div className="mt-8">{ctaButtons}</div>
            {trustRibbons}
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
