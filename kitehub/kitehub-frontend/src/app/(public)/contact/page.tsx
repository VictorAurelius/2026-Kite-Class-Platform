/**
 * Contact / Sales lead page (GAP-1101) — KiteHub PLATFORM Enterprise "Liên hệ" CTA.
 *
 * Prospective center owner contacting KiteHub sales about the Enterprise SaaS
 * plan. Reached from /pricing Enterprise CTA + in-app billing Enterprise tier.
 *
 * `searchParams` is read SERVER-side (like waitlist/page.tsx) and `plan` passed
 * as a prop — avoids `useSearchParams()` in a client component which would
 * require a `<Suspense>` boundary at production build (per
 * .claude/rules/fe-build-local-verify.md / GAP-801 Suspense bailout class).
 *
 * Distinct from kiteclass-core tenant-marketing contact (student → center). This
 * is the KiteHub PLATFORM sales funnel (kitehub-frontend :3001 per
 * .claude/rules/kitehub-kiteclass-boundary.md §2).
 */

import Link from 'next/link';
import { ContactForm } from './ContactForm';

export const metadata = {
  title: 'Liên hệ tư vấn — KiteHub',
  description:
    'Liên hệ đội ngũ KiteHub để được tư vấn gói Enterprise cho trung tâm của bạn. '
    + 'Chúng tôi sẽ phản hồi trong vòng 24 giờ.',
};

interface ContactPageProps {
  searchParams: Promise<{ plan?: string }>;
}

const VALID_PLANS = ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'];

export default async function ContactPage({ searchParams }: ContactPageProps) {
  const params = await searchParams;
  const planParam = (params.plan ?? '').toUpperCase();
  const planInterest = VALID_PLANS.includes(planParam) ? planParam : 'ENTERPRISE';

  // Header + nav are provided by (public)/layout.tsx → PublicLayout (logo + Bảng
  // giá / Đăng nhập / Dùng thử miễn phí). Don't render another <header> here — it
  // duplicates the site header and nests a second <main>. Keep only a lightweight
  // contextual back-link to /pricing.
  return (
    <div className="mx-auto max-w-2xl px-6 py-16">
      <Link
        href="/pricing"
        className="text-sm text-muted-foreground hover:underline"
      >
        ← Xem bảng giá
      </Link>

      <div className="mt-6 rounded-2xl border bg-card p-8 shadow-sm">
        <p className="text-sm font-medium text-primary">
          {planInterest === 'ENTERPRISE' ? 'Gói Enterprise' : 'Tư vấn gói dịch vụ'}
        </p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">
          Liên hệ tư vấn
        </h1>
        <p className="mt-3 text-sm text-muted-foreground">
          Để lại thông tin, đội ngũ KiteHub sẽ liên hệ tư vấn gói phù hợp với quy
          mô trung tâm của bạn trong vòng 24 giờ. Bạn cũng có thể email trực tiếp
          tới{' '}
          <a href="mailto:support@kitehub.me" className="underline">
            support@kitehub.me
          </a>
          .
        </p>

        <div className="mt-8">
          <ContactForm planInterest={planInterest} />
        </div>
      </div>
    </div>
  );
}
