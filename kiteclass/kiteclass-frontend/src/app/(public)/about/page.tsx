/**
 * About page with company information, values, and statistics.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import { Metadata } from 'next';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Target,
  Heart,
  Users,
  TrendingUp,
  Award,
  Shield,
  Zap,
  Globe,
  BookOpen,
  GraduationCap,
  Building2,
  CheckCircle2,
} from 'lucide-react';

export const metadata: Metadata = {
  title: 'Giới thiệu',
  description:
    'Giới thiệu về KiteClass - Nền tảng quản lý trung tâm tiếng Anh toàn diện, giúp tối ưu hóa vận hành và nâng cao chất lượng giảng dạy.',
  keywords: [
    'giới thiệu KiteClass',
    'về chúng tôi',
    'quản lý trung tâm',
    'nền tảng giáo dục',
  ],
};

export default function AboutPage() {
  return (
    <div className="container mx-auto px-4 py-12">
      {/* Hero Section */}
      <div className="mb-16 text-center">
        <Badge className="mb-4" variant="outline">
          Về chúng tôi
        </Badge>
        <h1 className="text-4xl font-bold mb-4 md:text-5xl">
          Giới thiệu về KiteClass
        </h1>
        <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
          Nền tảng quản lý trung tâm tiếng Anh toàn diện, được thiết kế để giúp
          các trung tâm tối ưu hóa vận hành và nâng cao chất lượng giảng dạy.
        </p>
      </div>

      {/* Mission & Vision */}
      <div className="grid md:grid-cols-2 gap-8 mb-16">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-3 mb-2">
              <Target className="h-8 w-8 text-primary" />
              <CardTitle className="text-2xl">Sứ mệnh</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground leading-relaxed">
              Chúng tôi tin rằng công nghệ có thể giúp các trung tâm tiếng Anh
              vận hành hiệu quả hơn, để giáo viên có thể tập trung vào việc
              giảng dạy và học viên có trải nghiệm học tập tốt nhất. Sứ mệnh
              của KiteClass là mang đến giải pháp quản lý toàn diện, dễ sử dụng
              và phù hợp với mọi quy mô trung tâm.
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-3 mb-2">
              <Heart className="h-8 w-8 text-primary" />
              <CardTitle className="text-2xl">Tầm nhìn</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground leading-relaxed">
              Trở thành nền tảng quản lý trung tâm tiếng Anh hàng đầu tại Việt
              Nam, giúp hàng nghìn trung tâm nâng cao chất lượng giảng dạy và
              trải nghiệm học viên. Chúng tôi hướng tới một hệ sinh thái giáo
              dục số hóa, kết nối giáo viên, học viên và phụ huynh một cách
              liền mạch.
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Statistics */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-8">
          KiteClass trong con số
        </h2>
        <div className="grid md:grid-cols-4 gap-6">
          <Card className="text-center">
            <CardContent className="pt-6">
              <Building2 className="h-12 w-12 text-primary mx-auto mb-3" />
              <div className="text-4xl font-bold mb-2">100+</div>
              <p className="text-sm text-muted-foreground">Trung tâm tin dùng</p>
            </CardContent>
          </Card>

          <Card className="text-center">
            <CardContent className="pt-6">
              <Users className="h-12 w-12 text-primary mx-auto mb-3" />
              <div className="text-4xl font-bold mb-2">10,000+</div>
              <p className="text-sm text-muted-foreground">Học viên đang học</p>
            </CardContent>
          </Card>

          <Card className="text-center">
            <CardContent className="pt-6">
              <GraduationCap className="h-12 w-12 text-primary mx-auto mb-3" />
              <div className="text-4xl font-bold mb-2">500+</div>
              <p className="text-sm text-muted-foreground">Giáo viên</p>
            </CardContent>
          </Card>

          <Card className="text-center">
            <CardContent className="pt-6">
              <BookOpen className="h-12 w-12 text-primary mx-auto mb-3" />
              <div className="text-4xl font-bold mb-2">1,000+</div>
              <p className="text-sm text-muted-foreground">Khóa học</p>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Core Values */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-8">Giá trị cốt lõi</h2>
        <div className="grid md:grid-cols-3 gap-6">
          <Card>
            <CardHeader>
              <Shield className="h-10 w-10 text-primary mb-3" />
              <CardTitle>Đáng tin cậy</CardTitle>
              <CardDescription>
                Dữ liệu được mã hóa và bảo mật tuyệt đối
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Mã hóa SSL/TLS end-to-end</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Backup tự động hàng ngày</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Tuân thủ GDPR & quy định Việt Nam</span>
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <Zap className="h-10 w-10 text-primary mb-3" />
              <CardTitle>Hiệu quả</CardTitle>
              <CardDescription>
                Tối ưu hóa quy trình vận hành trung tâm
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Tự động hóa điểm danh & báo cáo</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Giảm 70% thời gian quản lý</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Tích hợp thanh toán online</span>
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <Globe className="h-10 w-10 text-primary mb-3" />
              <CardTitle>Dễ sử dụng</CardTitle>
              <CardDescription>
                Giao diện thân thiện, học 1 ngày là quen
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Giao diện tiếng Việt 100%</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Hỗ trợ 24/7 qua Zalo/Hotline</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span>Video hướng dẫn chi tiết</span>
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Key Features */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-8">
          Tại sao chọn KiteClass?
        </h2>
        <div className="grid md:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <Award className="h-8 w-8 text-primary mb-2" />
              <CardTitle>Quản lý toàn diện</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground mb-4">
                Tất cả trong một nền tảng: từ quản lý học viên, giáo viên, khóa
                học, lớp học, điểm danh, điểm số đến thanh toán và báo cáo.
              </p>
              <ul className="space-y-2 text-sm">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Quản lý học viên & giáo viên</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Lịch học & điểm danh tự động</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Hệ thống LMS tích hợp</span>
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <TrendingUp className="h-8 w-8 text-primary mb-2" />
              <CardTitle>Báo cáo & Phân tích</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground mb-4">
                Thống kê chi tiết về doanh thu, tỷ lệ tham dự, kết quả học tập
                giúp bạn ra quyết định chính xác.
              </p>
              <ul className="space-y-2 text-sm">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Báo cáo doanh thu theo thời gian</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Phân tích tỷ lệ điểm danh</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Thống kê kết quả học tập</span>
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <Users className="h-8 w-8 text-primary mb-2" />
              <CardTitle>Multi-tenant SaaS</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground mb-4">
                Mỗi trung tâm có instance riêng với dữ liệu hoàn toàn độc lập,
                tùy chỉnh branding theo thương hiệu.
              </p>
              <ul className="space-y-2 text-sm">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Dữ liệu riêng biệt 100%</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Custom logo & màu sắc</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Tên miền riêng (optional)</span>
                </li>
              </ul>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <BookOpen className="h-8 w-8 text-primary mb-2" />
              <CardTitle>LMS & E-Learning</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground mb-4">
                Hệ thống quản lý học tập (LMS) tích hợp sẵn với video bài giảng,
                bài tập, quiz và theo dõi tiến độ.
              </p>
              <ul className="space-y-2 text-sm">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Upload video bài giảng</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Bài tập & quiz tự động chấm</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                  <span>Theo dõi tiến độ học tập</span>
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Timeline (Optional - can be made dynamic later) */}
      <div className="mb-16">
        <h2 className="text-3xl font-bold text-center mb-8">Hành trình phát triển</h2>
        <div className="max-w-3xl mx-auto">
          <div className="space-y-8">
            <div className="flex gap-6">
              <div className="flex flex-col items-center">
                <div className="h-10 w-10 rounded-full bg-primary/20 flex items-center justify-center">
                  <div className="h-5 w-5 rounded-full bg-primary"></div>
                </div>
                <div className="h-full w-px bg-border"></div>
              </div>
              <div className="pb-8">
                <div className="font-semibold mb-1">Q1 2024</div>
                <div className="text-sm text-muted-foreground">
                  Ra mắt phiên bản Beta với các tính năng cơ bản: quản lý học viên,
                  giáo viên, lớp học
                </div>
              </div>
            </div>

            <div className="flex gap-6">
              <div className="flex flex-col items-center">
                <div className="h-10 w-10 rounded-full bg-primary/20 flex items-center justify-center">
                  <div className="h-5 w-5 rounded-full bg-primary"></div>
                </div>
                <div className="h-full w-px bg-border"></div>
              </div>
              <div className="pb-8">
                <div className="font-semibold mb-1">Q3 2024</div>
                <div className="text-sm text-muted-foreground">
                  Tích hợp LMS và hệ thống thanh toán online (VietQR, MoMo, VNPay)
                </div>
              </div>
            </div>

            <div className="flex gap-6">
              <div className="flex flex-col items-center">
                <div className="h-10 w-10 rounded-full bg-primary/20 flex items-center justify-center">
                  <div className="h-5 w-5 rounded-full bg-primary"></div>
                </div>
                <div className="h-full w-px bg-border"></div>
              </div>
              <div className="pb-8">
                <div className="font-semibold mb-1">Q1 2025</div>
                <div className="text-sm text-muted-foreground">
                  Ra mắt AI Branding tự động tạo landing page và marketing materials
                </div>
              </div>
            </div>

            <div className="flex gap-6">
              <div className="flex flex-col items-center">
                <div className="h-10 w-10 rounded-full bg-primary/20 flex items-center justify-center">
                  <div className="h-5 w-5 rounded-full bg-primary"></div>
                </div>
              </div>
              <div className="pb-8">
                <div className="font-semibold mb-1">2026</div>
                <div className="text-sm text-muted-foreground">
                  Mở rộng sang Parent Portal và Advanced Analytics với AI/ML
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* CTA Section */}
      <Card className="bg-gradient-to-r from-primary/10 to-primary/5">
        <CardContent className="pt-8 pb-8 text-center">
          <h2 className="text-3xl font-bold mb-4">
            Sẵn sàng chuyển đổi số trung tâm của bạn?
          </h2>
          <p className="text-muted-foreground mb-6 max-w-2xl mx-auto">
            Tham gia cùng hàng trăm trung tâm đang tin dùng KiteClass để nâng cao
            chất lượng giảng dạy và trải nghiệm học viên.
          </p>
          <div className="flex gap-4 justify-center">
            <a href="/register">
              <button className="px-6 py-3 bg-primary text-primary-foreground rounded-md hover:bg-primary/90">
                Dùng thử miễn phí
              </button>
            </a>
            <a href="/contact">
              <button className="px-6 py-3 border border-input bg-background rounded-md hover:bg-accent">
                Liên hệ tư vấn
              </button>
            </a>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
