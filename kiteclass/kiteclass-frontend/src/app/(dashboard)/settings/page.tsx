/**
 * Settings page — KC pro v2 token-styled tabs with G11 ThemePreview.
 *
 * Wave 30 Bucket D — adds a "Theme preview" tab that wires `@kite/shared-ui`
 * G11 `ThemePreview` (reflexive WCAG AA contrast demo + auto-fix CTA per
 * `ai-branding-guidelines.md` §5). Owners can preview their brand palette
 * before deploying via the AI Branding wizard.
 *
 * @author KiteClass Team
 * @since 1.0.0 — G11 integration Wave 30 (GAP-266)
 */

'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ExternalLink } from 'lucide-react';
import { ThemePreview, type BrandColors } from '@kite/shared-ui';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BrandingSettings } from '@/components/settings/branding-settings';
import { PreferencesSettings } from '@/components/settings/preferences-settings';
import { OnboardingReplayCard } from '@/components/onboarding/OnboardingReplayCard';
import { useAuthStore } from '@/stores/auth-store';
import { DashboardLayout } from '@/components/layout';

/**
 * Sensible default palette for the theme-preview tab.
 *
 * In production, BrandingProvider context will hydrate these from the tenant's
 * current theme. For Wave 30 we ship safe defaults so the preview tab renders
 * meaningful WCAG-AA-passing colours without server fetch (per AI Branding §4.2
 * — preview before commit is mandatory; defaults must NOT block render).
 */
const DEFAULT_BRAND_COLORS: BrandColors = {
  primary: '#0EA5E9',
  secondary: '#F97316',
  background: '#FFFFFF',
  foreground: '#0F172A',
};

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState('branding');
  const user = useAuthStore((state) => state.user);
  // GAP-979: per-user "Tùy chọn" preferences require a numeric domain reference id
  // (parents.id / teachers.id / students.id). An OWNER has a KiteHub-level UUID identity
  // with no KiteClass domain ref-id, so GET /api/v1/users/{id}/preferences returns 403.
  // Hide the tab for OWNER to avoid surfacing a broken settings section.
  // NB: backend role 'OWNER' is not in the FE UserType enum (ADMIN/STAFF/TEACHER/PARENT/
  // STUDENT) — useAuth casts role as UserType, so compare as string at runtime.
  const showPreferences = (user?.userType as string | undefined) !== 'OWNER';

  return (
    <DashboardLayout>
      <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Cài đặt</h1>
        <p className="text-muted-foreground">
          Quản lý thông tin tổ chức, theme và tùy chọn cá nhân
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="branding">Branding</TabsTrigger>
          <TabsTrigger value="theme">Theme preview</TabsTrigger>
          {showPreferences && <TabsTrigger value="preferences">Tùy chọn</TabsTrigger>}
        </TabsList>

        <TabsContent value="branding" className="mt-6">
          <BrandingSettings />
        </TabsContent>

        <TabsContent value="theme" className="mt-6 space-y-6">
          <Card className="rounded-xl shadow-sm">
            <CardHeader>
              <CardTitle>Xem trước theme</CardTitle>
              <CardDescription>
                Kiểm tra độ tương phản WCAG AA và xem trước light/dark trước khi triển
                khai. Nhấn &ldquo;Áp dụng full theme&rdquo; để chuyển sang AI Branding wizard.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div data-testid="settings-theme-preview">
                <ThemePreview brandColors={DEFAULT_BRAND_COLORS} />
              </div>
              <div className="mt-6 flex justify-end">
                <Link href="/branding">
                  <Button>
                    Áp dụng full theme
                    <ExternalLink className="ml-2 h-4 w-4" aria-hidden />
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {showPreferences && (
          <TabsContent value="preferences" className="mt-6 space-y-6">
            <OnboardingReplayCard />
            <PreferencesSettings />
          </TabsContent>
        )}
      </Tabs>
      </div>
    </DashboardLayout>
  );
}
