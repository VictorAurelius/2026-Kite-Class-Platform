'use client';

import { useState, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import {
  useActiveSubscription,
  useUpgradeSubscription,
  useDowngradeSubscription,
  useCreateSubscription,
} from '@/hooks/use-subscriptions';
import { StepIndicator } from '@/components/billing/StepIndicator';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';

// GAP-236 Sub-PR B — only one wizard step is rendered at a time, so each
// loads on demand. StepIndicator stays static (small, always visible).
const stepLoading = () => (
  <div className="flex items-center justify-center py-12">
    <LoadingSpinner />
  </div>
);
const TierSelector = dynamic(
  () => import('@/components/billing/TierSelector').then((m) => ({ default: m.TierSelector })),
  { ssr: false, loading: stepLoading }
);
const ChangeConfirmation = dynamic(
  () => import('@/components/billing/ChangeConfirmation').then((m) => ({ default: m.ChangeConfirmation })),
  { ssr: false, loading: stepLoading }
);
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { ArrowLeft, ArrowUpCircle, Sparkles } from 'lucide-react';
import { isUpgrade, type PricingTier } from '@/lib/pricing';
import { toast } from 'sonner';

/**
 * UpgradePage — Owner thay đổi gói đăng ký (UC-SUB-02 cho subscription đã có
 * + UC-SUB-01 fallback cho TRIAL/FREE Owner chưa có subscription).
 *
 * Wave flow-kh3 pre-walk fixes:
 *  - Fix C (Finding #2): bỏ `createPayment.mutateAsync` để tránh tạo Payment
 *    thứ 2 không liên kết với `pendingPaymentId` của BE → vi phạm idempotency
 *    BR-SUB-17 và làm admin confirm fail (`pendingPaymentId ≠ paymentId`).
 *    Redirect dùng `updatedSub.pendingPaymentId` từ response upgrade (UC-SUB-02
 *    bước 7-8 trong `documents/01-business/kitehub/subscription-billing/use-cases.md`).
 *  - Fix D (Finding #3): khi `subscription === null` (TRIAL/FREE Owner), branch
 *    sang `useCreateSubscription` (UC-SUB-01) thay vì silent exit. Path
 *    decision: chọn UC-SUB-01 vì TrialToPaidController trả `UpgradeResponse`
 *    chỉ có `pollUrl` + `migrationPhase`, KHÔNG có `pendingPaymentId` → không
 *    khớp redirect pattern `/billing/payment/{pendingPaymentId}` của VietQR
 *    manual flow. UC-SUB-01 (POST /api/platform/subscriptions) trả
 *    SubscriptionResponse với `pendingPaymentId` UUID — đúng pattern.
 */
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
  const createSubscription = useCreateSubscription();

  // Pre-select tier from URL params
  useEffect(() => {
    const tierParam = searchParams.get('tier');
    if (tierParam && ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'].includes(tierParam)) {
      setSelectedTier(tierParam as PricingTier);
      setStep(2); // Skip to confirmation
    }
  }, [searchParams]);

  /**
   * Branch 1: Owner đã có subscription ACTIVE → upgrade/downgrade
   * (UC-SUB-02 / UC-SUB-03).
   */
  const handleChangeExistingSubscription = async () => {
    if (!selectedTier || !subscription) return;

    const isUpgrading = isUpgrade(subscription.tier, selectedTier);

    const updatedSub = isUpgrading
      ? await upgrade.mutateAsync({
          subscriptionId: subscription.id,
          newTier: selectedTier,
        })
      : await downgrade.mutateAsync({
          subscriptionId: subscription.id,
          newTier: selectedTier,
        });

    if (isUpgrading) {
      // Fix C: BE đã tạo Payment PENDING và set `pendingPaymentId` trong
      // SubscriptionService.upgradeSubscription. FE redirect bằng
      // `updatedSub.pendingPaymentId` — KHÔNG gọi createPayment tạo Payment #2.
      const pendingPaymentId = updatedSub.pendingPaymentId;
      if (!pendingPaymentId) {
        throw new Error(
          'Không nhận được mã thanh toán từ máy chủ. Vui lòng thử lại hoặc liên hệ hỗ trợ.'
        );
      }

      toast.success('Đã tạo yêu cầu nâng cấp, vui lòng thanh toán');
      router.push(`/billing/payment/${pendingPaymentId}`);
    } else {
      // Downgrade scheduled
      toast.success('Đã lên lịch hạ gói. Thay đổi sẽ có hiệu lực cuối kỳ thanh toán.');
      router.push('/billing?success=downgrade');
    }
  };

  /**
   * Branch 2: TRIAL/FREE Owner chưa có subscription → tạo subscription mới
   * (UC-SUB-01). BE trả SubscriptionResponse với `pendingPaymentId`.
   */
  const handleCreateNewSubscription = async () => {
    if (!selectedTier || !instanceId) return;
    if (selectedTier === 'FREE') {
      throw new Error('Gói FREE không cần tạo subscription. Vui lòng chọn gói trả phí.');
    }

    const newSub = await createSubscription.mutateAsync({
      instanceId: instanceId.toString(),
      tier: selectedTier,
      billingCycle: 'MONTHLY', // default — UI cycle picker là follow-up scope
    });

    const pendingPaymentId = newSub.pendingPaymentId;
    if (!pendingPaymentId) {
      throw new Error(
        'Không nhận được mã thanh toán từ máy chủ. Vui lòng thử lại hoặc liên hệ hỗ trợ.'
      );
    }

    toast.success('Đã tạo gói đăng ký, vui lòng thanh toán');
    router.push(`/billing/payment/${pendingPaymentId}`);
  };

  const handleConfirm = async () => {
    if (!selectedTier) return;

    setIsProcessing(true);

    try {
      if (subscription) {
        await handleChangeExistingSubscription();
      } else {
        await handleCreateNewSubscription();
      }
    } catch (error: unknown) {
      console.error('Failed to change/create subscription:', error);
      const message =
        error instanceof Error ? error.message : 'Không thể xử lý yêu cầu. Vui lòng thử lại.';
      toast.error(message);
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  // Fix D: TRIAL/FREE Owner case — không hiển thị ErrorAlert "Không tìm thấy
  // gói đăng ký" (đó là regression chính surfaced trong pre-walk). Thay vào đó
  // hiển thị wizard với banner giải thích.
  if (!instanceId) {
    return <ErrorAlert message="Không tìm thấy instance. Vui lòng liên hệ hỗ trợ." />;
  }

  const isTrialOrFree = !subscription;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-4 mb-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push('/billing')}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại
          </Button>
        </div>
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-primary/10 p-3 text-primary">
            <ArrowUpCircle className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">
              {isTrialOrFree ? 'Chọn gói đăng ký' : 'Thay đổi gói đăng ký'}
            </h1>
            <p className="text-muted-foreground">
              {isTrialOrFree
                ? 'Bạn đang dùng gói Trial/FREE. Chọn gói trả phí để mở khóa đầy đủ tính năng.'
                : 'Chọn gói mới phù hợp với nhu cầu của bạn'}
            </p>
          </div>
        </div>
      </div>

      {/* Trial/FREE banner */}
      {isTrialOrFree && (
        <div className="rounded-xl border border-primary/20 bg-primary/5 p-4 flex items-start gap-3">
          <Sparkles className="h-5 w-5 text-primary mt-0.5 shrink-0" />
          <div className="text-sm">
            <p className="font-medium mb-1">Bạn chưa có gói trả phí</p>
            <p className="text-muted-foreground">
              Chọn gói BASIC, PREMIUM, hoặc ENTERPRISE. Sau khi xác nhận, hệ thống
              sẽ tạo yêu cầu thanh toán VietQR — bạn chuyển khoản theo nội dung
              hiển thị, admin sẽ đối soát và kích hoạt gói.
            </p>
          </div>
        </div>
      )}

      <StepIndicator currentStep={step} totalSteps={2} />

      <div className="mt-2">
        {step === 1 && (
          <TierSelector
            // Khi chưa có subscription, treat current tier = FREE để TierSelector
            // hiển thị mọi tier paid như "upgrade" candidate.
            currentTier={subscription?.tier ?? 'FREE'}
            selectedTier={selectedTier}
            onSelect={(tier) => {
              setSelectedTier(tier);
              setStep(2);
            }}
          />
        )}

        {step === 2 && selectedTier && (
          <ChangeConfirmation
            subscription={
              subscription ?? {
                // Synthetic placeholder subscription cho TRIAL/FREE Owner để
                // ChangeConfirmation render được. priceVnd=0, billingCycle
                // mặc định MONTHLY. ChangeConfirmation chỉ dùng tier +
                // billingCycle + expiresAt cho prorated calc; với new
                // subscription (no proration) → giá hiển thị = full plan price.
                id: '',
                instanceId: instanceId.toString(),
                tier: 'FREE',
                status: 'ACTIVE',
                billingCycle: 'MONTHLY',
                priceVnd: 0,
                startedAt: new Date().toISOString(),
                expiresAt: new Date(Date.now() + 30 * 86400000).toISOString(),
                autoRenew: false,
                isActive: true,
                isExpired: false,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString(),
              }
            }
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
