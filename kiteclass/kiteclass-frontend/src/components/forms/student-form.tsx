/**
 * Student form component for create/edit operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { FormInput, FormSelect } from '@/components/forms';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common';
import { Gender, StudentStatus } from '@/types/auth';
import type { CreateStudentRequest, UpdateStudentRequest } from '@/types/student';

const studentSchema = z.object({
  name: z.string().min(1, 'Tên không được để trống'),
  email: z.string().email('Email không hợp lệ'),
  phone: z.string().optional(),
  dateOfBirth: z.string().optional(),
  gender: z.preprocess(
    (val) => (val === '' ? undefined : val),
    z.nativeEnum(Gender).optional()
  ),
  address: z.string().optional(),
  status: z.nativeEnum(StudentStatus).optional(),
});

type StudentFormData = z.infer<typeof studentSchema>;

interface StudentFormProps {
  initialData?: Partial<StudentFormData>;
  onSubmit: (data: CreateStudentRequest | UpdateStudentRequest) => void;
  isSubmitting?: boolean;
  isEditing?: boolean;
}

const genderOptions = [
  { label: 'Nam', value: Gender.MALE },
  { label: 'Nữ', value: Gender.FEMALE },
];

const statusOptions = [
  { label: 'Đang học', value: StudentStatus.ACTIVE },
  { label: 'Không hoạt động', value: StudentStatus.INACTIVE },
  { label: 'Đã tốt nghiệp', value: StudentStatus.GRADUATED },
  { label: 'Đình chỉ', value: StudentStatus.SUSPENDED },
];

export function StudentForm({
  initialData,
  onSubmit,
  isSubmitting = false,
  isEditing = false,
}: StudentFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<StudentFormData>({
    resolver: zodResolver(studentSchema),
    defaultValues: initialData,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <FormInput
          label="Tên học viên"
          placeholder="Nguyễn Văn A"
          error={errors.name?.message}
          required
          disabled={isSubmitting}
          {...register('name')}
        />

        <FormInput
          label="Email"
          type="email"
          placeholder="student@example.com"
          error={errors.email?.message}
          required
          disabled={isSubmitting}
          {...register('email')}
        />

        <FormInput
          label="Số điện thoại"
          type="tel"
          placeholder="0912345678"
          error={errors.phone?.message}
          disabled={isSubmitting}
          {...register('phone')}
        />

        <FormInput
          label="Ngày sinh"
          type="date"
          error={errors.dateOfBirth?.message}
          disabled={isSubmitting}
          {...register('dateOfBirth')}
        />

        <FormSelect
          label="Giới tính"
          placeholder="Chọn giới tính"
          options={genderOptions}
          error={errors.gender?.message}
          disabled={isSubmitting}
          {...register('gender')}
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

      <FormInput
        label="Địa chỉ"
        placeholder="123 Đường ABC, Quận 1, TP.HCM"
        error={errors.address?.message}
        disabled={isSubmitting}
        {...register('address')}
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
