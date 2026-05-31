/**
 * Real FAQ section (replaces the empty PlaceholderSection for `faq`).
 * Uses native <details>/<summary> so it stays a Server Component (no JS hydration
 * needed for accordion toggle) — accessible + crisp Vietnamese.
 * Falls back to demo questions when no slot data is configured.
 *
 * Slot shape: slots.questions = SlotItem[] where
 *   title       = question
 *   description = answer
 */

import type { SlotData, SlotItem } from '@/lib/template/slots';

const DEFAULT_QUESTIONS: SlotItem[] = [
  {
    title: 'Học phí một khóa là bao nhiêu?',
    description:
      'Học phí từ 1.500.000đ/tháng tùy gói và lịch học. Có thể thanh toán theo tháng hoặc theo khóa, hỗ trợ chuyển khoản và VietQR.',
  },
  {
    title: 'Có học thử miễn phí không?',
    description:
      'Có. Bạn được học thử 1 buổi miễn phí và làm bài kiểm tra trình độ đầu vào trước khi quyết định đăng ký.',
  },
  {
    title: 'Lớp học có bao nhiêu học viên?',
    description:
      'Lớp nhỏ 6-8 học viên để đảm bảo mỗi bạn đều được giáo viên quan tâm sát sao. Cũng có lớp kèm 1-1 theo yêu cầu.',
  },
  {
    title: 'Phụ huynh theo dõi tiến độ con như thế nào?',
    description:
      'Phụ huynh nhận báo cáo tiến độ định kỳ và thông báo điểm danh qua Zalo. Mọi điểm số, nhận xét đều được cập nhật minh bạch.',
  },
  {
    title: 'Lịch học có linh hoạt không?',
    description:
      'Trung tâm mở lớp các buổi tối trong tuần và cuối tuần (Thứ Hai đến Thứ Bảy), phù hợp với học sinh đi học chính khóa ban ngày.',
  },
  {
    title: 'Nếu chưa đạt mục tiêu đầu ra thì sao?',
    description:
      'Với các gói có cam kết đầu ra, nếu chưa đạt bạn được học lại miễn phí cho đến khi đạt mục tiêu đã cam kết.',
  },
];

interface FaqSectionProps {
  slots?: SlotData;
}

export function FaqSection({ slots }: FaqSectionProps) {
  const questions = (slots?.questions as SlotItem[] | undefined) || DEFAULT_QUESTIONS;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="mb-4 text-center text-3xl font-bold">Câu hỏi thường gặp</h2>
        <p className="mx-auto mb-12 max-w-2xl text-center text-muted-foreground">
          Những điều phụ huynh và học viên thường hỏi trước khi đăng ký.
        </p>
        <div className="mx-auto max-w-3xl space-y-3">
          {questions.map((q) => (
            <details
              key={q.title}
              className="group rounded-lg border bg-card p-4 [&_summary::-webkit-details-marker]:hidden"
            >
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 font-semibold">
                <span>{q.title}</span>
                <span className="shrink-0 text-theme-primary transition-transform group-open:rotate-45">
                  +
                </span>
              </summary>
              <p className="mt-3 text-sm text-muted-foreground">{q.description}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}
