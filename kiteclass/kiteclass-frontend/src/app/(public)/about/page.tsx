/**
 * Public per-tenant "Về giáo viên" page (GAP-274 phase-2 / GAP-1208 voice).
 *
 * Persona P1 Solo Teacher — voice xưng "tôi/cô", giọng cá nhân. Sections render
 * from the tenant landing payload and HIDE when their source is null/empty:
 *   - Câu chuyện (centerName + aboutText / teacherBio)
 *   - Số liệu thật (landing.stats — anti-fabrication GAP-958, no hardcoded numbers)
 *   - Giáo viên đồng hành (landing.teachers)
 *   - Chứng chỉ & chuyên môn (teacher credentials)
 *
 * Labels follow GAP-1208 (GV độc lập): "Về giáo viên", "Giáo viên đồng hành".
 *
 * @author KiteClass Team
 */

import { Metadata } from 'next';
import Link from 'next/link';
import { Award, GraduationCap, Info } from 'lucide-react';
import {
  getTenantLanding,
  landingStr,
  landingArray,
} from '@/lib/api/tenant-landing';

export const metadata: Metadata = {
  title: 'Về giáo viên',
  description: 'Câu chuyện, đội ngũ và số liệu thật của lớp học.',
};

interface TeacherEntry {
  name?: string;
  subject?: string;
  photoUrl?: string;
  credentials?: string[];
}
interface StatEntry {
  value?: string;
  label?: string;
}

export default async function AboutPage() {
  const landing = await getTenantLanding();

  const centerName = landingStr(landing, 'centerName') || landingStr(landing, 'heroTitle') || 'giáo viên';
  const story = landingStr(landing, 'aboutText') || landingStr(landing, 'teacherBio');
  const tagline = landingStr(landing, 'tagline');
  const teachers = landingArray<TeacherEntry>(landing, 'teachers');
  const stats = landingArray<StatEntry>(landing, 'stats').filter((s) => s.value && s.label);
  const credentials = (teachers[0]?.credentials ?? []).filter((c) => typeof c === 'string' && c.trim());

  return (
    <div>
      {/* Hero */}
      <div className="bg-gradient-to-br from-theme-primary to-theme-secondary text-white">
        <div className="container mx-auto px-4 py-14 text-center">
          <span className="mb-3 inline-flex rounded-full bg-white/15 px-3 py-1.5 text-xs font-extrabold uppercase tracking-wider">
            Về giáo viên
          </span>
          <h1 className="text-3xl font-extrabold md:text-4xl">{centerName}</h1>
          {tagline && <p className="mx-auto mt-3 max-w-2xl text-white/90">{tagline}</p>}
        </div>
      </div>

      <div className="container mx-auto px-4 py-12">
        {/* Câu chuyện */}
        {story && (
          <section className="mb-14 grid items-start gap-8 md:grid-cols-[auto,1fr]">
            <div
              className="flex h-24 w-24 items-center justify-center rounded-2xl bg-theme-primary/10 text-5xl"
              aria-hidden="true"
            >
              👩‍🏫
            </div>
            <div>
              <h2 className="mb-3 text-2xl font-bold">Câu chuyện của {centerName}</h2>
              <p className="whitespace-pre-line leading-relaxed text-muted-foreground">{story}</p>
            </div>
          </section>
        )}

        {/* Số liệu thật — anti-fabrication: only when tenant actually has stats */}
        {stats.length > 0 && (
          <section className="mb-14" aria-labelledby="stats-title">
            <div className="mb-6 text-center">
              <h2 id="stats-title" className="text-2xl font-bold">
                Số liệu thật từ lớp học
              </h2>
              <p className="text-muted-foreground">Không phải con số marketing — đây là dữ liệu lớp thật.</p>
            </div>
            <div className="grid gap-6 sm:grid-cols-2 md:grid-cols-3">
              {stats.map((s, i) => (
                <div key={i} className="rounded-2xl border bg-white p-6 text-center shadow-sm">
                  <div className="text-4xl font-black text-theme-primary">{s.value}</div>
                  <div className="mt-1 text-sm font-semibold text-muted-foreground">{s.label}</div>
                </div>
              ))}
            </div>
            <p className="mt-4 flex items-start justify-center gap-2 text-center text-xs text-muted-foreground">
              <Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              <span>
                Số liệu lấy trực tiếp từ dữ liệu tenant trên KiteClass, cập nhật tự động — không nhập tay,
                không phóng đại. Khi chưa đủ dữ liệu, hệ thống ẩn chỉ số thay vì hiển thị số ước lượng.
              </span>
            </p>
          </section>
        )}

        {/* Giáo viên đồng hành */}
        {teachers.length > 0 && (
          <section className="mb-14" aria-labelledby="team-title">
            <div className="mb-6 text-center">
              <h2 id="team-title" className="text-2xl font-bold">
                Giáo viên đồng hành
              </h2>
            </div>
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {teachers.map((t, i) => (
                <div key={i} className="rounded-2xl border bg-white p-6 text-center shadow-sm">
                  <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-theme-primary/10 text-xl font-black text-theme-primary">
                    {(t.name ?? 'GV').charAt(0)}
                  </div>
                  {t.name && <b className="block">{t.name}</b>}
                  {t.subject && <div className="text-sm text-muted-foreground">{t.subject}</div>}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Chứng chỉ & chuyên môn */}
        {credentials.length > 0 && (
          <section className="mb-14" aria-labelledby="cert-title">
            <div className="mb-6 text-center">
              <h2 id="cert-title" className="text-2xl font-bold">
                Chứng chỉ &amp; chuyên môn
              </h2>
            </div>
            <div className="mx-auto grid max-w-3xl gap-4 sm:grid-cols-2">
              {credentials.map((c, i) => (
                <div key={i} className="flex items-center gap-3 rounded-xl border bg-white p-4 shadow-sm">
                  <Award className="h-6 w-6 shrink-0 text-theme-primary" aria-hidden="true" />
                  <span className="font-semibold">{c}</span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Empty fallback — when tenant supplied no about content at all */}
        {!story && stats.length === 0 && teachers.length === 0 && (
          <section className="mb-14 rounded-2xl border bg-muted/40 p-10 text-center">
            <GraduationCap className="mx-auto mb-3 h-10 w-10 text-theme-primary" aria-hidden="true" />
            <p className="mx-auto max-w-md text-muted-foreground">
              Thông tin giới thiệu đang được cập nhật. Anh/chị có thể liên hệ trực tiếp để được tư vấn.
            </p>
          </section>
        )}

        {/* CTA */}
        <section className="rounded-2xl bg-gradient-to-br from-theme-primary to-theme-secondary p-8 text-center text-white">
          <h2 className="text-2xl font-extrabold">Cho con học thử một buổi miễn phí</h2>
          <p className="mx-auto mt-2 max-w-xl text-white/90">
            Đăng ký để được kiểm tra trình độ và tư vấn lộ trình phù hợp với con.
          </p>
          <Link
            href="/contact"
            className="mt-5 inline-block rounded-xl bg-white px-6 py-3 font-bold text-theme-primary"
          >
            Đăng ký học thử
          </Link>
        </section>
      </div>
    </div>
  );
}
