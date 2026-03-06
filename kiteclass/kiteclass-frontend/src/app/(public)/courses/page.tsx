import { Metadata } from 'next';
import { Button } from '@/components/ui/button';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Khóa học',
  description: 'Danh sách các khóa học tiếng Anh chất lượng cao',
};

export default function CoursesPage() {
  return (
    <div className="container mx-auto px-4 py-12">
      <h1 className="text-4xl font-bold mb-4">Khóa học</h1>
      <p className="text-muted-foreground mb-8">
        Khám phá các khóa học tiếng Anh chất lượng cao tại trung tâm của chúng
        tôi.
      </p>

      <div className="text-center py-12">
        <p className="text-lg text-muted-foreground mb-6">
          Trang này đang được phát triển. Vui lòng quay lại sau!
        </p>
        <Button asChild>
          <Link href="/">Quay về trang chủ</Link>
        </Button>
      </div>
    </div>
  );
}
