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
import { AlertTriangle, XCircle, Trash2, Download, Undo2 } from 'lucide-react';
import { Instance } from '@/types/instance';
import { useRouter } from 'next/navigation';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

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
  // GAP-1268 — cancel wizard: step 1 = export data, step 2 = confirm + undo notice.
  const [cancelStep, setCancelStep] = useState<1 | 2>(1);
  const [isExporting, setIsExporting] = useState(false);
  const [exportMessage, setExportMessage] = useState<string | null>(null);

  const instanceName = instance?.organizationName || instance?.subdomain || '';
  const canDelete = confirmInstanceName === instanceName;

  const resetCancelWizard = (open: boolean) => {
    setCancelDialogOpen(open);
    if (!open) {
      setCancelStep(1);
      setExportMessage(null);
    }
  };

  // GAP-1268 — "Tải dữ liệu về". Code-to-contract: BE export endpoint may not
  // exist yet → handle failure gracefully (KHÔNG block cancel flow).
  const handleExportData = async () => {
    if (!instance?.id) return;
    setIsExporting(true);
    setExportMessage(null);
    try {
      await apiClient.get(endpoints.instances.exportData(instance.id));
      setExportMessage(
        'Đã gửi yêu cầu xuất dữ liệu — bạn sẽ nhận liên kết tải về qua email khi sẵn sàng.'
      );
    } catch (error) {
      console.error('Failed to export instance data:', error);
      setExportMessage(
        'Tính năng xuất dữ liệu đang được hoàn thiện. Vui lòng liên hệ hỗ trợ để được trợ giúp xuất dữ liệu trước khi hủy.'
      );
    } finally {
      setIsExporting(false);
    }
  };

  const handleCancelSubscription = async () => {
    setIsCanceling(true);
    try {
      if (instance?.subscriptionId) {
        await apiClient.delete(endpoints.subscriptions.cancel(instance.subscriptionId));
      }
      resetCancelWizard(false);
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
      if (instance?.id) {
        await apiClient.delete(endpoints.instances.delete(instance.id));
      }
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
          <Dialog open={cancelDialogOpen} onOpenChange={resetCancelWizard}>
            <DialogTrigger asChild>
              <Button variant="destructive">
                <XCircle className="h-4 w-4 mr-2" />
                Hủy đăng ký
              </Button>
            </DialogTrigger>
            <DialogContent>
              {/* GAP-1268 — Cancel wizard. Bước 1: tải dữ liệu. Bước 2: xác nhận + nhắc hoàn tác 30 ngày. */}
              {cancelStep === 1 ? (
                <div data-testid="cancel-wizard-step-1">
                  <DialogHeader>
                    <DialogTitle>Trước khi hủy — tải dữ liệu về</DialogTitle>
                    <DialogDescription>
                      Bạn nên tải dữ liệu của trung tâm về máy trước khi hủy đăng ký, phòng
                      trường hợp cần dùng lại sau này.
                    </DialogDescription>
                  </DialogHeader>
                  <div className="rounded-xl border bg-muted/40 p-4 space-y-3">
                    <div className="flex items-start gap-3">
                      <Download className="h-5 w-5 text-primary mt-0.5 shrink-0" />
                      <div className="text-sm">
                        <p className="font-medium">Xuất dữ liệu trung tâm</p>
                        <p className="text-muted-foreground">
                          Học viên, khóa học, điểm danh, hóa đơn — xuất ra file để lưu trữ.
                        </p>
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      onClick={handleExportData}
                      disabled={isExporting}
                      data-testid="cancel-wizard-export-cta"
                    >
                      <Download className="h-4 w-4 mr-2" />
                      {isExporting ? 'Đang chuẩn bị...' : 'Tải dữ liệu về'}
                    </Button>
                    {exportMessage && (
                      <p className="text-sm text-muted-foreground" data-testid="cancel-wizard-export-msg">
                        {exportMessage}
                      </p>
                    )}
                  </div>
                  <DialogFooter className="gap-2 sm:gap-0 mt-4">
                    <Button variant="outline" onClick={() => resetCancelWizard(false)}>
                      Hủy bỏ
                    </Button>
                    <Button
                      variant="destructive"
                      onClick={() => setCancelStep(2)}
                      data-testid="cancel-wizard-continue"
                    >
                      Tiếp tục hủy đăng ký
                    </Button>
                  </DialogFooter>
                </div>
              ) : (
                <div data-testid="cancel-wizard-step-2">
                  <DialogHeader>
                    <DialogTitle>Xác nhận hủy đăng ký</DialogTitle>
                    <DialogDescription>
                      Bạn có chắc chắn muốn hủy đăng ký? Sau khi hủy:
                    </DialogDescription>
                  </DialogHeader>

                  {/* Prominent "Hoàn tác trong 30 ngày" messaging */}
                  <Alert className="border-primary/40 bg-primary/5">
                    <Undo2 className="h-4 w-4" />
                    <AlertTitle>Có thể hoàn tác trong 30 ngày</AlertTitle>
                    <AlertDescription>
                      Dữ liệu của bạn được giữ lại <strong>30 ngày</strong>. Trong thời gian
                      này bạn có thể <strong>kích hoạt lại</strong> để khôi phục đầy đủ —
                      không mất dữ liệu.
                    </AlertDescription>
                  </Alert>

                  <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground mt-3">
                    <li>Gói đăng ký sẽ còn hiệu lực đến hết chu kỳ thanh toán</li>
                    <li>Sau đó, instance sẽ bị tạm ngưng</li>
                    <li>Dữ liệu được giữ lại trong 30 ngày (có thể hoàn tác)</li>
                    <li>Bạn có thể đăng ký lại bất cứ lúc nào</li>
                  </ul>
                  <DialogFooter className="gap-2 sm:gap-0 mt-4">
                    <Button variant="outline" onClick={() => setCancelStep(1)}>
                      Quay lại
                    </Button>
                    <Button
                      variant="destructive"
                      onClick={handleCancelSubscription}
                      disabled={isCanceling}
                      data-testid="cancel-wizard-confirm"
                    >
                      {isCanceling ? 'Đang xử lý...' : 'Xác nhận hủy'}
                    </Button>
                  </DialogFooter>
                </div>
              )}
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
