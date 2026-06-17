/**
 * Owner Billing Page — KH pro v2 token-styled subscription billing.
 *
 * Wave 31 Bucket B (GAP-273) — applies kitehub-pro-v2 design tokens to the
 * existing trial / subscribed / error layout AND wires `@kite/shared-ui`
 * components shipped Wave 27 (`formatVNCurrency` for KPI cards, G5
 * `PaymentMethodSelector` for the tier upgrade CTA).
 *
 * Wave flow-kh3 (GAP-1472) — the "Lịch sử hóa đơn" list previously rendered a
 * hardcoded `MOCK_INVOICES` fixture (fake "PRO · 499.000đ" rows that never
 * matched a real payment). It is now wired to REAL payment data via
 * `usePaymentHistory(subscriptionId)` → `GET /api/platform/payments/subscription/{id}`.
 * Only fields present on `PaymentResponse` are displayed (no fabricated tier /
 * period / center-count / KHB- invoice number / VN tax-invoice block). See the
 * GAP-1472 file for the Option A (wire-to-real) vs Option B (full invoice gen,
 * deferred Phase 1.5) split.
 *
 * Spec source: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/billing-{default,empty,loading,payment,dark}.html`.
 *
 * @author KiteHub Team
 * @since 1.0.0 — Wave 31 Bucket B (GAP-273) port; Wave flow-kh3 (GAP-1472) real-data wiring
 */

'use client';

import dynamic from 'next/dynamic';
import { useMemo, useState, useEffect } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import {
  CreditCard,
  Receipt,
  ArrowUpCircle,
  Wallet,
  AlertTriangle,
  FileText,
  QrCode,
} from 'lucide-react';
import { toast } from 'sonner';
import {
  formatVNCurrency,
  PaymentMethodSelector,
  type PaymentMethod,
  type PaymentMethodOption,
} from '@kite/shared-ui';

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription, usePendingPaymentStatus } from '@/hooks/use-subscriptions';
import { usePaymentHistory } from '@/hooks/use-payments';
import type { Payment } from '@/types/payment';
import { CurrentPlanCard } from '@/components/billing/CurrentPlanCard';
import { PendingPaymentBanner } from '@/components/billing/PendingPaymentBanner';
import { ReactivateBanner } from '@/components/billing/ReactivateBanner';
import { TierRecommender } from '@/components/billing/TierRecommender';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

// Plan comparison stays dynamic — full pricing matrix is large.
const PlanComparison = dynamic(
  () =>
    import('@/components/billing/PlanComparison').then((m) => ({
      default: m.PlanComparison,
    })),
  {
    ssr: false,
    loading: () => (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    ),
  },
);

const PAYMENT_OPTIONS: PaymentMethodOption[] = [
  {
    id: 'VNPAY',
    label: 'VNPay',
    description: 'Thanh toán qua thẻ ngân hàng / QR VNPay',
    redirect: true,
  },
  {
    id: 'MOMO',
    label: 'MoMo',
    description: 'Quét mã QR bằng app MoMo',
    popular: true,
  },
  {
    id: 'ZALOPAY',
    label: 'ZaloPay',
    description: 'Quét mã QR bằng app ZaloPay',
  },
  {
    id: 'BANK',
    label: 'Chuyển khoản ngân hàng',
    description: 'Vietcombank · Techcombank · BIDV · ACB',
  },
];

/**
 * Payment status → badge config. String-keyed (not `Record<PaymentStatus>`)
 * with a defensive fallback so any BE-returned status — including REFUNDED /
 * CANCELLED that the FE `PaymentStatus` union does not yet enumerate — renders
 * without crashing. (FE↔BE PaymentStatus enum drift tracked separately.)
 */
const PAYMENT_STATUS_CONFIG: Record<
  string,
  { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }
> = {
  PENDING: { label: 'Đang chờ', variant: 'secondary' },
  COMPLETED: { label: 'Đã thanh toán', variant: 'default' },
  FAILED: { label: 'Thất bại', variant: 'destructive' },
  REFUNDED: { label: 'Đã hoàn tiền', variant: 'outline' },
  CANCELLED: { label: 'Đã hủy', variant: 'outline' },
  EXPIRED: { label: 'Hết hạn', variant: 'outline' },
};

function paymentStatusMeta(status: string) {
  return (
    PAYMENT_STATUS_CONFIG[status] ?? { label: status, variant: 'outline' as const }
  );
}

function paymentMethodLabel(method: string): string {
  switch (method) {
    case 'VIETQR':
      return 'VietQR';
    case 'MOMO':
      return 'MoMo';
    case 'VNPAY':
      return 'VNPay';
    case 'BANK_TRANSFER':
      return 'Chuyển khoản ngân hàng';
    case 'MANUAL':
      return 'Thủ công';
    default:
      return method;
  }
}

function formatPaymentDate(iso: string | null | undefined, withTime = false): string {
  if (!iso) return '—';
  const opts: Intl.DateTimeFormatOptions = withTime
    ? { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }
    : { day: '2-digit', month: '2-digit', year: 'numeric' };
  return new Date(iso).toLocaleDateString('vi-VN', opts);
}

/** Reference label for a real payment — gateway txnRef, else transactionId, else short id. */
function paymentRef(p: Payment): string {
  return p.txnRef ?? p.transactionId ?? p.id.slice(0, 8).toUpperCase();
}

/**
 * Real-payment detail panel. Renders ONLY fields present on `PaymentResponse`
 * (no fabricated VN tax-invoice / VAT / MST / student block). For a PENDING
 * payment it links to the existing `/billing/payment/[id]` page where the
 * VietQR linkage lives.
 */
function RealPaymentDetail({
  payment,
  onContinuePayment,
}: {
  payment: Payment;
  onContinuePayment: () => void;
}) {
  const meta = paymentStatusMeta(payment.status);
  const isPending = payment.status === 'PENDING';
  const hasBankInfo = payment.bankCode || payment.accountNumber || payment.accountName;

  return (
    <Card className="rounded-2xl shadow-sm" data-testid="real-payment-detail">
      <CardHeader>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Giao dịch
            </p>
            <p className="mt-1 font-mono text-2xl font-bold">{paymentRef(payment)}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Ngày tạo: {formatPaymentDate(payment.createdAt, true)}
            </p>
          </div>
          <Badge variant={meta.variant}>{meta.label}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="rounded-xl border bg-muted/30 p-4">
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Số tiền
          </p>
          <p className="mt-1 text-3xl font-bold">{formatVNCurrency(payment.amountVnd)}</p>
        </div>

        <dl className="grid gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-muted-foreground">Phương thức</dt>
            <dd className="font-medium">{paymentMethodLabel(payment.paymentMethod)}</dd>
          </div>
          {payment.transactionId && (
            <div>
              <dt className="text-muted-foreground">Mã giao dịch</dt>
              <dd className="font-mono font-medium">{payment.transactionId}</dd>
            </div>
          )}
          {payment.paidAt && (
            <div>
              <dt className="text-muted-foreground">Đã thanh toán lúc</dt>
              <dd className="font-medium">{formatPaymentDate(payment.paidAt, true)}</dd>
            </div>
          )}
          {payment.paymentContent && (
            <div className="sm:col-span-2">
              <dt className="text-muted-foreground">Nội dung chuyển khoản</dt>
              <dd className="font-medium">{payment.paymentContent}</dd>
            </div>
          )}
        </dl>

        {hasBankInfo && (
          <div className="rounded-xl border bg-muted/30 p-4">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Thông tin chuyển khoản
            </p>
            <dl className="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
              {payment.accountName && (
                <div>
                  <dt className="text-muted-foreground">Chủ tài khoản</dt>
                  <dd className="font-medium">{payment.accountName}</dd>
                </div>
              )}
              {payment.accountNumber && (
                <div>
                  <dt className="text-muted-foreground">Số tài khoản</dt>
                  <dd className="font-mono font-medium">{payment.accountNumber}</dd>
                </div>
              )}
              {payment.bankCode && (
                <div>
                  <dt className="text-muted-foreground">Ngân hàng</dt>
                  <dd className="font-medium">{payment.bankCode}</dd>
                </div>
              )}
            </dl>
          </div>
        )}

        {isPending && (
          <Button className="w-full" onClick={onContinuePayment} data-testid="continue-payment-cta">
            <QrCode className="mr-2 h-4 w-4" />
            Tiếp tục thanh toán
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

export default function BillingPage() {
  const user = useAuthStore((state) => state.user);
  const searchParams = useSearchParams();
  const router = useRouter();

  // 1 user = 1 instance assumption (Phase 1 single-instance owners).
  const {
    data: instances,
    isLoading: instancesLoading,
    error: instancesError,
  } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;

  const {
    data: subscription,
    isLoading: subLoading,
    error: subError,
  } = useActiveSubscription(instanceId?.toString());

  // GAP-1472 — real subscription payment history (replaces MOCK_INVOICES).
  // `enabled` only when a subscription id exists (TRIAL/FREE owners have none).
  const subscriptionId = subscription?.id;
  const {
    data: payments,
    isLoading: paymentsLoading,
    error: paymentsError,
  } = usePaymentHistory(subscriptionId);

  // GAP-1257-FE — awaiting-confirmation pending payment (VietQR manual, SUB-19).
  // Code-to-contract: returns null until BE-4 ships the endpoint.
  const { data: pendingPayment } = usePendingPaymentStatus(instanceId?.toString());
  const instanceStatus = instances?.[0]?.status;

  const [selectedPaymentId, setSelectedPaymentId] = useState<string | null>(null);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState<
    PaymentMethod | undefined
  >(undefined);

  // Handle success messages from redirects.
  useEffect(() => {
    const success = searchParams.get('success');
    if (success === 'payment') {
      toast.success('Thanh toán thành công!');
    } else if (success === 'downgrade') {
      toast.success('Đã lên lịch hạ gói thành công!');
    }
  }, [searchParams]);

  // Owner KPIs derived from REAL payments (GAP-1472):
  //   - Đã thanh toán  = sum(amountVnd) where status === COMPLETED
  //   - Còn phải thu    = sum(amountVnd) where status === PENDING
  //   - Quá hạn         = 0 — PaymentResponse exposes no expiry field, so an
  //                       overdue count cannot be honestly derived (Option B / Phase 1.5).
  const summary = useMemo(() => {
    const rows = payments ?? [];
    const totalPaid = rows
      .filter((p) => p.status === 'COMPLETED')
      .reduce((acc, p) => acc + p.amountVnd, 0);
    const totalOutstanding = rows
      .filter((p) => p.status === 'PENDING')
      .reduce((acc, p) => acc + p.amountVnd, 0);
    return { totalPaid, totalOutstanding, overdueCount: 0, count: rows.length };
  }, [payments]);

  // Selected payment defaults to the first (most recent) row without an effect.
  const selectedPayment = useMemo(() => {
    const rows = payments ?? [];
    if (rows.length === 0) return null;
    return rows.find((p) => p.id === selectedPaymentId) ?? rows[0];
  }, [payments, selectedPaymentId]);

  function handleUpgradeClick() {
    if (!selectedPaymentMethod) {
      toast.error('Vui lòng chọn phương thức thanh toán trước khi nâng cấp.');
      return;
    }
    router.push(`/billing/upgrade?method=${selectedPaymentMethod}`);
  }

  if (instancesLoading || subLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (instancesError) {
    return (
      <ErrorAlert message="Không thể tải thông tin thanh toán. Vui lòng thử lại." />
    );
  }

  // GAP-1079-FE: `GET .../active` trả 404 khi chưa có gói trả phí (TRIAL tenant
  // = bình thường). Hook đã map 404 → null, KHÔNG phải error toast. Hiển thị
  // empty-state "Chưa có gói trả phí" + gợi ý gói, KHÔNG crash.
  if (!subscription || subError) {
    return (
      <div className="space-y-6">
        <div className="rounded-2xl border bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Chưa có gói trả phí</h1>
              <p className="text-muted-foreground">
                Bạn đang dùng gói Trial/Miễn phí. Chọn gói phù hợp để mở khóa đầy đủ tính năng.
              </p>
            </div>
          </div>
        </div>

        {/* GAP-1263-FE — reactivate CTA khi instance bị tạm ngưng/hết hạn */}
        <ReactivateBanner subscription={null} instanceStatus={instanceStatus} />

        {/* GAP-1257-FE — đang chờ admin xác nhận chuyển khoản */}
        {pendingPayment && <PendingPaymentBanner pending={pendingPayment} />}

        {/* GAP-1269 — gợi ý gói theo số học viên */}
        <TierRecommender />

        <PlanComparison currentTier={null} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page header — pro v2 gradient */}
      <div className="rounded-2xl border bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Hóa đơn & Thanh toán</h1>
              <p className="text-muted-foreground">
                Quản lý gói đăng ký · hóa đơn · phương thức thanh toán
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push('/billing/history')}
            >
              <Receipt className="mr-2 h-4 w-4" />
              Lịch sử
            </Button>
            <Button size="sm" onClick={() => router.push('/billing/upgrade')}>
              <ArrowUpCircle className="mr-2 h-4 w-4" />
              Nâng cấp
            </Button>
          </div>
        </div>
      </div>

      {/* GAP-1263-FE — reactivate CTA khi gói đã hủy/hết hạn hoặc instance tạm ngưng */}
      <ReactivateBanner subscription={subscription} instanceStatus={instanceStatus} />

      {/* GAP-1257-FE — đang chờ admin xác nhận chuyển khoản VietQR */}
      {pendingPayment && <PendingPaymentBanner pending={pendingPayment} />}

      {/* Owner KPI tiles — pro v2 token-style cards using formatVNCurrency.
          GAP-1472: values computed from REAL payments. */}
      <div className="grid gap-4 sm:grid-cols-3" data-testid="billing-summary">
        <Card className="rounded-xl shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Còn phải thu
            </CardTitle>
            <Wallet className="h-4 w-4 text-muted-foreground" aria-hidden />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">
              {formatVNCurrency(summary.totalOutstanding)}
            </p>
            <p className="text-xs text-muted-foreground">
              {summary.count} giao dịch
            </p>
          </CardContent>
        </Card>
        <Card className="rounded-xl shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Đã thanh toán
            </CardTitle>
            <Receipt className="h-4 w-4 text-muted-foreground" aria-hidden />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">{formatVNCurrency(summary.totalPaid)}</p>
            <p className="text-xs text-muted-foreground">Tích lũy</p>
          </CardContent>
        </Card>
        <Card className="rounded-xl shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Quá hạn
            </CardTitle>
            <AlertTriangle className="h-4 w-4 text-destructive" aria-hidden />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">
              {summary.overdueCount}
              <span className="ml-2 text-sm font-normal text-muted-foreground">
                giao dịch
              </span>
            </p>
            <p className="text-xs text-muted-foreground">Cần xử lý sớm</p>
          </CardContent>
        </Card>
      </div>

      {/* Current plan + Tier upgrade with G5 method selector */}
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-1">
          <CurrentPlanCard subscription={subscription} />
        </div>

        <Card className="rounded-xl shadow-sm lg:col-span-2" data-testid="tier-upgrade-card">
          <CardHeader>
            <CardTitle className="text-base">Phương thức thanh toán cho lần nâng cấp</CardTitle>
            <p className="text-sm text-muted-foreground">
              Chọn cổng thanh toán bạn muốn sử dụng — hệ thống sẽ ghi nhớ để áp dụng cho
              hóa đơn nâng cấp tiếp theo.
            </p>
          </CardHeader>
          <CardContent className="space-y-4">
            <PaymentMethodSelector
              options={PAYMENT_OPTIONS}
              selectedMethod={selectedPaymentMethod}
              onChange={setSelectedPaymentMethod}
              ariaLabel="Phương thức thanh toán cho gói nâng cấp"
            />
            <Button
              type="button"
              className="w-full"
              onClick={handleUpgradeClick}
              disabled={!selectedPaymentMethod}
              data-testid="tier-upgrade-cta"
            >
              <ArrowUpCircle className="mr-2 h-4 w-4" />
              Tiếp tục nâng cấp
            </Button>
          </CardContent>
        </Card>
      </div>

      {/* Plan comparison row */}
      <PlanComparison currentTier={subscription.tier} />

      {/* Invoice (payment) list + real-payment detail panel */}
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="rounded-xl shadow-sm lg:col-span-1" data-testid="invoice-list">
          <CardHeader>
            <CardTitle className="text-base">Lịch sử hóa đơn</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {paymentsLoading ? (
              <div className="flex items-center justify-center py-12">
                <LoadingSpinner />
              </div>
            ) : paymentsError ? (
              <div className="flex flex-col items-center justify-center rounded-xl border border-dashed py-12 text-center">
                <AlertTriangle className="mb-4 h-12 w-12 text-destructive" />
                <p className="text-sm text-muted-foreground">
                  Không thể tải lịch sử hóa đơn. Vui lòng thử lại.
                </p>
              </div>
            ) : !payments || payments.length === 0 ? (
              <div
                className="flex flex-col items-center justify-center rounded-xl border border-dashed py-12 text-center"
                data-testid="payment-empty-state"
              >
                <FileText className="mb-4 h-12 w-12 text-muted-foreground" />
                <p className="text-lg font-medium">Chưa có hóa đơn nào</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Các giao dịch thanh toán gói đăng ký sẽ hiển thị ở đây.
                </p>
              </div>
            ) : (
              payments.map((p) => {
                const isActive = p.id === selectedPayment?.id;
                const meta = paymentStatusMeta(p.status);
                return (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => setSelectedPaymentId(p.id)}
                    className={`w-full rounded-lg border p-3 text-left transition-colors hover:bg-muted/50 ${
                      isActive ? 'border-primary bg-primary/5' : ''
                    }`}
                    data-testid={`invoice-row-${p.id}`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-sm font-medium">{paymentRef(p)}</span>
                      <span className="text-sm font-semibold">
                        {formatVNCurrency(p.amountVnd)}
                      </span>
                    </div>
                    <div className="mt-1 flex items-center justify-between gap-2">
                      <p className="text-xs text-muted-foreground">
                        {formatPaymentDate(p.createdAt)}
                      </p>
                      <Badge variant={meta.variant} className="text-[10px]">
                        {meta.label}
                      </Badge>
                    </div>
                  </button>
                );
              })
            )}
          </CardContent>
        </Card>

        <div className="lg:col-span-2" data-testid="invoice-detail-panel">
          {selectedPayment ? (
            <RealPaymentDetail
              payment={selectedPayment}
              onContinuePayment={() =>
                router.push(`/billing/payment/${selectedPayment.id}`)
              }
            />
          ) : (
            <div className="flex h-full items-center justify-center rounded-xl border border-dashed p-12 text-sm text-muted-foreground">
              {paymentsLoading
                ? 'Đang tải hóa đơn…'
                : 'Chưa có hóa đơn để hiển thị chi tiết.'}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
