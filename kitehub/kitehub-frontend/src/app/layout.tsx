import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { GoogleAnalytics } from '@next/third-parties/google';
import './globals.css';
import { ReactQueryProvider } from '@/providers/ReactQueryProvider';
import { ThemeProvider } from '@/providers/ThemeProvider';
import { Toaster } from 'sonner';

const inter = Inter({ subsets: ['latin', 'vietnamese'] });

// GA4 measurement ID (format: G-XXXXXXXXXX). When unset, <GoogleAnalytics> is skipped.
const GA_ID = process.env.NEXT_PUBLIC_GA_ID;

export const metadata: Metadata = {
  metadataBase: new URL('https://kitehub.vn'),
  title: {
    default: 'KiteHub - Nền tảng quản lý trung tâm giáo dục',
    template: '%s | KiteHub',
  },
  description: 'Tạo website và quản lý trung tâm giáo dục chuyên nghiệp. AI tự động tạo thương hiệu. Dùng thử miễn phí 14 ngày.',
  alternates: {
    canonical: 'https://kitehub.vn',
  },
  openGraph: {
    type: 'website',
    locale: 'vi_VN',
    url: 'https://kitehub.vn',
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
        {GA_ID ? <GoogleAnalytics gaId={GA_ID} /> : null}
      </body>
    </html>
  );
}
