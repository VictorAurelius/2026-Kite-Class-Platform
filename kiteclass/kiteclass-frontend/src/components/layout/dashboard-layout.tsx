/**
 * Dashboard layout with sidebar, header, and content area.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import type { ReactNode } from 'react';
import { Sidebar } from './sidebar';
import { Header } from './header';
import { Footer } from './footer';

interface DashboardLayoutProps {
  children: ReactNode;
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar */}
      <Sidebar />

      {/* Main Content */}
      <div className="flex flex-1 flex-col overflow-hidden pl-64">
        {/* Header */}
        <Header />

        {/* Content */}
        <main className="flex-1 overflow-y-auto bg-muted/50">
          <div className="container mx-auto p-6">{children}</div>
        </main>

        {/* Footer */}
        <Footer />
      </div>
    </div>
  );
}
