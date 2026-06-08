/**
 * Owner Billing Page — KH pro v2 token-styled subscription billing.
 *
 * Wave 31 Bucket B (GAP-273) — applies kitehub-pro-v2 design tokens to the
 * existing trial / subscribed / error layout AND wires `@kite/shared-ui`
 * components shipped Wave 27:
 *   - G6 `InvoiceDetail` — renders subscription billing invoices using the VN
 *     tax-invoice format (Nghị định 123/2020/NĐ-CP). KH invoices are subscription
 *     billing cycle charges (monthly / annual) + addon usage overages, NOT
 *     class fee invoices like KC.
 *   - `formatVNCurrency` — drives summary KPI cards.
 *   - G5 `PaymentMethodSelector` — drives tier upgrade entry CTA. User picks a
 *     payment method first, which is then handed off to `/billing/upgrade` in
 *     query string so the upgrade flow can pre-select.
 *
 * Spec source: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/billing-{default,empty,loading,payment,dark}.html`.
 *
 * @author KiteHub Team
 * @since 1.0.0 — Wave 31 Bucket B (GAP-273) port
 */

'use client';

import dynamic from 'next/dynamic';
import { useMemo, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useEffect } from 'react';
import {
  CreditCard,
  Receipt,
  ArrowUpCircle,
  Wallet,
  AlertTriangle,
  FileText,
} from 'lucide-react';
import { toast } from 'sonner';
import {
  formatVNCurrency,
  PaymentMethodSelector,
  type InvoiceData,
  type InvoiceState,
  type PaymentMethod,
  type PaymentMethodOption,
} from '@kite/shared-ui';

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription } from '@/hooks/use-subscriptions';
import { CurrentPlanCard } from '@/components/billing/CurrentPlanCard';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

// G6 InvoiceDetail loaded dynamically — VN tax-invoice block is heavy
// (table + totals + tenant block) and only renders when the user expands a
// pending invoice. Defer it so the header + KPI cards paint first.
const InvoiceDetail = dynamic(
  () => import('@kite/shared-ui').then((m) => ({ default: m.InvoiceDetail })),
  {
    ssr: false,
    loading: () => (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    ),
  },
);

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

/**
 * Mock subscription invoice rows. KH backend invoice endpoints are tracked as
 * a follow-up (no `/api/v1/subscription/invoices` available yet — see
 * `documents/04-quality/gaps/` for the contract). We render plausible fixtures
 * so the screen demonstrates the G6 integration; data shape mirrors the
 * spec's billing-default.html invoice list.
 */
type MockSubscriptionInvoice = {
  id: string;
  number: string;
  issueDate: string; // ISO
  dueDate: string;
  total: number;
  balance: number;
  status: InvoiceData['status'];
  description: string;
};

const MOCK_INVOICES: MockSubscriptionInvoice[] = [
  {
    id: 'inv-2026-04',
    number: 'KHB-2026-04-001',
    issueDate: '2026-04-23',
    dueDate: '2026-04-30',
    total: 499000,
    balance: 499000,
    status: 'PENDING_PAYMENT',
    description: 'PRO · tháng 04/2026 · 3 trung tâm',
  },
  {
    id: 'inv-2026-03',
    number: 'KHB-2026-03-001',
    issueDate: '2026-03-23',
    dueDate: '2026-03-30',
    total: 499000,
    balance: 0,
    status: 'PAID',
    description: 'PRO · tháng 03/2026 · 3 trung tâm',
  },
  {
    id: 'inv-2026-02',
    number: 'KHB-2026-02-001',
    issueDate: '2026-02-23',
    dueDate: '2026-02-30',
    total: 499000,
    balance: 0,
    status: 'PAID',
    description: 'PRO · tháng 02/2026 · 2 trung tâm',
  },
];

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

/** Build a G6 `InvoiceData` payload for the subscription invoice fixture. */
function toG6Invoice(row: MockSubscriptionInvoice): InvoiceData {
  return {
    number: row.number,
    status: row.status,
    issueDate: new Date(row.issueDate),
    dueDate: new Date(row.dueDate),
    paidDate: row.status === 'PAID' ? new Date(row.dueDate) : undefined,
    items: [
      {
        title: row.description,
        quantity: 1,
        unitPrice: row.total,
        lineTotal: row.total,
      },
    ],
    discounts: [],
    subtotal: row.total,
    total: row.total,
    balance: row.balance,
    student: {
      // For KH the "student" slot represents the owner / billing contact
      // (Phase 1 lock — sub-billing per-instance contact is a follow-up).
      fullName: 'Chủ trung tâm',
      className: 'Hợp đồng dịch vụ',
    },
    tenant: {
      name: 'KiteHub Platform',
      address: 'Vietnam · KiteHub SaaS',
    },
  };
}

function deriveG6State(status: InvoiceData['status']): InvoiceState {
  if (status === 'PAID') return 'paid';
  if (status === 'OVERDUE') return 'overdue';
  if (status === 'PENDING_PAYMENT' || status === 'PARTIAL_PAID') return 'pending';
  return 'default';
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

  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(
    MOCK_INVOICES[0]?.id ?? null,
  );
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

  // Owner KPIs from current invoice page (mirrors KC pro v2 pattern, GAP-266).
  const summary = useMemo(() => {
    const totalOutstanding = MOCK_INVOICES.reduce(
      (acc, inv) => acc + inv.balance,
      0,
    );
    const totalPaid = MOCK_INVOICES.filter((inv) => inv.status === 'PAID').reduce(
      (acc, inv) => acc + inv.total,
      0,
    );
    const overdueCount = MOCK_INVOICES.filter(
      (inv) => inv.status === 'OVERDUE',
    ).length;
    return { totalOutstanding, totalPaid, overdueCount, count: MOCK_INVOICES.length };
  }, []);

  const selectedInvoice = useMemo(
    () => MOCK_INVOICES.find((inv) => inv.id === selectedInvoiceId) ?? null,
    [selectedInvoiceId],
  );

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

  // subscription === null (404 no active sub) hoặc subError = trial user chưa nâng cấp → show plan comparison.
  if (!subscription || subError) {
    return (
      <div className="space-y-6">
        <div className="rounded-2xl border bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Chưa có gói đăng ký</h1>
              <p className="text-muted-foreground">
                Bạn đang trong giai đoạn dùng thử. Chọn gói phù hợp với nhu cầu của bạn.
              </p>
            </div>
          </div>
        </div>

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

      {/* Owner KPI tiles — pro v2 token-style cards using formatVNCurrency */}
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
              {summary.count} hóa đơn trong trang
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
            <p className="text-xs text-muted-foreground">Tích lũy hiển thị</p>
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
                hóa đơn
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

      {/* Invoice list + G6 detail panel */}
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="rounded-xl shadow-sm lg:col-span-1" data-testid="invoice-list">
          <CardHeader>
            <CardTitle className="text-base">Lịch sử hóa đơn</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {MOCK_INVOICES.length === 0 ? (
              <div className="flex flex-col items-center justify-center rounded-xl border border-dashed py-12 text-center">
                <FileText className="mb-4 h-12 w-12 text-muted-foreground" />
                <p className="text-lg font-medium">Chưa có hóa đơn nào</p>
              </div>
            ) : (
              MOCK_INVOICES.map((inv) => {
                const isActive = inv.id === selectedInvoiceId;
                return (
                  <button
                    key={inv.id}
                    type="button"
                    onClick={() => setSelectedInvoiceId(inv.id)}
                    className={`w-full rounded-lg border p-3 text-left transition-colors hover:bg-muted/50 ${
                      isActive ? 'border-primary bg-primary/5' : ''
                    }`}
                    data-testid={`invoice-row-${inv.id}`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-sm font-medium">{inv.number}</span>
                      <span className="text-sm font-semibold">
                        {formatVNCurrency(inv.total)}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {inv.description}
                    </p>
                  </button>
                );
              })
            )}
          </CardContent>
        </Card>

        <div className="lg:col-span-2" data-testid="invoice-detail-panel">
          {selectedInvoice ? (
            <InvoiceDetail
              invoice={toG6Invoice(selectedInvoice)}
              state={deriveG6State(selectedInvoice.status)}
              onPayNow={() => router.push(`/billing/payment?invoice=${selectedInvoice.id}`)}
            />
          ) : (
            <div className="flex h-full items-center justify-center rounded-xl border border-dashed p-12 text-sm text-muted-foreground">
              Chọn một hóa đơn để xem chi tiết.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
