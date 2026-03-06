/**
 * Course card component for public course catalog.
 * Displays course summary with CTA to view details.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Clock, Users, BookOpen } from 'lucide-react';
import type { Course } from '@/types/course';

interface CourseCardProps {
  course: Course;
}

export function CourseCard({ course }: CourseCardProps) {
  // Format price
  const formatPrice = (price: number | null) => {
    if (price === null || price === 0) return 'Miễn phí';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  // Get level badge color
  const getLevelColor = (level: string) => {
    switch (level) {
      case 'Beginner':
        return 'bg-green-500';
      case 'Intermediate':
        return 'bg-blue-500';
      case 'Advanced':
        return 'bg-purple-500';
      default:
        return 'bg-gray-500';
    }
  };

  // Get level label in Vietnamese
  const getLevelLabel = (level: string) => {
    switch (level) {
      case 'Beginner':
        return 'Cơ bản';
      case 'Intermediate':
        return 'Trung cấp';
      case 'Advanced':
        return 'Nâng cao';
      default:
        return level;
    }
  };

  return (
    <Card className="h-full flex flex-col hover:shadow-lg transition-shadow">
      <CardHeader>
        <div className="flex items-start justify-between gap-2 mb-2">
          {course.level && (
            <Badge className={getLevelColor(course.level)}>
              {getLevelLabel(course.level)}
            </Badge>
          )}
          {course.status === 'PUBLISHED' && (
            <Badge variant="outline" className="text-green-600 border-green-600">
              Đang tuyển sinh
            </Badge>
          )}
        </div>
        <CardTitle className="line-clamp-2">{course.name}</CardTitle>
        <CardDescription className="line-clamp-3">
          {course.description || 'Khóa học chất lượng cao'}
        </CardDescription>
      </CardHeader>

      <CardContent className="flex-1">
        <div className="space-y-2 text-sm text-muted-foreground">
          {course.durationWeeks && (
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4" />
              <span>{course.durationWeeks} tuần</span>
            </div>
          )}
          {course.maxStudents && (
            <div className="flex items-center gap-2">
              <Users className="h-4 w-4" />
              <span>Tối đa {course.maxStudents} học viên</span>
            </div>
          )}
          <div className="flex items-center gap-2">
            <BookOpen className="h-4 w-4" />
            <span>Mã khóa học: {course.code}</span>
          </div>
        </div>

        {course.price !== null && (
          <div className="mt-4 pt-4 border-t">
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-bold text-primary">
                {formatPrice(course.price)}
              </span>
              {course.durationWeeks && (
                <span className="text-sm text-muted-foreground">
                  / {course.durationWeeks} tuần
                </span>
              )}
            </div>
          </div>
        )}
      </CardContent>

      <CardFooter>
        <Button asChild className="w-full">
          <Link href={`/courses/${course.id}`}>Xem chi tiết</Link>
        </Button>
      </CardFooter>
    </Card>
  );
}
