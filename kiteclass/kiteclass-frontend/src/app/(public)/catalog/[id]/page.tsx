/**
 * Public course detail page.
 * Shows course details with LMS preview for guest users.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import {
  Clock,
  Users,
  BookOpen,
  GraduationCap,
  CheckCircle2,
  ArrowRight,
  Lock,
} from 'lucide-react';
import { publicApi } from '@/lib/api/public';
import type { Course } from '@/types/course';

interface CourseModule {
  id: number;
  title: string;
  orderNumber: number;
  lessons: {
    id: number;
    title: string;
    isTrial: boolean;
  }[];
}

interface CourseDetailData extends Course {
  modules: CourseModule[];
  objectivesArray: string[];
}

/**
 * Fetch course data from backend API.
 */
const getCourseData = async (id: string): Promise<CourseDetailData | null> => {
  try {
    const courseId = parseInt(id, 10);
    if (isNaN(courseId)) {
      return null;
    }

    // Fetch course details
    const course = await publicApi.getCourseById(courseId);

    // Check if course is published (public access only)
    if (course.status !== 'PUBLISHED') {
      return null;
    }

    // Fetch course structure (modules & lessons)
    let modules: CourseModule[] = [];
    try {
      const structure = await publicApi.getCourseStructure(courseId);
      modules = structure || [];
    } catch (error) {
      console.warn('Failed to fetch course structure:', error);
      // Continue without modules (better than failing entirely)
    }

    // Parse objectives string to array
    let objectivesArray: string[] = [];
    if (course.objectives) {
      try {
        // Try parsing as JSON array first
        objectivesArray = JSON.parse(course.objectives);
      } catch {
        // Fallback: split by newlines or semicolons
        objectivesArray = course.objectives
          .split(/[\n;]/)
          .map((obj) => obj.trim())
          .filter((obj) => obj.length > 0);
      }
    }

    // Fallback objectives if none provided
    if (objectivesArray.length === 0) {
      objectivesArray = [
        'Nắm vững kiến thức cơ bản của khóa học',
        'Áp dụng kỹ năng vào thực tế',
        'Hoàn thành bài tập và đánh giá',
      ];
    }

    return {
      ...course,
      modules,
      objectivesArray,
    };
  } catch (error) {
    console.error('Failed to fetch course data:', error);
    return null;
  }
};

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const course = await getCourseData(id);

  if (!course) {
    return {
      title: 'Không tìm thấy khóa học',
    };
  }

  return {
    title: course.name,
    description: course.description,
  };
}

export default async function CourseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const course = await getCourseData(id);

  if (!course) {
    notFound();
  }

  // Format price
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  // Get level label
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
    <div className="container mx-auto px-4 py-12">
      {/* Breadcrumb */}
      <div className="mb-6 text-sm text-muted-foreground">
        <Link href="/" className="hover:text-primary">
          Trang chủ
        </Link>
        {' / '}
        <Link href="/catalog" className="hover:text-primary">
          Khóa học
        </Link>
        {' / '}
        <span className="text-foreground">{course.name}</span>
      </div>

      <div className="grid lg:grid-cols-3 gap-8">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-8">
          {/* Course Header */}
          <div>
            <div className="flex items-center gap-3 mb-4">
              <Badge>{getLevelLabel(course.level)}</Badge>
              <Badge variant="outline">Mã: {course.code}</Badge>
            </div>
            <h1 className="text-4xl font-bold mb-4">{course.name}</h1>
            <p className="text-xl text-muted-foreground">{course.description}</p>
          </div>

          <Separator />

          {/* Course Info */}
          <div>
            <h2 className="text-2xl font-bold mb-4">Thông tin khóa học</h2>
            <div className="grid md:grid-cols-3 gap-4">
              <Card>
                <CardContent className="pt-6">
                  <div className="flex items-center gap-3">
                    <Clock className="h-5 w-5 text-primary" />
                    <div>
                      <p className="text-sm text-muted-foreground">Thời lượng</p>
                      <p className="font-semibold">{course.durationWeeks || 12} tuần</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="pt-6">
                  <div className="flex items-center gap-3">
                    <Users className="h-5 w-5 text-primary" />
                    <div>
                      <p className="text-sm text-muted-foreground">Sĩ số</p>
                      <p className="font-semibold">
                        {course.maxStudents ? `Tối đa ${course.maxStudents}` : 'Liên hệ'}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="pt-6">
                  <div className="flex items-center gap-3">
                    <GraduationCap className="h-5 w-5 text-primary" />
                    <div>
                      <p className="text-sm text-muted-foreground">Trình độ</p>
                      <p className="font-semibold">
                        {course.level ? getLevelLabel(course.level) : 'Tất cả trình độ'}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>

          <Separator />

          {/* Learning Objectives */}
          <div>
            <h2 className="text-2xl font-bold mb-4">Mục tiêu học tập</h2>
            <ul className="space-y-3">
              {course.objectivesArray.map((objective, index) => (
                <li key={index} className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>{objective}</span>
                </li>
              ))}
            </ul>
          </div>

          <Separator />

          {/* Course Modules (LMS Preview) */}
          <div>
            <h2 className="text-2xl font-bold mb-4">
              Nội dung khóa học
            </h2>

            {course.modules && course.modules.length > 0 ? (
              <>
                <p className="text-muted-foreground mb-6">
                  <Lock className="inline h-4 w-4 mr-1" />
                  Bài học có biểu tượng khóa yêu cầu đăng ký khóa học để truy cập
                </p>

                <div className="space-y-4">
                  {course.modules.map((module) => (
                    <Card key={module.id}>
                      <CardHeader>
                        <CardTitle className="text-lg">{module.title}</CardTitle>
                        <CardDescription>
                          {module.lessons?.length || 0} bài học
                        </CardDescription>
                      </CardHeader>
                      <CardContent>
                        <ul className="space-y-2">
                          {module.lessons?.map((lesson) => (
                            <li
                              key={lesson.id}
                              className="flex items-center justify-between p-2 hover:bg-muted rounded"
                            >
                              <div className="flex items-center gap-3">
                                <BookOpen className="h-4 w-4 text-muted-foreground" />
                                <span className="text-sm">{lesson.title}</span>
                              </div>
                              {lesson.isTrial ? (
                                <Badge variant="secondary">Học thử</Badge>
                              ) : (
                                <Lock className="h-4 w-4 text-muted-foreground" />
                              )}
                            </li>
                          ))}
                        </ul>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </>
            ) : (
              <Card>
                <CardContent className="pt-6 text-center">
                  <p className="text-muted-foreground mb-2">
                    Nội dung chi tiết sẽ được cập nhật sau khi đăng ký
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {course.syllabus || 'Chương trình học đầy đủ sẽ được giảng viên cung cấp'}
                  </p>
                </CardContent>
              </Card>
            )}
          </div>
        </div>

        {/* Sidebar - Enrollment CTA */}
        <div className="lg:col-span-1">
          <Card className="sticky top-4">
            <CardHeader>
              {course.price && course.price > 0 ? (
                <>
                  <CardTitle className="text-3xl text-primary">
                    {formatPrice(course.price)}
                  </CardTitle>
                  <CardDescription>
                    Học phí cho {course.durationWeeks || 12} tuần
                  </CardDescription>
                </>
              ) : (
                <>
                  <CardTitle className="text-3xl text-primary">
                    Liên hệ
                  </CardTitle>
                  <CardDescription>
                    Liên hệ để biết học phí chi tiết
                  </CardDescription>
                </>
              )}
            </CardHeader>
            <CardContent className="space-y-4">
              <Button className="w-full" size="lg" asChild>
                <Link href="/register">
                  Đăng ký ngay
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Link>
              </Button>

              <Button
                variant="outline"
                className="w-full"
                size="lg"
                asChild
              >
                <Link href="/contact">Liên hệ tư vấn</Link>
              </Button>

              <Separator />

              <div className="space-y-3 text-sm">
                <h3 className="font-semibold">Khóa học bao gồm:</h3>
                <ul className="space-y-2">
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>{course.durationWeeks || 12} tuần học</span>
                  </li>
                  {course.totalSessions && (
                    <li className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                      <span>{course.totalSessions} buổi học</span>
                    </li>
                  )}
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Tài liệu học tập đầy đủ</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Bài tập và quiz</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Chứng chỉ hoàn thành</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Hỗ trợ từ giảng viên</span>
                  </li>
                </ul>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
