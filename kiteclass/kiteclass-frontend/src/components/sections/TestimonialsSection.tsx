import Image from 'next/image';
import { Card, CardContent } from '@/components/ui/card';
import type { SlotData, SlotItem } from '@/lib/template/slots';

// Anti-fabrication (GAP-958): testimonials are real social proof — NEVER invent
// parent/student quotes or results. Render ONLY tenant-provided reviews; hide the
// whole section when none configured. page.tsx only emits slots.testimonials when
// the backend returns a non-empty array.

interface TestimonialsSectionProps {
  slots?: SlotData;
}

export function TestimonialsSection({ slots }: TestimonialsSectionProps) {
  const testimonials = slots?.testimonials as SlotItem[] | undefined;
  if (!testimonials || testimonials.length === 0) return null;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-12">Phụ huynh &amp; học viên nói gì</h2>
        {/* Mobile: carousel scroll-snap ngang; Desktop: grid 3 cột */}
        <div className="flex snap-x snap-mandatory gap-6 overflow-x-auto pb-4 md:grid md:grid-cols-3 md:gap-8 md:overflow-visible md:pb-0 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {testimonials.map((t) => (
            <Card
              key={t.title}
              className="w-[85%] shrink-0 snap-center rounded-xl shadow-md transition-shadow hover:shadow-xl md:w-auto md:shrink"
            >
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
