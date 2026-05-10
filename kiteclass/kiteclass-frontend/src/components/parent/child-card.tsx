/**
 * Child summary card — avatar initials + name + class meta + drill-in chevron.
 *
 * Wave 49 Bucket A (GAP-267). Ports `.child-card` from
 * `ui_kits/kiteclass-parent/styles.css`. Click navigates to the transcript
 * view (Wave 18b1 logic preserved — `parent/transcript/[childId]`).
 */

'use client';

import Link from 'next/link';
import { ChevronRight } from 'lucide-react';

export interface ChildSummary {
  studentId: number;
  studentName: string;
  className?: string | null;
  grade?: string | null;
  /** PRIMARY (chính) vs SECONDARY (phụ). */
  linkType?: 'PRIMARY' | 'SECONDARY' | string;
}

function initials(fullName: string): string {
  const parts = fullName.trim().split(/\s+/);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0]!.slice(0, 2).toUpperCase();
  // VN naming convention: last token is given name, first token is family.
  return (parts[0]![0]! + parts[parts.length - 1]![0]!).toUpperCase();
}

export function ChildCard({ child }: { child: ChildSummary }) {
  const meta = [child.grade, child.className].filter(Boolean).join(' · ');

  return (
    <Link
      href={`/parent/transcript/${child.studentId}`}
      className="mx-4 mt-4 flex items-center gap-3 rounded-2xl border bg-card p-4 shadow-sm transition-colors hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      aria-label={`Xem học bạ con ${child.studentName}${meta ? `, ${meta}` : ''}`}
      data-testid={`parent-child-card-${child.studentId}`}
    >
      <div
        className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary"
        aria-hidden
      >
        {initials(child.studentName)}
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold">
          Con {child.studentName}
        </p>
        {meta && (
          <p className="truncate text-xs text-muted-foreground">{meta}</p>
        )}
      </div>
      <ChevronRight
        className="h-5 w-5 flex-shrink-0 text-muted-foreground"
        aria-hidden
      />
    </Link>
  );
}
