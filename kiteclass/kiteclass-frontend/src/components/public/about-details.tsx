/**
 * About-page below-the-fold sections — lazy-loaded.
 *
 * Pulls the heavier "Why KiteClass / Timeline / CTA" sections out of
 * the about route's initial chunk. Rendered server-side (`ssr: true`)
 * so SEO crawlers still see all content, but the JSX + lucide icons
 * for these sections live in their own chunk.
 *
 * GAP-236 Sub-PR B Agent A — code-splitting for `/about`.
 *
 * @author KiteClass Team
 */

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  TrendingUp,
  Award,
  Users,
  BookOpen,
  CheckCircle2,
} from 'lucide-react';

export function AboutDetails() {
  return (
    <>
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

      {/* Timeline */}
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
    </>
  );
}
