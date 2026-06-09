/**
 * Settings → Branding → Advanced Mode page.
 *
 * Wave 32 Bucket D — Direction C 6-step refactor.
 *
 * Per `ai-branding-guidelines.md` §2.4: ENTERPRISE Advanced Mode opt-in MUST
 * happen via Settings (NOT inline default). The toggle is hidden entirely
 * for non-ENTERPRISE tiers — they get a friendly tier-required notice.
 *
 * The toggle ON path opens AdvancedModeDisclaimer (per §2.4 explicit consent).
 * The toggle OFF path is immediate (no disclaimer needed to disable).
 *
 * Local state today (advancedModeEnabled) persists to localStorage so the
 * preference survives reload until backend ships
 * `PATCH /branding/preferences/advanced-mode` (tracked GAP-272m).
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { ChevronRight, Lock, Shield, AlertTriangle } from 'lucide-react';
import { useBrandingTier } from '@/hooks/use-branding-tier';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { AdvancedModeDisclaimer } from '@/components/branding/wizard/AdvancedModeDisclaimer';

const STORAGE_KEY = 'kitehub.branding.advanced-mode';

export default function BrandingAdvancedModePage() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  // GAP-1091b: useBrandingTier cần instanceId (→ /subscriptions/instance/{id}/active),
  // KHÔNG phải owner id. Resolve instance từ owner's instances (như billing page).
  const { data: instances } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;
  const tier = useBrandingTier(instanceId);
  const [enabled, setEnabled] = useState(false);
  const [disclaimerOpen, setDisclaimerOpen] = useState(false);

  // Hydrate persisted preference from localStorage on mount.
  useEffect(() => {
    if (typeof window === 'undefined') return;
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      setEnabled(stored === 'true');
    } catch {
      // ignore localStorage failures (Safari private mode)
    }
  }, []);

  // Persist preference whenever the toggle flips.
  useEffect(() => {
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.setItem(STORAGE_KEY, String(enabled));
    } catch {
      // ignore
    }
  }, [enabled]);

  const handleToggleClick = (next: boolean) => {
    if (next === true) {
      // Turning ON requires explicit consent per `ai-branding-guidelines.md` §2.4.
      setDisclaimerOpen(true);
    } else {
      // Turning OFF is immediate.
      setEnabled(false);
    }
  };

  const handleDisclaimerConfirm = () => {
    setEnabled(true);
    setDisclaimerOpen(false);
  };

  const handleDisclaimerCancel = () => {
    setDisclaimerOpen(false);
  };

  return (
    <main className="container mx-auto px-4 py-6 max-w-3xl" data-testid="advanced-mode-page">
      {/* Breadcrumb */}
      <nav
        className="text-xs text-muted-foreground mb-3 flex items-center gap-1.5"
        aria-label="Breadcrumb"
      >
        <button
          type="button"
          onClick={() => router.push('/settings')}
          className="hover:text-foreground"
        >
          Settings
        </button>
        <ChevronRight className="w-3 h-3" />
        <button
          type="button"
          onClick={() => router.push('/branding')}
          className="hover:text-foreground"
        >
          Branding
        </button>
        <ChevronRight className="w-3 h-3" />
        <span>Advanced Mode</span>
      </nav>

      <h1 className="text-2xl font-bold text-foreground mb-2">Advanced Mode</h1>
      <p className="text-muted-foreground text-sm mb-6">
        Mở khoá custom-prompt 200 ký tự cho ENTERPRISE. Mặc định: TẮT — chỉ wizard 6 bước được phép.
      </p>

      {!tier.advancedModeEnabled ? (
        // Tier !== ENTERPRISE — toggle hidden per `ai-branding-guidelines.md` §2.4
        <Alert
          data-testid="advanced-mode-tier-required-notice"
          className="border-amber-300 bg-amber-50"
        >
          <Lock className="h-4 w-4" />
          <AlertTitle>Tính năng chỉ dành cho ENTERPRISE</AlertTitle>
          <AlertDescription>
            Gói hiện tại của bạn ({tier.tier}) không hỗ trợ Advanced Mode. Liên hệ với
            đội kinh doanh để nâng cấp lên ENTERPRISE.
          </AlertDescription>
        </Alert>
      ) : (
        <>
          <Card className="p-6" data-testid="advanced-mode-toggle-card">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="font-bold">Free-form prompt input</h3>
                  <Badge
                    variant="default"
                    className="bg-gradient-to-r from-amber-200 to-orange-200 text-orange-900 hover:bg-orange-200"
                  >
                    CHỈ ENTERPRISE
                  </Badge>
                </div>
                <p className="text-sm text-muted-foreground mb-2">
                  Khi BẬT, ở Bước 5 (Mẫu thiết kế) sẽ có thêm ô nhập custom-prompt tối đa 200 ký tự.
                  AI sẽ kết hợp wizard choices + prompt này để tạo banner riêng.
                </p>
                <div className="grid sm:grid-cols-2 gap-2 mt-3 text-xs">
                  <div className="p-2 rounded bg-amber-50 border border-amber-200 flex gap-2 text-amber-900">
                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                    <span>Output có thể không khớp brand guideline</span>
                  </div>
                  <div className="p-2 rounded bg-amber-50 border border-amber-200 flex gap-2 text-amber-900">
                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                    <span>Quality gate fail sẽ tự fallback về template</span>
                  </div>
                </div>
              </div>
              <div className="shrink-0 flex flex-col items-center gap-1.5">
                <Switch
                  checked={enabled}
                  onCheckedChange={handleToggleClick}
                  aria-label="Bật Advanced Mode"
                  data-testid="advanced-mode-toggle"
                />
                <p
                  className={`text-xs font-bold ${
                    enabled ? 'text-emerald-700' : 'text-muted-foreground'
                  }`}
                  data-testid="advanced-mode-status-text"
                >
                  {enabled ? 'ĐANG BẬT' : 'ĐANG TẮT'}
                </p>
              </div>
            </div>
          </Card>

          <Card
            className="p-4 mt-4 flex items-start gap-3 text-sm"
            style={{ background: 'hsl(199 89% 48% / 0.08)' }}
            data-testid="advanced-mode-info"
          >
            <Shield
              className="w-5 h-5 shrink-0 mt-0.5"
              style={{ color: 'hsl(199 89% 48%)' }}
            />
            <div>
              <p className="font-semibold mb-1">Tại sao ENTERPRISE-only?</p>
              <p className="text-muted-foreground">
                Free-form prompt có thể tạo output không theo brand guideline + cost cao hơn.
                Ràng buộc cho FREE/BASIC/PREMIUM giúp đảm bảo &ldquo;best possible branded instance&rdquo;
                thay vì &ldquo;user creativity playground&rdquo;.
              </p>
            </div>
          </Card>
        </>
      )}

      <AdvancedModeDisclaimer
        open={disclaimerOpen}
        onOpenChange={setDisclaimerOpen}
        onConfirm={handleDisclaimerConfirm}
        onCancel={handleDisclaimerCancel}
      />
    </main>
  );
}
