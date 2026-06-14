/**
 * Dashboard layout with sidebar, header, and content area.
 * Desktop: fixed sidebar + content with pl-64.
 * Mobile: hidden sidebar, hamburger in header opens Sheet drawer.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useState, type ReactNode } from 'react';
import { Sidebar } from './sidebar';
import { Header } from './header';
import { Footer } from './footer';
import { SkipToContent } from '@/components/a11y/skip-to-content';

interface DashboardLayoutProps {
  children: ReactNode;
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Skip to main content (accessibility — WCAG 2.4.1) */}
      <SkipToContent />
      {/* Desktop sidebar — hidden on mobile via sidebar.tsx */}
      <Sidebar />

      {/* Main Content */}
      <div className="flex flex-1 flex-col overflow-hidden pl-0 md:pl-64">
        {/* Header — receives hamburger state */}
        <Header
          mobileSidebarOpen={mobileSidebarOpen}
          onMobileSidebarToggle={() => setMobileSidebarOpen((v) => !v)}
          onMobileSidebarClose={() => setMobileSidebarOpen(false)}
        />

        {/* Content */}
        <main id="main-content" role="main" className="flex-1 overflow-y-auto bg-muted/50">
          <div className="container mx-auto p-6">{children}</div>
        </main>

        {/* Footer */}
        <Footer />
      </div>
    </div>
  );
}
