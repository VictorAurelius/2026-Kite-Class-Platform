/**
 * Contact page.
 *
 * Server component shell — static contact info renders immediately for
 * SEO/FCP, while the form (react-hook-form + zod) is loaded as a
 * separate chunk via `next/dynamic` with SSR enabled so the HTML still
 * includes the form skeleton on first paint.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/contact`.
 *
 * @author KiteClass Team
 * @since 3.12.0
 */

import nextDynamic from 'next/dynamic';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Mail, Phone, MapPin } from 'lucide-react';

const ContactForm = nextDynamic(
  () =>
    import('@/components/public/contact-form').then((m) => ({
      default: m.ContactForm,
    })),
  {
    // ssr: true keeps the form rendered server-side so the HTML payload
    // is complete on first paint; the bundler still emits the form as a
    // separate chunk to keep page-level First Load JS small.
    ssr: true,
    loading: () => (
      <Card>
        <CardContent className="space-y-4 pt-6">
          <Skeleton className="h-6 w-32" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-10 w-full" />
        </CardContent>
      </Card>
    ),
  },
);

export default function ContactPage() {
  return (
    <div className="container mx-auto px-4 py-12">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-4xl font-bold mb-4 text-center">Liên hệ</h1>
        <p className="text-muted-foreground mb-12 text-center">
          Liên hệ với chúng tôi để được tư vấn và hỗ trợ
        </p>

        <div className="grid md:grid-cols-2 gap-8">
          {/* Contact Form (lazy-loaded) */}
          <ContactForm />

          {/* Contact Info — static, server-rendered */}
          <div className="space-y-6">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                  <Mail className="h-6 w-6 text-primary mt-1" />
                  <div>
                    <h3 className="font-semibold mb-1">Email</h3>
                    <p className="text-sm text-muted-foreground">
                      {process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'support@kiteclass.com'}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                  <Phone className="h-6 w-6 text-primary mt-1" />
                  <div>
                    <h3 className="font-semibold mb-1">Hotline</h3>
                    <p className="text-sm text-muted-foreground">
                      {process.env.NEXT_PUBLIC_CONTACT_PHONE || '1900 xxxx'} (8:00 -
                      18:00)
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                  <MapPin className="h-6 w-6 text-primary mt-1" />
                  <div>
                    <h3 className="font-semibold mb-1">Địa chỉ</h3>
                    <p className="text-sm text-muted-foreground">Hà Nội, Việt Nam</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
