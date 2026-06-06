'use client';

import { useState } from 'react';
import dynamic from 'next/dynamic';
import { useParams, useRouter } from 'next/navigation';
import { useAdminInstance, useSuspendInstance, useActivateInstance, useExtendTrial, useRetryProvisioning } from '@/hooks/use-admin';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { ArrowLeft, Pause, Play, Calendar, Building, Mail, Phone, Database, Users, GraduationCap, BookOpen, RefreshCw } from 'lucide-react';
import type { InstanceStatus, SubscriptionTier } from '@/types/instance';
import { toast } from 'sonner';
import { getTenantDisplayUrl } from '@/lib/tenant-url';

// GAP-236 Sub-PR B — lazy-load action dialogs (radix Dialog + AlertDialog
// trees ship in their own chunk; only loaded when admin opens an action).
const InstanceActionDialogs = dynamic(() => import('./InstanceActionDialogs'), {
  ssr: false,
});

const statusConfig: Record<InstanceStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
  TRIAL: { label: 'Dùng thử', variant: 'secondary' },
  ACTIVE: { label: 'Hoạt động', variant: 'default' },
  SUSPENDED: { label: 'Tạm ngưng', variant: 'destructive' },
  EXPIRED: { label: 'Hết hạn', variant: 'outline' },
};

const tierConfig: Record<SubscriptionTier, { label: string; color: string }> = {
  FREE: { label: 'Free', color: 'text-gray-500 dark:text-gray-400' },
  BASIC: { label: 'Basic', color: 'text-blue-500 dark:text-blue-400' },
  PREMIUM: { label: 'Premium', color: 'text-purple-500 dark:text-purple-400' },
  ENTERPRISE: { label: 'Enterprise', color: 'text-amber-500 dark:text-amber-400' },
};

export default function AdminInstanceDetailPage() {
  const params = useParams();
  const router = useRouter();
  const instanceId = params.id as string;

  const { data: instance, isLoading, error, refetch } = useAdminInstance(instanceId);

  // Action states
  const [showSuspendDialog, setShowSuspendDialog] = useState(false);
  const [showActivateDialog, setShowActivateDialog] = useState(false);
  const [showExtendDialog, setShowExtendDialog] = useState(false);
  const [showRetryDialog, setShowRetryDialog] = useState(false);
  const [extendDays, setExtendDays] = useState('7');

  const suspendMutation = useSuspendInstance();
  const activateMutation = useActivateInstance();
  const extendMutation = useExtendTrial();
  const retryMutation = useRetryProvisioning();

  const formatDate = (date: string | null) => {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleSuspend = async () => {
    try {
      await suspendMutation.mutateAsync(instanceId);
      toast.success('Đã tạm ngưng instance');
      setShowSuspendDialog(false);
      refetch();
    } catch {
      toast.error('Có lỗi xảy ra');
    }
  };

  const handleActivate = async () => {
    try {
      await activateMutation.mutateAsync(instanceId);
      toast.success('Đã kích hoạt instance');
      setShowActivateDialog(false);
      refetch();
    } catch {
      toast.error('Có lỗi xảy ra');
    }
  };

  const handleExtendTrial = async () => {
    const days = parseInt(extendDays, 10);
    if (isNaN(days) || days < 1) {
      toast.error('Vui lòng nhập số ngày hợp lệ');
      return;
    }

    try {
      await extendMutation.mutateAsync({ instanceId, days });
      toast.success(`Đã gia hạn thêm ${days} ngày`);
      setShowExtendDialog(false);
      refetch();
    } catch {
      toast.error('Có lỗi xảy ra');
    }
  };

  const handleRetryProvisioning = async () => {
    try {
      await retryMutation.mutateAsync({ instanceId });
      toast.success('Đã kích hoạt lại provisioning cho instance');
      setShowRetryDialog(false);
      refetch();
    } catch {
      toast.error('Có lỗi xảy ra');
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !instance) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" onClick={() => router.back()}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
        <ErrorAlert message="Không tìm thấy instance" onRetry={() => refetch()} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="sm" onClick={() => router.back()}>
              <ArrowLeft className="mr-2 h-4 w-4" />
              Quay lại
            </Button>
            <div>
              <h1 className="text-2xl font-bold">{instance.organizationName}</h1>
              <p className="text-muted-foreground">
                <code className="text-sm">{getTenantDisplayUrl(instance.subdomain)}</code>
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant={statusConfig[instance.status]?.variant ?? 'outline'} className="text-sm">
              {statusConfig[instance.status]?.label ?? instance.status}
            </Badge>
            <span className={`font-medium ${tierConfig[instance.tier]?.color ?? 'text-muted-foreground'}`}>
              {tierConfig[instance.tier]?.label ?? instance.tier}
            </span>
          </div>
        </div>
      </div>

      {/* Content Grid */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Instance Info */}
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="rounded-lg bg-primary/10 p-2 text-primary">
                <Building className="h-4 w-4" />
              </div>
              Thông tin Instance
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label className="text-muted-foreground">ID</Label>
                <p className="font-mono text-sm">{instance.id}</p>
              </div>
              <div>
                <Label className="text-muted-foreground">Subdomain</Label>
                <p>{instance.subdomain}</p>
              </div>
              <div>
                <Label className="text-muted-foreground">Ngày tạo</Label>
                <p>{formatDate(instance.createdAt)}</p>
              </div>
              <div>
                <Label className="text-muted-foreground">Cập nhật lần cuối</Label>
                <p>{formatDate(instance.updatedAt)}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Owner Info */}
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="rounded-lg bg-blue-500/10 p-2 text-blue-600">
                <Mail className="h-4 w-4" />
              </div>
              Thông tin chủ sở hữu
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-2">
              <Mail className="h-4 w-4 text-muted-foreground" />
              <span>{instance.ownerEmail || 'Không có'}</span>
            </div>
            <div className="flex items-center gap-2">
              <Phone className="h-4 w-4 text-muted-foreground" />
              <span>{instance.ownerPhone || 'Không có'}</span>
            </div>
          </CardContent>
        </Card>

        {/* Subscription Info */}
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="rounded-lg bg-green-500/10 p-2 text-green-600">
                <Calendar className="h-4 w-4" />
              </div>
              Subscription
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label className="text-muted-foreground">Gói</Label>
                <p className={tierConfig[instance.tier]?.color ?? 'text-muted-foreground'}>
                  {tierConfig[instance.tier]?.label ?? instance.tier}
                </p>
              </div>
              <div>
                <Label className="text-muted-foreground">Trạng thái</Label>
                <Badge variant={statusConfig[instance.status]?.variant ?? 'outline'}>
                  {statusConfig[instance.status]?.label ?? instance.status}
                </Badge>
              </div>
              {instance.status === 'TRIAL' && (
                <div className="col-span-2">
                  <Label className="text-muted-foreground">Trial kết thúc</Label>
                  <p>{formatDate(instance.trialEndDate)}</p>
                </div>
              )}
              {instance.subscriptionEndDate && (
                <div className="col-span-2">
                  <Label className="text-muted-foreground">Subscription kết thúc</Label>
                  <p>{formatDate(instance.subscriptionEndDate)}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Statistics */}
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="rounded-lg bg-amber-500/10 p-2 text-amber-600">
                <Users className="h-4 w-4" />
              </div>
              Thống kê
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-4 text-center">
              <div className="p-4 bg-muted/50 rounded-xl">
                <div className="rounded-lg bg-primary/10 p-2 text-primary w-fit mx-auto mb-2">
                  <Users className="h-5 w-5" />
                </div>
                <p className="text-2xl font-bold">{instance.totalUsers}</p>
                <p className="text-sm text-muted-foreground">Users</p>
              </div>
              <div className="p-4 bg-muted/50 rounded-xl">
                <div className="rounded-lg bg-green-500/10 p-2 text-green-600 w-fit mx-auto mb-2">
                  <GraduationCap className="h-5 w-5" />
                </div>
                <p className="text-2xl font-bold">{instance.totalStudents}</p>
                <p className="text-sm text-muted-foreground">Học sinh</p>
              </div>
              <div className="p-4 bg-muted/50 rounded-xl">
                <div className="rounded-lg bg-blue-500/10 p-2 text-blue-600 w-fit mx-auto mb-2">
                  <BookOpen className="h-5 w-5" />
                </div>
                <p className="text-2xl font-bold">{instance.totalCourses}</p>
                <p className="text-sm text-muted-foreground">Khóa học</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Database Info */}
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="rounded-lg bg-purple-500/10 p-2 text-purple-600">
                <Database className="h-4 w-4" />
              </div>
              Database
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Label className="text-muted-foreground">Database URL</Label>
            <p className="font-mono text-sm break-all bg-muted/50 p-3 rounded-xl mt-2 border">
              {instance.databaseUrl || 'Không có thông tin'}
            </p>
          </CardContent>
        </Card>

        {/* Admin Actions */}
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle>Hành động Admin</CardTitle>
            <CardDescription>
              Các hành động quản trị cho instance này
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {instance.status !== 'SUSPENDED' ? (
              <Button
                variant="destructive"
                className="w-full"
                onClick={() => setShowSuspendDialog(true)}
              >
                <Pause className="mr-2 h-4 w-4" />
                Tạm ngưng Instance
              </Button>
            ) : (
              <Button
                variant="default"
                className="w-full"
                onClick={() => setShowActivateDialog(true)}
              >
                <Play className="mr-2 h-4 w-4" />
                Kích hoạt Instance
              </Button>
            )}

            {instance.status === 'TRIAL' && (
              <Button
                variant="outline"
                className="w-full"
                onClick={() => setShowExtendDialog(true)}
              >
                <Calendar className="mr-2 h-4 w-4" />
                Gia hạn Trial
              </Button>
            )}

            {/* GAP-953 — retry provisioning for a failed/stuck instance (UC-PROV-05). */}
            <Button
              variant="outline"
              className="w-full"
              onClick={() => setShowRetryDialog(true)}
            >
              <RefreshCw className="mr-2 h-4 w-4" />
              Thử lại Provisioning
            </Button>
          </CardContent>
        </Card>
      </div>

      {/* Action dialogs lazy-loaded only when an action button is clicked. */}
      {(showSuspendDialog || showActivateDialog || showExtendDialog || showRetryDialog) && (
        <InstanceActionDialogs
          organizationName={instance.organizationName}
          trialEndDateLabel={formatDate(instance.trialEndDate)}
          showSuspendDialog={showSuspendDialog}
          showActivateDialog={showActivateDialog}
          showExtendDialog={showExtendDialog}
          showRetryDialog={showRetryDialog}
          onSuspendOpenChange={setShowSuspendDialog}
          onActivateOpenChange={setShowActivateDialog}
          onExtendOpenChange={setShowExtendDialog}
          onRetryOpenChange={setShowRetryDialog}
          onSuspendConfirm={handleSuspend}
          onActivateConfirm={handleActivate}
          onExtendConfirm={handleExtendTrial}
          onRetryConfirm={handleRetryProvisioning}
          suspendPending={suspendMutation.isPending}
          activatePending={activateMutation.isPending}
          extendPending={extendMutation.isPending}
          retryPending={retryMutation.isPending}
          extendDays={extendDays}
          onExtendDaysChange={setExtendDays}
        />
      )}
    </div>
  );
}
