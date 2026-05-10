/**
 * Parent grades subject detail — bar list of test scores + teacher note.
 *
 * Wave 49 Bucket A (GAP-267). Ports `grades-subject-detail.html`.
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { ParentShell } from '@/components/parent/parent-shell';
import { MOCK_GRADES } from '@/components/parent/parent-mock-data';

interface ScoreEntry {
  label: string;
  weight: string;
  score: number;
  date: string;
}

const MOCK_SUBJECT_SCORES: ScoreEntry[] = [
  { label: 'Kiểm tra miệng', weight: 'Hệ số 1', score: 9.0, date: '08/04/2026' },
  { label: 'Kiểm tra 15 phút', weight: 'Hệ số 1', score: 8.5, date: '11/04/2026' },
  { label: 'Kiểm tra giữa kỳ', weight: 'Hệ số 2', score: 8.5, date: '15/04/2026' },
  { label: 'Bài tập về nhà', weight: 'Hệ số 1', score: 9.5, date: '18/04/2026' },
];

export default function ParentGradesSubjectPage() {
  const params = useParams<{ subject: string }>();
  const subject = decodeURIComponent(params?.subject ?? '');
  const summary = MOCK_GRADES.find((g) => g.subject === subject);

  return (
    <ParentShell title={subject || 'Môn học'} subtitle="Chi tiết điểm số">
      <div className="px-4 pt-3">
        <Link
          href="/parent/grades"
          className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
        >
          <ArrowLeft className="h-3.5 w-3.5" aria-hidden />
          Quay lại học bạ
        </Link>
      </div>

      {summary && (
        <section
          className="mx-4 mt-3 rounded-2xl border bg-card p-4 shadow-sm"
          data-testid="parent-grades-subject-summary"
        >
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Điểm trung bình môn
          </p>
          <p className="mt-1 text-4xl font-extrabold tracking-tight">
            {summary.score.toFixed(1)}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            Học lực: {summary.level}
          </p>
        </section>
      )}

      <section className="mx-4 mt-4">
        <h2 className="mb-2 text-sm font-bold">Lịch sử điểm</h2>
        <ul
          className="rounded-2xl border bg-card"
          data-testid="parent-grades-subject-list"
        >
          {MOCK_SUBJECT_SCORES.map((entry, idx) => (
            <li key={idx} className="border-b last:border-b-0">
              <div className="flex items-center gap-3 p-4">
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold">{entry.label}</p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {entry.weight} · {entry.date}
                  </p>
                </div>
                <span className="text-xl font-bold tabular-nums">
                  {entry.score.toFixed(1)}
                </span>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section className="mx-4 mt-4 rounded-2xl border bg-card p-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Nhận xét giáo viên bộ môn
        </p>
        <p className="mt-2 text-sm leading-relaxed">
          Con tích cực phát biểu, cần chú ý cẩn thận hơn khi trình bày bài tự
          luận. Phụ huynh có thể nhắc con luyện thêm các đề tham khảo cuối SGK.
        </p>
      </section>
    </ParentShell>
  );
}
