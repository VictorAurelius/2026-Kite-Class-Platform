/**
 * Certificates section — displays recognized certifications.
 * Uses default demo data until CMS slot data is available.
 *
 * @since 2026-04-04
 */

import { Badge } from '@/components/ui/badge';
import type { SlotData, SlotItem } from '@/lib/template/slots';

const DEFAULT_CERTIFICATES: SlotItem[] = [
  {
    title: 'IELTS',
    description: 'International English Language Testing System',
    icon: '🎓',
    items: ['Band 4.0 – 9.0', 'Được công nhận toàn cầu', 'Lộ trình 3–12 tháng'],
  },
  {
    title: 'TOEIC',
    description: 'Test of English for International Communication',
    icon: '💼',
    items: ['450 – 990 điểm', 'Định hướng công việc', 'Lộ trình 2–6 tháng'],
  },
  {
    title: 'Cambridge',
    description: 'Cambridge English Qualifications (A2–C2)',
    icon: '📚',
    items: ['KET / PET / FCE / CAE', 'Chứng chỉ trọn đời', 'Lộ trình 6–18 tháng'],
  },
  {
    title: 'VSTEP',
    description: 'Vietnamese Standardized Test of English Proficiency',
    icon: '🇻🇳',
    items: ['Bậc 1–5', 'Chuẩn Bộ GD&ĐT', 'Lộ trình 1–4 tháng'],
  },
];

interface CertificatesSectionProps {
  slots?: SlotData;
}

export function CertificatesSection({ slots }: CertificatesSectionProps) {
  const dataDriven = Array.isArray(slots?.certificates) && (slots!.certificates as SlotItem[]).length > 0;
  const certificates = dataDriven ? (slots!.certificates as SlotItem[]) : DEFAULT_CERTIFICATES;
  // Khi data-driven (programs từ tenant) → tiêu đề "Chương trình giảng dạy" trung lập;
  // fallback giữ "Chứng chỉ" cho template tổ chức dùng default.
  const heading = dataDriven ? 'Chương trình giảng dạy' : 'Chứng chỉ';
  const subtitle = dataDriven
    ? 'Các chương trình và lộ trình học được thiết kế theo từng nhóm học viên'
    : 'Luyện thi và đạt chứng chỉ tiếng Anh được công nhận trong nước và quốc tế';

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-4">{heading}</h2>
        <p className="text-center text-muted-foreground mb-12 max-w-2xl mx-auto">
          {subtitle}
        </p>
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {certificates.map((cert) => (
            <div
              key={cert.title}
              className="rounded-xl border bg-card p-6 text-center shadow-md transition-all hover:-translate-y-1 hover:shadow-xl"
            >
              <div className="text-4xl mb-3">{cert.icon}</div>
              <h3 className="font-bold text-xl mb-1">{cert.title}</h3>
              <p className="text-xs text-muted-foreground mb-4">{cert.description}</p>
              {cert.items && (
                <ul className="space-y-1">
                  {cert.items.map((item) => (
                    <li key={item}>
                      <Badge variant="secondary" className="text-xs font-normal w-full justify-center">
                        {item}
                      </Badge>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
