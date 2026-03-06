import { Metadata } from 'next';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  ArrowRight,
  BookOpen,
  Users,
  TrendingUp,
  CheckCircle2,
} from 'lucide-react';

export const metadata: Metadata = {
  title: 'Trang chủ',
  description:
    'Hệ thống quản lý trung tâm tiếng Anh toàn diện với LMS, quản lý học viên, điểm danh tự động.',
};

export default function LandingPage() {
  return (
    <div className="flex flex-col">
      {/* Hero Section */}
      <section className="py-20 bg-gradient-to-b from-primary/5 to-background">
        <div className="container mx-auto px-4 text-center">
          <h1 className="text-4xl md:text-6xl font-bold mb-6">
            Quản lý Trung tâm Tiếng Anh
            <br />
            <span className="text-primary">Chuyên nghiệp & Hiệu quả</span>
          </h1>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng
            Anh với LMS, quản lý học viên, điểm danh tự động và thanh toán
            online.
          </p>
          <div className="flex gap-4 justify-center">
            <Button size="lg" asChild>
              <Link href="/register">
                Dùng thử miễn phí <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </Button>
            <Button size="lg" variant="outline" asChild>
              <Link href="/courses">Xem khóa học</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl font-bold text-center mb-12">
            Tính năng nổi bật
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            <Card>
              <CardHeader>
                <BookOpen className="h-12 w-12 text-primary mb-4" />
                <CardTitle>Hệ thống LMS</CardTitle>
                <CardDescription>
                  Quản lý bài giảng, tài liệu và theo dõi tiến độ học tập của
                  học viên
                </CardDescription>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2 text-sm">
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Upload video bài giảng</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Theo dõi tiến độ học</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Bài tập & quiz tự động</span>
                  </li>
                </ul>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <Users className="h-12 w-12 text-primary mb-4" />
                <CardTitle>Quản lý Học viên</CardTitle>
                <CardDescription>
                  Theo dõi toàn bộ thông tin học viên, điểm danh và kết quả học
                  tập
                </CardDescription>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2 text-sm">
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Hồ sơ học viên chi tiết</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Điểm danh tự động</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Báo cáo kết quả học tập</span>
                  </li>
                </ul>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <TrendingUp className="h-12 w-12 text-primary mb-4" />
                <CardTitle>Thanh toán & Báo cáo</CardTitle>
                <CardDescription>
                  Quản lý học phí, thanh toán online và báo cáo tài chính chi
                  tiết
                </CardDescription>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2 text-sm">
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Thanh toán VietQR</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Quản lý công nợ</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                    <span>Báo cáo doanh thu</span>
                  </li>
                </ul>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 bg-primary/5">
        <div className="container mx-auto px-4 text-center">
          <h2 className="text-3xl font-bold mb-4">
            Sẵn sàng bắt đầu chưa?
          </h2>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            Dùng thử miễn phí 30 ngày, không cần thẻ tín dụng. Nâng cấp quản lý
            trung tâm của bạn ngay hôm nay.
          </p>
          <div className="flex gap-4 justify-center">
            <Button size="lg" asChild>
              <Link href="/register">
                Đăng ký ngay <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </Button>
            <Button size="lg" variant="outline" asChild>
              <Link href="/contact">Liên hệ tư vấn</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl font-bold text-center mb-12">
            Khách hàng nói gì về chúng tôi
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            <Card>
              <CardContent className="pt-6">
                <p className="text-sm mb-4">
                  "KiteClass giúp tôi quản lý hơn 200 học viên một cách dễ dàng.
                  Tính năng điểm danh tự động và LMS rất tiện lợi!"
                </p>
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <span className="font-semibold">TH</span>
                  </div>
                  <div>
                    <p className="font-semibold text-sm">Trần Hương</p>
                    <p className="text-xs text-muted-foreground">
                      Giám đốc Trung tâm Anh Ngữ Hương
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <p className="text-sm mb-4">
                  "Báo cáo tài chính chi tiết giúp tôi nắm rõ doanh thu và chi
                  phí. Thanh toán online cũng rất tiện cho phụ huynh."
                </p>
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <span className="font-semibold">NP</span>
                  </div>
                  <div>
                    <p className="font-semibold text-sm">Nguyễn Phương</p>
                    <p className="text-xs text-muted-foreground">
                      Chủ Trung tâm English Plus
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <p className="text-sm mb-4">
                  "Hệ thống rất dễ sử dụng, nhân viên chỉ cần 1 ngày là làm
                  quen. Support team cũng rất nhiệt tình!"
                </p>
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <span className="font-semibold">LM</span>
                  </div>
                  <div>
                    <p className="font-semibold text-sm">Lê Minh</p>
                    <p className="text-xs text-muted-foreground">
                      Giám đốc Anh Văn Tương Lai
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>
    </div>
  );
}
