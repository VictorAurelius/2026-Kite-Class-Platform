'use client';

/**
 * SupportMenu — single floating `?` button bottom-right + dropdown menu
 * (Wave 98 GAP-656 UI Coordinator, eliminates GAP-540 + GAP-542 floating
 * button collision on mobile ≤375px).
 *
 * Replaces GAP-540 standalone SupportWidget and GAP-542 FeedbackWidget
 * floating buttons with ONE shared entry point. Per outside-in audit F-NEW-2
 * + F-NEW-4 + failure-mode matrix M-NEW-7.
 *
 * Dropdown items (4):
 *  1. "Hướng dẫn nhanh" → persona-aware help route
 *  2. "Liên hệ hỗ trợ" → mailto:support@kitehub.me
 *  3. "Gửi phản hồi" → opens FeedbackForm modal placeholder (B5 wires actual)
 *  4. "Trạng thái beta" → /beta-status
 *
 * Accessibility:
 *  - Floating button ≥44×44px (WCAG 2.5.5 touch target)
 *  - Dropdown via Radix DropdownMenu (focus trap + ESC close + keyboard nav)
 *  - aria-label tiếng Việt
 *
 * Mobile considerations (≤375px):
 *  - Bottom-right anchored với 24px margin (avoid Safari bottom bar overlap)
 *  - z-index 50 above content but below modal/dialog
 *  - Tap target 56×56px (above 44px minimum cho thumb comfort)
 *
 * @since Wave 98 — GAP-656
 */

import { useState } from 'react';
import Link from 'next/link';
import { HelpCircle, Mail, MessageSquare, Activity, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import type { OnboardingPhase } from '@/hooks/useOnboardingPhase';

export interface SupportMenuProps {
  /** Phase from useOnboardingPhase — drives help route persona mapping. */
  phase?: OnboardingPhase;
  /** Override CSS positioning (testing only). */
  className?: string;
  /** Callback when user clicks "Gửi phản hồi" — Bucket B5 will wire to actual form modal. */
  onFeedbackClick?: () => void;
}

/**
 * Derive persona-aware help route từ phase + role hint.
 * Anonymous → public help; authenticated → persona-specific.
 */
function helpRouteForPhase(phase: OnboardingPhase | undefined): string {
  if (!phase || phase === 'anonymous') {
    return '/help/anonymous';
  }
  // Authenticated users default to general help; persona detection deferred to B5.
  // TODO B5 — read JWT role claim → route to /help/p1, /help/p2-owner, /help/p3-manager
  return '/help';
}

export function SupportMenu({ phase, className, onFeedbackClick }: SupportMenuProps) {
  const [feedbackModalOpen, setFeedbackModalOpen] = useState(false);

  const helpRoute = helpRouteForPhase(phase);

  const handleFeedback = () => {
    if (onFeedbackClick) {
      onFeedbackClick();
    } else {
      // B5 placeholder: open in-place stub modal (actual FeedbackForm by Bucket B5).
      setFeedbackModalOpen(true);
    }
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            data-testid="support-menu-trigger"
            aria-label="Mở menu hỗ trợ"
            className={cn(
              // Bottom-right anchor — fixed 24px margin avoid Safari bottom bar overlap
              'fixed bottom-6 right-6 z-50',
              // Tap target 56×56px (WCAG 2.5.5 min 44px; 56px for thumb comfort)
              'h-14 w-14 rounded-full',
              // Visual style — primary color với shadow
              'bg-primary text-primary-foreground shadow-lg',
              'hover:bg-primary/90 hover:shadow-xl transition-all',
              'flex items-center justify-center',
              // Focus state per WCAG 2.4.7
              'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
              className
            )}
          >
            <HelpCircle className="h-6 w-6" aria-hidden />
          </button>
        </DropdownMenuTrigger>

        <DropdownMenuContent
          align="end"
          side="top"
          sideOffset={8}
          className="w-64"
          data-testid="support-menu-content"
        >
          <DropdownMenuLabel>Hỗ trợ KiteHub</DropdownMenuLabel>
          <DropdownMenuSeparator />

          <DropdownMenuItem asChild>
            <Link
              href={helpRoute}
              data-testid="support-menu-help-link"
              className="cursor-pointer"
            >
              <HelpCircle className="mr-2 h-4 w-4" aria-hidden />
              <span>Hướng dẫn nhanh</span>
            </Link>
          </DropdownMenuItem>

          <DropdownMenuItem asChild>
            <a
              href="mailto:support@kitehub.me?subject=Hỗ trợ KiteHub"
              data-testid="support-menu-contact-link"
              className="cursor-pointer"
            >
              <Mail className="mr-2 h-4 w-4" aria-hidden />
              <span>Liên hệ hỗ trợ</span>
            </a>
            {/* TODO B6 Zalo OA link — defer until Zalo OA hoạt động per GAP-660 */}
          </DropdownMenuItem>

          <DropdownMenuItem
            onClick={handleFeedback}
            data-testid="support-menu-feedback-trigger"
            className="cursor-pointer"
          >
            <MessageSquare className="mr-2 h-4 w-4" aria-hidden />
            <span>Gửi phản hồi</span>
          </DropdownMenuItem>

          <DropdownMenuItem asChild>
            <Link
              href="/beta-status"
              data-testid="support-menu-beta-status-link"
              className="cursor-pointer"
            >
              <Activity className="mr-2 h-4 w-4" aria-hidden />
              <span>Trạng thái beta</span>
            </Link>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Placeholder modal — Bucket B5 sẽ replace với actual FeedbackForm wire */}
      {feedbackModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="feedback-placeholder-title"
          data-testid="support-menu-feedback-modal-placeholder"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          onClick={() => setFeedbackModalOpen(false)}
        >
          <div
            className="relative w-full max-w-md rounded-lg bg-background p-6 shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setFeedbackModalOpen(false)}
              aria-label="Đóng modal"
              className="absolute right-4 top-4 rounded p-1 hover:bg-muted"
            >
              <X className="h-4 w-4" aria-hidden />
            </button>
            <h2 id="feedback-placeholder-title" className="text-lg font-semibold">
              Gửi phản hồi
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              Form phản hồi sẽ được Bucket B5 wire. Tạm thời, vui lòng gửi email
              tới <a href="mailto:support@kitehub.me" className="underline">support@kitehub.me</a>.
            </p>
          </div>
        </div>
      )}
    </>
  );
}
