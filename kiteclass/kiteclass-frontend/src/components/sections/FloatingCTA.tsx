'use client';

/**
 * Floating CTA — a sticky bottom-right action cluster that appears after the
 * visitor scrolls past the hero. Ported from the marketing-site kit per
 * wave-landing-100 Bucket F.
 *
 * Always shows the primary "Đăng ký học thử" action (a valid link for every
 * tenant). The Zalo deep-link and click-to-call (tel:) buttons render ONLY when
 * the tenant has configured zaloUrl / contactPhone — otherwise they hide (per
 * Bucket F constraint "ẩn nếu chưa cấu hình"). Honours prefers-reduced-motion.
 *
 * Props come from the landing payload (TemplateRenderer passes data).
 */

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Sparkles, MessageCircle, Phone } from 'lucide-react';
import { cn } from '@/lib/utils';

interface FloatingCTAProps {
  /** Tenant contact phone (E.164 or VN local). Hidden when absent. */
  phone?: string;
  /** Zalo ID or full zalo.me URL. Hidden when absent. */
  zaloUrl?: string;
  /** Primary CTA href — defaults to the trial registration route. */
  registerHref?: string;
}

/** Normalise a Zalo ID/URL into a clickable https://zalo.me link. */
function toZaloHref(raw: string): string {
  const v = raw.trim();
  if (/^https?:\/\//i.test(v)) return v;
  // Bare id / phone → zalo.me deep link (strip spaces).
  return `https://zalo.me/${v.replace(/\s+/g, '')}`;
}

export function FloatingCTA({ phone, zaloUrl, registerHref = '/register' }: FloatingCTAProps) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > 600);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const telHref = phone ? `tel:${phone.replace(/[^\d+]/g, '')}` : undefined;
  const zaloHref = zaloUrl ? toZaloHref(zaloUrl) : undefined;

  return (
    <div
      className={cn(
        'fixed bottom-5 right-5 z-50 flex flex-col items-end gap-3 transition-all duration-300 motion-reduce:transition-none',
        visible ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-4 opacity-0',
      )}
    >
      {zaloHref && (
        <a
          href={zaloHref}
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Nhắn tin qua Zalo"
          className="flex h-12 w-12 items-center justify-center rounded-full bg-[#0068FF] text-white shadow-lg transition hover:scale-105 hover:shadow-xl"
        >
          <MessageCircle className="h-6 w-6" />
        </a>
      )}
      {telHref && (
        <a
          href={telHref}
          aria-label="Gọi điện tư vấn"
          className="flex h-12 w-12 items-center justify-center rounded-full bg-green-600 text-white shadow-lg transition hover:scale-105 hover:shadow-xl"
        >
          <Phone className="h-6 w-6" />
        </a>
      )}
      <Link
        href={registerHref}
        className="flex h-12 items-center gap-2 rounded-full bg-theme-cta px-5 text-sm font-bold uppercase tracking-wide text-white shadow-lg shadow-theme-cta/30 transition hover:scale-105 hover:bg-theme-cta/90 hover:shadow-xl"
      >
        <Sparkles className="h-5 w-5" aria-hidden /> Đăng ký học thử
      </Link>
    </div>
  );
}
