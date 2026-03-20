import { Card, CardContent } from '@/components/ui/card';

const DEFAULT_TESTIMONIALS = [
  {
    quote: 'KiteClass giúp tôi quản lý hơn 200 học viên một cách dễ dàng. Tính năng điểm danh tự động và LMS rất tiện lợi!',
    name: 'Trần Hương',
    role: 'Giám đốc Trung tâm Anh Ngữ Hương',
    initials: 'TH',
  },
  {
    quote: 'Báo cáo tài chính chi tiết giúp tôi nắm rõ doanh thu và chi phí. Thanh toán online cũng rất tiện cho phụ huynh.',
    name: 'Nguyễn Phương',
    role: 'Chủ Trung tâm English Plus',
    initials: 'NP',
  },
  {
    quote: 'Hệ thống rất dễ sử dụng, nhân viên chỉ cần 1 ngày là làm quen. Support team cũng rất nhiệt tình!',
    name: 'Lê Minh',
    role: 'Giám đốc Anh Văn Tương Lai',
    initials: 'LM',
  },
];

export function TestimonialsSection() {
  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-12">Khách hàng nói gì về chúng tôi</h2>
        <div className="grid md:grid-cols-3 gap-8">
          {DEFAULT_TESTIMONIALS.map((t) => (
            <Card key={t.name}>
              <CardContent className="pt-6">
                <p className="text-sm mb-4">&ldquo;{t.quote}&rdquo;</p>
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full flex items-center justify-center bg-theme-primary/10">
                    <span className="font-semibold">{t.initials}</span>
                  </div>
                  <div>
                    <p className="font-semibold text-sm">{t.name}</p>
                    <p className="text-xs text-muted-foreground">{t.role}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
