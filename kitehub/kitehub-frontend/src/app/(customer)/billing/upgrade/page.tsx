'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription, useUpgradeSubscription, useDowngradeSubscription } from '@/hooks/use-subscriptions';
import { useCreatePayment } from '@/hooks/use-payments';
import { StepIndicator } from '@/components/billing/StepIndicator';
import { TierSelector } from '@/components/billing/TierSelector';
import { ChangeConfirmation } from '@/components/billing/ChangeConfirmation';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { calculateProration, getDaysRemaining, isUpgrade, type PricingTier } from '@/lib/pricing';
import { toast } from 'sonner';

export default function UpgradePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const user = useAuthStore((state) => state.user);

  const [step, setStep] = useState(1);
  const [selectedTier, setSelectedTier] = useState<PricingTier | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  // Get user's instance and subscription
  const { data: instances } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;
  const { data: subscription, isLoading } = useActiveSubscription(instanceId?.toString());

  // Mutations
  const upgrade = useUpgradeSubscription();
  const downgrade = useDowngradeSubscription();
  const createPayment = useCreatePayment();

  // Pre-select tier from URL params
  useEffect(() => {
    const tierParam = searchParams.get('tier');
    if (tierParam && ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'].includes(tierParam)) {
      setSelectedTier(tierParam as PricingTier);
      setStep(2); // Skip to confirmation
    }
  }, [searchParams]);

  const handleConfirm = async () => {
    if (!selectedTier || !subscription) return;

    setIsProcessing(true);

    try {
      const isUpgrading = isUpgrade(subscription.tier, selectedTier);

      // Step 1: Update subscription
      const updatedSub = isUpgrading
        ? await upgrade.mutateAsync({
            subscriptionId: subscription.id,
            newTier: selectedTier,
          })
        : await downgrade.mutateAsync({
            subscriptionId: subscription.id,
            newTier: selectedTier,
          });

      // Step 2: Create payment (only for upgrades)
      if (isUpgrading) {
        const daysRemaining = getDaysRemaining(subscription.expiresAt);
        const proratedAmount = calculateProration(
          subscription.tier,
          selectedTier,
          daysRemaining,
          subscription.billingCycle
        );

        const payment = await createPayment.mutateAsync({
          subscriptionId: updatedSub.id,
          amountVnd: proratedAmount,
          paymentMethod: 'VIETQR',
        });

        toast.success('Đã tạo thanh toán thành công');

        // Redirect to payment page
        router.push(`/billing/payment/${payment.id}`);
      } else {
        // Downgrade scheduled
        toast.success('Đã lên lịch hạ gói. Thay đổi sẽ có hiệu lực cuối kỳ thanh toán.');
        router.push('/billing?success=downgrade');
      }
    } catch (error: unknown) {
      console.error('Failed to change tier:', error);
      const message = error instanceof Error ? error.message : 'Không thể thay đổi gói. Vui lòng thử lại.';
      toast.error(message);
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  if (!subscription) {
    return <ErrorAlert message="Không tìm thấy gói đăng ký" />;
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Thay đổi gói đăng ký</h1>
        <p className="text-muted-foreground">
          Chọn gói mới phù hợp với nhu cầu của bạn
        </p>
      </div>

      <StepIndicator currentStep={step} totalSteps={2} />

      <div className="mt-8">
        {step === 1 && (
          <TierSelector
            currentTier={subscription.tier}
            selectedTier={selectedTier}
            onSelect={(tier) => {
              setSelectedTier(tier);
              setStep(2);
            }}
          />
        )}

        {step === 2 && selectedTier && (
          <ChangeConfirmation
            subscription={subscription}
            newTier={selectedTier}
            onBack={() => setStep(1)}
            onConfirm={handleConfirm}
            isProcessing={isProcessing}
          />
        )}
      </div>
    </div>
  );
}
