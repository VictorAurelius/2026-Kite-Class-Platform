/**
 * Lazy `ClassForm` wrapper.
 *
 * Pulls `react-hook-form`, `zod`, and `@hookform/resolvers/zod` out of every
 * page that renders the class form. The form ships in its own chunk and
 * hydrates on mount, keeping the initial page payload small for routes that
 * are predominantly form-driven (`/classes/[id]/edit`, `/courses/[id]/classes/new`).
 *
 * GAP-236 Sub-PR B Agent C — code-splitting for form-heavy dashboard routes.
 *
 * @author KiteClass Team
 */

'use client';

import nextDynamic from 'next/dynamic';
import type { ComponentProps } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import type { ClassForm as ClassFormComponent } from './class-form';

const FormSkeleton = () => (
  <div className="space-y-4">
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-24 w-full" />
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-full" />
    <Skeleton className="h-10 w-2/3" />
    <Skeleton className="h-10 w-1/3" />
  </div>
);

const LazyClassForm = nextDynamic(
  () => import('./class-form').then((m) => ({ default: m.ClassForm })),
  {
    ssr: false,
    loading: FormSkeleton,
  },
) as unknown as typeof ClassFormComponent;

export const ClassForm = LazyClassForm;
export type ClassFormProps = ComponentProps<typeof ClassFormComponent>;
