/**
 * Authentication layout for login/register pages.
 *
 * Wave 4 (GAP-037): pulls tenant branding via {@link useTenantBranding}
 * so the logo, display name, and primary color match the tenant the user
 * is signing in to. Falls back cleanly to the default KiteClass palette
 * when no tenant context is present (e.g. direct marketing landing).
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import type { ReactNode } from 'react';
import { GraduationCap } from 'lucide-react';
import { useTenantBranding } from '@/providers/BrandingProvider';
import { SkipToContent } from '@/components/a11y/skip-to-content';

interface AuthLayoutProps {
  children: ReactNode;
}

export function AuthLayout({ children }: AuthLayoutProps) {
  const { branding } = useTenantBranding();
  const displayName = branding.displayName || 'KiteClass';
  const logoUrl = branding.logoUrl;

  return (
    <div className="flex min-h-screen">
      {/* Skip to main content (accessibility — WCAG 2.4.1) */}
      <SkipToContent />
      {/* Left Side - Branding */}
      <div className="hidden w-1/2 bg-primary lg:flex lg:flex-col lg:justify-center lg:px-12">
        <div className="mx-auto max-w-md space-y-6 text-primary-foreground">
          <div className="flex items-center gap-3">
            {logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={logoUrl}
                alt={displayName}
                className="h-12 w-12 rounded bg-white/10 object-contain p-1"
              />
            ) : (
              <GraduationCap className="h-12 w-12" />
            )}
            <h1 className="text-4xl font-bold">{displayName}</h1>
          </div>
          <h2 className="text-2xl font-semibold">
            Quản lý trung tâm giáo dục dễ dàng & hiệu quả
          </h2>
          <p className="text-lg text-primary-foreground/90">
            Quản lý học viên, điểm danh, học phí và nhiều hơn nữa — tất cả trong một nền tảng.
          </p>
          <ul className="space-y-3">
            <li className="flex items-center gap-2">
              <div className="h-2 w-2 rounded-full bg-primary-foreground" />
              <span>Nền tảng SaaS đa trung tâm</span>
            </li>
            <li className="flex items-center gap-2">
              <div className="h-2 w-2 rounded-full bg-primary-foreground" />
              <span>Điểm danh &amp; chấm điểm thời gian thực</span>
            </li>
            <li className="flex items-center gap-2">
              <div className="h-2 w-2 rounded-full bg-primary-foreground" />
              <span>Học phí &amp; thanh toán tự động</span>
            </li>
          </ul>
        </div>
      </div>

      {/* Right Side - Auth Form */}
      <main id="main-content" role="main" className="flex w-full flex-col justify-center px-6 lg:w-1/2 lg:px-12">
        <div className="mx-auto w-full max-w-md space-y-6">
          {/* Logo for mobile */}
          <div className="flex items-center justify-center gap-2 lg:hidden">
            {logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={logoUrl} alt={displayName} className="h-8 w-8 object-contain" />
            ) : (
              <GraduationCap className="h-8 w-8 text-primary" />
            )}
            <span className="text-2xl font-bold">{displayName}</span>
          </div>

          {/* Form Content */}
          {children}

          {/* Footer */}
          <p className="text-center text-sm text-muted-foreground">
            © {new Date().getFullYear()} {displayName}. Bảo lưu mọi quyền.
          </p>
        </div>
      </main>
    </div>
  );
}
