/**
 * Teachers section — displays teaching staff cards.
 * Uses default demo data until CMS slot data is available.
 *
 * @since 2026-04-04
 */

import Image from 'next/image';
import { Card, CardContent } from '@/components/ui/card';
import type { SlotData, SlotItem } from '@/lib/template/slots';

const DEFAULT_TEACHERS: SlotItem[] = [
  {
    title: 'Nguyễn Thị Lan',
    description: 'Tiếng Anh Giao Tiếp & IELTS',
    icon: 'NL',
    items: ['10 năm kinh nghiệm', 'IELTS 8.0', 'Cử nhân ĐH Ngoại ngữ HN'],
  },
  {
    title: 'Trần Minh Đức',
    description: 'TOEIC & Tiếng Anh Thương Mại',
    icon: 'TD',
    items: ['8 năm kinh nghiệm', 'TOEIC 990', 'Thạc sĩ ĐH Hà Nội'],
  },
  {
    title: 'Phạm Thu Hà',
    description: 'Tiếng Anh Trẻ Em & Cambridge',
    icon: 'PH',
    items: ['6 năm kinh nghiệm', 'Cambridge C2', 'Chứng chỉ TESOL'],
  },
];

interface TeachersSectionProps {
  slots?: SlotData;
}

export function TeachersSection({ slots }: TeachersSectionProps) {
  const teachers = (slots?.teachers as SlotItem[] | undefined) || DEFAULT_TEACHERS;

  return (
    <section className="py-16 bg-muted/30">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-4">Đội ngũ giáo viên</h2>
        <p className="text-center text-muted-foreground mb-12 max-w-2xl mx-auto">
          Giáo viên giàu kinh nghiệm, nhiệt tình và tận tâm với học viên
        </p>
        <div className="grid md:grid-cols-3 gap-8">
          {teachers.map((teacher) => (
            <article key={teacher.title}>
              <Card className="h-full rounded-xl text-center shadow-md transition-shadow hover:shadow-xl">
                <CardContent className="pt-8 pb-6">
                  <div className="mx-auto mb-4 flex h-24 w-24 items-center justify-center rounded-full bg-theme-primary/10 ring-4 ring-theme-primary/20">
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
