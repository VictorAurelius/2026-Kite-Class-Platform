/**
 * Real FAQ section (replaces the empty PlaceholderSection for `faq`).
 * Uses native <details>/<summary> so it stays a Server Component (no JS hydration
 * needed for accordion toggle) — accessible + crisp Vietnamese.
 *
 * Anti-fabrication (GAP-958): renders ONLY real tenant-provided Q&A. When none
 * are configured the section hides entirely — never asserts generic policies
 * (free trial, class size, refund guarantee) the center may not actually offer.
 * page.tsx emits slots.questions from the backend `faqs` array when non-empty.
 *
 * Slot shape: slots.questions = SlotItem[] where
 *   title       = question
 *   description = answer
 */

import type { SlotData, SlotItem } from '@/lib/template/slots';

interface FaqSectionProps {
  slots?: SlotData;
}

export function FaqSection({ slots }: FaqSectionProps) {
  const questions = slots?.questions as SlotItem[] | undefined;
  if (!questions || questions.length === 0) return null;

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
