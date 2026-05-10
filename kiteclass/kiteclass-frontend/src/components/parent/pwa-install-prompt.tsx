/**
 * PWA install prompt — A2HS (add-to-home-screen) banner.
 *
 * Wave 49 Bucket A (GAP-267). Ports `pwa-install-prompt.html`. Listens for
 * the `beforeinstallprompt` event so we can defer the native dialog until
 * the user clicks our themed CTA. iOS Safari does not fire that event, so
 * the inline iOS instructions are shown when standalone is unavailable.
 */

'use client';

import { useEffect, useState } from 'react';
import { Share2, Smartphone, X } from 'lucide-react';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

export function PwaInstallPrompt() {
  const [deferred, setDeferred] = useState<BeforeInstallPromptEvent | null>(
    null,
  );
  const [dismissed, setDismissed] = useState(false);
  const [isStandalone, setIsStandalone] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    setIsStandalone(
      window.matchMedia('(display-mode: standalone)').matches ||
        // iOS Safari
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (window.navigator as any).standalone === true,
    );

    function onBeforeInstall(e: Event) {
      e.preventDefault();
      setDeferred(e as BeforeInstallPromptEvent);
    }
    window.addEventListener('beforeinstallprompt', onBeforeInstall);
    return () =>
      window.removeEventListener('beforeinstallprompt', onBeforeInstall);
  }, []);

  if (isStandalone || dismissed) return null;

  async function handleInstall() {
    if (!deferred) return;
    await deferred.prompt();
    const choice = await deferred.userChoice;
    if (choice.outcome === 'accepted') setDismissed(true);
    setDeferred(null);
  }

  return (
    <section
      className="mx-4 mt-4 rounded-2xl border-2 border-primary/30 bg-primary/5 p-4 shadow-sm"
      role="region"
      aria-labelledby="parent-pwa-install-title"
      data-testid="parent-pwa-install"
    >
      <div className="flex items-start gap-3">
        <span
          className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-primary/15 text-primary"
          aria-hidden
        >
          <Smartphone className="h-5 w-5" />
        </span>
        <div className="min-w-0 flex-1">
          <p
            id="parent-pwa-install-title"
            className="text-sm font-bold"
          >
            Cài đặt KiteClass Phụ huynh
          </p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            Thêm vào màn hình chính để mở nhanh — không cần qua trình duyệt.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setDismissed(true)}
          className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full text-muted-foreground hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          aria-label="Đóng"
          data-testid="parent-pwa-install-dismiss"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {deferred ? (
        <button
          type="button"
          onClick={handleInstall}
          className="mt-3 flex h-11 w-full items-center justify-center rounded-xl bg-primary text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          data-testid="parent-pwa-install-cta"
        >
          Cài đặt ngay
        </button>
      ) : (
        <div className="mt-3 rounded-xl bg-card p-3 text-xs">
          <p className="font-semibold">Trên iPhone (Safari):</p>
          <ol className="mt-1 list-inside list-decimal space-y-0.5 text-muted-foreground">
            <li className="inline-flex items-center gap-1">
              Nhấn nút <Share2 className="h-3 w-3" aria-hidden /> chia sẻ
            </li>
            <li>Chọn “Thêm vào Màn hình chính”</li>
            <li>Nhấn “Thêm” — biểu tượng KiteClass sẽ xuất hiện</li>
          </ol>
        </div>
      )}
    </section>
  );
}
