/**
 * Teacher management — list 50+ teachers, assign to class, role hierarchy.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/teacher-management.html
 */

'use client';

import { useMemo, useState } from 'react';
import { Search, UserPlus } from 'lucide-react';
import { PageHeader, Pill } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE, TEACHERS, SUBJECTS } from '@/components/school-admin/school-admin-mock-data';

const STATUS_PILL = {
  active: { kind: 'success' as const, label: 'Đang dạy' },
  leave: { kind: 'warning' as const, label: 'Nghỉ phép' },
  pending: { kind: 'info' as const, label: 'Chờ kích hoạt' },
};

export default function TeacherManagementPage() {
  const [query, setQuery] = useState('');
  const [subjectFilter, setSubjectFilter] = useState<string>('all');
  const [roleFilter, setRoleFilter] = useState<string>('all');

  const filtered = useMemo(() => {
    return TEACHERS.filter((t) => {
      const matchQ = !query || t.fullName.toLowerCase().includes(query.toLowerCase()) || t.email.toLowerCase().includes(query.toLowerCase());
      const matchSubj = subjectFilter === 'all' || t.subject === subjectFilter;
      const matchRole = roleFilter === 'all' || t.role === roleFilter;
      return matchQ && matchSubj && matchRole;
    });
  }, [query, subjectFilter, roleFilter]);

  return (
    <div>
      <PageHeader
        title="Quản lý giáo viên"
        subtitle={`${SCHOOL_PROFILE.totalTeachers} giáo viên · ${TEACHERS.filter((t) => t.role === 'GVCN').length}+ GVCN · Năm học ${SCHOOL_PROFILE.academicYear}`}
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Nhân sự' }, { label: 'Giáo viên' }]}
        actions={
          <button type="button" className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <UserPlus className="h-4 w-4" aria-hidden="true" />
            Thêm giáo viên
          </button>
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm tên, email..."
            className="w-full rounded-md border bg-background py-1.5 pl-8 pr-3 text-sm"
            aria-label="Tìm giáo viên"
          />
        </div>
        <select
          value={subjectFilter}
          onChange={(e) => setSubjectFilter(e.target.value)}
          className="rounded-md border bg-background px-3 py-1.5 text-sm"
          aria-label="Lọc theo môn"
        >
          <option value="all">Tất cả môn</option>
          {SUBJECTS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          className="rounded-md border bg-background px-3 py-1.5 text-sm"
          aria-label="Lọc theo vai trò"
        >
          <option value="all">Tất cả vai trò</option>
          <option value="GVCN">GVCN</option>
          <option value="GV bộ môn">GV bộ môn</option>
          <option value="Tổ trưởng">Tổ trưởng</option>
          <option value="Phó hiệu trưởng">Phó hiệu trưởng</option>
        </select>
      </div>

      <div className="overflow-x-auto rounded-lg border bg-background">
        <table className="w-full text-sm" aria-label="Danh sách giáo viên">
          <thead>
            <tr className="border-b bg-muted/40 text-xs uppercase text-muted-foreground">
              <th className="px-3 py-2 text-left">Họ tên</th>
              <th className="px-3 py-2 text-left">Môn</th>
              <th className="px-3 py-2 text-left">Vai trò</th>
              <th className="px-3 py-2 text-left">Chủ nhiệm</th>
              <th className="px-3 py-2 text-left">Số lớp</th>
              <th className="px-3 py-2 text-left">Liên hệ</th>
              <th className="px-3 py-2 text-left">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((t) => (
              <tr key={t.id} className="border-b last:border-0 hover:bg-muted/30">
                <td className="px-3 py-2">
                  <div className="flex items-center gap-2">
                    <span className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-[10px] font-semibold text-primary">
                      {t.initials}
                    </span>
                    <span className="font-medium">{t.fullName}</span>
                  </div>
                </td>
                <td className="px-3 py-2">{t.subject}</td>
                <td className="px-3 py-2">
                  <Pill kind={t.role === 'Phó hiệu trưởng' ? 'warning' : t.role === 'Tổ trưởng' ? 'info' : 'neutral'}>{t.role}</Pill>
                </td>
                <td className="px-3 py-2 font-mono">{t.homeroom ?? '—'}</td>
                <td className="px-3 py-2 tabular-nums">{t.classCount}</td>
                <td className="px-3 py-2 text-xs text-muted-foreground">
                  <div>{t.email}</div>
                  <div className="font-mono">{t.phone}</div>
                </td>
                <td className="px-3 py-2">
                  <Pill kind={STATUS_PILL[t.status].kind}>{STATUS_PILL[t.status].label}</Pill>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={7} className="px-3 py-12 text-center text-sm text-muted-foreground">
                  Không tìm thấy giáo viên phù hợp.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        <div className="flex items-center justify-between border-t bg-muted/20 px-3 py-2 text-xs text-muted-foreground">
          <span>
            Hiển thị {filtered.length} / {SCHOOL_PROFILE.totalTeachers} giáo viên
          </span>
        </div>
      </div>
    </div>
  );
}
