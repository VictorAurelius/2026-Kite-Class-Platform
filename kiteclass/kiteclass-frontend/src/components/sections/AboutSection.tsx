import { GraduationCap } from 'lucide-react';
import type { SlotData } from '@/lib/template/slots';

interface AboutSectionProps {
  slots?: SlotData;
  content?: string;
  /** Title override (GAP-1208) — e.g. personal "Về giáo viên"; defaults to "Giới thiệu". */
  heading?: string;
}

export function AboutSection({ slots, content, heading }: AboutSectionProps) {
  const aboutContent = (slots?.content as string) || content;
  const mission = slots?.mission as string | undefined;
  const vision = slots?.vision as string | undefined;

  return (
    <section className="relative overflow-hidden py-16">
      {/* Decorative subject icon (very low opacity, behind content). */}
      <GraduationCap
        className="pointer-events-none absolute -right-6 top-8 h-40 w-40 text-theme-primary/[0.04]"
        aria-hidden
      />
      <div className="container relative mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-8">{heading ?? 'Giới thiệu'}</h2>
        <div className="max-w-3xl mx-auto space-y-6">
          <p className="text-muted-foreground leading-relaxed">
            {aboutContent ||
              'Chúng tôi là trung tâm giáo dục uy tín với sứ mệnh mang đến chất lượng đào tạo tốt nhất cho học viên.'}
          </p>
          {mission && (
            <div className="rounded-xl bg-theme-primary/5 p-6">
              <h3 className="font-semibold mb-2 text-theme-primary">Sứ mệnh</h3>
              <p className="text-muted-foreground">{mission}</p>
            </div>
          )}
          {vision && (
            <div className="rounded-xl bg-theme-secondary/5 p-6">
              <h3 className="font-semibold mb-2 text-theme-accent">Tầm nhìn</h3>
              <p className="text-muted-foreground">{vision}</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
