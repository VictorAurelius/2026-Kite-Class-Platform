/**
 * Learning roadmap timeline — shows the step-by-step path a student follows.
 * Vertical timeline on mobile, horizontal-feel cards on desktop.
 * Falls back to a demo 4-step roadmap when no slot data is configured.
 *
 * Slot shape: slots.steps = SlotItem[] where
 *   title       = step name (e.g. "Kiểm tra trình độ")
 *   description = step detail
 *   icon        = optional emoji/marker
 */

import type { SlotData, SlotItem } from '@/lib/template/slots';

const DEFAULT_STEPS: SlotItem[] = [
  {
    title: 'Kiểm tra trình độ đầu vào',
    description: 'Bài test miễn phí xác định đúng trình độ, xây lộ trình riêng cho từng học viên.',
    icon: '📝',
  },
  {
    title: 'Học theo lộ trình cá nhân hóa',
    description: 'Lớp nhỏ 6-8 học viên, giáo trình bám sát mục tiêu (giao tiếp, thi cử, đầu ra).',
    icon: '📚',
  },
  {
    title: 'Luyện đề & kiểm tra định kỳ',
    description: 'Mock test hàng tháng, báo cáo tiến độ gửi phụ huynh qua Zalo.',
    icon: '🎯',
  },
  {
    title: 'Đạt mục tiêu & cấp chứng nhận',
    description: 'Cam kết đầu ra rõ ràng; chưa đạt được học lại miễn phí.',
    icon: '🏆',
  },
];

interface TimelineSectionProps {
  slots?: SlotData;
  /** Title override (GAP-1208); defaults to "Lộ trình học tập". */
  heading?: string;
  /** Sub-heading override; defaults to center voice. */
  subheading?: string;
}

export function TimelineSection({ slots, heading, subheading }: TimelineSectionProps) {
  const steps = (slots?.steps as SlotItem[] | undefined) || DEFAULT_STEPS;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="mb-4 text-center text-3xl font-bold">{heading ?? 'Lộ trình học tập'}</h2>
        <p className="mx-auto mb-12 max-w-2xl text-center text-muted-foreground">
          {subheading ?? 'Bốn bước rõ ràng từ ngày đầu đến khi đạt mục tiêu — minh bạch, có cam kết đầu ra.'}
        </p>
        <ol className="mx-auto grid max-w-5xl gap-6 md:grid-cols-4">
          {steps.map((step, index) => (
            <li key={step.title} className="relative">
              <div className="flex h-full flex-col rounded-xl border bg-card p-6 shadow-theme-sm">
                <div className="mb-3 flex items-center gap-3">
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-theme-primary text-sm font-bold text-white">
                    {index + 1}
                  </span>
                  {step.icon && <span className="text-2xl">{step.icon}</span>}
                </div>
                <h3 className="mb-2 font-semibold">{step.title}</h3>
                <p className="text-sm text-muted-foreground">{step.description}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
