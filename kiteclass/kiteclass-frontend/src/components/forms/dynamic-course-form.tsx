/**
 * Lazy `CourseForm` wrapper.
 *
 * Course form is the heaviest in this bucket (350+ lines) — it pulls
 * `react-hook-form`, `zod`, `@hookform/resolvers/zod`, plus the teacher
 * dropdown query. Lazy boundary keeps `/courses/new` and
 * `/courses/[id]/edit` fast to first paint.
 *
 * GAP-236 Sub-PR B Agent C — code-splitting for form-heavy dashboard routes.
 *
 * @author KiteClass Team
 */

'use client';

import nextDynamic from 'next/dynamic';
import type { ComponentProps } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import type { CourseForm as CourseFormComponent } from './course-form';

const FormSkeleton = () => (
  <div className="space-y-4">
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-24 w-full" />
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-2/3" />
    <Skeleton className="h-10 w-1/3" />
  </div>
);

const LazyCourseForm = nextDynamic(
  () => import('./course-form').then((m) => ({ default: m.CourseForm })),
  {
    ssr: false,
    loading: FormSkeleton,
  },
) as unknown as typeof CourseFormComponent;

export const CourseForm = LazyCourseForm;
export type CourseFormProps = ComponentProps<typeof CourseFormComponent>;
