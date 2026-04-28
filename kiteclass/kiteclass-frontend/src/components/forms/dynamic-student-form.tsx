/**
 * Lazy `StudentForm` wrapper.
 *
 * Pulls `react-hook-form`, `zod`, `@hookform/resolvers/zod` out of student
 * create/edit page bundles.
 *
 * GAP-236 Sub-PR B Agent C — code-splitting for form-heavy dashboard routes.
 *
 * @author KiteClass Team
 */

'use client';

import nextDynamic from 'next/dynamic';
import type { ComponentProps } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import type { StudentForm as StudentFormComponent } from './student-form';

const FormSkeleton = () => (
  <div className="space-y-4">
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-2/3" />
    <Skeleton className="h-10 w-1/3" />
  </div>
);

const LazyStudentForm = nextDynamic(
  () => import('./student-form').then((m) => ({ default: m.StudentForm })),
  {
    ssr: false,
    loading: FormSkeleton,
  },
) as unknown as typeof StudentFormComponent;

export const StudentForm = LazyStudentForm;
export type StudentFormProps = ComponentProps<typeof StudentFormComponent>;
