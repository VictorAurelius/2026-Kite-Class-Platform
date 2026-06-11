/**
 * Teachers section — displays teaching staff cards from tenant CMS data.
 *
 * Anti-fabrication (GAP-958): renders ONLY real tenant-provided teachers. When
 * no teacher data is configured the section hides entirely — never invents
 * fictitious instructors / credentials. page.tsx only emits slots.teachers when
 * the backend returns a non-empty array.
 *
 * @since 2026-04-04
 */

import Image from 'next/image';
import { Card, CardContent } from '@/components/ui/card';
import type { SlotData, SlotItem } from '@/lib/template/slots';

interface TeachersSectionProps {
  slots?: SlotData;
  /** Title override (GAP-1208) — e.g. personal "Giáo viên đồng hành"; defaults to center voice. */
  heading?: string;
  /** Sub-heading override; defaults to center voice. */
  subheading?: string;
}

export function TeachersSection({ slots, heading, subheading }: TeachersSectionProps) {
  const teachers = slots?.teachers as SlotItem[] | undefined;
  if (!teachers || teachers.length === 0) return null;

  // Single independent teacher (GAP-1208): render one large centered profile card
  // instead of a 3-column grid that reads as a center's staff roster.
  const isSolo = teachers.length === 1;

  return (
    <section className="py-16 bg-muted/30">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-4">{heading ?? 'Đội ngũ giáo viên'}</h2>
        <p className="text-center text-muted-foreground mb-12 max-w-2xl mx-auto">
          {subheading ?? 'Giáo viên giàu kinh nghiệm, nhiệt tình và tận tâm với học viên'}
        </p>
        <div className={isSolo ? 'mx-auto max-w-xl' : 'grid md:grid-cols-3 gap-8'}>
          {teachers.map((teacher) => (
            <article key={teacher.title}>
              <Card className={`h-full rounded-xl text-center shadow-md transition-shadow hover:shadow-xl${isSolo ? ' p-2' : ''}`}>
                <CardContent className="pt-8 pb-6">
                  <div className={`mx-auto mb-4 flex items-center justify-center rounded-full bg-theme-primary/10 ring-4 ring-theme-primary/20 ${isSolo ? 'h-32 w-32' : 'h-24 w-24'}`}>
                    {teacher.image ? (
                      <Image
                        src={teacher.image}
                        alt={teacher.title}
                        width={96}
                        height={96}
                        unoptimized
                        className="h-24 w-24 rounded-full object-cover ring-2 ring-theme-primary/30"
                      />
                    ) : (
                      <span className="text-2xl font-bold text-theme-primary">
                        {teacher.icon || teacher.title.split(' ').map((w) => w[0]).join('')}
                      </span>
                    )}
                  </div>
                  <h3 className="font-semibold text-lg mb-1">{teacher.title}</h3>
                  <p className="text-sm text-muted-foreground mb-4">{teacher.description}</p>
                  {teacher.items && teacher.items.length > 0 && (
                    <ul className="space-y-1">
                      {teacher.items.map((item) => (
                        <li key={item} className="text-xs text-muted-foreground flex items-center justify-center gap-1">
                          <span className="text-theme-primary">✓</span> {item}
                        </li>
                      ))}
                    </ul>
                  )}
                </CardContent>
              </Card>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
