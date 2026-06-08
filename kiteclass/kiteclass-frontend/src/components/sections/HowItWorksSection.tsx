/**
 * How-it-works section — the 3-step "get started" path. Ported from the
 * marketing-site kit (HowItWorks) per wave-landing-100 Bucket F.
 *
 * Layout: numbered 3-step grid (stacks on mobile), scroll-reveal staggered.
 *
 * Slot shape: slots.steps = SlotItem[] where
 *   title       = step name (e.g. "Tạo lớp & nhập học viên")
 *   description = step detail
 *
 * Falls back to a generic 3-step onboarding demo when no slot data — generic
 * platform copy, not fabricated data (cf. Bucket A empty-state spirit).
 */

import type { SlotData, SlotItem } from '@/lib/template/slots';
import { ScrollReveal } from './ScrollReveal';

const DEFAULT_STEPS: SlotItem[] = [
  {
    title: 'Tạo lớp & nhập học viên',
    description:
      'Tạo lớp, xếp lịch và thêm danh sách học viên. Nhập nhanh từ file Excel sẵn có của trung tâm.',
  },
  {
    title: 'Mời giáo viên & phụ huynh',
    description:
      'Gửi lời mời cho giáo viên và phụ huynh qua email. Mỗi người có tài khoản với đúng quyền của mình.',
  },
  {
    title: 'Vận hành tự động',
    description:
      'Điểm danh, học phí và điểm số liên thông với nhau. Phụ huynh nhận cập nhật, anh/chị xem báo cáo tổng quan.',
  },
];

interface HowItWorksSectionProps {
  slots?: SlotData;
}

export function HowItWorksSection({ slots }: HowItWorksSectionProps) {
  const steps = (slots?.steps as SlotItem[] | undefined) || DEFAULT_STEPS;
  if (steps.length === 0) return null;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <div className="mx-auto mb-12 max-w-2xl text-center">
          <span className="mb-3 inline-block text-sm font-semibold uppercase tracking-wide text-theme-primary">
            Cách hoạt động
          </span>
          <h2 className="text-3xl font-bold md:text-4xl">Bắt đầu trong ba bước</h2>
          <p className="mt-3 text-muted-foreground">
            Không cần cài đặt phức tạp. Anh/chị có thể đưa trung tâm lên nền tảng ngay trong ngày đầu.
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
