/**
 * Multi-class roster — class × subject × teacher matrix (25 × 9 grid).
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/multi-class-roster.html
 */

'use client';

import { PageHeader } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE, CLASSES, SUBJECTS, TEACHERS } from '@/components/school-admin/school-admin-mock-data';

// Deterministic teacher assignment per (class, subject) — for mock data only
function assignTeacher(classCode: string, subject: string): string {
  const seed = classCode.charCodeAt(0) + subject.charCodeAt(0) + classCode.length;
  const teacher = TEACHERS[seed % TEACHERS.length];
  if (!teacher) return '';
  const parts = teacher.fullName.split(' ');
  return parts[parts.length - 1] ?? '';
}

function lastNameOf(fullName: string): string {
  const parts = fullName.split(' ');
  return parts[parts.length - 1] ?? '';
}

export default function MultiClassRosterPage() {
  return (
    <div>
      <PageHeader
        title="Bảng phân công giảng dạy"
        subtitle={`Ma trận ${CLASSES.length} lớp × ${SUBJECTS.length} môn · ${SCHOOL_PROFILE.semester} năm học ${SCHOOL_PROFILE.academicYear}`}
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Tổng quan' }, { label: 'Bảng phân công' }]}
      />

      <div className="overflow-x-auto rounded-lg border bg-background">
        <table className="w-full text-sm" aria-label="Bảng phân công giảng dạy">
          <thead>
            <tr className="border-b bg-muted/40">
              <th className="sticky left-0 z-10 bg-muted/40 px-3 py-2 text-left text-xs font-semibold uppercase">Lớp</th>
              <th className="px-3 py-2 text-left text-xs font-semibold uppercase">Sĩ số</th>
              <th className="px-3 py-2 text-left text-xs font-semibold uppercase">GVCN</th>
              {SUBJECTS.map((s) => (
                <th key={s} className="px-2 py-2 text-center text-xs font-semibold uppercase">
                  {s}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {CLASSES.map((c) => (
              <tr key={c.code} className="border-b last:border-0 hover:bg-muted/30">
                <td className="sticky left-0 z-10 bg-background px-3 py-2 font-mono font-semibold">{c.code}</td>
                <td className="px-3 py-2 tabular-nums">{c.studentCount}</td>
                <td className="px-3 py-2">{lastNameOf(c.homeroomTeacher)}</td>
                {SUBJECTS.map((s) => (
                  <td key={s} className="px-2 py-2 text-center text-xs">
                    {assignTeacher(c.code, s)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
