/**
 * Public per-tenant contact page (GAP-274 phase-2 — ported to kiteclass-public kit).
 *
 * Layout: page-head + 2 cột (form trái · thông tin liên hệ phải). The form
 * (validation + submit) is a client chunk loaded via next/dynamic; the aside is
 * server-rendered from the tenant landing payload.
 *
 * Anti-fabrication (GAP-958): phone / email / address surface ONLY when the tenant
 * configured them — no `1900 xxxx` / `support@kiteclass.com` placeholders. The Zalo
 * button renders only when landing.zaloUrl is present (kit spec). PDPL consent kept.
 *
 * @author KiteClass Team
 */

import nextDynamic from 'next/dynamic';
import Link from 'next/link';
import { Mail, Phone, MapPin, MessageCircle } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { getTenantLanding, landingStr } from '@/lib/api/tenant-landing';

const ContactForm = nextDynamic(
  () => import('@/components/public/contact-form').then((m) => ({ default: m.ContactForm })),
  {
    ssr: true,
    loading: () => (
      <div className="space-y-4 rounded-2xl border bg-white p-7 shadow-sm">
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-11 w-full" />
        <Skeleton className="h-11 w-full" />
        <Skeleton className="h-11 w-full" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-11 w-full" />
      </div>
    ),
  }
);

export default async function ContactPage() {
  const landing = await getTenantLanding();
  const centerName = landingStr(landing, 'centerName') || landingStr(landing, 'heroTitle') || 'chúng tôi';
  const zaloUrl = landingStr(landing, 'zaloUrl');
  const phone = landingStr(landing, 'contactPhone');
  const email = landingStr(landing, 'contactEmail');
  const address = landingStr(landing, 'address');
  const hasContactInfo = Boolean(phone || email || address);

  return (
    <div>
      {/* Page head */}
      <div className="bg-gradient-to-br from-theme-primary to-theme-secondary text-white">
        <div className="container mx-auto px-4 py-12">
          <span className="mb-3 inline-flex rounded-full bg-white/15 px-3 py-1.5 text-xs font-extrabold uppercase tracking-wider">
            Liên hệ &amp; đăng ký
          </span>
          <h1 className="text-3xl font-extrabold md:text-4xl">Để lại lời nhắn, {centerName} phản hồi trong ngày</h1>
          <p className="mt-2 max-w-2xl text-white/90">
            Đăng ký học thử miễn phí hoặc hỏi tư vấn lộ trình cho con.
          </p>
        </div>
      </div>

      <div className="container mx-auto grid gap-8 px-4 py-12 md:grid-cols-[1.1fr,0.9fr]">
        {/* Form */}
        <ContactForm />

        {/* Aside — server-rendered from landing payload */}
        <aside className="space-y-5" aria-label="Thông tin liên hệ">
          {/* Zalo — only when tenant configured zaloUrl (kit spec) */}
          {zaloUrl && (
            <div className="rounded-2xl border bg-white p-6 shadow-sm">
              <h3 className="mb-3 font-bold">Nhắn nhanh qua Zalo</h3>
              <a
                href={zaloUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-[rgb(0_104_255)] py-3 font-bold text-white"
              >
                <MessageCircle className="h-5 w-5" aria-hidden="true" />
                Nhắn Zalo cho {centerName}
              </a>
              <p className="mt-2 text-center text-xs text-muted-foreground">
                Phản hồi nhanh nhất — thường trong vài giờ.
              </p>
            </div>
          )}

          {/* Contact info — anti-fabrication: render only what the tenant set */}
          {hasContactInfo ? (
            <div className="rounded-2xl border bg-white p-6 shadow-sm">
              <h3 className="mb-4 font-bold">Thông tin liên hệ</h3>
              <ul className="space-y-4 text-sm">
                {phone && (
                  <li className="flex items-start gap-3">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-theme-primary/10 text-theme-primary">
                      <Phone className="h-4 w-4" aria-hidden="true" />
                    </span>
                    <div>
                      <a href={`tel:${phone}`} className="block font-semibold hover:text-theme-primary">
                        {phone}
                      </a>
                      <small className="text-muted-foreground">Gọi / nhắn tin trong giờ làm việc</small>
                    </div>
                  </li>
                )}
                {address && (
                  <li className="flex items-start gap-3">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-theme-primary/10 text-theme-primary">
                      <MapPin className="h-4 w-4" aria-hidden="true" />
                    </span>
                    <div className="font-semibold">{address}</div>
                  </li>
                )}
                {email && (
                  <li className="flex items-start gap-3">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-theme-primary/10 text-theme-primary">
                      <Mail className="h-4 w-4" aria-hidden="true" />
                    </span>
                    <div>
                      <a href={`mailto:${email}`} className="block font-semibold hover:text-theme-primary">
                        {email}
                      </a>
                      <small className="text-muted-foreground">Email tư vấn</small>
                    </div>
                  </li>
                )}
              </ul>
            </div>
          ) : (
            <div className="rounded-2xl border bg-white p-6 text-sm text-muted-foreground shadow-sm">
              Hãy để lại lời nhắn qua{' '}
              <Link href="#main-content" className="font-semibold text-theme-primary">
                biểu mẫu bên cạnh
              </Link>{' '}
              — chúng tôi sẽ liên hệ lại sớm nhất.
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
