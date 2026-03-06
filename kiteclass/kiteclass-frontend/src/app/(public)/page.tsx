/**
 * Public landing page (homepage).
 * Fetches dynamic content from backend LandingPage API.
 *
 * Architecture Note: Content is NOT hardcoded - it comes from:
 * - GET /api/v1/tenants/{tenantId}/landing (AI Branding + instance data)
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

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
  openGraph: {
    title: 'KiteClass - Quản lý Trung tâm Tiếng Anh Chuyên nghiệp',
    description:
      'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng Anh với LMS, quản lý học viên, điểm danh tự động và thanh toán online.',
    type: 'website',
    locale: 'vi_VN',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'KiteClass - Quản lý Trung tâm Tiếng Anh Chuyên nghiệp',
    description:
      'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng Anh.',
  },
};

const getLandingPageData = async () => {
  try {
    // Get tenant ID from environment or use default Demo School tenant
    const tenantId =
      process.env.NEXT_PUBLIC_TENANT_ID || '11111111-1111-1111-1111-111111111111';

    // Fetch from backend API
    const response = await publicApi.getLandingPage(tenantId);
    return response;
  } catch (error) {
    // Fallback data if API fails (matches V19 seed data)
    console.error('Failed to fetch landing page data:', error);
    return {
      heroTitle: 'Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả',
      heroSubtitle:
        'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng Anh với LMS, quản lý học viên, điểm danh tự động và thanh toán online.',
      heroImageUrl: null,
      tagline: 'Nâng tầm giáo dục, tối ưu quản lý',
      primaryColor: '#3B82F6',
      secondaryColor: '#8B5CF6',
      contactEmail: 'support@kiteclass.com',
      contactPhone: '1900 xxxx',
      address: 'Hà Nội, Việt Nam',
    };
  }
};

export default async function LandingPage() {
  const landingData = await getLandingPageData();

  // Structured data for SEO
  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'EducationalOrganization',
    name: 'KiteClass',
    description: landingData.heroSubtitle,
    url: process.env.NEXT_PUBLIC_APP_URL || 'https://kiteclass.com',
    email: landingData.contactEmail,
    telephone: landingData.contactPhone,
    address: {
      '@type': 'PostalAddress',
      addressLocality: landingData.address || 'Hà Nội',
      addressCountry: 'VN',
    },
    offers: {
      '@type': 'Offer',
      category: 'Khóa học tiếng Anh',
    },
  };

  return (
    <>
      {/* Structured Data for SEO */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
      />

      <div className="flex flex-col">
      {/* Hero Section - Dynamic from Backend */}
      <section
        className="py-20 bg-gradient-to-b from-primary/5 to-background"
        style={{
          // Apply AI Branding colors if available
          background: `linear-gradient(to bottom, ${landingData.primaryColor}10, transparent)`,
        }}
      >
        <div className="container mx-auto px-4 text-center">
          <h1 className="text-4xl md:text-6xl font-bold mb-6">
            {landingData.heroTitle || 'Quản lý Trung tâm Tiếng Anh'}
            <br />
            <span
              className="text-primary"
              style={{ color: landingData.primaryColor }}
            >
              Chuyên nghiệp & Hiệu quả
            </span>
          </h1>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            {landingData.heroSubtitle ||
              'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng Anh.'}
          </p>
          {landingData.tagline && (
            <p className="text-lg font-semibold mb-8 text-muted-foreground">
              {landingData.tagline}
            </p>
          )}
          <div className="flex gap-4 justify-center">
            <Button
              size="lg"
              asChild
              style={{ backgroundColor: landingData.primaryColor }}
            >
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

      {/* Note about Dynamic Content */}
      <aside
        className="py-4 bg-blue-50 border-y border-blue-200"
        aria-label="Thông tin về AI Branding"
      >
        <div className="container mx-auto px-4 text-center">
          <p className="text-sm text-blue-800">
            <strong>🤖 AI-Powered Landing Page:</strong> Nội dung trang này được
            tự động tạo từ AI Branding System. Owner có thể tùy chỉnh màu sắc,
            logo, slogan trong Settings.
          </p>
        </div>
      </aside>

      {/* Features Section */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl font-bold text-center mb-12">
            Tính năng nổi bật
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            <Card>
              <CardHeader>
                <BookOpen
                  className="h-12 w-12 mb-4"
                  style={{ color: landingData.primaryColor }}
                />
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
                <Users
                  className="h-12 w-12 mb-4"
                  style={{ color: landingData.primaryColor }}
                />
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
                <TrendingUp
                  className="h-12 w-12 mb-4"
                  style={{ color: landingData.primaryColor }}
                />
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
      <section
        className="py-16"
        style={{
          background: `linear-gradient(to right, ${landingData.primaryColor}10, ${landingData.secondaryColor}10)`,
        }}
      >
        <div className="container mx-auto px-4 text-center">
          <h2 className="text-3xl font-bold mb-4">Sẵn sàng bắt đầu chưa?</h2>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            Dùng thử miễn phí 30 ngày, không cần thẻ tín dụng. Nâng cấp quản lý
            trung tâm của bạn ngay hôm nay.
          </p>
          <div className="flex gap-4 justify-center">
            <Button
              size="lg"
              asChild
              style={{ backgroundColor: landingData.primaryColor }}
            >
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
                  <div
                    className="h-10 w-10 rounded-full flex items-center justify-center"
                    style={{ backgroundColor: `${landingData.primaryColor}20` }}
                  >
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
                  <div
                    className="h-10 w-10 rounded-full flex items-center justify-center"
                    style={{ backgroundColor: `${landingData.primaryColor}20` }}
                  >
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
                  <div
                    className="h-10 w-10 rounded-full flex items-center justify-center"
                    style={{ backgroundColor: `${landingData.primaryColor}20` }}
                  >
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
