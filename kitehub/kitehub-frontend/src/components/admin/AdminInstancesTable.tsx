'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Eye, MoreHorizontal, Pause, Play, Search } from 'lucide-react';
import { useSuspendInstance, useActivateInstance } from '@/hooks/use-admin';
import type { AdminInstanceSummary } from '@/types/admin';
import type { InstanceStatus, SubscriptionTier } from '@/types/instance';
import { toast } from 'sonner';

interface AdminInstancesTableProps {
  instances: AdminInstanceSummary[];
}

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

export function AdminInstancesTable({ instances }: AdminInstancesTableProps) {
  const router = useRouter();
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<InstanceStatus | 'ALL'>('ALL');
  const [tierFilter, setTierFilter] = useState<SubscriptionTier | 'ALL'>('ALL');

  // Action states
  const [actionInstance, setActionInstance] = useState<AdminInstanceSummary | null>(null);
  const [actionType, setActionType] = useState<'suspend' | 'activate' | null>(null);

  const suspendMutation = useSuspendInstance();
  const activateMutation = useActivateInstance();

  const formatDate = (date: string | null) => {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  // Filter instances
  const filteredInstances = instances.filter((instance) => {
    const matchesSearch =
      instance.organizationName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      instance.subdomain.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (instance.ownerEmail?.toLowerCase().includes(searchTerm.toLowerCase()) ?? false);

    const matchesStatus = statusFilter === 'ALL' || instance.status === statusFilter;
    const matchesTier = tierFilter === 'ALL' || instance.tier === tierFilter;

    return matchesSearch && matchesStatus && matchesTier;
  });

  const handleAction = (instance: AdminInstanceSummary, action: 'suspend' | 'activate') => {
    setActionInstance(instance);
    setActionType(action);
  };

  const confirmAction = async () => {
    if (!actionInstance || !actionType) return;

    try {
      if (actionType === 'suspend') {
        await suspendMutation.mutateAsync(actionInstance.id);
        toast.success(`Đã tạm ngưng instance ${actionInstance.organizationName}`);
      } else {
        await activateMutation.mutateAsync(actionInstance.id);
        toast.success(`Đã kích hoạt instance ${actionInstance.organizationName}`);
      }
    } catch {
      toast.error('Có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setActionInstance(null);
      setActionType(null);
    }
  };

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex flex-wrap items-center gap-4">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Tìm theo tên, subdomain, email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>

        <Select
          value={statusFilter}
          onValueChange={(value) => setStatusFilter(value as InstanceStatus | 'ALL')}
        >
          <SelectTrigger className="w-[150px]">
            <SelectValue placeholder="Trạng thái" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tất cả</SelectItem>
            <SelectItem value="TRIAL">Dùng thử</SelectItem>
            <SelectItem value="ACTIVE">Hoạt động</SelectItem>
            <SelectItem value="SUSPENDED">Tạm ngưng</SelectItem>
            <SelectItem value="EXPIRED">Hết hạn</SelectItem>
          </SelectContent>
        </Select>

        <Select
          value={tierFilter}
          onValueChange={(value) => setTierFilter(value as SubscriptionTier | 'ALL')}
        >
          <SelectTrigger className="w-[150px]">
            <SelectValue placeholder="Gói" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tất cả</SelectItem>
            <SelectItem value="FREE">Free</SelectItem>
            <SelectItem value="BASIC">Basic</SelectItem>
            <SelectItem value="PREMIUM">Premium</SelectItem>
            <SelectItem value="ENTERPRISE">Enterprise</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <div className="border rounded-lg">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tổ chức</TableHead>
              <TableHead>Subdomain</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Gói</TableHead>
              <TableHead>Trial/Sub End</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead className="text-right">Hành động</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredInstances.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground py-8">
                  Không tìm thấy instance nào
                </TableCell>
              </TableRow>
            ) : (
              filteredInstances.map((instance) => (
                <TableRow key={instance.id}>
                  <TableCell>
                    <div>
                      <div className="font-medium">{instance.organizationName}</div>
                      {instance.ownerEmail && (
                        <div className="text-sm text-muted-foreground">
                          {instance.ownerEmail}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <code className="text-sm">{instance.subdomain}</code>
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusConfig[instance.status]?.variant ?? 'outline'}>
                      {statusConfig[instance.status]?.label ?? instance.status ?? 'N/A'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <span className={tierConfig[instance.tier]?.color ?? 'text-muted-foreground'}>
                      {tierConfig[instance.tier]?.label ?? instance.tier ?? 'N/A'}
                    </span>
                  </TableCell>
                  <TableCell>
                    {instance.status === 'TRIAL'
                      ? formatDate(instance.trialEndDate)
                      : formatDate(instance.subscriptionEndDate)}
                  </TableCell>
                  <TableCell>{formatDate(instance.createdAt)}</TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" aria-label="Instance actions">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => router.push(`/admin/instances/${instance.id}`)}
                        >
                          <Eye className="mr-2 h-4 w-4" />
                          Xem chi tiết
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        {instance.status !== 'SUSPENDED' ? (
                          <DropdownMenuItem
                            onClick={() => handleAction(instance, 'suspend')}
                            className="text-destructive"
                          >
                            <Pause className="mr-2 h-4 w-4" />
                            Tạm ngưng
                          </DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem
                            onClick={() => handleAction(instance, 'activate')}
                            className="text-green-600 dark:text-green-400"
                          >
                            <Play className="mr-2 h-4 w-4" />
                            Kích hoạt
                          </DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Summary */}
      <div className="flex justify-between items-center text-sm text-muted-foreground">
        <span>
          Hiển thị {filteredInstances.length} / {instances.length} instances
        </span>
      </div>

      {/* Confirmation Dialog */}
      <AlertDialog open={!!actionInstance} onOpenChange={() => setActionInstance(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {actionType === 'suspend' ? 'Tạm ngưng instance?' : 'Kích hoạt instance?'}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {actionType === 'suspend' ? (
                <>
                  Bạn có chắc muốn tạm ngưng instance{' '}
                  <strong>{actionInstance?.organizationName}</strong>? Khách hàng sẽ không
                  thể truy cập instance này cho đến khi được kích hoạt lại.
                </>
              ) : (
                <>
                  Bạn có chắc muốn kích hoạt lại instance{' '}
                  <strong>{actionInstance?.organizationName}</strong>?
                </>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmAction}
              className={actionType === 'suspend' ? 'bg-destructive hover:bg-destructive/90' : ''}
            >
              {actionType === 'suspend' ? 'Tạm ngưng' : 'Kích hoạt'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
