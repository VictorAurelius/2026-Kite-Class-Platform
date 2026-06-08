'use client';

/**
 * Stats counters section — social proof for independent teachers / centers.
 * Shows headline numbers with a count-up animation triggered when the section
 * scrolls into view (IntersectionObserver, runs once). Honours
 * prefers-reduced-motion (jumps straight to value).
 *
 * Anti-fabrication (GAP-958): renders ONLY real tenant-provided stats — never
 * invents "500+ học viên" / "95% đạt mục tiêu" headline numbers. When no stats
 * are configured the section hides entirely. page.tsx emits slots.stats from the
 * backend `stats` array when non-empty.
 *
 * Slot shape: slots.stats = SlotItem[] where
 *   title       = the number (e.g. "8" / "4.9")
 *   icon        = optional suffix marker (e.g. "+", "%", "/5")
 *   description = the label (e.g. "Năm kinh nghiệm")
 */

import { useEffect, useRef, useState } from 'react';
import type { SlotData, SlotItem } from '@/lib/template/slots';

interface StatsSectionProps {
  slots?: SlotData;
}

/** Count-up number that animates from 0 → target once `active` flips true. */
function CountUp({ value, active }: { value: string; active: boolean }) {
  const target = parseFloat(value);
  const isNumeric = Number.isFinite(target);
  // Preserve original formatting (decimals) by deriving precision from source.
  const decimals = value.includes('.') ? (value.split('.')[1]?.length ?? 0) : 0;
  const [display, setDisplay] = useState(isNumeric ? '0' : value);

  useEffect(() => {
    if (!active || !isNumeric) return;

    const prefersReduced =
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (prefersReduced) {
      setDisplay(target.toFixed(decimals));
      return;
    }

    const duration = 1200;
    const start = performance.now();
    let raf = 0;
    const tick = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      // easeOutCubic for a natural deceleration.
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplay((target * eased).toFixed(decimals));
      if (progress < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [active, isNumeric, target, decimals]);

  return <>{isNumeric ? display : value}</>;
}

export function StatsSection({ slots }: StatsSectionProps) {
  const stats = (slots?.stats as SlotItem[] | undefined) ?? [];
  const sectionRef = useRef<HTMLElement>(null);
  const [active, setActive] = useState(false);

  useEffect(() => {
    const el = sectionRef.current;
    if (!el || active || stats.length === 0) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          setActive(true);
          observer.disconnect();
        }
      },
      { threshold: 0.3 },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [active, stats.length]);

  // Anti-fabrication: hide when no real stats configured (guard AFTER hooks to
  // preserve hook order per rules-of-hooks).
  if (stats.length === 0) return null;

  return (
    <section ref={sectionRef} className="bg-theme-primary/5 py-16">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-2 gap-6 md:grid-cols-4">
          {stats.map((stat) => (
            <div key={stat.description} className="text-center">
              <p className="text-5xl font-extrabold tracking-tight text-theme-primary md:text-6xl">
                <CountUp value={stat.title as string} active={active} />
                {stat.icon && <span className="text-3xl md:text-4xl">{stat.icon}</span>}
              </p>
              <p className="mt-2 text-sm font-medium text-muted-foreground md:text-base">
                {stat.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
