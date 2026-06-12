'use client';

import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import {
  Building2,
  LayoutDashboard,
  ExternalLink,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  X,
  Sparkles,
  Palette,
  CreditCard,
} from 'lucide-react';
import type { Instance } from '@/types/instance';
import { getTenantUrl } from '@/lib/tenant-url';

interface OnboardingWizardProps {
  instance: Instance;
  open: boolean;
  onClose: () => void;
}

const ONBOARDING_STORAGE_KEY = 'kite hub_onboarding_completed';

export function OnboardingWizard({ instance, open, onClose }: OnboardingWizardProps) {
  const [currentStep, setCurrentStep] = useState(0);

  const steps = [
    {
      title: `Chúc mừng! Trung tâm "${instance.organizationName}" đã sẵn sàng 🎉`,
      description: 'KiteClass đã tạo trung tâm của bạn thành công. Hãy cùng khám phá các tính năng!',
      icon: Building2,
      content: (
        <div className="space-y-4 py-4">
          <div className="rounded-xl border bg-muted/50 p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">Tên trung tâm</span>
              <span className="text-sm text-muted-foreground">{instance.organizationName}</span>
            </div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">URL</span>
              <span className="text-sm text-muted-foreground">{instance.subdomain}.kitehub.me</span>
            </div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">Gói</span>
              <span className="inline-flex items-center gap-1 rounded-lg bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                {instance.tier}
              </span>
            </div>
            {instance.isOnTrial && (
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Dùng thử</span>
                <span className="text-sm text-blue-600 dark:text-blue-400">
                  Còn {instance.trialDaysLeft} ngày
                </span>
              </div>
            )}
          </div>

          <p className="text-sm text-muted-foreground">
            Trang quản lý này cho phép bạn tùy chỉnh branding, thanh toán, và cài đặt trung tâm.
            Học viên và giáo viên sẽ truy cập trang web riêng của trung tâm để học và giảng dạy.
          </p>
        </div>
      ),
    },
    {
      title: 'Trang quản lý của bạn',
      description: 'Sidebar bên trái chứa các tính năng quản lý chính',
      icon: LayoutDashboard,
      content: (
        <div className="space-y-3 py-4">
          {[
            { icon: LayoutDashboard, name: 'Dashboard', desc: 'Tổng quan và truy cập nhanh' },
            { icon: CreditCard, name: 'Thanh toán', desc: 'Quản lý gói và hóa đơn' },
            { icon: Palette, name: 'Thương hiệu', desc: 'AI tạo logo, màu sắc, website' },
            { icon: Building2, name: 'Cài đặt', desc: 'Cấu hình trung tâm' },
          ].map((item) => (
            <div key={item.name} className="flex items-start gap-3 rounded-xl border bg-card p-3">
              <div className="rounded-lg bg-primary/10 p-2 text-primary">
                <item.icon className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium">{item.name}</p>
                <p className="text-xs text-muted-foreground mt-0.5">{item.desc}</p>
              </div>
            </div>
          ))}
        </div>
      ),
    },
    {
      title: 'Truy cập trang web trung tâm',
      description: 'Đây là trang web mà học viên và phụ huynh sẽ thấy',
      icon: ExternalLink,
      content: (
        <div className="space-y-4 py-4">
          <div className="rounded-xl border bg-gradient-to-r from-primary/10 to-accent/10 p-4">
            <div className="flex items-center gap-2 mb-2">
              <Sparkles className="h-5 w-5 text-primary" />
              <span className="font-semibold">Website trung tâm của bạn</span>
            </div>
            <p className="text-sm text-muted-foreground mb-3">
              Học viên có thể xem khóa học, đăng ký, và truy cập lớp học tại:
            </p>
            <a
              href={getTenantUrl(instance.subdomain)}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              <ExternalLink className="h-4 w-4" />
              Mở website trung tâm
            </a>
          </div>

          <div className="rounded-xl border bg-muted/50 p-4">
            <p className="text-sm text-muted-foreground">
              💡 <strong>Mẹo:</strong> Sử dụng tính năng <strong>AI Branding</strong> để tạo logo và
              thiết kế website chuyên nghiệp trong 5 phút.
            </p>
          </div>
        </div>
      ),
    },
    {
      title: 'Bước tiếp theo',
      description: 'Checklist để bắt đầu sử dụng KiteClass',
      icon: CheckCircle2,
      content: (
        <div className="space-y-3 py-4">
          {[
            { label: 'Tạo thương hiệu AI', desc: 'Upload logo → AI tạo website', href: '/branding' },
            { label: 'Thêm khóa học đầu tiên', desc: 'Cài đặt khóa học để học viên đăng ký', external: true },
            { label: 'Mời giáo viên', desc: 'Thêm giáo viên vào hệ thống', external: true },
            { label: 'Nâng cấp gói (tùy chọn)', desc: 'Không giới hạn, nhiều tính năng', href: '/billing/upgrade' },
          ].map((step, idx) => (
            <div key={idx} className="flex items-start gap-3 rounded-xl border bg-card p-3">
              <div className="rounded-full border-2 border-primary/20 flex items-center justify-center h-6 w-6 shrink-0 text-xs font-semibold text-primary">
                {idx + 1}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium">{step.label}</p>
                <p className="text-xs text-muted-foreground mt-0.5">{step.desc}</p>
              </div>
            </div>
          ))}

          <p className="text-xs text-muted-foreground text-center pt-2">
            Bạn có thể xem lại hướng dẫn này bất kỳ lúc nào từ Dashboard
          </p>
        </div>
      ),
    },
  ];

  const currentStepData = steps[currentStep];
  const isLastStep = currentStep === steps.length - 1;
  const isFirstStep = currentStep === 0;

  // Guard: if currentStepData is undefined (shouldn't happen), close dialog
  if (!currentStepData) {
    return null;
  }

  const handleNext = () => {
    if (isLastStep) {
      handleComplete();
    } else {
      setCurrentStep((prev) => prev + 1);
    }
  };

  const handleBack = () => {
    if (!isFirstStep) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const handleComplete = () => {
    // Save to localStorage
    if (typeof window !== 'undefined') {
      localStorage.setItem(ONBOARDING_STORAGE_KEY, 'true');
    }
    onClose();
  };

  const handleSkip = () => {
    handleComplete();
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* Close button */}
        <button
          onClick={handleSkip}
          className="absolute right-4 top-4 rounded-lg opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none"
        >
          <X className="h-4 w-4" />
          <span className="sr-only">Close</span>
        </button>

        <DialogHeader>
          {/* Icon */}
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/20 to-accent/20">
            <currentStepData.icon className="h-8 w-8 text-primary" />
          </div>

          <DialogTitle className="text-center text-2xl">
            {currentStepData.title}
          </DialogTitle>
          <DialogDescription className="text-center">
            {currentStepData.description}
          </DialogDescription>
        </DialogHeader>

        {/* Content */}
        <div className="px-2">{currentStepData.content}</div>

        {/* Progress dots */}
        <div className="flex justify-center gap-2 py-4">
          {steps.map((_, idx) => (
            <button
              key={idx}
              onClick={() => setCurrentStep(idx)}
              className={`h-2 rounded-full transition-all ${
                idx === currentStep
                  ? 'w-8 bg-primary'
                  : 'w-2 bg-muted-foreground/30 hover:bg-muted-foreground/50'
              }`}
              aria-label={`Go to step ${idx + 1}`}
            />
          ))}
        </div>

        <DialogFooter className="flex-row gap-2 sm:gap-2">
          {!isFirstStep && (
            <Button variant="outline" onClick={handleBack} className="flex-1 sm:flex-1">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Quay lại
            </Button>
          )}
          {isFirstStep && (
            <Button variant="ghost" onClick={handleSkip} className="flex-1 sm:flex-1">
              Bỏ qua
            </Button>
          )}
          <Button onClick={handleNext} className="flex-1 sm:flex-1">
            {isLastStep ? 'Hoàn thành' : 'Tiếp theo'}
            {!isLastStep && <ArrowRight className="ml-2 h-4 w-4" />}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function useOnboardingWizard() {
  const [shouldShow, setShouldShow] = useState(false);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const completed = localStorage.getItem(ONBOARDING_STORAGE_KEY);
      setShouldShow(!completed);
    }
  }, []);

  const showWizard = () => setShouldShow(true);
  const hideWizard = () => setShouldShow(false);

  return { shouldShow, showWizard, hideWizard };
}
