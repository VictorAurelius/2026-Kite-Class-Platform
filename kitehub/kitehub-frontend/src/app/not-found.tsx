/**
 * Root-level 404 Not Found page (Next.js App Router convention).
 *
 * Triggered automatically when:
 * - A page calls `notFound()` (e.g. blog/[slug] when slug invalid).
 * - Visitor hits an unknown URL not matched by any route.
 *
 * Vietnamese-first, themed via Shadcn Button + CSS variables (dark-mode aware).
 *
 * @author KiteHub Team
 * @since GAP-136
 */

import Link from 'next/link';
import { FileQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <main className="flex min-h-[70vh] flex-col items-center justify-center bg-background px-4 py-16 text-foreground">
      <div className="flex flex-col items-center text-center">
        <FileQuestion
          className="mb-6 h-16 w-16 text-muted-foreground"
          aria-hidden="true"
        />
        <p className="mb-2 text-sm font-medium uppercase tracking-wider text-primary">
          Lỗi 404
        </p>
        <h1 className="mb-3 text-3xl font-bold tracking-tight sm:text-4xl">
          Không tìm thấy trang
        </h1>
        <p className="mb-8 max-w-md text-base text-muted-foreground">
          Trang bạn đang tìm kiếm không tồn tại, đã bị di chuyển hoặc đường dẫn
          sai. Vui lòng kiểm tra lại URL hoặc quay về trang chủ KiteHub.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Button asChild size="lg">
            <Link href="/">Về trang chủ</Link>
          </Button>
          <Button asChild size="lg" variant="outline">
            <Link href="/pricing">Xem bảng giá</Link>
          </Button>
        </div>
      </div>
    </main>
  );
}
