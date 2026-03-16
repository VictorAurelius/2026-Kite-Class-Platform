'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { AlertTriangle, XCircle, Trash2 } from 'lucide-react';
import { Instance } from '@/types/instance';
import { useRouter } from 'next/navigation';

interface DangerZoneProps {
  instance: Instance | undefined;
}

export function DangerZone({ instance }: DangerZoneProps) {
  const router = useRouter();
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [confirmInstanceName, setConfirmInstanceName] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [isCanceling, setIsCanceling] = useState(false);

  const instanceName = instance?.organizationName || instance?.subdomain || '';
  const canDelete = confirmInstanceName === instanceName;

  const handleCancelSubscription = async () => {
    setIsCanceling(true);
    try {
      // TODO: Implement cancel subscription API call
      // await cancelSubscription(instance?.subscriptionId);
      await new Promise(resolve => setTimeout(resolve, 1000));
      setCancelDialogOpen(false);
      router.push('/billing?success=cancelled');
    } catch (error) {
      console.error('Failed to cancel subscription:', error);
    } finally {
      setIsCanceling(false);
    }
  };

  const handleDeleteInstance = async () => {
    if (!canDelete) return;

    setIsDeleting(true);
    try {
      // TODO: Implement delete instance API call
      // await deleteInstance(instance?.id);
      await new Promise(resolve => setTimeout(resolve, 1000));
      setDeleteDialogOpen(false);
      router.push('/?deleted=true');
    } catch (error) {
      console.error('Failed to delete instance:', error);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <Alert variant="destructive">
        <AlertTriangle className="h-4 w-4" />
        <AlertTitle>Khu vực nguy hiểm</AlertTitle>
        <AlertDescription>
          Các hành động trong khu vực này có thể ảnh hưởng nghiêm trọng đến dữ liệu của bạn.
          Vui lòng cân nhắc kỹ trước khi thực hiện.
        </AlertDescription>
      </Alert>

      {/* Cancel Subscription */}
      <Card className="border-destructive/50">
        <CardHeader>
          <div className="flex items-center gap-2">
            <XCircle className="h-5 w-5 text-destructive" />
            <CardTitle className="text-destructive">Hủy đăng ký</CardTitle>
          </div>
          <CardDescription>
            Hủy gói đăng ký hiện tại. Instance sẽ chuyển về trạng thái dùng thử
            hoặc bị tạm ngưng sau khi hết hạn.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Dialog open={cancelDialogOpen} onOpenChange={setCancelDialogOpen}>
            <DialogTrigger asChild>
              <Button variant="destructive">
                <XCircle className="h-4 w-4 mr-2" />
                Hủy đăng ký
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Xác nhận hủy đăng ký</DialogTitle>
                <DialogDescription>
                  Bạn có chắc chắn muốn hủy đăng ký? Sau khi hủy:
                </DialogDescription>
              </DialogHeader>
              <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
                <li>Gói đăng ký sẽ còn hiệu lực đến hết chu kỳ thanh toán</li>
                <li>Sau đó, instance sẽ bị tạm ngưng</li>
                <li>Dữ liệu sẽ được giữ lại trong 30 ngày</li>
                <li>Bạn có thể đăng ký lại bất cứ lúc nào</li>
              </ul>
              <DialogFooter className="gap-2 sm:gap-0">
                <Button
                  variant="outline"
                  onClick={() => setCancelDialogOpen(false)}
                >
                  Hủy bỏ
                </Button>
                <Button
                  variant="destructive"
                  onClick={handleCancelSubscription}
                  disabled={isCanceling}
                >
                  {isCanceling ? 'Đang xử lý...' : 'Xác nhận hủy'}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </CardContent>
      </Card>

      {/* Delete Instance */}
      <Card className="border-destructive">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Trash2 className="h-5 w-5 text-destructive" />
            <CardTitle className="text-destructive">Xóa Instance</CardTitle>
          </div>
          <CardDescription>
            Xóa vĩnh viễn instance và tất cả dữ liệu. Hành động này không thể hoàn tác.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
            <DialogTrigger asChild>
              <Button variant="destructive">
                <Trash2 className="h-4 w-4 mr-2" />
                Xóa Instance
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle className="text-destructive">
                  Xóa Instance vĩnh viễn
                </DialogTitle>
                <DialogDescription>
                  Hành động này sẽ xóa vĩnh viễn:
                </DialogDescription>
              </DialogHeader>
              <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
                <li>Tất cả học viên, giáo viên, khóa học</li>
                <li>Lịch sử điểm danh, thanh toán</li>
                <li>Tài liệu, hình ảnh đã tải lên</li>
                <li>Cấu hình branding, tên miền</li>
              </ul>

              <Alert variant="destructive" className="mt-4">
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>
                  Dữ liệu <strong>không thể khôi phục</strong> sau khi xóa!
                </AlertDescription>
              </Alert>

              <div className="space-y-2 mt-4">
                <Label htmlFor="confirmName">
                  Nhập <strong className="text-destructive">{instanceName}</strong> để xác nhận:
                </Label>
                <Input
                  id="confirmName"
                  value={confirmInstanceName}
                  onChange={(e) => setConfirmInstanceName(e.target.value)}
                  placeholder={instanceName}
                  className="border-destructive/50 focus:border-destructive"
                />
              </div>

              <DialogFooter className="gap-2 sm:gap-0 mt-4">
                <Button
                  variant="outline"
                  onClick={() => {
                    setDeleteDialogOpen(false);
                    setConfirmInstanceName('');
                  }}
                >
                  Hủy bỏ
                </Button>
                <Button
                  variant="destructive"
                  onClick={handleDeleteInstance}
                  disabled={!canDelete || isDeleting}
                >
                  {isDeleting ? 'Đang xóa...' : 'Xóa vĩnh viễn'}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </CardContent>
      </Card>
    </div>
  );
}
