/**
 * Public course catalog page with search and filtering.
 *
 * @author KiteClass Team
 * @since 3.12.0
 */

'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
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
import { Search, Filter, Loader2 } from 'lucide-react';
import { publicApi } from '@/lib/api/public';

export default function CoursesPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [level, setLevel] = useState<string>('all');
  const [sortBy, setSortBy] = useState<string>('newest');
  const [page, setPage] = useState(0);
  const pageSize = 9;

  const { data, isLoading, error } = useQuery({
    queryKey: ['publicCourses', { searchTerm, level, sortBy, page }],
    queryFn: () =>
      publicApi.getCourses({
        query: searchTerm || undefined,
        page,
        size: pageSize,
        sort: sortBy === 'newest' ? 'createdAt,desc' : sortBy === 'name' ? 'name,asc' : sortBy === 'price-low' ? 'price,asc' : 'price,desc',
      }),
    retry: 1,
  });

  const totalPages = data ? Math.ceil(data.totalElements / pageSize) : 0;

  return (
    <div className="container mx-auto px-4 py-12">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-4">Khóa học</h1>
        <p className="text-muted-foreground text-lg">
          Khám phá các khóa học chất lượng cao phù hợp với mọi trình độ
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
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
          />
        </div>

        {/* Level Filter */}
        <Select
          value={level}
          onValueChange={(value) => {
            setLevel(value);
            setPage(0);
          }}
        >
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
        <Select
          value={sortBy}
          onValueChange={(value) => {
            setSortBy(value);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-full sm:w-[180px]">
            <SelectValue placeholder="Sắp xếp" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="newest">Mới nhất</SelectItem>
            <SelectItem value="name">Theo tên</SelectItem>
            <SelectItem value="price-low">Giá thấp đến cao</SelectItem>
            <SelectItem value="price-high">Giá cao đến thấp</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <span className="ml-2">Đang tải khóa học...</span>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="text-center py-12">
          <p className="text-destructive mb-4">Không thể tải danh sách khóa học</p>
          <Button onClick={() => window.location.reload()}>Thử lại</Button>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && data && data.content.length === 0 && (
        <div className="text-center py-12">
          <p className="text-muted-foreground mb-4">
            Không tìm thấy khóa học nào phù hợp
          </p>
          <Button
            variant="outline"
            onClick={() => {
              setSearchTerm('');
              setLevel('all');
              setPage(0);
            }}
          >
            Xóa bộ lọc
          </Button>
        </div>
      )}

      {/* Course Grid */}
      {!isLoading && !error && data && data.content.length > 0 && (
        <>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            {data.content.map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <Button
                variant="outline"
                onClick={() => setPage(page - 1)}
                disabled={page === 0}
              >
                Trang trước
              </Button>

              <div className="flex items-center gap-2">
                {[...Array(Math.min(totalPages, 5))].map((_, i) => {
                  const pageNum =
                    totalPages <= 5
                      ? i
                      : page < 3
                        ? i
                        : page > totalPages - 4
                          ? totalPages - 5 + i
                          : page - 2 + i;

                  return (
                    <Button
                      key={pageNum}
                      variant={page === pageNum ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => setPage(pageNum)}
                    >
                      {pageNum + 1}
                    </Button>
                  );
                })}
              </div>

              <Button
                variant="outline"
                onClick={() => setPage(page + 1)}
                disabled={page >= totalPages - 1}
              >
                Trang sau
              </Button>
            </div>
          )}
        </>
      )}

      {/* Result Count */}
      {!isLoading && data && (
        <div className="mt-6 text-center text-sm text-muted-foreground">
          Hiển thị {data.content.length} / {data.totalElements} khóa học
        </div>
      )}

      {/* CTA Section */}
      <div className="mt-16 text-center bg-primary/5 rounded-lg p-8">
        <h2 className="text-2xl font-bold mb-4">
          Không tìm thấy khóa học phù hợp?
        </h2>
        <p className="text-muted-foreground mb-6">
          Liên hệ với chúng tôi để được tư vấn khóa học phù hợp nhất với nhu cầu
          của bạn
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
