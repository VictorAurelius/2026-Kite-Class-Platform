import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { BookOpen, Users, TrendingUp, CheckCircle2 } from 'lucide-react';

const DEFAULT_FEATURES = [
  {
    icon: BookOpen,
    title: 'Hệ thống LMS',
    description: 'Quản lý bài giảng, tài liệu và theo dõi tiến độ học tập',
    items: ['Upload video bài giảng', 'Theo dõi tiến độ học', 'Bài tập & quiz tự động'],
  },
  {
    icon: Users,
    title: 'Quản lý Học viên',
    description: 'Theo dõi toàn bộ thông tin học viên, điểm danh và kết quả',
    items: ['Hồ sơ học viên chi tiết', 'Điểm danh tự động', 'Báo cáo kết quả học tập'],
  },
  {
    icon: TrendingUp,
    title: 'Thanh toán & Báo cáo',
    description: 'Quản lý học phí, thanh toán online và báo cáo tài chính',
    items: ['Thanh toán VietQR', 'Quản lý công nợ', 'Báo cáo doanh thu'],
  },
];

export function FeaturesSection() {
  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-12">Tính năng nổi bật</h2>
        <div className="grid md:grid-cols-3 gap-8">
          {DEFAULT_FEATURES.map((feature) => (
            <Card key={feature.title}>
              <CardHeader>
                <feature.icon className="h-12 w-12 mb-4 text-theme-primary" />
                <CardTitle>{feature.title}</CardTitle>
                <CardDescription>{feature.description}</CardDescription>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2 text-sm">
                  {feature.items.map((item) => (
                    <li key={item} className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
