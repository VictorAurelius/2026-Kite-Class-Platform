/**
 * Problem → Solution section — names the pain a center owner feels today, then
 * shows how the platform resolves it. Ported from the marketing-site kit
 * (ProblemSolution) per wave-landing-100 Bucket F.
 *
 * Layout: 3 pain/solution cards (responsive grid; stacks on mobile).
 * Scroll-reveal staggered per card.
 *
 * Slot shape: slots.items = SlotItem[] where
 *   title       = pain headline (e.g. "Điểm danh thủ công")
 *   description = the problem detail
 *   items[0]    = the fix (one-line solution)
 *
 * Falls back to a VN-context demo (centre-owner pains) when no slot data — this
 * is generic platform marketing copy, NOT fabricated partner/customer data, so
 * rendering a default is safe (cf. Bucket A empty-state spirit / GAP-958).
 */

import { MessageSquareWarning, CheckCircle2 } from 'lucide-react';
import type { SlotData, SlotItem } from '@/lib/template/slots';
import { ScrollReveal } from './ScrollReveal';

const DEFAULT_ITEMS: SlotItem[] = [
  {
    title: 'Điểm danh thủ công',
    description:
      'Điểm danh bằng sổ giấy mất thời gian, dễ sót, cuối tháng khó tổng hợp số buổi học của từng em.',
    items: ['Điểm danh 1 chạm, tự tổng hợp theo lớp và theo tháng.'],
  },
  {
    title: 'Học phí dễ tính nhầm',
    description:
      'Học phí theo buổi, theo khóa, có nghỉ — tính tay dễ sai, vừa mất tiền vừa mất lòng tin của phụ huynh.',
    items: ['Tự tính học phí từ điểm danh, xuất hóa đơn rõ ràng.'],
  },
  {
    title: 'Phụ huynh hỏi liên tục',
    description:
      'Phụ huynh nhắn Zalo hỏi con đi học chưa, điểm thế nào, đóng tiền đến đâu — trả lời cả ngày không xuể.',
    items: ['Phụ huynh tự xem điểm danh, điểm số, học phí của con.'],
  },
];

interface ProblemSolutionSectionProps {
  slots?: SlotData;
}

export function ProblemSolutionSection({ slots }: ProblemSolutionSectionProps) {
  const items = (slots?.items as SlotItem[] | undefined) || DEFAULT_ITEMS;
  if (items.length === 0) return null;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <div className="mx-auto mb-12 max-w-2xl text-center">
          <span className="mb-3 inline-block text-sm font-semibold uppercase tracking-wide text-theme-cta">
            Vấn đề quen thuộc
          </span>
          <h2 className="text-3xl font-bold md:text-4xl">Vận hành trung tâm không nên vất vả đến vậy</h2>
          <p className="mt-3 text-muted-foreground">
            Nếu anh/chị đang xoay xở giữa Excel, Zalo và sổ giấy, đây là những việc nền tảng gỡ rối ngay.
          </p>
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          {items.map((item, index) => {
            const fix = item.items?.[0];
            return (
              <ScrollReveal key={item.title} delayMs={index * 120}>
                <div className="flex h-full flex-col rounded-xl border bg-card p-6 shadow-theme-sm">
                  <div className="mb-3 inline-flex w-fit items-center gap-2 rounded-full bg-destructive/10 px-3 py-1 text-sm font-medium text-destructive">
                    <MessageSquareWarning className="h-4 w-4" aria-hidden /> {item.title}
                  </div>
                  {item.description && (
                    <p className="mb-4 flex-1 text-sm text-muted-foreground">{item.description}</p>
                  )}
                  {fix && (
                    <div className="mt-auto flex items-start gap-2 rounded-lg bg-theme-primary/5 p-3 text-sm font-medium text-theme-primary">
                      <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-green-500" aria-hidden />
                      <span>{fix}</span>
                    </div>
                  )}
                </div>
              </ScrollReveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}
