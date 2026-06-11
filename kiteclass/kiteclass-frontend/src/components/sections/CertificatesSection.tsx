/**
 * Programs / certificates section — displays the tenant's teaching programs.
 *
 * Anti-fabrication (GAP-958): renders ONLY real tenant-configured programs. When
 * none are configured the section hides entirely — never invents a generic
 * IELTS/TOEIC/Cambridge catalogue the center may not actually teach. page.tsx
 * emits slots.certificates from the backend `programs` array when non-empty.
 *
 * @since 2026-04-04
 */

import { Badge } from '@/components/ui/badge';
import type { SlotData, SlotItem } from '@/lib/template/slots';

interface CertificatesSectionProps {
  slots?: SlotData;
  /** Title override (GAP-1208); defaults to "Chương trình giảng dạy". */
  heading?: string;
  /** Sub-heading override; defaults to center voice. */
  subheading?: string;
}

export function CertificatesSection({ slots, heading, subheading }: CertificatesSectionProps) {
  const certificates = slots?.certificates as SlotItem[] | undefined;
  if (!certificates || certificates.length === 0) return null;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-4">{heading ?? 'Chương trình giảng dạy'}</h2>
        <p className="text-center text-muted-foreground mb-12 max-w-2xl mx-auto">
          {subheading ?? 'Các chương trình và lộ trình học được thiết kế theo từng nhóm học viên'}
        </p>
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {certificates.map((cert) => (
            <div
              key={cert.title}
              className="rounded-xl border bg-card p-6 text-center shadow-md transition-all hover:-translate-y-1 hover:shadow-xl"
            >
              <div className="text-4xl mb-3">{cert.icon}</div>
              <h3 className="font-bold text-xl mb-1">{cert.title}</h3>
              <p className="text-xs text-muted-foreground mb-4">{cert.description}</p>
              {cert.items && (
                <ul className="space-y-1">
                  {cert.items.map((item) => (
                    <li key={item}>
                      <Badge variant="secondary" className="text-xs font-normal w-full justify-center">
                        {item}
                      </Badge>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
