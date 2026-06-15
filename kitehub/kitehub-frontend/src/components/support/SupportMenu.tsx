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
 * Dropdown items (5):
 *  1. "Hướng dẫn nhanh" → persona-aware help route
 *  2. "Liên hệ qua email" → mailto:support@kitehub.me
 *  3. "Liên hệ Zalo OA" → https://zalo.me/{oa_id} (web) + zalo://chat?oa_id={oa_id} (mobile deep-link)
 *  4. "Gửi phản hồi" → opens FeedbackForm Radix Dialog (Wave 98 B5 — GAP-540 + GAP-542 merge)
 *  5. "Trạng thái beta" → /beta-status
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
import { HelpCircle, Mail, MessageCircle, MessageSquare, Activity } from 'lucide-react';
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
import { FeedbackForm } from '@/components/feedback/FeedbackForm';
import { useAuthStore } from '@/stores/auth-store';

// Zalo OA fast-path (Wave 98 B6 GAP-660) — env-var-driven OA ID with placeholder
// fallback per documents/05-guides/account-prep/zalo-oa-setup-runbook.md §2.2.
// Default `kitehub` is a placeholder slug; production deploys MUST set the real
// OA ID (13-16 digit) via NEXT_PUBLIC_KITEHUB_ZALO_OA_ID env var.
const ZALO_OA_ID = process.env.NEXT_PUBLIC_KITEHUB_ZALO_OA_ID ?? 'kitehub';
const ZALO_OA_WEB_URL = `https://zalo.me/${ZALO_OA_ID}`;
const ZALO_OA_DEEPLINK = `zalo://chat?oa_id=${ZALO_OA_ID}`;

export interface SupportMenuProps {
  /** Phase from useOnboardingPhase — drives help route persona mapping. */
  phase?: OnboardingPhase;
  /** Override CSS positioning (testing only). */
  className?: string;
  /**
   * Optional callback when user clicks "Gửi phản hồi". When provided, the parent
   * controls the modal open state (advanced wiring). When omitted (default),
   * SupportMenu manages its own FeedbackForm modal — recommended path.
   */
  onFeedbackClick?: () => void;
  /** Pre-fill email field in FeedbackForm (e.g., logged-in user's address). */
  defaultEmail?: string;
}

/**
 * Derive persona-aware help route từ phase + auth role.
 *
 * GAP-1394: wires role-based help routing (previously a TODO-B5 stub returning
 * `/help`, which 404s — no `/help` base route exists). Anonymous users → public
 * help; authenticated users → the persona help page matching their platform role.
 *
 * Role → route map (existing help pages: anonymous / p2-owner / p3-manager /
 * platform-admin):
 *  - PLATFORM_ADMIN | ADMIN          → /help/platform-admin
 *  - STAFF                           → /help/p3-manager
 *  - OWNER (+ authenticated fallback) → /help/p2-owner
 *
 * Pure function (no hooks) so it stays unit-testable in isolation.
 */
export function helpRouteFor(
  phase: OnboardingPhase | undefined,
  role: string | undefined,
): string {
  if (!role || phase === 'anonymous') {
    return '/help/anonymous';
  }
  switch (role.trim().toUpperCase()) {
    case 'PLATFORM_ADMIN':
    case 'ADMIN':
      return '/help/platform-admin';
    case 'STAFF':
      return '/help/p3-manager';
    case 'OWNER':
    default:
      return '/help/p2-owner';
  }
}

export function SupportMenu({
  phase,
  className,
  onFeedbackClick,
  defaultEmail,
}: SupportMenuProps) {
  const [feedbackModalOpen, setFeedbackModalOpen] = useState(false);

  // GAP-1394: read raw platform role (distinguishes PLATFORM_ADMIN from OWNER —
  // useRole collapses the alias) to route the "Hướng dẫn nhanh" help link.
  const role = useAuthStore((s) => s.user?.role);

  const helpRoute = helpRouteFor(phase, role);

  const handleFeedback = () => {
    if (onFeedbackClick) {
      onFeedbackClick();
    } else {
      // Wave 98 B5: open the actual FeedbackForm Radix Dialog (GAP-540 + GAP-542 merge).
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
              <span>Liên hệ qua email</span>
            </a>
          </DropdownMenuItem>

          {/* Wave 98 B6 GAP-660 — Zalo OA fast-path. Mobile clicks try deep-link
              first (zalo://chat?oa_id=...), then fall back to web (zalo.me/...)
              after 300ms if Zalo app không cài. Desktop always uses web link. */}
          <DropdownMenuItem asChild>
            <a
              href={ZALO_OA_WEB_URL}
              target="_blank"
              rel="noopener noreferrer"
              data-testid="support-menu-zalo-oa-link"
              className="cursor-pointer"
              onClick={(e) => {
                // Mobile UA heuristic — attempt deep-link, browser falls back to
                // web link if Zalo app KHÔNG cài (default href takes over).
                if (typeof window !== 'undefined' && /Android|iPhone|iPad|iPod/i.test(navigator.userAgent)) {
                  e.preventDefault();
                  // Try Zalo app deep-link first
                  window.location.href = ZALO_OA_DEEPLINK;
                  // Fallback to web after 500ms if deep-link didn't navigate
                  setTimeout(() => {
                    window.location.href = ZALO_OA_WEB_URL;
                  }, 500);
                }
              }}
            >
              <MessageCircle className="mr-2 h-4 w-4" aria-hidden />
              <span>Liên hệ Zalo OA</span>
            </a>
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

      {/* Wave 98 B5: actual FeedbackForm Radix Dialog (GAP-540 + GAP-542 merge).
       * Only mount when the parent did NOT supply onFeedbackClick — otherwise
       * the parent controls its own feedback flow. */}
      {!onFeedbackClick && (
        <FeedbackForm
          open={feedbackModalOpen}
          onClose={() => setFeedbackModalOpen(false)}
          defaultEmail={defaultEmail}
        />
      )}
    </>
  );
}
