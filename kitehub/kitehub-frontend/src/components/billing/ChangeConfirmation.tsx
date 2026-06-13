'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { ArrowRight, Info, AlertCircle, AlertTriangle } from 'lucide-react';
import type { Subscription, PricingTier } from '@/types/subscription';
import {
  PLAN_DETAILS,
  calculateProration,
  getDaysRemaining,
  isUpgrade,
  formatPrice,
  formatVnd,
  computeDowngradeImpact,
} from '@/lib/pricing';

interface ChangeConfirmationProps {
  subscription: Subscription;
  newTier: PricingTier;
  onBack: () => void;
  onConfirm: () => void;
  isProcessing: boolean;
}

export function ChangeConfirmation({
  subscription,
  newTier,
  onBack,
  onConfirm,
  isProcessing,
}: ChangeConfirmationProps) {
  const currentPlan = PLAN_DETAILS[subscription.tier];
  const newPlan = PLAN_DETAILS[newTier];

  const isUpgrading = isUpgrade(subscription.tier, newTier);
  const daysRemaining = getDaysRemaining(subscription.expiresAt);
  const proratedAmount = isUpgrading
    ? calculateProration(subscription.tier, newTier, daysRemaining, subscription.billingCycle)
    : 0;

  // GAP-1262 — prorated breakdown inputs.
  const cycleLabel = subscription.billingCycle === 'MONTHLY' ? 'tháng' : 'năm';
  const daysInCycle = subscription.billingCycle === 'MONTHLY' ? 30 : 365;
  const currentCyclePrice =
    subscription.billingCycle === 'MONTHLY' ? currentPlan.monthlyPrice : currentPlan.yearlyPrice;
  const newCyclePrice =
    subscription.billingCycle === 'MONTHLY' ? newPlan.monthlyPrice : newPlan.yearlyPrice;

  // GAP-1261 — downgrade over-cap impact (client-side from caps).
  const downgradeImpact = !isUpgrading
    ? computeDowngradeImpact(subscription.tier, newTier)
    : null;

  const renewalDate = new Date(subscription.expiresAt).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

  // Feature comparison
  const changes = [
    {
      label: 'Học viên tối đa',
      from: currentPlan.maxStudents === -1 ? 'Unlimited' : currentPlan.maxStudents,
      to: newPlan.maxStudents === -1 ? 'Unlimited' : newPlan.maxStudents,
    },
    {
      label: 'Giảng viên tối đa',
      from: currentPlan.maxTeachers === -1 ? 'Unlimited' : currentPlan.maxTeachers,
      to: newPlan.maxTeachers === -1 ? 'Unlimited' : newPlan.maxTeachers,
    },
    {
      label: 'Dung lượng lưu trữ',
      from: currentPlan.storageMB === -1 ? 'Unlimited' : `${currentPlan.storageMB}MB`,
      to: newPlan.storageMB === -1 ? 'Unlimited' : `${newPlan.storageMB}MB`,
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold mb-2">Xác nhận thay đổi</h2>
        <p className="text-muted-foreground">
          Vui lòng kiểm tra kỹ thông tin trước khi xác nhận
        </p>
      </div>

      {/* Plan Change Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Thay đổi gói đăng ký</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center gap-4 mb-6">
            <div className="text-center">
              <p className="text-sm text-muted-foreground mb-1">Gói hiện tại</p>
              <p className="text-2xl font-bold">{currentPlan.name}</p>
              <p className="text-sm text-muted-foreground">
                {formatPrice(currentPlan.monthlyPrice, subscription.billingCycle)}
              </p>
            </div>

            <ArrowRight className={`h-8 w-8 ${isUpgrading ? 'text-green-600 dark:text-green-400' : 'text-orange-600 dark:text-orange-400'}`} />

            <div className="text-center">
              <p className="text-sm text-muted-foreground mb-1">Gói mới</p>
              <p className="text-2xl font-bold">{newPlan.name}</p>
              <p className="text-sm text-muted-foreground">
                {formatPrice(newPlan.monthlyPrice, subscription.billingCycle)}
              </p>
            </div>
          </div>

          {/* Feature Changes */}
          <div className="space-y-2 border-t pt-4">
            <h3 className="font-medium mb-3">Những gì thay đổi:</h3>
            {changes.map((change, idx) => (
              <div key={idx} className="flex items-center justify-between py-2 px-3 bg-muted rounded">
                <span className="text-sm">{change.label}</span>
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">{change.from}</span>
                  <ArrowRight className="h-4 w-4" />
                  <span className="text-sm font-medium">{change.to}</span>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Upgrade Notice — GAP-1262 prorated breakdown */}
      {isUpgrading && (
        <Alert>
          <Info className="h-4 w-4" />
          <AlertDescription className="ml-2">
            <p className="font-medium mb-2">Chi tiết khoản thanh toán nâng cấp:</p>
            <div
              className="space-y-1 text-sm border-l-2 border-primary/30 pl-3 mb-3"
              data-testid="proration-breakdown"
            >
              <div className="flex justify-between gap-4">
                <span className="text-muted-foreground">Còn lại trong kỳ hiện tại</span>
                <span className="font-medium">{daysRemaining}/{daysInCycle} ngày</span>
              </div>
              <div className="flex justify-between gap-4">
                <span className="text-muted-foreground">Giá gói {currentPlan.name}/{cycleLabel}</span>
                <span>{formatVnd(currentCyclePrice)}</span>
              </div>
              <div className="flex justify-between gap-4">
                <span className="text-muted-foreground">Giá gói {newPlan.name}/{cycleLabel}</span>
                <span>{formatVnd(newCyclePrice)}</span>
              </div>
              <div className="flex justify-between gap-4 border-t pt-1 mt-1">
                <span className="text-muted-foreground">
                  Chênh lệch tính cho {daysRemaining} ngày còn lại
                </span>
                <span className="font-semibold">{formatVnd(proratedAmount)}</span>
              </div>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-sm text-muted-foreground">Bạn chỉ trả phần chênh lệch:</span>
              <span className="text-2xl font-bold" data-testid="proration-amount">
                {formatVnd(proratedAmount)}
              </span>
            </div>
            <p className="text-sm text-muted-foreground mt-2">
              Còn {daysRemaining} ngày của gói {currentPlan.name} được trừ vào — bạn chỉ trả
              phần chênh lệch <strong>{formatVnd(proratedAmount)}</strong>, không phải giá đầy đủ
              {' '}{formatVnd(newCyclePrice)}. Gói mới có hiệu lực ngay sau khi thanh toán được
              xác nhận.
            </p>
          </AlertDescription>
        </Alert>
      )}

      {/* Downgrade Notice */}
      {!isUpgrading && (
        <Alert>
          <AlertCircle className="h-4 w-4" />
          <AlertDescription className="ml-2">
            <p className="font-medium mb-1">Lưu ý về hạ gói:</p>
            <ul className="text-sm text-muted-foreground space-y-1">
              <li>• Gói mới sẽ áp dụng từ <strong>{renewalDate}</strong> (cuối kỳ thanh toán hiện tại)</li>
              <li>• Bạn vẫn sử dụng gói {currentPlan.name} đến hết ngày {renewalDate}</li>
              <li>• Không cần thanh toán thêm</li>
            </ul>
          </AlertDescription>
        </Alert>
      )}

      {/* Downgrade over-cap warning — GAP-1261 (client-side cap deltas) */}
      {!isUpgrading && downgradeImpact?.hasImpact && (
        <Alert variant="destructive" data-testid="downgrade-impact-warning">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription className="ml-2">
            <p className="font-medium mb-2">
              Gói {newPlan.name} có giới hạn thấp hơn — hãy kiểm tra mức sử dụng hiện tại:
            </p>
            <ul className="text-sm space-y-1">
              {downgradeImpact.studentCapTo !== downgradeImpact.studentCapFrom && (
                <li data-testid="impact-students">
                  • Học viên tối đa:{' '}
                  <strong>
                    {downgradeImpact.studentCapFrom === -1 ? 'Không giới hạn' : downgradeImpact.studentCapFrom}
                  </strong>{' '}
                  →{' '}
                  <strong>
                    {downgradeImpact.studentCapTo === -1 ? 'Không giới hạn' : downgradeImpact.studentCapTo}
                  </strong>
                </li>
              )}
              {downgradeImpact.teacherCapTo !== downgradeImpact.teacherCapFrom && (
                <li>
                  • Giảng viên tối đa:{' '}
                  <strong>
                    {downgradeImpact.teacherCapFrom === -1 ? 'Không giới hạn' : downgradeImpact.teacherCapFrom}
                  </strong>{' '}
                  →{' '}
                  <strong>
                    {downgradeImpact.teacherCapTo === -1 ? 'Không giới hạn' : downgradeImpact.teacherCapTo}
                  </strong>
                </li>
              )}
              {downgradeImpact.storageToMB !== downgradeImpact.storageFromMB && (
                <li>
                  • Dung lượng lưu trữ:{' '}
                  <strong>
                    {downgradeImpact.storageFromMB === -1 ? 'Không giới hạn' : `${downgradeImpact.storageFromMB}MB`}
                  </strong>{' '}
                  →{' '}
                  <strong>
                    {downgradeImpact.storageToMB === -1 ? 'Không giới hạn' : `${downgradeImpact.storageToMB}MB`}
                  </strong>
                </li>
              )}
              {downgradeImpact.losesCustomDomain && (
                <li data-testid="impact-custom-domain">
                  • <strong>Mất tên miền tùy chỉnh (custom domain)</strong> — sẽ quay về subdomain mặc định
                </li>
              )}
              {downgradeImpact.losesAiBranding && (
                <li>• <strong>Mất tính năng AI Branding</strong></li>
              )}
            </ul>
            <p className="text-sm mt-2">
              Nếu mức sử dụng hiện tại (số học viên/giảng viên, dữ liệu, tên miền) vượt giới
              hạn gói mới, bạn cần điều chỉnh trước khi hạ gói để tránh gián đoạn.
            </p>
          </AlertDescription>
        </Alert>
      )}

      {/* Action Buttons */}
      <div className="flex gap-3">
        <Button
          variant="outline"
          onClick={onBack}
          disabled={isProcessing}
          className="flex-1"
        >
          Quay lại
        </Button>
        <Button
          onClick={onConfirm}
          disabled={isProcessing}
          className="flex-1"
        >
          {isProcessing
            ? 'Đang xử lý...'
            : isUpgrading
            ? 'Xác nhận & Thanh toán'
            : 'Xác nhận hạ gói'}
        </Button>
      </div>
    </div>
  );
}
