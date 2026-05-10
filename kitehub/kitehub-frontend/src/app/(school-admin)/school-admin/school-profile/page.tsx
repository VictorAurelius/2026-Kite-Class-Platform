/**
 * School profile + settings.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/school-profile.html
 */

'use client';

import { Save, Building2, Users, Calendar, ShieldCheck } from 'lucide-react';
import { PageHeader, MoetStamp } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE } from '@/components/school-admin/school-admin-mock-data';

function Field({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="grid grid-cols-1 gap-1 md:grid-cols-3 md:gap-4">
      <label className="text-sm font-medium text-muted-foreground md:col-span-1 md:pt-1.5">{label}</label>
      <div className="md:col-span-2">
        <input
          type="text"
          defaultValue={value}
          className="w-full rounded-md border bg-background px-3 py-1.5 text-sm"
        />
        {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
      </div>
    </div>
  );
}

export default function SchoolProfilePage() {
  return (
    <div>
      <PageHeader
        title="Hồ sơ trường"
        subtitle="Thông tin pháp nhân, cấu hình học vụ, mã định danh trên hệ thống Sở GD"
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Cấu hình' }, { label: 'Hồ sơ trường' }]}
        actions={
          <button type="button" className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Save className="h-4 w-4" aria-hidden="true" />
            Lưu thay đổi
          </button>
        }
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <section className="rounded-lg border bg-background p-5">
            <div className="mb-4 flex items-center gap-2">
              <Building2 className="h-5 w-5 text-primary" aria-hidden="true" />
              <h2 className="text-base font-semibold">Thông tin chung</h2>
            </div>
            <div className="space-y-4">
              <Field label="Tên trường" value={SCHOOL_PROFILE.name} />
              <Field label="Mã trường" value={SCHOOL_PROFILE.shortCode} hint="Mã định danh nội bộ + đồng bộ với hệ thống Sở GD&ĐT" />
              <Field label="Địa chỉ" value={`123 Nguyễn Du, ${SCHOOL_PROFILE.district}`} />
              <Field label="Hiệu trưởng" value={SCHOOL_PROFILE.principalName} />
              <Field label="Email liên hệ" value="contact@nguyendu.edu.vn" />
              <Field label="Điện thoại" value="028 3838 1234" />
            </div>
          </section>

          <section className="rounded-lg border bg-background p-5">
            <div className="mb-4 flex items-center gap-2">
              <Calendar className="h-5 w-5 text-primary" aria-hidden="true" />
              <h2 className="text-base font-semibold">Cấu hình học vụ</h2>
            </div>
            <div className="space-y-4">
              <Field label="Năm học hiện tại" value={SCHOOL_PROFILE.academicYear} />
              <Field label="Học kỳ" value={`${SCHOOL_PROFILE.semester} (Tuần ${SCHOOL_PROFILE.weekNumber}/${SCHOOL_PROFILE.totalWeeks})`} />
              <Field label="Ngày khai giảng" value="05/09/2026" />
              <Field label="Ngày bế giảng" value="30/05/2027" />
            </div>
          </section>

          <section className="rounded-lg border bg-background p-5">
            <div className="mb-4 flex items-center gap-2">
              <Users className="h-5 w-5 text-primary" aria-hidden="true" />
              <h2 className="text-base font-semibold">Quy mô</h2>
            </div>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              <div>
                <div className="text-xs uppercase text-muted-foreground">Học sinh</div>
                <div className="mt-1 text-2xl font-bold tabular-nums">{SCHOOL_PROFILE.totalStudents.toLocaleString('vi-VN')}</div>
              </div>
              <div>
                <div className="text-xs uppercase text-muted-foreground">Lớp</div>
                <div className="mt-1 text-2xl font-bold tabular-nums">{SCHOOL_PROFILE.totalClasses}</div>
              </div>
              <div>
                <div className="text-xs uppercase text-muted-foreground">Khối</div>
                <div className="mt-1 text-2xl font-bold tabular-nums">{SCHOOL_PROFILE.totalGrades}</div>
              </div>
              <div>
                <div className="text-xs uppercase text-muted-foreground">Giáo viên</div>
                <div className="mt-1 text-2xl font-bold tabular-nums">{SCHOOL_PROFILE.totalTeachers}</div>
              </div>
            </div>
          </section>
        </div>

        <div className="space-y-4">
          <section className="rounded-lg border-2 border-amber-600/30 bg-amber-50 p-5 dark:border-amber-400/30 dark:bg-amber-950/20">
            <div className="mb-3 flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-amber-700 dark:text-amber-300" aria-hidden="true" />
              <h2 className="text-base font-semibold">Tuân thủ MoET</h2>
            </div>
            <div className="space-y-3">
              <MoetStamp size="md" />
              <div className="text-sm">
                <div className="font-medium">Mã trường Sở GD</div>
                <div className="font-mono text-xs">79.001.012.001</div>
              </div>
              <div className="text-sm">
                <div className="font-medium">Đồng bộ Sở GD</div>
                <div className="text-xs text-muted-foreground">Lần cuối: hôm nay 09:14</div>
              </div>
            </div>
          </section>

          <section className="rounded-lg border bg-background p-5">
            <h2 className="mb-3 text-base font-semibold">Tích hợp ngoài</h2>
            <ul className="space-y-2 text-sm">
              <li className="flex justify-between">
                <span>Zalo OA</span>
                <span className="text-emerald-600">3.241 PH</span>
              </li>
              <li className="flex justify-between">
                <span>VietQR (học phí)</span>
                <span className="text-emerald-600">Hoạt động</span>
              </li>
              <li className="flex justify-between">
                <span>Cổng Sở GD&ĐT</span>
                <span className="text-emerald-600">OK</span>
              </li>
            </ul>
          </section>
        </div>
      </div>
    </div>
  );
}
