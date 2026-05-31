import type { Metadata, Viewport } from 'next';
import { Suspense } from 'react';
import { Be_Vietnam_Pro } from 'next/font/google';
import { ReactQueryProvider } from '@/providers/ReactQueryProvider';
import { NextThemesProvider } from '@/providers/NextThemesProvider';
import { ThemeProvider } from '@/contexts/ThemeContext';
import { ThemeReceiver } from '@/components/theme/ThemeReceiver';
import { ThemePreviewPanel } from '@/components/theme/ThemePreviewPanel';
import { Toaster } from '@/components/ui/toaster';
import { ServiceWorkerRegistrar } from '@/components/pwa/ServiceWorkerRegistrar';
import './globals.css';

const beVietnamPro = Be_Vietnam_Pro({
  subsets: ['latin', 'vietnamese'],
  weight: ['400', '500', '600', '700'],
  display: 'swap',
});

// Wave 49 Bucket 0 — PWA infra. Manifest + theme color + iOS standalone hints
// for the parent (GAP-267) and student (GAP-269) mobile personas.
export const metadata: Metadata = {
  title: 'KiteClass - Quản Lý Trung Tâm Tiếng Anh',
  description: 'Hệ thống quản lý trung tâm tiếng Anh toàn diện',
  manifest: '/manifest.json',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'default',
    title: 'KiteClass',
  },
};

export const viewport: Viewport = {
  themeColor: '#3b82f6',
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={beVietnamPro.className}>
        <NextThemesProvider>
        <ThemeProvider>
          <ThemeReceiver />
          <Suspense>
            <ThemePreviewPanel />
          </Suspense>
          <ReactQueryProvider>
            {children}
            <Toaster />
          </ReactQueryProvider>
          <ServiceWorkerRegistrar />
        </ThemeProvider>
        </NextThemesProvider>
      </body>
    </html>
  );
}
