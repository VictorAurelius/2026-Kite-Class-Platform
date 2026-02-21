/**
 * Teacher form component for create/edit operations.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FormInput, FormSelect } from '@/components/forms';
import { FormTextarea } from '@/components/forms/form-textarea';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common';
import { TeacherStatus } from '@/types/auth';
import type { CreateTeacherRequest, UpdateTeacherRequest } from '@/types/teacher';

const teacherSchema = z.object({
  name: z.string().min(1, 'Tên không được để trống'),
  email: z.string().email('Email không hợp lệ'),
  phoneNumber: z.string().optional(),
  specialization: z.string().optional(),
  bio: z.string().optional(),
  qualification: z.string().optional(),
  experienceYears: z.preprocess(
    (val) => (val === '' || val === undefined ? undefined : Number(val)),
    z.number().int().min(0, 'Kinh nghiệm phải >= 0').optional()
  ),
  status: z.nativeEnum(TeacherStatus).optional(),
});

type TeacherFormData = z.infer<typeof teacherSchema>;

interface TeacherFormProps {
  initialData?: Partial<TeacherFormData>;
  onSubmit: (data: CreateTeacherRequest | UpdateTeacherRequest) => void;
  isSubmitting?: boolean;
  isEditing?: boolean;
}

const statusOptions = [
  { label: 'Đang hoạt động', value: TeacherStatus.ACTIVE },
  { label: 'Tạm ngưng', value: TeacherStatus.INACTIVE },
  { label: 'Nghỉ phép', value: TeacherStatus.ON_LEAVE },
];

export function TeacherForm({
  initialData,
  onSubmit,
  isSubmitting = false,
  isEditing = false,
}: TeacherFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TeacherFormData>({
    resolver: zodResolver(teacherSchema),
    defaultValues: initialData,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <FormInput
          label="Tên giáo viên"
          placeholder="Nguyễn Văn A"
          error={errors.name?.message}
          required
          disabled={isSubmitting}
          {...register('name')}
        />

        <FormInput
          label="Email"
          type="email"
          placeholder="teacher@example.com"
          error={errors.email?.message}
          required
          disabled={isSubmitting}
          {...register('email')}
        />

        <FormInput
          label="Số điện thoại"
          type="tel"
          placeholder="0912345678"
          error={errors.phoneNumber?.message}
          disabled={isSubmitting}
          {...register('phoneNumber')}
        />

        <FormInput
          label="Chuyên môn"
          placeholder="Tiếng Anh, Toán học..."
          error={errors.specialization?.message}
          disabled={isSubmitting}
          {...register('specialization')}
        />

        <FormInput
          label="Bằng cấp / Chứng chỉ"
          placeholder="Thạc sĩ Giáo dục..."
          error={errors.qualification?.message}
          disabled={isSubmitting}
          {...register('qualification')}
        />

        <FormInput
          label="Số năm kinh nghiệm"
          type="number"
          placeholder="5"
          error={errors.experienceYears?.message}
          disabled={isSubmitting}
          {...register('experienceYears')}
        />

        {isEditing && (
          <FormSelect
            label="Trạng thái"
            options={statusOptions}
            error={errors.status?.message}
            disabled={isSubmitting}
            {...register('status')}
          />
        )}
      </div>

      <FormTextarea
        label="Giới thiệu"
        placeholder="Mô tả ngắn về giáo viên..."
        error={errors.bio?.message}
        disabled={isSubmitting}
        rows={4}
        {...register('bio')}
      />

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
            <>{isEditing ? 'Cập nhật' : 'Tạo mới'}</>
          )}
        </Button>
      </div>
    </form>
  );
}
