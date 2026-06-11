/**
 * Trust strip — a row of trust signals (security/compliance, support promise,
 * VN-built). Ported from the marketing-site kit (TrustSection) per
 * wave-landing-100 Bucket F.
 *
 * IMPORTANT (per Bucket F constraint): this strip shows factual trust signals
 * the tenant configured, NOT fabricated "đối tác"/customer logos. Custom signals
 * come from slots.signals; partner/customer logos are NEVER hardcoded.
 *
 * Anti-fabrication + audience fit (GAP-1205): the ported default signals were
 * platform value-props ("Xây dựng cho trung tâm Việt Nam" etc.) — KiteClass's
 * own pitch, not the tenant's. So this strip renders ONLY tenant-provided
 * signals and hides entirely when none is configured (cf. GAP-958).
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

interface TrustStripSectionProps {
  slots?: SlotData;
}

export function TrustStripSection({ slots }: TrustStripSectionProps) {
  const signals = slots?.signals as SlotItem[] | undefined;
  // Render only tenant-configured signals; hide when none (no platform default).
  if (!signals || signals.length === 0) return null;

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
