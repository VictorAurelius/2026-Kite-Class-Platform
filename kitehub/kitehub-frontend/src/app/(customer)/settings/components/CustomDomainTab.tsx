'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Globe, Lock, CheckCircle2, Clock, AlertTriangle, Copy, ExternalLink, RefreshCw } from 'lucide-react';
import {
  useDomainStatus,
  useInitiateDomain,
  useVerifyDomain,
  useRemoveDomain,
} from '@/hooks/use-domain';
import type { Instance, DomainStatus } from '@/types/instance';

interface CustomDomainTabProps {
  instance: Instance | undefined;
}

/**
 * Custom Domain Tab — Settings page.
 *
 * Behavior:
 * - FREE / BASIC tier: shows locked state with upgrade CTA
 * - PREMIUM / ENTERPRISE: shows full domain management UI
 *
 * Domain status flow:
 *   NONE → form to enter domain
 *   PENDING_VERIFY → show TXT record instructions + verify button
 *   VERIFIED → show success state
 *   FAILED → show error + option to retry
 */
export function CustomDomainTab({ instance }: CustomDomainTabProps) {
  const [inputDomain, setInputDomain] = useState('');
  const [copied, setCopied] = useState(false);

  const canUseCustomDomain =
    instance?.tier === 'PREMIUM' || instance?.tier === 'ENTERPRISE';

  const {
    data: domainStatus,
    isLoading,
  } = useDomainStatus(canUseCustomDomain ? instance?.id : undefined);

  const initiate = useInitiateDomain(instance?.id ?? '');
  const verify = useVerifyDomain(instance?.id ?? '');
  const remove = useRemoveDomain(instance?.id ?? '');

  // ── Locked state for FREE / BASIC ────────────────────────────────────────
  if (!canUseCustomDomain) {
    return (
      <Card data-testid="domain-locked">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Lock className="h-5 w-5 text-muted-foreground" />
            Tên miền tùy chỉnh
          </CardTitle>
          <CardDescription>
            Sử dụng tên miền riêng thay vì {instance?.subdomain}.kitehub.me
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert>
            <Lock className="h-4 w-4" />
            <AlertTitle>Tính năng Premium</AlertTitle>
            <AlertDescription>
              Tên miền tùy chỉnh chỉ có trên gói <strong>Premium</strong> và{' '}
              <strong>Enterprise</strong>. Gói hiện tại của bạn:{' '}
              <strong>{instance?.tier ?? 'FREE'}</strong>.
            </AlertDescription>
          </Alert>
          <div className="rounded-lg border border-dashed p-6 text-center">
            <Globe className="mx-auto h-10 w-10 text-muted-foreground mb-3" />
            <p className="font-medium mb-1">Ví dụ: school.example.com</p>
            <p className="text-sm text-muted-foreground mb-4">
              Nâng cấp để sử dụng tên miền riêng của bạn.
            </p>
            <Button variant="default" asChild>
              <a href="/dashboard/upgrade">Nâng cấp gói</a>
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // ── Loading ───────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <Card>
        <CardContent className="py-8 text-center text-muted-foreground">
          Đang tải thông tin tên miền...
        </CardContent>
      </Card>
    );
  }

  const status: DomainStatus = domainStatus?.status ?? 'NONE';
  const backupUrl = domainStatus?.backupUrl ?? `https://${instance?.subdomain}.kitehub.me`;

  // ── Status badge ──────────────────────────────────────────────────────────
  const StatusBadge = () => {
    switch (status) {
      case 'VERIFIED':
        return <Badge variant="default" className="bg-green-600">Đã xác minh</Badge>;
      case 'PENDING_VERIFY':
        return <Badge variant="secondary">Đang chờ xác minh</Badge>;
      case 'FAILED':
        return <Badge variant="destructive">Xác minh thất bại</Badge>;
      default:
        return <Badge variant="outline">Chưa cài đặt</Badge>;
    }
  };

  // ── Copy helper ───────────────────────────────────────────────────────────
  const handleCopy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback: ignore
    }
  };

  // ── VERIFIED state ────────────────────────────────────────────────────────
  if (status === 'VERIFIED') {
    return (
      <Card data-testid="domain-verified">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Globe className="h-5 w-5 text-primary" />
            Tên miền tùy chỉnh
            <StatusBadge />
          </CardTitle>
          <CardDescription>Tên miền của bạn đã được kích hoạt thành công.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert>
            <CheckCircle2 className="h-4 w-4 text-green-600" />
            <AlertTitle>Tên miền đã hoạt động!</AlertTitle>
            <AlertDescription className="space-y-2">
              <p>
                Học viên có thể truy cập qua:{' '}
                <a
                  href={`https://${domainStatus?.customDomain}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-medium text-primary hover:underline"
                >
                  https://{domainStatus?.customDomain}
                  <ExternalLink className="inline h-3 w-3 ml-1" />
                </a>
              </p>
              <p className="text-xs text-muted-foreground">
                URL dự phòng vẫn hoạt động:{' '}
                <a href={backupUrl} target="_blank" rel="noopener noreferrer" className="hover:underline">
                  {backupUrl}
                </a>
              </p>
            </AlertDescription>
          </Alert>

          <div className="pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => remove.mutate()}
              disabled={remove.isPending}
              aria-label="Xóa tên miền tùy chỉnh đã được xác minh"
            >
              {remove.isPending ? 'Đang xóa...' : 'Xóa tên miền tùy chỉnh'}
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // ── PENDING_VERIFY state ──────────────────────────────────────────────────
  if (status === 'PENDING_VERIFY') {
    const token = domainStatus?.verifyToken ?? '';

    return (
      <Card data-testid="domain-pending">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Globe className="h-5 w-5 text-primary" />
            Tên miền tùy chỉnh
            <StatusBadge />
          </CardTitle>
          <CardDescription>
            Thêm TXT record vào DNS của bạn để xác minh quyền sở hữu.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert>
            <Clock className="h-4 w-4" />
            <AlertTitle>Đang chờ xác minh DNS</AlertTitle>
            <AlertDescription>
              Tên miền: <strong>{domainStatus?.customDomain}</strong>. Bạn có tối đa 48 giờ để
              hoàn thành xác minh.
            </AlertDescription>
          </Alert>

          <div className="rounded-lg border p-4 space-y-3 bg-muted/30">
            <p className="text-sm font-medium">Hướng dẫn:</p>
            <ol className="text-sm space-y-2 list-decimal list-inside">
              <li>Đăng nhập vào nhà cung cấp tên miền của bạn (Cloudflare, GoDaddy, v.v.)</li>
              <li>Vào phần quản lý DNS</li>
              <li>Thêm TXT record sau:</li>
            </ol>

            <div className="mt-2 space-y-2">
              <div className="grid grid-cols-[auto,1fr,auto] gap-2 items-center text-xs bg-background border rounded p-2">
                <span className="font-mono text-muted-foreground">Type: TXT</span>
                <span className="font-mono break-all">{token}</span>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6"
                  onClick={() => handleCopy(token)}
                  aria-label="Copy token"
                >
                  {copied ? (
                    <CheckCircle2 className="h-3 w-3 text-green-600" />
                  ) : (
                    <Copy className="h-3 w-3" />
                  )}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                Host/Name: @ (hoặc tên miền chính)
              </p>
            </div>
          </div>

          <div className="flex gap-2">
            <Button
              onClick={() => verify.mutate()}
              disabled={verify.isPending}
              data-testid="btn-verify"
              aria-label="Kiểm tra lại xác minh DNS"
            >
              {verify.isPending ? (
                <>
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  Đang kiểm tra...
                </>
              ) : (
                <>
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Kiểm tra lại
                </>
              )}
            </Button>
            <Button
              variant="outline"
              onClick={() => remove.mutate()}
              disabled={remove.isPending}
              aria-label="Hủy thiết lập tên miền đang chờ xác minh"
            >
              Hủy
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // ── NONE / FAILED state — show form ──────────────────────────────────────
  return (
    <Card data-testid="domain-form">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Globe className="h-5 w-5 text-primary" />
          Tên miền tùy chỉnh
          <StatusBadge />
        </CardTitle>
        <CardDescription>
          Sử dụng tên miền riêng thay vì{' '}
          <a href={backupUrl} target="_blank" rel="noopener noreferrer" className="font-medium hover:underline">
            {backupUrl}
          </a>
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {status === 'FAILED' && (
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertTitle>Xác minh thất bại</AlertTitle>
            <AlertDescription>
              Không tìm thấy TXT record đúng. Vui lòng kiểm tra lại cấu hình DNS và thử lại.
            </AlertDescription>
          </Alert>
        )}

        <div className="space-y-2">
          <Label htmlFor="custom-domain">Tên miền tùy chỉnh</Label>
          <Input
            id="custom-domain"
            placeholder="school.example.com"
            value={inputDomain}
            onChange={(e) => setInputDomain(e.target.value)}
            disabled={initiate.isPending}
            data-testid="input-domain"
          />
          <p className="text-xs text-muted-foreground">
            Ví dụ: school.example.com, app.myorg.vn
          </p>
        </div>

        {initiate.isError && (
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              {(initiate.error as Error)?.message ?? 'Có lỗi xảy ra. Vui lòng thử lại.'}
            </AlertDescription>
          </Alert>
        )}

        <Button
          onClick={() => {
            if (inputDomain.trim()) {
              initiate.mutate(inputDomain.trim());
            }
          }}
          disabled={initiate.isPending || !inputDomain.trim()}
          data-testid="btn-submit-domain"
          aria-label="Bắt đầu cài đặt tên miền tùy chỉnh"
        >
          {initiate.isPending ? 'Đang xử lý...' : 'Cài đặt tên miền'}
        </Button>
      </CardContent>
    </Card>
  );
}
