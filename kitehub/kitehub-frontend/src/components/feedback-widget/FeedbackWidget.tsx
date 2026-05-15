'use client';

/**
 * In-app feedback widget — floating button + form (GAP-542 Wave 78 Bucket F).
 *
 * Per outside-in audit Tier 1 beta tenant: real-time bug-report channel is
 * mandatory; email survey alone is too slow for retention. Per wave plan §1
 * Q2 — ship BOTH in-app widget AND day-7/14 email survey.
 *
 * Contract: `documents/01-business/kitehub/feedback/api-contract.md`
 *  - POST /api/v1/feedback (public, 10 req/min/IP at gateway)
 *  - rating 1..5 + comment 5..2000 chars + honeypot empty
 *
 * UX:
 *  - Floating "💬 Góp ý" pill button anchored bottom-right
 *  - Click → Radix Dialog (focus-trap + Escape + auto-focus per WCAG 2.1.1 + 2.4.3)
 *  - 5-star rating + textarea + optional email + category
 *  - Submit success → toast / inline success message + auto-close ~2s
 *  - Submit error → inline error message; retry available
 *
 * Wave 79 Bucket D (GAP-545): migrated from hand-rolled `<div role="dialog">`
 * to `@radix-ui/react-dialog` primitive for focus-trap + Escape + scroll lock.
 *
 * @since Wave 78 — GAP-542; @updated Wave 79 — GAP-545
 */

import { useCallback, useEffect, useId, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { cn } from '@/lib/utils';

const ENDPOINT = '/api/v1/feedback';
const MIN_COMMENT = 5;
const MAX_COMMENT = 2000;

type Category = 'BUG' | 'USABILITY' | 'FEATURE_REQUEST' | 'GENERAL';

const CATEGORIES: Array<{ value: Category; label: string }> = [
  { value: 'GENERAL', label: 'Chung' },
  { value: 'BUG', label: 'Lỗi' },
  { value: 'USABILITY', label: 'Trải nghiệm' },
  { value: 'FEATURE_REQUEST', label: 'Đề xuất tính năng' },
];

type SubmitState =
  | { kind: 'idle' }
  | { kind: 'submitting' }
  | { kind: 'success'; id: string }
  | { kind: 'error'; message: string };

interface FeedbackWidgetProps {
  /** Override the floating button placement — defaults to bottom-right. */
  className?: string;
  /** Override the API endpoint (used in tests). */
  endpoint?: string;
}

export function FeedbackWidget({ className, endpoint = ENDPOINT }: FeedbackWidgetProps) {
  const [open, setOpen] = useState(false);
  const [rating, setRating] = useState<number>(0);
  const [comment, setComment] = useState('');
  const [email, setEmail] = useState('');
  const [category, setCategory] = useState<Category>('GENERAL');
  const [honeypot, setHoneypot] = useState(''); // MUST stay empty
  const [submitState, setSubmitState] = useState<SubmitState>({ kind: 'idle' });

  const formId = useId();

  // Auto-close on success after 2s.
  useEffect(() => {
    if (submitState.kind !== 'success') return;
    const t = setTimeout(() => {
      setOpen(false);
      // Reset form for next session.
      setRating(0);
      setComment('');
      setEmail('');
      setCategory('GENERAL');
      setSubmitState({ kind: 'idle' });
    }, 2000);
    return () => clearTimeout(t);
  }, [submitState]);

  const trimmedComment = comment.trim();
  const commentLen = trimmedComment.length;
  const canSubmit =
    rating >= 1 &&
    rating <= 5 &&
    commentLen >= MIN_COMMENT &&
    commentLen <= MAX_COMMENT &&
    submitState.kind !== 'submitting';

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      if (!canSubmit) return;

      setSubmitState({ kind: 'submitting' });

      const pageUrl =
        typeof window !== 'undefined' ? window.location.href : undefined;

      try {
        const res = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            rating,
            comment: trimmedComment,
            email: email.trim() || undefined,
            pageUrl,
            category,
            honeypot,
          }),
        });
        if (res.ok) {
          const data = (await res.json()) as { id: string };
          setSubmitState({ kind: 'success', id: data.id });
        } else {
          let msg = `Gửi thất bại (HTTP ${res.status})`;
          try {
            const body = (await res.json()) as { message?: string };
            if (body?.message) msg = body.message;
          } catch {
            // ignore JSON parse error
          }
          setSubmitState({ kind: 'error', message: msg });
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Lỗi kết nối';
        setSubmitState({ kind: 'error', message: msg });
      }
    },
    [canSubmit, endpoint, rating, trimmedComment, email, category, honeypot]
  );

  return (
    <>
      {/* Floating trigger button — bottom-right (rendered when modal closed) */}
      {!open && (
        <button
          type="button"
          onClick={() => setOpen(true)}
          data-testid="feedback-widget-trigger"
          aria-label="Mở form góp ý"
          className={cn(
            'fixed bottom-6 right-6 z-50',
            'rounded-full bg-primary px-5 py-3 text-sm font-medium text-primary-foreground',
            'shadow-lg transition-all hover:bg-primary/90 hover:shadow-xl',
            'flex items-center gap-2',
            className
          )}
        >
          <span aria-hidden>💬</span>
          <span>Góp ý</span>
        </button>
      )}

      {/* Radix Dialog — focus-trap + Escape + auto-focus per WCAG 2.1.1 + 2.4.3 (GAP-545) */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent
          data-testid="feedback-widget-dialog"
          className="sm:max-w-md"
          aria-labelledby={`${formId}-title`}
        >
          <DialogHeader>
            <DialogTitle id={`${formId}-title`}>Góp ý cho KiteHub</DialogTitle>
            <DialogDescription>
              Chia sẻ trải nghiệm của bạn — đánh giá 1-5 sao và mô tả ngắn gọn.
            </DialogDescription>
          </DialogHeader>

          {submitState.kind === 'success' ? (
            <div
              data-testid="feedback-widget-success"
              role="status"
              className="rounded-lg bg-green-50 p-4 text-sm text-green-700"
            >
              Cảm ơn bạn đã gửi góp ý! Chúng tôi đã ghi nhận và sẽ phản hồi sớm.
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Rating — 5 stars */}
              <div>
                <Label htmlFor={`${formId}-rating`}>Đánh giá trải nghiệm</Label>
                <div
                  id={`${formId}-rating`}
                  role="radiogroup"
                  aria-label="Đánh giá 1-5 sao"
                  className="mt-2 flex gap-2"
                >
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      role="radio"
                      aria-checked={rating === star}
                      aria-label={`${star} sao`}
                      data-testid={`feedback-widget-star-${star}`}
                      onClick={() => setRating(star)}
                      className={cn(
                        'text-2xl transition-colors',
                        rating >= star ? 'text-yellow-400' : 'text-muted-foreground'
                      )}
                    >
                      ★
                    </button>
                  ))}
                </div>
              </div>

              {/* Category */}
              <div>
                <Label htmlFor={`${formId}-category`}>Loại góp ý</Label>
                <select
                  id={`${formId}-category`}
                  data-testid="feedback-widget-category"
                  value={category}
                  onChange={(e) => setCategory(e.target.value as Category)}
                  className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  {CATEGORIES.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Comment */}
              <div>
                <Label htmlFor={`${formId}-comment`}>
                  Nội dung ({commentLen}/{MAX_COMMENT})
                </Label>
                <Textarea
                  id={`${formId}-comment`}
                  data-testid="feedback-widget-comment"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Bạn nghĩ gì? (tối thiểu 5 ký tự)"
                  rows={4}
                  maxLength={MAX_COMMENT}
                  required
                  className="mt-1"
                />
              </div>

              {/* Email — optional */}
              <div>
                <Label htmlFor={`${formId}-email`}>
                  Email (không bắt buộc — nếu cần phản hồi)
                </Label>
                <Input
                  id={`${formId}-email`}
                  data-testid="feedback-widget-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="ban@truonghoc.edu.vn"
                  className="mt-1"
                />
              </div>

              {/* Honeypot — visually hidden, must stay empty */}
              <div
                aria-hidden="true"
                style={{ position: 'absolute', left: '-9999px', top: 'auto', width: 1, height: 1, overflow: 'hidden' }}
              >
                <label htmlFor={`${formId}-honeypot`}>Để trống</label>
                <input
                  id={`${formId}-honeypot`}
                  type="text"
                  tabIndex={-1}
                  autoComplete="off"
                  value={honeypot}
                  onChange={(e) => setHoneypot(e.target.value)}
                />
              </div>

              {submitState.kind === 'error' && (
                <div
                  data-testid="feedback-widget-error"
                  role="alert"
                  className="rounded-md bg-destructive/10 p-3 text-sm text-destructive"
                >
                  {submitState.message}
                </div>
              )}

              <div className="flex items-center justify-end gap-2">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setOpen(false)}
                  data-testid="feedback-widget-cancel"
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  disabled={!canSubmit}
                  data-testid="feedback-widget-submit"
                >
                  {submitState.kind === 'submitting' ? 'Đang gửi...' : 'Gửi góp ý'}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
