/**
 * Problem → Solution section — names a pain the center's audience feels, then
 * shows how the center resolves it. Ported from the marketing-site kit
 * (ProblemSolution) per wave-landing-100 Bucket F.
 *
 * Layout: 3 pain/solution cards (responsive grid; stacks on mobile).
 * Scroll-reveal staggered per card.
 *
 * Slot shape: slots.items = SlotItem[] where
 *   title       = pain headline (e.g. "Mất gốc môn Toán")
 *   description = the problem detail
 *   items[0]    = the fix (one-line solution)
 *
 * Anti-fabrication + audience fit (GAP-1205): this section was ported from the
 * platform marketing site, whose default copy pitches KiteClass TO center
 * owners — wrong audience for a tenant landing (visitors are parents/students)
 * and not the tenant's own content. So it renders ONLY tenant-provided slot
 * data and hides entirely when none is configured — never shows a platform
 * pitch (cf. Teachers/Pricing hide-when-empty, GAP-958).
 */

import { MessageSquareWarning, CheckCircle2 } from 'lucide-react';
import type { SlotData, SlotItem } from '@/lib/template/slots';
import { ScrollReveal } from './ScrollReveal';

interface ProblemSolutionSectionProps {
  slots?: SlotData;
  /** Title override (GAP-1208); defaults to center voice. */
  heading?: string;
  /** Sub-heading override; defaults to center voice. */
  subheading?: string;
}

export function ProblemSolutionSection({ slots, heading, subheading }: ProblemSolutionSectionProps) {
  const items = slots?.items as SlotItem[] | undefined;
  if (!items || items.length === 0) return null;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <div className="mx-auto mb-12 max-w-2xl text-center">
          <span className="mb-3 inline-block text-sm font-semibold uppercase tracking-wide text-theme-cta">
            Vấn đề quen thuộc
          </span>
          <h2 className="text-3xl font-bold md:text-4xl">{heading ?? 'Những trăn trở quen thuộc — và cách chúng tôi đồng hành'}</h2>
          <p className="mt-3 text-muted-foreground">
            {subheading ?? 'Mỗi học viên một xuất phát điểm. Đây là những khó khăn thường gặp và cách chúng tôi giúp các em vượt qua.'}
          </p>
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          {items.map((item, index) => {
            const fix = item.items?.[0];
            return (
              <ScrollReveal key={item.title} delayMs={index * 120}>
                <div className="flex h-full flex-col rounded-xl border bg-card p-6 shadow-theme-sm">
                  <div className="mb-3 inline-flex w-fit items-center gap-2 rounded-full bg-destructive/10 px-3 py-1 text-sm font-medium text-destructive">
                    <MessageSquareWarning className="h-4 w-4" aria-hidden /> {item.title}
                  </div>
                  {item.description && (
                    <p className="mb-4 flex-1 text-sm text-muted-foreground">{item.description}</p>
                  )}
                  {fix && (
                    <div className="mt-auto flex items-start gap-2 rounded-lg bg-theme-primary/5 p-3 text-sm font-medium text-theme-primary">
                      <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-green-500" aria-hidden />
                      <span>{fix}</span>
                    </div>
                  )}
                </div>
              </ScrollReveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}
