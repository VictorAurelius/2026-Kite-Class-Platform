/**
 * About page with company information, values, and statistics.
 *
 * Above-the-fold (Hero / Mission+Vision / Stats / Core Values) renders
 * synchronously in this server component. The "Why KiteClass / Timeline /
 * CTA" sections are lazy-loaded via `next/dynamic` (`ssr: true`) so SEO
 * crawlers still see the full content but the bundler emits those
 * sections as a separate chunk.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/about`.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import { Metadata } from 'next';
import nextDynamic from 'next/dynamic';
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
  Shield,
  Zap,
  Globe,
  BookOpen,
  GraduationCap,
  Building2,
  CheckCircle2,
} from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';

const AboutDetails = nextDynamic(
  () =>
    import('@/components/public/about-details').then((m) => ({
      default: m.AboutDetails,
    })),
  {
    ssr: true,
    loading: () => (
      <div className="mb-16 space-y-6">
        <Skeleton className="h-9 w-1/3 mx-auto" />
        <div className="grid md:grid-cols-2 gap-6">
          <Skeleton className="h-64 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      </div>
    ),
  },
);

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

      {/* Below-the-fold sections — lazy loaded */}
      <AboutDetails />
    </div>
  );
}
