/**
 * Floating Zalo contact button — fixed bottom-right CTA for quick parent/student
 * contact (Zalo is the dominant VN edu communication channel).
 * Falls back to a tel: link when no Zalo handle is configured.
 *
 * Env:
 *   NEXT_PUBLIC_ZALO_URL    — full Zalo OA / personal link (e.g. https://zalo.me/0901234567)
 *   NEXT_PUBLIC_CONTACT_PHONE — used to build a zalo.me link when ZALO_URL unset
 */

const ZALO_BRAND = 'rgb(0 104 255)'; // Zalo blue

function resolveZaloHref(): string {
  const explicit = process.env.NEXT_PUBLIC_ZALO_URL;
  if (explicit) return explicit;
  const phone = (process.env.NEXT_PUBLIC_CONTACT_PHONE || '').replace(/[^0-9]/g, '');
  if (phone) return `https://zalo.me/${phone}`;
  return 'https://zalo.me';
}

export function FloatingZaloButton() {
  const href = resolveZaloHref();

  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Liên hệ tư vấn qua Zalo"
      className="fixed bottom-5 right-5 z-50 flex items-center gap-2 rounded-full px-4 py-3 text-sm font-semibold text-white shadow-lg transition-transform hover:scale-105"
      style={{ backgroundColor: ZALO_BRAND }}
    >
      {/* Chat bubble glyph — no extra icon dependency */}
      <span aria-hidden className="text-lg leading-none">💬</span>
      <span className="hidden sm:inline">Tư vấn qua Zalo</span>
    </a>
  );
}
