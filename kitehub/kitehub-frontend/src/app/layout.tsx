import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { ReactQueryProvider } from '@/providers/ReactQueryProvider';
import { ThemeProvider } from '@/providers/ThemeProvider';
import { ConsentGatedAnalytics } from '@/components/legal/ConsentGatedAnalytics';
import { Toaster } from 'sonner';
import { SITE_URL } from '@/lib/site-config';

const inter = Inter({ subsets: ['latin', 'vietnamese'] });

// GA4 measurement ID (format: G-XXXXXXXXXX). When unset, <ConsentGatedAnalytics>
// renders nothing. When set, GA script tag is mounted ONLY after the user opts
// into analytics via <ConsentBanner /> per PDPL Art 11 + Decree 13/2023 Art 4
// (GAP-558 Wave 83 Bucket E).
const GA_ID = process.env.NEXT_PUBLIC_GA_ID;

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: 'KiteHub - Nền tảng quản lý trung tâm giáo dục',
    template: '%s | KiteHub',
  },
  description: 'Tạo website và quản lý trung tâm giáo dục chuyên nghiệp. AI tự động tạo thương hiệu. Dùng thử miễn phí 14 ngày.',
  alternates: {
    canonical: SITE_URL,
  },
  openGraph: {
    type: 'website',
    locale: 'vi_VN',
    url: SITE_URL,
    siteName: 'KiteHub',
    title: 'KiteHub - Nền tảng quản lý trung tâm giáo dục',
    description: 'Tạo website và quản lý trung tâm giáo dục chuyên nghiệp.',
    images: [{ url: '/og-image.png', width: 1200, height: 630 }],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'KiteHub - Nền tảng quản lý trung tâm giáo dục',
    description: 'Tạo website và quản lý trung tâm giáo dục chuyên nghiệp.',
    images: ['/og-image.png'],
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={inter.className}>
        <ThemeProvider>
          <ReactQueryProvider>
            {children}
            <Toaster position="top-right" richColors />
          </ReactQueryProvider>
        </ThemeProvider>
        <ConsentGatedAnalytics gaId={GA_ID} />
      </body>
    </html>
  );
}
