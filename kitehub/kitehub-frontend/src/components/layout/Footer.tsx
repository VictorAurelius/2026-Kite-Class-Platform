'use client';

/**
 * Public footer with support channel discoverability (GAP-540 Wave 78 Bucket F).
 *
 * Extracted from `PublicLayout.tsx` inline footer to make support channels
 * (email, FAQ, status page) trivially discoverable per N7 outside-in audit
 * finding: Tier 1 beta tenants must reach support in ≤2 clicks.
 *
 * MVP support channel: `mailto:support@kitehub.me` link. Per wave plan §1 Q4
 * decision — paid chat-widget vendor (Crisp/Tawk.to) deferred to Wave 79+.
 *
 * @since Wave 78 — GAP-540
 */

import Link from 'next/link';
import { KiteLogo } from '@/components/brand/KiteLogo';

const SUPPORT_EMAIL = 'support@kitehub.me';
// Zalo OA fast-path (Wave 98 B6 GAP-660) — see
// documents/05-guides/account-prep/zalo-oa-setup-runbook.md §4
const ZALO_OA_ID = process.env.NEXT_PUBLIC_KITEHUB_ZALO_OA_ID ?? 'kitehub';
const ZALO_OA_URL = `https://zalo.me/${ZALO_OA_ID}`;

export function Footer() {
  return (
    <footer className="border-t bg-muted/30" data-testid="public-footer">
      <div className="container py-12">
        <div className="grid gap-8 md:grid-cols-4">
          {/* Brand */}
          <div className="space-y-4">
            <KiteLogo size="md" />
            <p className="text-sm text-muted-foreground">
              Nền tảng quản lý trung tâm giáo dục thông minh.
              Giúp bạn bay cao cùng học viên.
            </p>
          </div>

          {/* Product */}
          <div>
            <h4 className="mb-4 text-sm font-semibold">Sản phẩm</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <Link href="/pricing" className="hover:text-foreground transition-colors">
                  Bảng giá
                </Link>
              </li>
              <li>
                <Link href="/register" className="hover:text-foreground transition-colors">
                  Đăng ký
                </Link>
              </li>
              <li>
                <Link href="/login" className="hover:text-foreground transition-colors">
                  Đăng nhập
                </Link>
              </li>
            </ul>
          </div>

          {/* Support (GAP-540) — discoverable support channels */}
          <div>
            <h4 className="mb-4 text-sm font-semibold">Hỗ trợ</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a
                  href={`mailto:${SUPPORT_EMAIL}`}
                  className="hover:text-foreground transition-colors"
                  data-testid="footer-support-email"
                >
                  {SUPPORT_EMAIL}
                </a>
              </li>
              <li>
                {/* Wave 98 B6 GAP-660 — Zalo OA fast-path inline với email + status */}
                <a
                  href={ZALO_OA_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-foreground transition-colors"
                  data-testid="footer-zalo-oa-link"
                >
                  Hỗ trợ qua Zalo OA
                </a>
              </li>
              <li>
                <Link
                  href="/help"
                  className="hover:text-foreground transition-colors"
                  data-testid="footer-help-link"
                >
                  Trung tâm trợ giúp
                </Link>
              </li>
              <li>
                <Link
                  href="/beta-status"
                  className="hover:text-foreground transition-colors"
                  data-testid="footer-status-link"
                >
                  Trạng thái Beta
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h4 className="mb-4 text-sm font-semibold">Liên hệ</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a
                  href={`mailto:${SUPPORT_EMAIL}`}
                  className="hover:text-foreground transition-colors"
                >
                  {SUPPORT_EMAIL}
                </a>
              </li>
              <li>
                <Link
                  href="/legal/privacy"
                  className="hover:text-foreground transition-colors"
                >
                  Chính sách bảo mật
                </Link>
              </li>
              <li>
                {/* Cookie policy link — GAP-558 Wave 83 Bucket E (PDPL Art 11 +
                    Decree 13/2023 Art 4) — pairs with <ConsentBanner /> opt-in
                    UI so user can re-read the policy before changing consent. */}
                <Link
                  href="/legal/cookies"
                  className="hover:text-foreground transition-colors"
                  data-testid="footer-cookie-policy-link"
                >
                  Chính sách Cookie
                </Link>
              </li>
              <li>
                <Link
                  href="/legal/terms"
                  className="hover:text-foreground transition-colors"
                >
                  Điều khoản dịch vụ
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-12 border-t pt-8 text-center text-sm text-muted-foreground">
          &copy; {new Date().getFullYear()} KiteHub. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
