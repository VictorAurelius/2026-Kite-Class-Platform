import Image from 'next/image';
import { Card, CardContent } from '@/components/ui/card';
import type { SlotData, SlotItem } from '@/lib/template/slots';

// Testimonials from parents + students (the audience of an independent teacher /
// small center landing page), including class + result for credibility.
const DEFAULT_TESTIMONIALS: SlotItem[] = [
  {
    title: 'Chị Phạm Thị Mai',
    description: 'Phụ huynh em Trần Thị Hồng · Lớp Anh ngữ 5A1',
    icon: 'PM',
    image: undefined,
    items: ['Sau 6 tháng, con tôi tự tin giao tiếp hẳn và đạt 8.5 môn Tiếng Anh cuối kỳ. Cô giáo còn cập nhật tiến độ qua Zalo mỗi tuần.'],
  },
  {
    title: 'Em Nguyễn Văn An',
    description: 'Học viên · Lớp IELTS 7.0 Buổi tối · đạt IELTS 7.5',
    icon: 'NA',
    items: ['Lộ trình rõ ràng, luyện đề sát thực tế. Em từ 6.0 lên 7.5 chỉ sau một khóa, vượt mục tiêu ban đầu.'],
  },
  {
    title: 'Chị Lê Thị Quỳnh',
    description: 'Phụ huynh em Lê Văn Quang · Lớp Toán tư duy 9B',
    icon: 'LQ',
    items: ['Lớp nhỏ nên con được kèm sát. Điểm tổng kết của con tăng từ 7 lên 9.2, gia đình rất yên tâm.'],
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
        <h2 className="text-3xl font-bold text-center mb-12">Phụ huynh &amp; học viên nói gì</h2>
        {/* Mobile: carousel scroll-snap ngang; Desktop: grid 3 cột */}
        <div className="flex snap-x snap-mandatory gap-6 overflow-x-auto pb-4 md:grid md:grid-cols-3 md:gap-8 md:overflow-visible md:pb-0 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {testimonials.map((t) => (
            <Card key={t.title} className="w-[85%] shrink-0 snap-center md:w-auto md:shrink">
              <CardContent className="pt-6">
                <div className="mb-3 text-sm text-amber-500" aria-label="Đánh giá 5 trên 5 sao">★★★★★</div>
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
