'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useBrandingJob, useAnalyzeLogo, useCreateBrandingJob } from '@/hooks/use-branding';
import { UploadStep } from '@/components/branding/UploadStep';
import { AnalyzeStep } from '@/components/branding/AnalyzeStep';
import { GenerateStep } from '@/components/branding/GenerateStep';
import { ReviewStep } from '@/components/branding/ReviewStep';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Sparkles, Check } from 'lucide-react';
import { toast } from 'sonner';
import type { LogoAnalysis } from '@/types/branding';

export default function BrandingWizardPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const { data: instances } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;

  const [step, setStep] = useState(1);
  const [logoUrl, setLogoUrl] = useState<string | null>(null);
  const [analysis, setAnalysis] = useState<LogoAnalysis | null>(null);
  const [jobId, setJobId] = useState<string | null>(null);

  const analyzeMutation = useAnalyzeLogo();
  const createJobMutation = useCreateBrandingJob();
  const { data: job } = useBrandingJob(jobId ?? undefined);

  // Auto-redirect when job completes
  useEffect(() => {
    if (job?.status === 'COMPLETED') {
      toast.success('Branding đã được tạo thành công!');
      setTimeout(() => setStep(4), 1000);
    } else if (job?.status === 'FAILED') {
      toast.error('Tạo branding thất bại. Vui lòng thử lại.');
    }
  }, [job?.status]);

  const handleUploadComplete = async (url: string) => {
    setLogoUrl(url);
    try {
      const result = await analyzeMutation.mutateAsync(url);
      setAnalysis(result);
      setStep(2);
    } catch {
      toast.error('Phân tích logo thất bại. Vui lòng thử lại.');
    }
  };

  const handleAnalysisConfirm = async (customizedAnalysis: LogoAnalysis) => {
    setAnalysis(customizedAnalysis);
    try {
      const job = await createJobMutation.mutateAsync({
        instanceId: instanceId!,
        logoUrl: logoUrl!,
        analysis: customizedAnalysis,
      });
      setJobId(job.id);
      setStep(3);
    } catch {
      toast.error('Khởi tạo job thất bại. Vui lòng thử lại.');
    }
  };

  const handlePublish = () => {
    router.push('/branding?success=true');
  };

  if (!instanceId) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-purple-500/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-4 mb-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push('/branding')}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại
          </Button>
        </div>
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-purple-500/10 p-3 text-purple-600">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Trình hướng dẫn AI Branding</h1>
            <p className="text-muted-foreground">
              Tạo bộ nhận diện thương hiệu hoàn chỉnh từ logo của bạn
            </p>
          </div>
        </div>
      </div>

      <BrandingStepIndicator currentStep={step} />

      <div className="mt-2">
        {step === 1 && (
          <UploadStep
            instanceId={instanceId}
            onUploadComplete={handleUploadComplete}
          />
        )}

        {step === 2 && analysis && (
          <AnalyzeStep
            analysis={analysis}
            onConfirm={handleAnalysisConfirm}
            onBack={() => setStep(1)}
          />
        )}

        {step === 3 && jobId && (
          <GenerateStep job={job} />
        )}

        {step === 4 && jobId && (
          <ReviewStep
            jobId={jobId}
            onPublish={handlePublish}
          />
        )}
      </div>
    </div>
  );
}

// Custom Step Indicator for Branding Wizard
function BrandingStepIndicator({ currentStep }: { currentStep: number }) {
  const steps = [
    { number: 1, label: 'Tải Logo' },
    { number: 2, label: 'Phân Tích' },
    { number: 3, label: 'Tạo' },
    { number: 4, label: 'Xem Trước' },
  ];

  return (
    <div className="flex items-center justify-center">
      {steps.map((step, idx) => (
        <div key={step.number} className="flex items-center">
          {/* Step Circle */}
          <div className="flex flex-col items-center">
            <div
              className={`
                w-10 h-10 rounded-full flex items-center justify-center text-sm font-medium transition-all
                ${step.number < currentStep
                  ? 'bg-primary text-primary-foreground'
                  : step.number === currentStep
                  ? 'bg-primary text-primary-foreground ring-4 ring-primary/20'
                  : 'bg-muted text-muted-foreground'}
              `}
            >
              {step.number < currentStep ? (
                <Check className="w-5 h-5" />
              ) : (
                step.number
              )}
            </div>
            <p className={`text-sm mt-2 font-medium ${step.number <= currentStep ? 'text-foreground' : 'text-muted-foreground'}`}>
              {step.label}
            </p>
          </div>

          {/* Connector Line */}
          {idx < steps.length - 1 && (
            <div
              className={`
                h-0.5 w-16 md:w-24 mx-2 md:mx-4 mb-6 transition-colors
                ${step.number < currentStep ? 'bg-primary' : 'bg-muted'}
              `}
            />
          )}
        </div>
      ))}
    </div>
  );
}
