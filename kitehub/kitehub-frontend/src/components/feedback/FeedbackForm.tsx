'use client';

/**
 * FeedbackForm — controlled modal wrapper exposing FeedbackWidget form
 * (Wave 98 GAP-540 + GAP-542 merge per Bucket B5).
 *
 * <p>Replaces the standalone floating `FeedbackWidget` mount: now the form
 * lives behind {@link SupportMenu}'s "Gửi phản hồi" dropdown item, opened as a
 * Radix Dialog with `open` + `onOpenChange` props controlled by the parent.
 * Eliminates the GAP-540/542 floating-button collision on mobile ≤375px
 * (outside-in audit F-NEW-2 / F-NEW-4).</p>
 *
 * <p>Contract: {@code documents/01-business/kitehub/feedback/api-contract.md}
 *  - POST /api/v1/feedback (public, 10 req/min/IP at gateway)
 *  - rating 1..5 + comment 5..2000 chars + honeypot empty</p>
 *
 * <p>UX:</p>
 * <ul>
 *   <li>Radix Dialog với focus-trap + Escape close + scroll lock (WCAG 2.1.1 + 2.4.3)</li>
 *   <li>5-star rating + category select + textarea + optional email + honeypot</li>
 *   <li>Submit success → success message inline + auto-close ~2s</li>
 *   <li>Submit error → inline alert; retry available</li>
 * </ul>
 *
 * @since Wave 98 — GAP-540 + GAP-542 (Bucket B5)
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
const AUTO_CLOSE_MS = 2000;

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

export interface FeedbackFormProps {
  /** Whether the modal is open. Controlled by parent (SupportMenu). */
  open: boolean;
  /** Called when the dialog requests to close (Escape, overlay click, Cancel button, auto-close). */
  onClose: () => void;
  /** Override the API endpoint (used in tests). */
  endpoint?: string;
  /** Pre-fill email (e.g., logged-in user's address). */
  defaultEmail?: string;
}

export function FeedbackForm({
  open,
  onClose,
  endpoint = ENDPOINT,
  defaultEmail = '',
}: FeedbackFormProps) {
  const [rating, setRating] = useState<number>(0);
  const [comment, setComment] = useState('');
  const [email, setEmail] = useState(defaultEmail);
  const [category, setCategory] = useState<Category>('GENERAL');
  const [honeypot, setHoneypot] = useState(''); // MUST stay empty
  const [submitState, setSubmitState] = useState<SubmitState>({ kind: 'idle' });

  const formId = useId();

  // Reset form when modal opens (avoid stale state across opens).
  useEffect(() => {
    if (open) {
      setRating(0);
      setComment('');
      setEmail(defaultEmail);
      setCategory('GENERAL');
      setHoneypot('');
      setSubmitState({ kind: 'idle' });
    }
  }, [open, defaultEmail]);

  // Auto-close on success after AUTO_CLOSE_MS.
  useEffect(() => {
    if (submitState.kind !== 'success') return;
    const t = setTimeout(() => {
      onClose();
    }, AUTO_CLOSE_MS);
    return () => clearTimeout(t);
  }, [submitState, onClose]);

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
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent
        data-testid="feedback-form-dialog"
        className="sm:max-w-md"
        aria-labelledby={`${formId}-title`}
      >
        <DialogHeader>
          <DialogTitle id={`${formId}-title`}>Gửi phản hồi cho KiteHub</DialogTitle>
          <DialogDescription>
            Cảm ơn anh/chị đã dành thời gian — chia sẻ trải nghiệm để chúng tôi cải thiện.
          </DialogDescription>
        </DialogHeader>

        {submitState.kind === 'success' ? (
          <div
            data-testid="feedback-form-success"
            role="status"
            className="rounded-lg bg-green-50 p-4 text-sm text-green-700"
          >
            Cảm ơn anh/chị đã gửi phản hồi 🙏 Chúng tôi đã ghi nhận và sẽ phản hồi sớm.
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
                    data-testid={`feedback-form-star-${star}`}
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
              <Label htmlFor={`${formId}-category`}>Loại phản hồi</Label>
              <select
                id={`${formId}-category`}
                data-testid="feedback-form-category"
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
                data-testid="feedback-form-comment"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Anh/chị nghĩ gì? (tối thiểu 5 ký tự)"
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
                data-testid="feedback-form-email"
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
              style={{
                position: 'absolute',
                left: '-9999px',
                top: 'auto',
                width: 1,
                height: 1,
                overflow: 'hidden',
              }}
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
                data-testid="feedback-form-error"
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
                onClick={onClose}
                data-testid="feedback-form-cancel"
              >
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={!canSubmit}
                data-testid="feedback-form-submit"
              >
                {submitState.kind === 'submitting' ? 'Đang gửi...' : 'Gửi phản hồi'}
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
