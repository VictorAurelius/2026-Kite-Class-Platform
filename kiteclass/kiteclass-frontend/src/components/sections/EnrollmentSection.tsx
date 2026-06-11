/**
 * Enrollment section — shows the center's registration process steps.
 *
 * Anti-fabrication + audience fit (GAP-1205): the ported default steps were a
 * generic KiteClass-platform onboarding ("Đăng ký tài khoản... Bắt đầu học") —
 * not the tenant's own enrollment process. So this section renders ONLY
 * tenant-provided slot data and hides entirely when none is configured
 * (cf. GAP-958).
 *
 * @since 2026-04-04
 */

import { Button } from '@/components/ui/button';
import Link from 'next/link';
import type { SlotData, SlotItem } from '@/lib/template/slots';

interface EnrollmentSectionProps {
  slots?: SlotData;
}

export function EnrollmentSection({ slots }: EnrollmentSectionProps) {
  const steps = slots?.steps as SlotItem[] | undefined;
  if (!steps || steps.length === 0) return null;

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
