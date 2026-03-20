import type { Metadata } from 'next';
import { Suspense } from 'react';
import { Inter } from 'next/font/google';
import { ReactQueryProvider } from '@/providers/ReactQueryProvider';
import { ThemeProvider } from '@/contexts/ThemeContext';
import { ThemeReceiver } from '@/components/theme/ThemeReceiver';
import { ThemePreviewPanel } from '@/components/theme/ThemePreviewPanel';
import { Toaster } from '@/components/ui/toaster';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'KiteClass - Quản Lý Trung Tâm Tiếng Anh',
  description: 'Hệ thống quản lý trung tâm tiếng Anh toàn diện',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={inter.className}>
        <ThemeProvider>
          <ThemeReceiver />
          <Suspense>
            <ThemePreviewPanel />
          </Suspense>
          <ReactQueryProvider>
            {children}
            <Toaster />
          </ReactQueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
