'use client';

import { Card } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Loader2, CheckCircle2, XCircle } from 'lucide-react';
import type { BrandingJob } from '@/types/branding';

interface GenerateStepProps {
  job: BrandingJob | undefined;
}

export function GenerateStep({ job }: GenerateStepProps) {
  if (!job) {
    return (
      <Card className="p-8">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin mx-auto mb-4 text-muted-foreground" />
          <p className="text-muted-foreground">Đang khởi tạo...</p>
        </div>
      </Card>
    );
  }

  const isProcessing = job.status === 'PROCESSING' || job.status === 'PENDING';
  const isCompleted = job.status === 'COMPLETED';
  const isFailed = job.status === 'FAILED';

  return (
    <Card className="p-8">
      <div className="text-center mb-8">
        {isProcessing && (
          <>
            <Loader2 className="w-16 h-16 animate-spin mx-auto mb-4 text-primary" />
            <h2 className="text-xl font-semibold mb-2">Đang Tạo Branding</h2>
            <p className="text-sm text-muted-foreground">
              AI đang tạo bộ nhận diện thương hiệu cho bạn. Quá trình này có thể mất vài phút.
            </p>
          </>
        )}

        {isCompleted && (
          <>
            <CheckCircle2 className="w-16 h-16 mx-auto mb-4 text-green-500" />
            <h2 className="text-xl font-semibold mb-2">Hoàn Thành!</h2>
            <p className="text-sm text-muted-foreground">
              Bộ nhận diện thương hiệu đã được tạo thành công.
            </p>
          </>
        )}

        {isFailed && (
          <>
            <XCircle className="w-16 h-16 mx-auto mb-4 text-red-500" />
            <h2 className="text-xl font-semibold mb-2">Thất Bại</h2>
            <p className="text-sm text-muted-foreground">
              Đã xảy ra lỗi khi tạo branding. Vui lòng thử lại.
            </p>
          </>
        )}
      </div>

      {/* Progress Bar */}
      <div className="space-y-4 mb-8">
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Tiến độ</span>
            <span className="font-medium">{job.progress}%</span>
          </div>
          <Progress value={job.progress} className="h-3" />
        </div>

        {/* Current Step */}
        {job.currentStep && (
          <div className="text-center">
            <p className="text-sm text-muted-foreground">
              {job.currentStep}
            </p>
          </div>
        )}
      </div>

      {/* Generation Steps */}
      <div className="space-y-3">
        <h3 className="font-medium text-sm text-muted-foreground mb-3">Các bước xử lý:</h3>

        <StepItem
          label="Phân tích logo"
          completed={job.progress > 0}
          active={job.progress <= 20}
        />
        <StepItem
          label="Tạo ảnh profile"
          completed={job.progress > 20}
          active={job.progress > 20 && job.progress <= 40}
        />
        <StepItem
          label="Tạo ảnh hero"
          completed={job.progress > 40}
          active={job.progress > 40 && job.progress <= 60}
        />
        <StepItem
          label="Tạo banner"
          completed={job.progress > 60}
          active={job.progress > 60 && job.progress <= 80}
        />
        <StepItem
          label="Tạo OG Image"
          completed={job.progress > 80}
          active={job.progress > 80 && job.progress < 100}
        />
        <StepItem
          label="Hoàn tất"
          completed={job.progress === 100}
          active={false}
        />
      </div>
    </Card>
  );
}

interface StepItemProps {
  label: string;
  completed: boolean;
  active: boolean;
}

function StepItem({ label, completed, active }: StepItemProps) {
  return (
    <div className="flex items-center gap-3">
      <div className={`
        w-6 h-6 rounded-full flex items-center justify-center text-xs
        ${completed
          ? 'bg-primary text-primary-foreground'
          : active
          ? 'bg-primary/20 text-primary border-2 border-primary'
          : 'bg-muted text-muted-foreground'}
      `}>
        {completed ? (
          <CheckCircle2 className="w-4 h-4" />
        ) : active ? (
          <Loader2 className="w-3 h-3 animate-spin" />
        ) : (
          ''
        )}
      </div>
      <span className={`text-sm ${completed ? 'text-foreground font-medium' : active ? 'text-primary font-medium' : 'text-muted-foreground'}`}>
        {label}
      </span>
    </div>
  );
}
