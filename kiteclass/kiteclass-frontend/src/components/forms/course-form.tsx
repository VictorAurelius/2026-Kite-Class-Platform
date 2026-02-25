/**
 * Course form component for create/edit operations.
 * Note: PUBLISHED/ARCHIVED courses have restricted editable fields
 * enforced server-side; the form submits all visible fields.
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

'use client';

import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FormInput } from '@/components/forms';
import { FormTextarea } from '@/components/forms/form-textarea';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common';
import { CourseStatus } from '@/types/course';
import type { CreateCourseRequest, UpdateCourseRequest } from '@/types/course';
import { useTeachers } from '@/hooks/use-teachers';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

const courseSchema = z.object({
  name: z.string().min(1, 'Tên khóa học không được để trống'),
  code: z.string().min(1, 'Mã khóa học không được để trống'),
  teacherId: z.preprocess(
    (v) => (v === '' || v === undefined || v === null ? undefined : Number(v)),
    z.number().int().positive('Vui lòng chọn giảng viên')
  ),
  description: z.string().optional(),
  syllabus: z.string().optional(),
  objectives: z.string().optional(),
  prerequisites: z.string().optional(),
  targetAudience: z.string().optional(),
  durationWeeks: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : Number(v)),
    z.number().int().min(1, 'Thời lượng phải >= 1 tuần').optional()
  ),
  totalSessions: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : Number(v)),
    z.number().int().min(1, 'Số buổi phải >= 1').optional()
  ),
  price: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : Number(v)),
    z.number().min(0, 'Học phí phải >= 0').optional()
  ),
  coverImageUrl: z
    .string()
    .optional()
    .refine((val) => !val || val === '' || /^https?:\/\/.+/.test(val), {
      message: 'URL ảnh không hợp lệ',
    }),
});

type CourseFormData = z.infer<typeof courseSchema>;

interface CourseFormProps {
  initialData?: Partial<CourseFormData>;
  onSubmit: (data: CreateCourseRequest | UpdateCourseRequest) => void;
  isSubmitting?: boolean;
  isEditing?: boolean;
  courseStatus?: CourseStatus;
}

export function CourseForm({
  initialData,
  onSubmit,
  isSubmitting = false,
  isEditing = false,
  courseStatus,
}: CourseFormProps) {
  const isPublished = courseStatus === CourseStatus.PUBLISHED;
  const isArchived = courseStatus === CourseStatus.ARCHIVED;
  const isReadOnly = isArchived;

  // Fetch teachers for dropdown
  const { data: teachersData, isLoading: isLoadingTeachers } = useTeachers({
    status: 'ACTIVE',
    page: 0,
    size: 100, // Get all active teachers
  });

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<CourseFormData>({
    resolver: zodResolver(courseSchema),
    defaultValues: initialData,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
      {isPublished && (
        <div className="rounded-md border border-yellow-200 bg-yellow-50 p-4 text-sm text-yellow-800">
          ⚠️ Khóa học đã xuất bản — chỉ có thể chỉnh sửa mô tả, giáo trình, mục tiêu và học phí.
        </div>
      )}
      {isArchived && (
        <div className="rounded-md border border-gray-200 bg-gray-50 p-4 text-sm text-gray-600">
          🔒 Khóa học đã lưu trữ — chỉ đọc, không thể chỉnh sửa.
        </div>
      )}

      {/* Basic Info */}
      <div className="space-y-4">
        <h3 className="text-lg font-semibold">Thông tin cơ bản</h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <FormInput
            label="Tên khóa học"
            placeholder="English for Business Communication"
            error={errors.name?.message}
            required
            disabled={isSubmitting || isReadOnly || isPublished}
            {...register('name')}
          />
          <FormInput
            label="Mã khóa học"
            placeholder="ENG-B1-001"
            error={errors.code?.message}
            required
            disabled={isSubmitting || isReadOnly || isPublished}
            {...register('code')}
          />

          {/* Teacher Selector */}
          <div className="space-y-2">
            <label className="text-sm font-medium">
              Giảng viên <span className="text-destructive">*</span>
            </label>
            <Controller
              name="teacherId"
              control={control}
              render={({ field }) => (
                <Select
                  onValueChange={(value) => field.onChange(value ? Number(value) : undefined)}
                  value={field.value ? field.value.toString() : undefined}
                  defaultValue={field.value ? field.value.toString() : undefined}
                  disabled={isSubmitting || isReadOnly || isPublished || isLoadingTeachers}
                >
                  <SelectTrigger className={errors.teacherId ? 'border-destructive' : ''}>
                    <SelectValue placeholder="Chọn giảng viên" />
                  </SelectTrigger>
                  <SelectContent>
                    {isLoadingTeachers ? (
                      <SelectItem value="loading" disabled>
                        Đang tải...
                      </SelectItem>
                    ) : teachersData?.content && teachersData.content.length > 0 ? (
                      teachersData.content.map((teacher) => (
                        <SelectItem key={teacher.id} value={teacher.id.toString()}>
                          {teacher.name}
                        </SelectItem>
                      ))
                    ) : (
                      <SelectItem value="empty" disabled>
                        Không có giảng viên
                      </SelectItem>
                    )}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.teacherId && (
              <p className="text-sm text-destructive">{errors.teacherId.message}</p>
            )}
          </div>

          <FormInput
            label="Thời lượng (tuần)"
            type="number"
            placeholder="12"
            error={errors.durationWeeks?.message}
            disabled={isSubmitting || isReadOnly || isPublished}
            {...register('durationWeeks')}
          />
          <FormInput
            label="Tổng số buổi học"
            type="number"
            placeholder="36"
            error={errors.totalSessions?.message}
            disabled={isSubmitting || isReadOnly || isPublished}
            {...register('totalSessions')}
          />
          <FormInput
            label="Học phí (VND)"
            type="number"
            placeholder="5000000"
            error={errors.price?.message}
            disabled={isSubmitting || isReadOnly}
            {...register('price')}
          />
          <FormInput
            label="Ảnh bìa (URL)"
            type="url"
            placeholder="https://example.com/cover.jpg"
            error={errors.coverImageUrl?.message}
            disabled={isSubmitting || isReadOnly}
            {...register('coverImageUrl')}
          />
        </div>
      </div>

      {/* Content */}
      <div className="space-y-4">
        <h3 className="text-lg font-semibold">Nội dung khóa học</h3>
        <FormTextarea
          label="Mô tả"
          placeholder="Mô tả ngắn về khóa học..."
          error={errors.description?.message}
          disabled={isSubmitting || isReadOnly}
          rows={3}
          {...register('description')}
        />
        <FormTextarea
          label="Giáo trình"
          placeholder="Tuần 1: Giới thiệu&#10;Tuần 2: Từ vựng chuyên ngành..."
          error={errors.syllabus?.message}
          disabled={isSubmitting || isReadOnly}
          rows={5}
          {...register('syllabus')}
        />
        <FormTextarea
          label="Mục tiêu học tập"
          placeholder="Sau khóa học, học viên có thể:&#10;- Giao tiếp tự tin trong môi trường kinh doanh..."
          error={errors.objectives?.message}
          disabled={isSubmitting || isReadOnly}
          rows={4}
          {...register('objectives')}
        />
        <FormTextarea
          label="Yêu cầu đầu vào"
          placeholder="Trình độ tiếng Anh A2 trở lên..."
          error={errors.prerequisites?.message}
          disabled={isSubmitting || isReadOnly}
          rows={2}
          {...register('prerequisites')}
        />
        <FormTextarea
          label="Đối tượng học viên"
          placeholder="Người đi làm cần sử dụng tiếng Anh trong công việc..."
          error={errors.targetAudience?.message}
          disabled={isSubmitting || isReadOnly}
          rows={2}
          {...register('targetAudience')}
        />
      </div>

      {!isReadOnly && (
        <div className="flex justify-end gap-4">
          <Button
            type="button"
            variant="outline"
            disabled={isSubmitting}
            onClick={() => window.history.back()}
          >
            Hủy
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <LoadingSpinner size="sm" className="mr-2" />
                {isEditing ? 'Đang cập nhật...' : 'Đang tạo...'}
              </>
            ) : (
              <>{isEditing ? 'Cập nhật' : 'Tạo khóa học'}</>
            )}
          </Button>
        </div>
      )}
    </form>
  );
}
