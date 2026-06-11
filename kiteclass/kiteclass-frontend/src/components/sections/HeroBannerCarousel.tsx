'use client';

import Image from 'next/image';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface HeroBannerCarouselProps {
  /** Ordered banner image URLs (slide order = array order). Must be length ≥ 2. */
  images: string[];
  /** Accessible label for the carousel region (the hero title). */
  label?: string;
  /** Auto-rotate interval in ms (default 5000, per ui_kits/marketing-site/carousel-demo.html). */
  intervalMs?: number;
}

/**
 * Hero banner carousel (GAP-826) — crossfade rotator for ≥2 banner slides inside the
 * HeroSection's right-hand framed card (aspect-[16/10], rounded, no scrim — GAP-1210).
 *
 * Behaviour mirrors ui_kits/marketing-site/carousel-demo.html: auto-rotate every 5s,
 * dots + prev/next arrows, pause on hover/focus. Accessibility: aria-roledescription
 * "carousel" + per-slide aria-label, labelled controls, and prefers-reduced-motion
 * disables auto-rotate (manual nav still works).
 *
 * Client component because rotation + interactivity need browser state; HeroSection
 * (server component) passes the resolved image list as a prop. A single image should be
 * rendered statically by HeroSection itself, NOT through this component.
 */
export function HeroBannerCarousel({ images, label, intervalMs = 5000 }: HeroBannerCarouselProps) {
  const [active, setActive] = useState(0);
  const [paused, setPaused] = useState(false);
  const count = images.length;

  const go = useCallback(
    (next: number) => setActive(((next % count) + count) % count),
    [count],
  );
  const prev = useCallback(() => go(active - 1), [active, go]);
  const next = useCallback(() => go(active + 1), [active, go]);

  // Respect prefers-reduced-motion: skip auto-rotate entirely (user still navigates).
  const reducedMotionRef = useRef(false);
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    reducedMotionRef.current = mq.matches;
    const onChange = (e: MediaQueryListEvent) => {
      reducedMotionRef.current = e.matches;
    };
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  // Auto-rotate (paused on hover/focus or when reduced-motion is requested).
  useEffect(() => {
    if (paused || count < 2 || reducedMotionRef.current) return;
    const id = window.setInterval(() => {
      setActive((cur) => (cur + 1) % count);
    }, intervalMs);
    return () => window.clearInterval(id);
  }, [paused, count, intervalMs]);

  return (
    <div
      className="relative aspect-[16/10] w-full overflow-hidden rounded-2xl shadow-2xl ring-1 ring-white/25"
      role="group"
      aria-roledescription="carousel"
      aria-label={label ? `Banner: ${label}` : 'Banner trung tâm'}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      {images.map((src, i) => (
        <div
          key={`${src}-${i}`}
          className={`absolute inset-0 transition-opacity duration-700 ease-in-out ${
            i === active ? 'opacity-100' : 'opacity-0'
          }`}
          role="group"
          aria-roledescription="slide"
          aria-label={`Ảnh ${i + 1} / ${count}`}
          aria-hidden={i === active ? undefined : true}
        >
          <Image
            src={src}
            alt=""
            fill
            unoptimized
            priority={i === 0}
            sizes="(min-width: 768px) 50vw, 100vw"
            aria-hidden
            className="object-cover object-center"
          />
        </div>
      ))}

      {/* Prev / next arrows */}
      <button
        type="button"
        onClick={prev}
        aria-label="Ảnh trước"
        className="absolute left-3 top-1/2 z-10 -translate-y-1/2 rounded-full bg-black/35 p-2 text-white backdrop-blur transition hover:bg-black/55 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
      >
        <ChevronLeft className="h-5 w-5" aria-hidden />
      </button>
      <button
        type="button"
        onClick={next}
        aria-label="Ảnh tiếp theo"
        className="absolute right-3 top-1/2 z-10 -translate-y-1/2 rounded-full bg-black/35 p-2 text-white backdrop-blur transition hover:bg-black/55 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
      >
        <ChevronRight className="h-5 w-5" aria-hidden />
      </button>

      {/* Dots */}
      <div className="absolute inset-x-0 bottom-3 z-10 flex justify-center gap-2" role="tablist" aria-label="Chọn ảnh banner">
        {images.map((_, i) => (
          <button
            key={i}
            type="button"
            role="tab"
            aria-selected={i === active}
            aria-label={`Tới ảnh ${i + 1}`}
            onClick={() => go(i)}
            className={`h-2.5 rounded-full transition-all focus-visible:outline focus-visible:outline-2 focus-visible:outline-white ${
              i === active ? 'w-6 bg-white' : 'w-2.5 bg-white/50 hover:bg-white/75'
            }`}
          />
        ))}
      </div>
    </div>
  );
}
