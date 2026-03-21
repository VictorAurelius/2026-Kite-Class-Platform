import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { BookOpen, Users, TrendingUp, CheckCircle2, Award, Calendar, Star } from 'lucide-react';
import type { SlotData, SlotItem } from '@/lib/template/slots';

const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  book: BookOpen,
  users: Users,
  trending: TrendingUp,
  award: Award,
  calendar: Calendar,
  star: Star,
};

const DEFAULT_FEATURES: SlotItem[] = [
  {
    icon: 'book',
    title: 'Hệ thống LMS',
    description: 'Quản lý bài giảng, tài liệu và theo dõi tiến độ học tập',
    items: ['Upload video bài giảng', 'Theo dõi tiến độ học', 'Bài tập & quiz tự động'],
  },
  {
    icon: 'users',
    title: 'Quản lý Học viên',
    description: 'Theo dõi toàn bộ thông tin học viên, điểm danh và kết quả',
    items: ['Hồ sơ học viên chi tiết', 'Điểm danh tự động', 'Báo cáo kết quả học tập'],
  },
  {
    icon: 'trending',
    title: 'Thanh toán & Báo cáo',
    description: 'Quản lý học phí, thanh toán online và báo cáo tài chính',
    items: ['Thanh toán VietQR', 'Quản lý công nợ', 'Báo cáo doanh thu'],
  },
];

interface FeaturesSectionProps {
  slots?: SlotData;
}

export function FeaturesSection({ slots }: FeaturesSectionProps) {
  const features = (slots?.features as SlotItem[] | undefined) || DEFAULT_FEATURES;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-12">Tính năng nổi bật</h2>
        <div className="grid md:grid-cols-3 gap-8">
          {features.map((feature) => {
            const IconComponent = ICON_MAP[feature.icon || 'star'] || Star;
            return (
              <Card key={feature.title}>
                <CardHeader>
                  <IconComponent className="h-12 w-12 mb-4 text-theme-primary" />
                  <CardTitle>{feature.title}</CardTitle>
                  {feature.description && <CardDescription>{feature.description}</CardDescription>}
                </CardHeader>
                {feature.items && feature.items.length > 0 && (
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
                )}
              </Card>
            );
          })}
        </div>
      </div>
    </section>
  );
}
