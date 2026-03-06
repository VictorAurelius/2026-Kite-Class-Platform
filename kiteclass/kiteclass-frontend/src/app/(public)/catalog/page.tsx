/**
 * Public course catalog page.
 * Shows all published courses with filtering and pagination.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import { Metadata } from 'next';
import { Suspense } from 'react';
import { CourseCard } from '@/components/landing/CourseCard';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Search, Filter } from 'lucide-react';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Khóa học',
  description: 'Danh sách các khóa học tiếng Anh chất lượng cao',
};

// Sample courses data (will be replaced with API call)
const sampleCourses = [
  {
    id: 1,
    name: 'Tiếng Anh Giao Tiếp Cơ Bản',
    code: 'ENG-101',
    description:
      'Khóa học tiếng Anh giao tiếp dành cho người mới bắt đầu. Tập trung vào kỹ năng nghe - nói trong các tình huống hàng ngày.',
    level: 'Beginner',
    durationWeeks: 12,
    price: 3000000,
    maxStudents: 20,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 2,
    name: 'IELTS 6.5+ Intensive',
    code: 'IELTS-65',
    description:
      'Khóa học luyện thi IELTS chuyên sâu nhằm đạt mục tiêu 6.5 điểm trở lên. Giáo viên 8.0 IELTS hướng dẫn.',
    level: 'Intermediate',
    durationWeeks: 16,
    price: 5000000,
    maxStudents: 15,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 3,
    name: 'Business English Advanced',
    code: 'BIZ-301',
    description:
      'Tiếng Anh thương mại nâng cao cho doanh nhân và nhân viên văn phòng. Tập trung presentations, negotiations, emails.',
    level: 'Advanced',
    durationWeeks: 10,
    price: 4500000,
    maxStudents: 12,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 4,
    name: 'Tiếng Anh Cho Trẻ Em 6-10 tuổi',
    code: 'KIDS-101',
    description:
      'Khóa học tiếng Anh vui nhộn dành cho trẻ em. Phương pháp giảng dạy qua trò chơi và hoạt động tương tác.',
    level: 'Beginner',
    durationWeeks: 20,
    price: 2500000,
    maxStudents: 15,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 5,
    name: 'TOEIC 700+ Preparation',
    code: 'TOEIC-700',
    description:
      'Khóa học luyện thi TOEIC với mục tiêu đạt 700 điểm trở lên. Chiến thuật làm bài và luyện đề chi tiết.',
    level: 'Intermediate',
    durationWeeks: 12,
    price: 3500000,
    maxStudents: 20,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 6,
    name: 'Phát Âm Chuẩn Mỹ',
    code: 'PRON-201',
    description:
      'Khóa học chuyên sâu về phát âm tiếng Anh chuẩn Mỹ. Sửa lỗi phát âm, luyện intonation và stress.',
    level: 'Intermediate',
    durationWeeks: 8,
    price: 2000000,
    maxStudents: 10,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
];

export default function CoursesPage() {
  return (
    <div className="container mx-auto px-4 py-12">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-4">Khóa học</h1>
        <p className="text-muted-foreground text-lg">
          Khám phá các khóa học tiếng Anh chất lượng cao phù hợp với mọi trình
          độ
        </p>
      </div>

      {/* Filters */}
      <div className="mb-8 flex flex-col sm:flex-row gap-4">
        {/* Search */}
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Tìm kiếm khóa học..."
            className="pl-10"
            disabled
          />
        </div>

        {/* Level Filter */}
        <Select disabled>
          <SelectTrigger className="w-full sm:w-[180px]">
            <Filter className="h-4 w-4 mr-2" />
            <SelectValue placeholder="Trình độ" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Tất cả</SelectItem>
            <SelectItem value="Beginner">Cơ bản</SelectItem>
            <SelectItem value="Intermediate">Trung cấp</SelectItem>
            <SelectItem value="Advanced">Nâng cao</SelectItem>
          </SelectContent>
        </Select>

        {/* Sort */}
        <Select disabled>
          <SelectTrigger className="w-full sm:w-[180px]">
            <SelectValue placeholder="Sắp xếp" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="newest">Mới nhất</SelectItem>
            <SelectItem value="price-low">Giá thấp đến cao</SelectItem>
            <SelectItem value="price-high">Giá cao đến thấp</SelectItem>
            <SelectItem value="popular">Phổ biến</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Note about sample data */}
      <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <p className="text-sm text-blue-800">
          <strong>Lưu ý:</strong> Hiện đang hiển thị dữ liệu mẫu. Tính năng
          tìm kiếm và lọc sẽ được kích hoạt khi kết nối với API backend.
        </p>
      </div>

      {/* Course Grid */}
      <Suspense
        fallback={
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <div
                key={i}
                className="h-96 bg-muted animate-pulse rounded-lg"
              />
            ))}
          </div>
        }
      >
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
          {sampleCourses.map((course) => (
            <CourseCard key={course.id} course={course} />
          ))}
        </div>
      </Suspense>

      {/* Pagination */}
      <div className="flex items-center justify-center gap-2">
        <Button variant="outline" disabled>
          Trang trước
        </Button>
        <div className="flex items-center gap-2">
          <Button variant="default" size="sm">
            1
          </Button>
          <Button variant="outline" size="sm" disabled>
            2
          </Button>
          <Button variant="outline" size="sm" disabled>
            3
          </Button>
        </div>
        <Button variant="outline" disabled>
          Trang sau
        </Button>
      </div>

      {/* CTA Section */}
      <div className="mt-16 text-center bg-primary/5 rounded-lg p-8">
        <h2 className="text-2xl font-bold mb-4">
          Không tìm thấy khóa học phù hợp?
        </h2>
        <p className="text-muted-foreground mb-6">
          Liên hệ với chúng tôi để được tư vấn khóa học phù hợp nhất với nhu
          cầu của bạn
        </p>
        <div className="flex gap-4 justify-center">
          <Button asChild size="lg">
            <Link href="/contact">Liên hệ tư vấn</Link>
          </Button>
          <Button asChild variant="outline" size="lg">
            <Link href="/register">Đăng ký ngay</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
