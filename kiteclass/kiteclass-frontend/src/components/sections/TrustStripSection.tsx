/**
 * Trust strip — a row of trust signals (security/compliance, support promise,
 * VN-built). Ported from the marketing-site kit (TrustSection) per
 * wave-landing-100 Bucket F.
 *
 * IMPORTANT (per Bucket F constraint): this strip shows factual platform
 * value-props, NOT fabricated "đối tác"/customer logos. Custom signals come
 * from slots.signals; partner/customer logos are NEVER hardcoded. If a tenant
 * explicitly clears all signals (empty array), the section hides itself.
 *
 * Slot shape: slots.signals = SlotItem[] where
 *   title       = signal label (e.g. "Tuân thủ Nghị định 13/2023/NĐ-CP")
 *   description = optional sub-line
 *   icon        = optional icon key ('shield' | 'lock' | 'support' | 'vn' | 'spark')
 */

import { ShieldCheck, Lock, Headphones, Sparkles, MapPin } from 'lucide-react';
import type { SlotData, SlotItem } from '@/lib/template/slots';
import { ScrollReveal } from './ScrollReveal';

const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  shield: ShieldCheck,
  lock: Lock,
  support: Headphones,
  vn: MapPin,
  spark: Sparkles,
};

// Factual platform claims (true for the product) — not fabricated partner data.
const DEFAULT_SIGNALS: SlotItem[] = [
  { icon: 'lock', title: 'Tuân thủ Nghị định 13/2023/NĐ-CP', description: 'Bảo vệ dữ liệu cá nhân' },
  { icon: 'vn', title: 'Xây dựng cho trung tâm Việt Nam', description: 'Bám sát cách vận hành thực tế' },
  { icon: 'support', title: 'Hỗ trợ tiếng Việt', description: 'Điện thoại, email & Zalo' },
];

interface TrustStripSectionProps {
  slots?: SlotData;
}

export function TrustStripSection({ slots }: TrustStripSectionProps) {
  const configured = slots?.signals as SlotItem[] | undefined;
  // Tenant explicitly cleared signals → hide (never fall back to demo in that case).
  if (configured && configured.length === 0) return null;
  const signals = configured && configured.length > 0 ? configured : DEFAULT_SIGNALS;

  return (
    <section className="py-12">
      <div className="container mx-auto px-4">
        <ScrollReveal>
          <div className="mx-auto flex max-w-5xl flex-wrap items-stretch justify-center gap-4">
            {signals.map((signal) => {
              const Icon = ICON_MAP[signal.icon || 'shield'] || ShieldCheck;
              return (
                <div
                  key={signal.title}
                  className="flex flex-1 basis-64 items-center gap-3 rounded-xl border bg-card px-5 py-4 shadow-theme-sm"
                >
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-theme-primary/10 text-theme-primary">
                    <Icon className="h-5 w-5" />
                  </span>
                  <div>
                    <p className="text-sm font-semibold leading-tight">{signal.title}</p>
                    {signal.description && (
                      <p className="text-xs text-muted-foreground">{signal.description}</p>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </ScrollReveal>
      </div>
    </section>
  );
}
