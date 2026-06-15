/**
 * Class form component for create/edit operations.
 * Note: IN_PROGRESS classes have restricted editable fields;
 * COMPLETED/CANCELLED classes are read-only.
 *
 * @author KiteClass Team
 * @since 3.7.0
 */

'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FormInput } from '@/components/forms';
import { FormTextarea } from '@/components/forms/form-textarea';
import { FormSelect } from '@/components/forms/form-select';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common';
import { ClassStatus } from '@/types/class';
import type { CreateClassRequest, UpdateClassRequest } from '@/types/class';

const classSchema = z
  .object({
    name: z.string().min(1, 'Tên lớp học không được để trống'),
    description: z.string().optional(),
    schedule: z.string().optional(),
    locationType: z.enum(['IN_PERSON', 'ONLINE'], {
      errorMap: () => ({ message: 'Chọn loại địa điểm' }),
    }),
    locationDetail: z.string().optional(),
    // GAP-1423: classes.start_date is NOT NULL in the DB — required here (and @NotNull
    // in CreateClassRequest) so a blank date returns a 400 field error instead of a 500
    // constraint violation. end_date stays optional (nullable column).
    startDate: z.string().min(1, 'Ngày bắt đầu không được để trống'),
    endDate: z.string().optional(),
    maxStudents: z.preprocess(
      (v) => (v === '' || v === undefined ? undefined : Number(v)),
      z.number().int().min(1, 'Sĩ số tối đa phải >= 1')
    ),
  })
  .refine(
    (data) => {
      if (data.startDate && data.endDate) {
        return new Date(data.startDate) <= new Date(data.endDate);
      }
      return true;
    },
    {
      message: 'Ngày kết thúc phải sau hoặc bằng ngày bắt đầu',
      path: ['endDate'],
    }
  );

type ClassFormData = z.infer<typeof classSchema>;

interface ClassFormProps {
  initialData?: Partial<ClassFormData>;
  onSubmit: (data: CreateClassRequest | UpdateClassRequest) => void;
  isSubmitting?: boolean;
  isEditing?: boolean;
  classStatus?: ClassStatus;
}

export function ClassForm({
  initialData,
  onSubmit,
  isSubmitting = false,
  isEditing = false,
  classStatus,
}: ClassFormProps) {
  const isInProgress = classStatus === 'IN_PROGRESS';
  const isCompleted = classStatus === 'COMPLETED';
  const isCancelled = classStatus === 'CANCELLED';
  const isReadOnly = isCompleted || isCancelled;
  const isRestrictedEdit = isInProgress;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ClassFormData>({
    resolver: zodResolver(classSchema),
    defaultValues: initialData,
  });

  const locationTypeOptions = [
    { value: 'IN_PERSON', label: 'Trực tiếp' },
    { value: 'ONLINE', label: 'Trực tuyến' },
  ];

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
      {isInProgress && (
        <div className="rounded-md border border-yellow-200 bg-yellow-50 p-4 text-sm text-yellow-800">
          ⚠️ Lớp học đang diễn ra — chỉ có thể chỉnh sửa mô tả và chi tiết địa điểm.
        </div>
      )}
      {isCompleted && (
        <div className="rounded-md border border-green-200 bg-green-50 p-4 text-sm text-green-800">
          ✓ Lớp học đã hoàn thành — chỉ đọc, không thể chỉnh sửa.
        </div>
      )}
      {isCancelled && (
        <div className="rounded-md border border-gray-200 bg-gray-50 p-4 text-sm text-gray-600">
          🔒 Lớp học đã bị hủy — chỉ đọc, không thể chỉnh sửa.
        </div>
      )}

      {/* Basic Info */}
      <div className="space-y-4">
        <h3 className="text-lg font-semibold">Thông tin cơ bản</h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <FormInput
            label="Tên lớp học"
            placeholder="Lớp Toán cao cấp K01"
            error={errors.name?.message}
            required
            disabled={isSubmitting || isReadOnly || isRestrictedEdit}
            {...register('name')}
          />
          <FormInput
            label="Sĩ số tối đa"
            type="number"
            placeholder="30"
            error={errors.maxStudents?.message}
            required
            disabled={isSubmitting || isReadOnly || isRestrictedEdit}
            {...register('maxStudents')}
          />
        </div>
        <FormTextarea
          label="Mô tả"
          placeholder="Mô tả ngắn về lớp học..."
          error={errors.description?.message}
          disabled={isSubmitting || isReadOnly}
          rows={3}
          {...register('description')}
        />
      </div>

      {/* Schedule & Location */}
      <div className="space-y-4">
        <h3 className="text-lg font-semibold">Lịch học & Địa điểm</h3>
        <FormInput
          label="Lịch học"
          placeholder="Thứ 2, 4, 6: 18:00-20:00"
          error={errors.schedule?.message}
          disabled={isSubmitting || isReadOnly || isRestrictedEdit}
          {...register('schedule')}
        />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <FormInput
            label="Ngày bắt đầu"
            type="date"
            required
            error={errors.startDate?.message}
            disabled={isSubmitting || isReadOnly || isRestrictedEdit}
            {...register('startDate')}
          />
          <FormInput
            label="Ngày kết thúc"
            type="date"
            error={errors.endDate?.message}
            disabled={isSubmitting || isReadOnly || isRestrictedEdit}
            {...register('endDate')}
          />
        </div>
        <FormSelect
          label="Loại địa điểm"
          options={locationTypeOptions}
          error={errors.locationType?.message}
          required
          disabled={isSubmitting || isReadOnly || isRestrictedEdit}
          {...register('locationType')}
        />
        <FormInput
          label="Chi tiết địa điểm"
          placeholder="Phòng A101, Tòa nhà B hoặc link Zoom"
          error={errors.locationDetail?.message}
          disabled={isSubmitting || isReadOnly}
          {...register('locationDetail')}
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
              <>{isEditing ? 'Cập nhật' : 'Tạo lớp học'}</>
            )}
          </Button>
        </div>
      )}
    </form>
  );
}
