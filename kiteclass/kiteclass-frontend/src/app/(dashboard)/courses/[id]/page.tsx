/**
 * Course detail page with lifecycle actions (Publish / Archive).
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { use, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Pencil, Trash2, BookOpen, Archive, CalendarPlus } from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { StatusBadge, LoadingSpinner, ErrorAlert } from '@/components/common';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useCourse, useDeleteCourse, usePublishCourse, useArchiveCourse } from '@/hooks/use-courses';
import { CourseStatus } from '@/types/course';
import { CourseContentManager } from '@/components/lms/course-content-manager';

const statusVariants: Record<CourseStatus, 'success' | 'warning' | 'default' | 'error'> = {
  [CourseStatus.DRAFT]: 'warning',
  [CourseStatus.PUBLISHED]: 'success',
  [CourseStatus.ARCHIVED]: 'default',
};

const statusLabels: Record<CourseStatus, string> = {
  [CourseStatus.DRAFT]: 'Bản nháp',
  [CourseStatus.PUBLISHED]: 'Đã xuất bản',
  [CourseStatus.ARCHIVED]: 'Đã lưu trữ',
};

export default function CourseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const courseId = parseInt(id);
  const router = useRouter();

  const { data: course, isLoading, error } = useCourse(courseId);
  const deleteMutation = useDeleteCourse();
  const publishMutation = usePublishCourse();
  const archiveMutation = useArchiveCourse();

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [publishDialogOpen, setPublishDialogOpen] = useState(false);
  const [archiveDialogOpen, setArchiveDialogOpen] = useState(false);

  const handleDelete = () => {
    deleteMutation.mutate(courseId, {
      onSuccess: () => router.push('/courses'),
    });
  };

  const handlePublish = () => {
    publishMutation.mutate(courseId);
  };

  const handleArchive = () => {
    archiveMutation.mutate(courseId);
  };

  if (isLoading) {
    return (
      <DashboardLayout>
        <div className="flex justify-center py-12"><LoadingSpinner /></div>
      </DashboardLayout>
    );
  }

  if (error || !course) {
    return (
      <DashboardLayout>
        <ErrorAlert title="Lỗi" message="Không tìm thấy khóa học" backHref="/courses" backLabel="Quay lại danh sách khóa học" />
      </DashboardLayout>
    );
  }

  const isDraft = course.status === CourseStatus.DRAFT;
  const isPublished = course.status === CourseStatus.PUBLISHED;

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              {/* Wave 30 Bucket B — kc-pro-v2 typography tokens. */}
              <h1 className="text-3xl font-semibold tracking-tight">
                {course.name}
              </h1>
              <StatusBadge
                status={statusLabels[course.status]}
                variant={statusVariants[course.status]}
              />
            </div>
            <p className="mt-1 font-mono text-sm text-muted-foreground">{course.code}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {isDraft && (
              <Button onClick={() => setPublishDialogOpen(true)} disabled={publishMutation.isPending}>
                <BookOpen className="mr-2 h-4 w-4" />
                Xuất bản
              </Button>
            )}
            {isPublished && (
              <Button variant="outline" onClick={() => setArchiveDialogOpen(true)} disabled={archiveMutation.isPending}>
                <Archive className="mr-2 h-4 w-4" />
                Lưu trữ
              </Button>
            )}
            {!course.status.includes('ARCHIVED') && (
              <Link href={`/courses/${id}/classes/new`}>
                <Button variant="outline">
                  <CalendarPlus className="mr-2 h-4 w-4" />
                  Thêm lớp học
                </Button>
              </Link>
            )}
            {!course.status.includes('ARCHIVED') && (
              <Link href={`/courses/${id}/edit`}>
                <Button variant="outline">
                  <Pencil className="mr-2 h-4 w-4" />
                  Chỉnh sửa
                </Button>
              </Link>
            )}
            {isDraft && (
              <Button variant="destructive" onClick={() => setDeleteDialogOpen(true)} disabled={deleteMutation.isPending}>
                <Trash2 className="mr-2 h-4 w-4" />
                Xóa
              </Button>
            )}
          </div>
        </div>

        {/* Info Card */}
        <div className="rounded-lg border bg-card p-6 space-y-6">
          {/* Basic */}
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Thời lượng</p>
              <p className="mt-1">{course.durationWeeks != null ? `${course.durationWeeks} tuần` : '—'}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-muted-foreground">Số buổi học</p>
              <p className="mt-1">{course.totalSessions ?? '—'}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-muted-foreground">Học phí</p>
              <p className="mt-1">
                {course.price != null
                  ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(course.price)
                  : 'Miễn phí'}
              </p>
            </div>
          </div>

          {/* Description */}
          {course.description && (
            <div>
              <p className="text-sm font-medium text-muted-foreground">Mô tả</p>
              <p className="mt-1 whitespace-pre-line">{course.description}</p>
            </div>
          )}

          {/* Objectives */}
          {course.objectives && (
            <div>
              <p className="text-sm font-medium text-muted-foreground">Mục tiêu học tập</p>
              <p className="mt-1 whitespace-pre-line">{course.objectives}</p>
            </div>
          )}

          {/* Syllabus */}
          {course.syllabus && (
            <div>
              <p className="text-sm font-medium text-muted-foreground">Giáo trình</p>
              <p className="mt-1 whitespace-pre-line">{course.syllabus}</p>
            </div>
          )}

          {/* Prerequisites & Target */}
          {(course.prerequisites || course.targetAudience) && (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {course.prerequisites && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Yêu cầu đầu vào</p>
                  <p className="mt-1 whitespace-pre-line">{course.prerequisites}</p>
                </div>
              )}
              {course.targetAudience && (
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Đối tượng</p>
                  <p className="mt-1 whitespace-pre-line">{course.targetAudience}</p>
                </div>
              )}
            </div>
          )}

          {/* Timestamps */}
          <div className="grid grid-cols-2 gap-4 border-t pt-4">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Ngày tạo</p>
              <p className="mt-1">{new Date(course.createdAt).toLocaleString('vi-VN')}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-muted-foreground">Cập nhật lần cuối</p>
              <p className="mt-1">{new Date(course.updatedAt).toLocaleString('vi-VN')}</p>
            </div>
          </div>
        </div>

        {/* LMS content authoring (GAP-1113 Increment A — Nội dung tab).
            BE gates module listing on COURSE_NOT_PUBLISHED, so only mount the
            manager for a published course; a DRAFT course shows guidance instead
            of letting CourseContentManager fire a 400. */}
        <div className="rounded-lg border bg-card p-6">
          {isPublished ? (
            <CourseContentManager courseId={courseId} />
          ) : (
            <div className="space-y-1 text-sm">
              <p className="font-medium">Nội dung bài học</p>
              <p className="text-muted-foreground">
                Xuất bản khóa học để bắt đầu thêm nội dung bài học. Quản lý nội dung
                khả dụng sau khi khóa học chuyển sang trạng thái “Đã xuất bản”.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Confirmation Dialogs */}
      <ConfirmDialog
        open={publishDialogOpen}
        onOpenChange={setPublishDialogOpen}
        onConfirm={handlePublish}
        title="Xuất bản khóa học"
        description="Sau khi xuất bản, một số trường sẽ bị giới hạn chỉnh sửa (tên, mã khóa học, giảng viên). Bạn có chắc chắn muốn xuất bản?"
        confirmText="Xuất bản"
      />

      <ConfirmDialog
        open={archiveDialogOpen}
        onOpenChange={setArchiveDialogOpen}
        onConfirm={handleArchive}
        title="Lưu trữ khóa học"
        description="Khóa học sẽ chuyển sang chế độ chỉ đọc và không thể chỉnh sửa. Bạn có chắc chắn muốn lưu trữ?"
        confirmText="Lưu trữ"
      />

      <ConfirmDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        onConfirm={handleDelete}
        title="Xóa khóa học"
        description="Hành động này không thể hoàn tác. Tất cả dữ liệu liên quan đến khóa học sẽ bị xóa vĩnh viễn."
        confirmText="Xóa"
        variant="destructive"
      />
    </DashboardLayout>
  );
}
