import Image from 'next/image';
import { Card, CardContent } from '@/components/ui/card';
import type { SlotData, SlotItem } from '@/lib/template/slots';

const DEFAULT_TESTIMONIALS: SlotItem[] = [
  {
    title: 'Trần Hương',
    description: 'Giám đốc Trung tâm Anh Ngữ Hương',
    icon: 'TH',
    image: undefined,
    items: ['KiteClass giúp tôi quản lý hơn 200 học viên một cách dễ dàng. Tính năng điểm danh tự động và LMS rất tiện lợi!'],
  },
  {
    title: 'Nguyễn Phương',
    description: 'Chủ Trung tâm English Plus',
    icon: 'NP',
    items: ['Báo cáo tài chính chi tiết giúp tôi nắm rõ doanh thu và chi phí. Thanh toán online cũng rất tiện cho phụ huynh.'],
  },
  {
    title: 'Lê Minh',
    description: 'Giám đốc Anh Văn Tương Lai',
    icon: 'LM',
    items: ['Hệ thống rất dễ sử dụng, nhân viên chỉ cần 1 ngày là làm quen. Support team cũng rất nhiệt tình!'],
  },
];

interface TestimonialsSectionProps {
  slots?: SlotData;
}

export function TestimonialsSection({ slots }: TestimonialsSectionProps) {
  const testimonials = (slots?.testimonials as SlotItem[] | undefined) || DEFAULT_TESTIMONIALS;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-12">Khách hàng nói gì về chúng tôi</h2>
        <div className="grid md:grid-cols-3 gap-8">
          {testimonials.map((t) => (
            <Card key={t.title}>
              <CardContent className="pt-6">
                <p className="text-sm mb-4">&ldquo;{t.items?.[0] || ''}&rdquo;</p>
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full flex items-center justify-center bg-theme-primary/10">
                    {t.image ? (
                      <Image src={t.image} alt={t.title} width={40} height={40} unoptimized className="h-10 w-10 rounded-full object-cover" />
                    ) : (
                      <span className="font-semibold text-sm">{t.icon || t.title.split(' ').map(w => w[0]).join('')}</span>
                    )}
                  </div>
                  <div>
                    <p className="font-semibold text-sm">{t.title}</p>
                    <p className="text-xs text-muted-foreground">{t.description}</p>
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
