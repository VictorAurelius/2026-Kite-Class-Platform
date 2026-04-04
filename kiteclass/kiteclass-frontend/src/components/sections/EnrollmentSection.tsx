/**
 * Enrollment section — shows registration process steps.
 * Uses default demo data until CMS slot data is available.
 *
 * @since 2026-04-04
 */

import { Button } from '@/components/ui/button';
import Link from 'next/link';
import type { SlotData, SlotItem } from '@/lib/template/slots';

const DEFAULT_STEPS: SlotItem[] = [
  {
    title: 'Đăng ký tài khoản',
    description: 'Tạo tài khoản học viên miễn phí trên hệ thống KiteClass',
    icon: '1',
  },
  {
    title: 'Kiểm tra đầu vào',
    description: 'Làm bài kiểm tra trình độ để xếp lớp phù hợp',
    icon: '2',
  },
  {
    title: 'Chọn khóa học',
    description: 'Tư vấn viên hỗ trợ chọn khóa học phù hợp với mục tiêu',
    icon: '3',
  },
  {
    title: 'Bắt đầu học',
    description: 'Hoàn tất đăng ký và tham gia buổi học đầu tiên',
    icon: '4',
  },
];

interface EnrollmentSectionProps {
  slots?: SlotData;
}

export function EnrollmentSection({ slots }: EnrollmentSectionProps) {
  const steps = (slots?.steps as SlotItem[] | undefined) || DEFAULT_STEPS;

  return (
    <section className="py-16 bg-muted/30">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-4">Tuyển sinh</h2>
        <p className="text-center text-muted-foreground mb-12 max-w-2xl mx-auto">
          Quy trình đăng ký đơn giản, nhanh chóng — chỉ 4 bước là bạn đã sẵn sàng học
        </p>
        <ol className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6 list-none">
          {steps.map((step) => (
            <li key={step.title} className="relative">
              <div className="text-center">
                <div className="h-14 w-14 rounded-full bg-theme-primary text-white flex items-center justify-center mx-auto mb-4 text-xl font-bold">
                  {step.icon}
                </div>
                <h3 className="font-semibold mb-2">{step.title}</h3>
                <p className="text-sm text-muted-foreground">{step.description}</p>
              </div>
            </li>
          ))}
        </ol>
        <div className="text-center mt-10">
          <Button asChild size="lg">
            <Link href="/register">Đăng ký ngay</Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
