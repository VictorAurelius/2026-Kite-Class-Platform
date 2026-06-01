/**
 * Multi-step onboarding wizard for first-time KiteClass users.
 * Guides through: school info, add teacher, create course, invite student, completion.
 * Progress is persisted via localStorage.
 *
 * @author KiteClass Team
 * @since 3.16.0
 */

'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import Link from 'next/link';
import { trackOnboarding } from '@/lib/onboarding-telemetry';

export const STORAGE_KEY = 'kiteclass-onboarding-progress';
const TOTAL_STEPS = 5;

/** Reset onboarding progress so the wizard fires again (replay from settings). */
export function resetOnboardingProgress(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore — best-effort reset.
  }
}

interface OnboardingProgress {
  currentStep: number;
  completed: boolean;
}

interface OnboardingWizardProps {
  onComplete?: () => void;
}

interface StepConfig {
  title: string;
  description: string;
  icon: string;
  actionLabel: string;
  actionHref: string;
  hint: string;
}

const STEPS: StepConfig[] = [
  {
    title: 'Xác nhận thông tin trường',
    description:
      'Kiểm tra và cập nhật thông tin trường học của bạn để đảm bảo mọi thứ chính xác.',
    icon: '🏫',
    actionLabel: 'Xem cài đặt trường',
    actionHref: '/settings',
    hint: 'Bạn có thể cập nhật tên trường, địa chỉ và logo sau trong phần Cài đặt.',
  },
  {
    title: 'Thêm giáo viên đầu tiên',
    description:
      'Mời giáo viên tham gia hệ thống để bắt đầu quản lý lớp học.',
    icon: '👩‍🏫',
    actionLabel: 'Thêm giáo viên',
    actionHref: '/teachers',
    hint: 'Giáo viên sẽ nhận email mời và có thể tự đăng nhập vào hệ thống.',
  },
  {
    title: 'Tạo khóa học đầu tiên',
    description:
      'Thiết lập khóa học với lịch học, phòng học và giáo viên phụ trách.',
    icon: '📚',
    actionLabel: 'Tạo khóa học',
    actionHref: '/courses',
    hint: 'Mỗi khóa học có thể có nhiều lớp với lịch học khác nhau.',
  },
  {
    title: 'Mời học sinh đầu tiên',
    description:
      'Thêm học sinh vào hệ thống và gán vào khóa học đã tạo.',
    icon: '🎓',
    actionLabel: 'Thêm học sinh',
    actionHref: '/students',
    hint: 'Bạn có thể thêm từng học sinh hoặc nhập danh sách từ file Excel.',
  },
  {
    title: 'Hoàn thành thiết lập!',
    description:
      'Chúc mừng! Bạn đã sẵn sàng sử dụng KiteClass để quản lý trung tâm của mình.',
    icon: '🎉',
    actionLabel: 'Xem tổng quan',
    actionHref: '/dashboard',
    hint: 'Bạn có thể quay lại hướng dẫn bất cứ lúc nào trong phần Trợ giúp.',
  },
];

function loadProgress(): OnboardingProgress {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored) as OnboardingProgress;
    }
  } catch {
    // Ignore parse errors, start fresh
  }
  return { currentStep: 1, completed: false };
}

function saveProgress(progress: OnboardingProgress): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
}

export function OnboardingWizard({ onComplete }: OnboardingWizardProps) {
  const [currentStep, setCurrentStep] = useState(1);
  const [completed, setCompleted] = useState(false);
  const [initialized, setInitialized] = useState(false);

  // Load progress from localStorage on mount
  useEffect(() => {
    const progress = loadProgress();
    setCurrentStep(progress.currentStep);
    setCompleted(progress.completed);
    setInitialized(true);
    // Telemetry: a fresh first-login wizard view (not yet completed) is a start.
    if (!progress.completed) {
      trackOnboarding('onboarding_start');
      trackOnboarding('onboarding_step_view', progress.currentStep);
    }
  }, []);

  const handleNext = useCallback(() => {
    if (currentStep < TOTAL_STEPS) {
      const nextStep = currentStep + 1;
      setCurrentStep(nextStep);
      saveProgress({ currentStep: nextStep, completed: false });
      trackOnboarding('onboarding_step_view', nextStep);
    }
  }, [currentStep]);

  const handleBack = useCallback(() => {
    if (currentStep > 1) {
      const prevStep = currentStep - 1;
      setCurrentStep(prevStep);
      saveProgress({ currentStep: prevStep, completed: false });
    }
  }, [currentStep]);

  const handleSkip = useCallback(() => {
    trackOnboarding('onboarding_step_skip', currentStep);
    handleNext();
  }, [handleNext, currentStep]);

  const handleSkipTour = useCallback(() => {
    // Skip the entire tour: mark complete so it never fires again,
    // but record the skip so the funnel reflects an abandoned tour.
    trackOnboarding('onboarding_step_skip', currentStep);
    setCompleted(true);
    saveProgress({ currentStep, completed: true });
    onComplete?.();
  }, [currentStep, onComplete]);

  const handleFinish = useCallback(() => {
    setCompleted(true);
    saveProgress({ currentStep: TOTAL_STEPS, completed: true });
    trackOnboarding('onboarding_complete');
    onComplete?.();
  }, [onComplete]);

  // Don't render until initialized from localStorage
  if (!initialized) return null;

  // Don't render if already completed
  if (completed) return null;

  const step = STEPS[currentStep - 1];
  if (!step) return null;

  const progressValue = (currentStep / TOTAL_STEPS) * 100;
  const isLastStep = currentStep === TOTAL_STEPS;

  return (
    <Card className="mb-6 border-primary/20 bg-primary/5">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between gap-2">
          <CardTitle className="text-lg">
            Bước {currentStep}/{TOTAL_STEPS}: {step.title}
          </CardTitle>
          <div className="flex items-center gap-2">
            <span className="text-2xl" role="img" aria-label={step.title}>
              {step.icon}
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleSkipTour}
              aria-label="Đóng hướng dẫn"
            >
              Đóng hướng dẫn
            </Button>
          </div>
        </div>
        <Progress value={progressValue} className="mt-2" />
      </CardHeader>
      <CardContent>
        <p className="mb-3 text-muted-foreground">{step.description}</p>
        <p className="mb-4 text-sm text-muted-foreground/70 italic">
          {step.hint}
        </p>

        <div className="mb-4">
          <Link href={step.actionHref}>
            <Button variant="outline" size="sm" aria-label={step.actionLabel}>
              {step.actionLabel}
            </Button>
          </Link>
        </div>

        <div className="flex items-center justify-between">
          <div>
            {currentStep > 1 && (
              <Button variant="ghost" size="sm" onClick={handleBack} aria-label={`Quay lại bước ${currentStep - 1}`}>
                Quay lại
              </Button>
            )}
          </div>
          <div className="flex gap-2">
            {!isLastStep && (
              <>
                <Button variant="ghost" size="sm" onClick={handleSkip} aria-label={`Bỏ qua bước ${currentStep}`}>
                  Bỏ qua
                </Button>
                <Button size="sm" onClick={handleNext} aria-label={`Tiếp theo: bước ${currentStep + 1}`}>
                  Tiếp theo
                </Button>
              </>
            )}
            {isLastStep && (
              <Button size="sm" onClick={handleFinish}>
                Bắt đầu sử dụng
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
