'use client';

import Link from 'next/link';
import type { ReactNode } from 'react';
import { KiteLogo } from '@/components/brand/KiteLogo';

export function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between">
          <Link href="/" className="transition-opacity hover:opacity-80">
            <KiteLogo size="md" />
          </Link>
          <nav className="flex items-center gap-6">
            <Link
              href="/pricing"
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              Bảng giá
            </Link>
            <Link
              href="/login"
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              Đăng nhập
            </Link>
            <Link
              href="/register"
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-all hover:bg-primary/90 hover:shadow-md"
            >
              Dùng thử miễn phí
            </Link>
          </nav>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1">{children}</main>

      {/* Footer */}
      <footer className="border-t bg-muted/30">
        <div className="container py-12">
          <div className="grid gap-8 md:grid-cols-4">
            {/* Brand */}
            <div className="space-y-4">
              <KiteLogo size="md" />
              <p className="text-sm text-muted-foreground">
                Nền tảng quản lý trung tâm giáo dục thông minh.
                Giúp bạn bay cao cùng học viên.
              </p>
            </div>

            {/* Product */}
            <div>
              <h4 className="mb-4 text-sm font-semibold">Sản phẩm</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><Link href="/pricing" className="hover:text-foreground transition-colors">Bảng giá</Link></li>
                <li><Link href="/register" className="hover:text-foreground transition-colors">Đăng ký</Link></li>
                <li><Link href="/login" className="hover:text-foreground transition-colors">Đăng nhập</Link></li>
              </ul>
            </div>

            {/* Features */}
            <div>
              <h4 className="mb-4 text-sm font-semibold">Tính năng</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>Quản lý học viên</li>
                <li>Quản lý khóa học</li>
                <li>Điểm danh tự động</li>
                <li>Thanh toán online</li>
              </ul>
            </div>

            {/* Contact */}
            <div>
              <h4 className="mb-4 text-sm font-semibold">Liên hệ</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>support@kitehub.vn</li>
                <li>1900 xxxx xx</li>
              </ul>
            </div>
          </div>

          <div className="mt-12 border-t pt-8 text-center text-sm text-muted-foreground">
            &copy; {new Date().getFullYear()} KiteHub. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}
