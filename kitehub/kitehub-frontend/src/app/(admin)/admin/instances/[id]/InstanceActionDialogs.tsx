'use client';

/**
 * Lazy-loaded admin instance action dialogs (Suspend / Activate / Extend Trial).
 *
 * GAP-236 Sub-PR B (Wave GAP-236 Agent D) — code-split heavy radix `Dialog`
 * + `AlertDialog` trees off the initial chunk for `/admin/instances/[id]`.
 * The page renders quickly with the read-only detail cards; mutation dialogs
 * only ship when the admin actually triggers an action.
 */

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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Loader2 } from 'lucide-react';

export interface InstanceActionDialogsProps {
  organizationName: string;
  trialEndDateLabel: string;
  showSuspendDialog: boolean;
  showActivateDialog: boolean;
  showExtendDialog: boolean;
  showRetryDialog: boolean;
  onSuspendOpenChange: (open: boolean) => void;
  onActivateOpenChange: (open: boolean) => void;
  onExtendOpenChange: (open: boolean) => void;
  onRetryOpenChange: (open: boolean) => void;
  onSuspendConfirm: () => void;
  onActivateConfirm: () => void;
  onExtendConfirm: () => void;
  onRetryConfirm: () => void;
  suspendPending: boolean;
  activatePending: boolean;
  extendPending: boolean;
  retryPending: boolean;
  extendDays: string;
  onExtendDaysChange: (value: string) => void;
}

export default function InstanceActionDialogs(props: InstanceActionDialogsProps) {
  const {
    organizationName,
    trialEndDateLabel,
    showSuspendDialog,
    showActivateDialog,
    showExtendDialog,
    showRetryDialog,
    onSuspendOpenChange,
    onActivateOpenChange,
    onExtendOpenChange,
    onRetryOpenChange,
    onSuspendConfirm,
    onActivateConfirm,
    onExtendConfirm,
    onRetryConfirm,
    suspendPending,
    activatePending,
    extendPending,
    retryPending,
    extendDays,
    onExtendDaysChange,
  } = props;

  return (
    <>
      {/* Suspend Dialog */}
      <AlertDialog open={showSuspendDialog} onOpenChange={onSuspendOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Tạm ngưng instance?</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc muốn tạm ngưng instance{' '}
              <strong>{organizationName}</strong>? Khách hàng sẽ không
              thể truy cập instance này cho đến khi được kích hoạt lại.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={onSuspendConfirm}
              className="bg-destructive hover:bg-destructive/90"
              disabled={suspendPending}
            >
              {suspendPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Tạm ngưng
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Activate Dialog */}
      <AlertDialog open={showActivateDialog} onOpenChange={onActivateOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Kích hoạt instance?</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc muốn kích hoạt lại instance{' '}
              <strong>{organizationName}</strong>?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={onActivateConfirm}
              disabled={activatePending}
            >
              {activatePending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Kích hoạt
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Extend Trial Dialog */}
      <Dialog open={showExtendDialog} onOpenChange={onExtendOpenChange}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Gia hạn Trial</DialogTitle>
            <DialogDescription>
              Gia hạn thời gian dùng thử cho instance{' '}
              <strong>{organizationName}</strong>
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="days">Số ngày gia hạn</Label>
              <Input
                id="days"
                type="number"
                min="1"
                max="90"
                value={extendDays}
                onChange={(e) => onExtendDaysChange(e.target.value)}
              />
            </div>
            <p className="text-sm text-muted-foreground">
              Trial hiện tại kết thúc: {trialEndDateLabel}
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => onExtendOpenChange(false)}>
              Hủy
            </Button>
            <Button onClick={onExtendConfirm} disabled={extendPending}>
              {extendPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Gia hạn
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Retry Provisioning Dialog (GAP-953, UC-PROV-05) */}
      <AlertDialog open={showRetryDialog} onOpenChange={onRetryOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Thử lại Provisioning?</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc muốn kích hoạt lại quá trình provisioning cho instance{' '}
              <strong>{organizationName}</strong>? Hệ thống sẽ phát lại sự kiện{' '}
              <code>tenant.created</code> để chạy lại saga tạo tenant KiteClass cho
              instance đang ở trạng thái lỗi/treo.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction onClick={onRetryConfirm} disabled={retryPending}>
              {retryPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Thử lại
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
