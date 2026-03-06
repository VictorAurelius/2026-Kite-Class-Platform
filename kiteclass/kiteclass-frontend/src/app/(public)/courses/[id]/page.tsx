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

// Sample course data (will be replaced with API call)
const getCourseData = async (id: string) => {
  const courses = {
    '1': {
      id: 1,
      name: 'Tiếng Anh Giao Tiếp Cơ Bản',
      code: 'ENG-101',
      description:
        'Khóa học tiếng Anh giao tiếp dành cho người mới bắt đầu. Tập trung vào kỹ năng nghe - nói trong các tình huống hàng ngày như: tự giới thiệu, hỏi đường, đặt hàng, mua sắm, v.v.',
      level: 'Beginner',
      durationWeeks: 12,
      price: 3000000,
      maxStudents: 20,
      status: 'PUBLISHED',
      objectives: [
        'Giao tiếp cơ bản trong các tình huống hàng ngày',
        'Phát âm chuẩn và tự tin khi nói',
        'Nghe hiểu đoạn hội thoại ngắn',
        'Viết email và tin nhắn đơn giản',
      ],
      syllabus: 'Chương trình học bao gồm 12 chủ đề chính từ cơ bản đến nâng cao.',
      modules: [
        {
          id: 1,
          title: 'Module 1: Giới thiệu bản thân',
          orderNumber: 1,
          lessons: [
            { id: 1, title: 'Lesson 1: Greetings', isTrial: true },
            { id: 2, title: 'Lesson 2: Self Introduction', isTrial: true },
            { id: 3, title: 'Lesson 3: Talking about hobbies', isTrial: false },
          ],
        },
        {
          id: 2,
          title: 'Module 2: Tình huống hàng ngày',
          orderNumber: 2,
          lessons: [
            { id: 4, title: 'Lesson 4: At the restaurant', isTrial: false },
            { id: 5, title: 'Lesson 5: Shopping', isTrial: false },
            { id: 6, title: 'Lesson 6: Asking for directions', isTrial: false },
          ],
        },
        {
          id: 3,
          title: 'Module 3: Ngữ pháp cơ bản',
          orderNumber: 3,
          lessons: [
            { id: 7, title: 'Lesson 7: Present Simple', isTrial: false },
            { id: 8, title: 'Lesson 8: Present Continuous', isTrial: false },
            { id: 9, title: 'Lesson 9: Past Simple', isTrial: false },
          ],
        },
      ],
    },
  };

  return courses[id as keyof typeof courses] || null;
};

export async function generateMetadata({
  params,
}: {
  params: { id: string };
}): Promise<Metadata> {
  const course = await getCourseData(params.id);

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
  params: { id: string };
}) {
  const course = await getCourseData(params.id);

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
        <Link href="/courses" className="hover:text-primary">
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
                      <p className="font-semibold">{course.durationWeeks} tuần</p>
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
                      <p className="font-semibold">Tối đa {course.maxStudents}</p>
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
                      <p className="font-semibold">{getLevelLabel(course.level)}</p>
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
              {course.objectives.map((objective, index) => (
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
                      {module.lessons.length} bài học
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ul className="space-y-2">
                      {module.lessons.map((lesson) => (
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
          </div>
        </div>

        {/* Sidebar - Enrollment CTA */}
        <div className="lg:col-span-1">
          <Card className="sticky top-4">
            <CardHeader>
              <CardTitle className="text-3xl text-primary">
                {formatPrice(course.price)}
              </CardTitle>
              <CardDescription>
                Học phí cho {course.durationWeeks} tuần
              </CardDescription>
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
                    <span>{course.durationWeeks} tuần học trực tuyến</span>
                  </li>
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
