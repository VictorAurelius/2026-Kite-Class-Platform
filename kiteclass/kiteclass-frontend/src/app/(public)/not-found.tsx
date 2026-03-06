/**
 * 404 Not Found page for public routes.
 * Displayed when a public page is not found.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import Link from 'next/link';
import { FileQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] p-8">
      <FileQuestion className="h-16 w-16 text-muted-foreground mb-4" />
      <h2 className="text-2xl font-bold mb-2">Không tìm thấy trang</h2>
      <p className="text-muted-foreground mb-6 text-center max-w-md">
        Trang bạn đang tìm kiếm không tồn tại hoặc đã bị xóa.
      </p>
      <div className="flex gap-4">
        <Button asChild>
          <Link href="/">Về trang chủ</Link>
        </Button>
        <Button variant="outline" asChild>
          <Link href="/courses">Xem khóa học</Link>
        </Button>
      </div>
    </div>
  );
}
