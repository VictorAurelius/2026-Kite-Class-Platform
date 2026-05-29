/**
 * Stats counters section — social proof for independent teachers / centers.
 * Shows headline numbers (years of experience, students taught, pass rate).
 * Falls back to demo data when no slot data is configured.
 *
 * Slot shape: slots.stats = SlotItem[] where
 *   title       = the number (e.g. "8")
 *   icon        = optional suffix marker (e.g. "+", "%")
 *   description = the label (e.g. "Năm kinh nghiệm")
 */

import type { SlotData, SlotItem } from '@/lib/template/slots';

const DEFAULT_STATS: SlotItem[] = [
  { title: '8', icon: '+', description: 'Năm kinh nghiệm giảng dạy' },
  { title: '500', icon: '+', description: 'Học viên đã đồng hành' },
  { title: '95', icon: '%', description: 'Học viên đạt mục tiêu đầu ra' },
  { title: '4.9', icon: '/5', description: 'Đánh giá từ phụ huynh' },
];

interface StatsSectionProps {
  slots?: SlotData;
}

export function StatsSection({ slots }: StatsSectionProps) {
  const stats = (slots?.stats as SlotItem[] | undefined) || DEFAULT_STATS;

  return (
    <section className="bg-theme-primary/5 py-14">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-2 gap-6 md:grid-cols-4">
          {stats.map((stat) => (
            <div key={stat.description} className="text-center">
              <p className="text-4xl font-extrabold text-theme-primary md:text-5xl">
                {stat.title}
                {stat.icon && <span className="text-2xl md:text-3xl">{stat.icon}</span>}
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
