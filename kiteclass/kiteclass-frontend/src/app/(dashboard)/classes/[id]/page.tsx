/**
 * Class detail page with lifecycle actions and sessions view.
 *
 * @author KiteClass Team
 * @since 3.7.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Pencil,
  Trash2,
  Play,
  CheckCircle,
  XCircle,
  Copy,
  Calendar,
  CalendarClock,
  MapPin,
  Users,
} from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { StatusBadge, LoadingSpinner, ErrorAlert } from '@/components/common';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  useClass,
  useDeleteClass,
  useStartClass,
  useCompleteClass,
  useCancelClass,
  useRescheduleClass,
  useClassSessions,
  useGenerateClassCode,
} from '@/hooks/use-classes';
import { formatDate, formatDateTime } from '@/lib/utils';
import {
  ClassStatus,
  RESCHEDULE_REASON_LABELS,
  type RescheduleReasonCategory,
} from '@/types/class';
import { toast } from '@/hooks/use-toast';

const statusVariants: Record<
  ClassStatus,
  'success' | 'warning' | 'default' | 'error' | 'info'
> = {
  DRAFT: 'default',
  SCHEDULED: 'info',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'error',
};

const statusLabels: Record<ClassStatus, string> = {
  DRAFT: 'Nháp',
  SCHEDULED: 'Đã lên lịch',
  IN_PROGRESS: 'Đang diễn ra',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
};

export default function ClassDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const classId = parseInt(id);
  const router = useRouter();
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [showRescheduleDialog, setShowRescheduleDialog] = useState(false);
  const [rescheduleStartDate, setRescheduleStartDate] = useState('');
  const [rescheduleEndDate, setRescheduleEndDate] = useState('');
  const [rescheduleReasonCategory, setRescheduleReasonCategory] = useState<
    RescheduleReasonCategory | ''
  >('');
  const [rescheduleReasonNotes, setRescheduleReasonNotes] = useState('');

  const { data: classData, isLoading, error } = useClass(classId);
  const { data: sessions } = useClassSessions(classId);

  const deleteMutation = useDeleteClass();
  const startMutation = useStartClass();
  const completeMutation = useCompleteClass();
  const cancelMutation = useCancelClass();
  const rescheduleMutation = useRescheduleClass();
  const generateCodeMutation = useGenerateClassCode();

  const handleDelete = () => {
    if (window.confirm('Xóa lớp học này? Hành động không thể hoàn tác.')) {
      deleteMutation.mutate(classId, {
        onSuccess: () => router.push('/classes'),
      });
    }
  };

  const handleStart = () => {
    if (window.confirm('Bắt đầu lớp học? Sau khi bắt đầu, một số trường sẽ bị giới hạn chỉnh sửa.')) {
      startMutation.mutate(classId);
    }
  };

  const handleComplete = () => {
    if (window.confirm('Hoàn thành lớp học? Lớp học sẽ chuyển sang chế độ chỉ đọc.')) {
      completeMutation.mutate(classId);
    }
  };

  const handleCancel = () => {
    if (!cancelReason.trim()) {
      toast({ title: 'Lỗi', description: 'Vui lòng nhập lý do hủy', variant: 'destructive' });
      return;
    }
    cancelMutation.mutate(
      { id: classId, data: { reason: cancelReason } },
      {
        onSuccess: () => setShowCancelDialog(false),
      }
    );
  };

  const openRescheduleDialog = () => {
    // Pre-fill with current dates so user only changes what's needed.
    setRescheduleStartDate(classData?.startDate ?? '');
    setRescheduleEndDate(classData?.endDate ?? '');
    setRescheduleReasonCategory('');
    setRescheduleReasonNotes('');
    setShowRescheduleDialog(true);
  };

  const handleReschedule = () => {
    if (!rescheduleStartDate || !rescheduleEndDate) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn ngày bắt đầu và ngày kết thúc mới',
        variant: 'destructive',
      });
      return;
    }
    if (!rescheduleReasonCategory) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn lý do đổi lịch',
        variant: 'destructive',
      });
      return;
    }
    if (new Date(rescheduleEndDate) <= new Date(rescheduleStartDate)) {
      toast({
        title: 'Lỗi',
        description: 'Ngày kết thúc phải sau ngày bắt đầu',
        variant: 'destructive',
      });
      return;
    }
    rescheduleMutation.mutate(
      {
        id: classId,
        data: {
          newStartDate: rescheduleStartDate,
          newEndDate: rescheduleEndDate,
          reasonCategory: rescheduleReasonCategory,
          reasonNotes: rescheduleReasonNotes.trim() || undefined,
        },
      },
      {
        onSuccess: () => setShowRescheduleDialog(false),
      }
    );
  };

  const handleGenerateCode = () => {
    if (window.confirm('Tạo hoặc tạo lại mã lớp học?')) {
      generateCodeMutation.mutate({ id: classId });
    }
  };

  const handleCopyCode = () => {
    if (classData?.classCode) {
      navigator.clipboard.writeText(classData.classCode);
      toast({ title: 'Thành công', description: 'Đã sao chép mã lớp học' });
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout>
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      </DashboardLayout>
    );
  }

  if (error || !classData) {
    return (
      <DashboardLayout>
        <ErrorAlert title="Lỗi" message="Không tìm thấy lớp học" backHref="/classes" backLabel="Quay lại danh sách lớp học" />
      </DashboardLayout>
    );
  }

  const isDraft = classData.status === 'DRAFT';
  const isScheduled = classData.status === 'SCHEDULED';
  const isInProgress = classData.status === 'IN_PROGRESS';

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              {/* Wave 30 Bucket B — kc-pro-v2 typography tokens. */}
              <h1 className="text-3xl font-semibold tracking-tight">
                {classData.name}
              </h1>
              <StatusBadge
                status={statusLabels[classData.status]}
                variant={statusVariants[classData.status]}
              />
            </div>
            {classData.classCode && (
              <div className="mt-2 flex items-center gap-2">
                <p className="font-mono text-sm font-medium">Mã: {classData.classCode}</p>
                <Button variant="ghost" size="icon" className="h-6 w-6" onClick={handleCopyCode}>
                  <Copy className="h-3 w-3" />
                </Button>
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex flex-wrap gap-2">
            {isScheduled && (
              <Button onClick={handleStart} disabled={startMutation.isPending}>
                <Play className="mr-2 h-4 w-4" />
                Bắt đầu
              </Button>
            )}
            {isInProgress && (
              <>
                <Button onClick={handleComplete} disabled={completeMutation.isPending}>
                  <CheckCircle className="mr-2 h-4 w-4" />
                  Hoàn thành
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowCancelDialog(true)}
                  disabled={cancelMutation.isPending}
                >
                  <XCircle className="mr-2 h-4 w-4" />
                  Hủy lớp
                </Button>
              </>
            )}
            {isScheduled && (
              <Button
                variant="outline"
                onClick={openRescheduleDialog}
                disabled={rescheduleMutation.isPending}
              >
                <CalendarClock className="mr-2 h-4 w-4" />
                Đổi lịch
              </Button>
            )}
            {(isDraft || isScheduled) && (
              <>
                <Button variant="outline" onClick={handleGenerateCode}>
                  Tạo mã lớp
                </Button>
                <Link href={`/classes/${id}/edit`}>
                  <Button variant="outline">
                    <Pencil className="mr-2 h-4 w-4" />
                    Chỉnh sửa
                  </Button>
                </Link>
              </>
            )}
            {isInProgress && (
              <Link href={`/classes/${id}/edit`}>
                <Button variant="outline">
                  <Pencil className="mr-2 h-4 w-4" />
                  Chỉnh sửa
                </Button>
              </Link>
            )}
            {isScheduled && classData.currentEnrolled === 0 && (
              <Button
                variant="destructive"
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Xóa
              </Button>
            )}
          </div>
        </div>

        {/* Reschedule Dialog (Wave beta-readiness-4 Bucket D — GAP-291).
            3-click flow: open modal → fill dates+reason → submit. */}
        {showRescheduleDialog && (
          <Card>
            <CardHeader>
              <CardTitle>Đổi lịch lớp học</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2" htmlFor="reschedule-start">
                    Ngày bắt đầu mới <span className="text-destructive">*</span>
                  </label>
                  <input
                    id="reschedule-start"
                    type="date"
                    className="w-full rounded-md border p-2"
                    value={rescheduleStartDate}
                    onChange={(e) => setRescheduleStartDate(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2" htmlFor="reschedule-end">
                    Ngày kết thúc mới <span className="text-destructive">*</span>
                  </label>
                  <input
                    id="reschedule-end"
                    type="date"
                    className="w-full rounded-md border p-2"
                    value={rescheduleEndDate}
                    onChange={(e) => setRescheduleEndDate(e.target.value)}
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium mb-2" htmlFor="reschedule-reason">
                  Lý do đổi lịch <span className="text-destructive">*</span>
                </label>
                <select
                  id="reschedule-reason"
                  className="w-full rounded-md border p-2"
                  value={rescheduleReasonCategory}
                  onChange={(e) =>
                    setRescheduleReasonCategory(e.target.value as RescheduleReasonCategory | '')
                  }
                >
                  <option value="">— Chọn lý do —</option>
                  {(Object.keys(RESCHEDULE_REASON_LABELS) as RescheduleReasonCategory[]).map(
                    (key) => (
                      <option key={key} value={key}>
                        {RESCHEDULE_REASON_LABELS[key]}
                      </option>
                    )
                  )}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-2" htmlFor="reschedule-notes">
                  Ghi chú (tùy chọn)
                </label>
                <textarea
                  id="reschedule-notes"
                  className="w-full rounded-md border p-2"
                  rows={3}
                  maxLength={2000}
                  value={rescheduleReasonNotes}
                  onChange={(e) => setRescheduleReasonNotes(e.target.value)}
                  placeholder="Ví dụ: Cô giáo phụ trách lớp xin nghỉ ốm 1 tuần."
                />
              </div>
              <p className="text-sm text-muted-foreground">
                💡 Phụ huynh sẽ nhận email thông báo nếu kênh thông báo đã được bật. Email vận
                hành luôn được gửi, không phụ thuộc trạng thái marketing.
              </p>
              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setShowRescheduleDialog(false)}>
                  Đóng
                </Button>
                <Button onClick={handleReschedule} disabled={rescheduleMutation.isPending}>
                  Xác nhận đổi lịch
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Cancel Dialog */}
        {showCancelDialog && (
          <Card>
            <CardHeader>
              <CardTitle>Hủy lớp học</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2">
                  Lý do hủy <span className="text-destructive">*</span>
                </label>
                <textarea
                  className="w-full rounded-md border p-2"
                  rows={3}
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  placeholder="Nhập lý do hủy lớp học..."
                />
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setShowCancelDialog(false)}>
                  Đóng
                </Button>
                <Button
                  variant="destructive"
                  onClick={handleCancel}
                  disabled={cancelMutation.isPending}
                >
                  Xác nhận hủy
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Info Card */}
        <Card>
          <CardHeader>
            <CardTitle>Thông tin lớp học</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Basic Info */}
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-sm text-muted-foreground">Sĩ số</p>
                  <p className="font-medium">
                    {classData.currentEnrolled}/{classData.maxStudents}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-sm text-muted-foreground">Địa điểm</p>
                  <p className="font-medium">
                    {classData.locationType === 'IN_PERSON' ? 'Trực tiếp' : 'Trực tuyến'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-sm text-muted-foreground">Thời gian</p>
                  <p className="font-medium">
                    {classData.startDate
                      ? `${formatDate(classData.startDate)}${classData.endDate ? ` - ${formatDate(classData.endDate)}` : ''}`
                      : '—'}
                  </p>
                </div>
              </div>
            </div>

            {/* Schedule */}
            {classData.schedule && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">Lịch học</p>
                <p className="mt-1">{classData.schedule}</p>
              </div>
            )}

            {/* Location Detail */}
            {classData.locationDetail && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">Chi tiết địa điểm</p>
                <p className="mt-1">{classData.locationDetail}</p>
              </div>
            )}

            {/* Description */}
            {classData.description && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">Mô tả</p>
                <p className="mt-1 whitespace-pre-line">{classData.description}</p>
              </div>
            )}

            {/* Timestamps */}
            <div className="grid grid-cols-2 gap-4 border-t pt-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Ngày tạo</p>
                <p className="mt-1">{formatDateTime(classData.createdAt)}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">Cập nhật lần cuối</p>
                <p className="mt-1">{formatDateTime(classData.updatedAt)}</p>
              </div>
              {classData.startedAt && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Ngày bắt đầu</p>
                  <p className="mt-1">{formatDateTime(classData.startedAt)}</p>
                </div>
              )}
              {classData.completedAt && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Ngày hoàn thành</p>
                  <p className="mt-1">{formatDateTime(classData.completedAt)}</p>
                </div>
              )}
              {classData.cancelledAt && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Ngày hủy</p>
                  <p className="mt-1">{formatDateTime(classData.cancelledAt)}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Sessions Card */}
        <Card>
          <CardHeader>
            <CardTitle>Buổi học</CardTitle>
          </CardHeader>
          <CardContent>
            {!sessions || sessions.length === 0 ? (
              <p className="text-center text-muted-foreground py-8">
                Chưa có buổi học nào được tạo
              </p>
            ) : (
              <div className="space-y-2">
                {sessions.map((session) => (
                  <div
                    key={session.id}
                    className="flex items-center justify-between rounded-md border p-3"
                  >
                    <div>
                      <p className="font-medium">
                        Buổi {session.sessionNumber}: {session.topic || 'Chưa có chủ đề'}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {formatDate(session.sessionDate)} • {session.startTime} - {session.endTime}
                      </p>
                    </div>
                    <StatusBadge
                      status={session.status}
                      variant={
                        session.status === 'COMPLETED'
                          ? 'success'
                          : session.status === 'CANCELLED'
                            ? 'error'
                            : 'default'
                      }
                    />
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
