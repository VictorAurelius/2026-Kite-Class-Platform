/**
 * How-it-works section — the "how to get started / how we teach" path. Ported
 * from the marketing-site kit (HowItWorks) per wave-landing-100 Bucket F.
 *
 * Layout: numbered step grid (stacks on mobile), scroll-reveal staggered.
 *
 * Slot shape: slots.steps = SlotItem[] where
 *   title       = step name (e.g. "Đăng ký học thử")
 *   description = step detail
 *
 * Anti-fabrication + audience fit (GAP-1205): the ported default copy described
 * a center-OWNER onboarding ("Tạo lớp & nhập học viên...") — wrong audience for
 * a tenant landing (visitors are parents/students) and not the tenant's own
 * content. So it renders ONLY tenant-provided slot data and hides entirely when
 * none is configured — never shows a platform pitch (cf. GAP-958).
 */

import type { SlotData, SlotItem } from '@/lib/template/slots';
import { ScrollReveal } from './ScrollReveal';

interface HowItWorksSectionProps {
  slots?: SlotData;
}

export function HowItWorksSection({ slots }: HowItWorksSectionProps) {
  const steps = slots?.steps as SlotItem[] | undefined;
  if (!steps || steps.length === 0) return null;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <div className="mx-auto mb-12 max-w-2xl text-center">
          <span className="mb-3 inline-block text-sm font-semibold uppercase tracking-wide text-theme-primary">
            Cách hoạt động
          </span>
          <h2 className="text-3xl font-bold md:text-4xl">Bắt đầu học thật dễ dàng</h2>
          <p className="mt-3 text-muted-foreground">
            Chỉ vài bước đơn giản để các em sẵn sàng cho buổi học đầu tiên.
          </p>
        </div>
        <ol className="mx-auto grid max-w-5xl gap-6 md:grid-cols-3">
          {steps.map((step, index) => (
            <li key={step.title}>
              <ScrollReveal delayMs={index * 120}>
                <div className="flex h-full flex-col rounded-xl border bg-card p-6 shadow-theme-sm">
                  <span className="mb-4 flex h-11 w-11 items-center justify-center rounded-full bg-theme-primary text-lg font-bold text-white">
                    {index + 1}
                  </span>
                  <h3 className="mb-2 font-semibold">{step.title}</h3>
                  <p className="text-sm text-muted-foreground">{step.description}</p>
                </div>
              </ScrollReveal>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
