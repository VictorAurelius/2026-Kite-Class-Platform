/**
 * Student PWA — Empty-states gallery (showcase / Storybook surrogate).
 *
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/empty-states.html`.
 * Mounted in production but linked only from QA/Design scope so reviewers
 * can spot-check empty visuals without seeding test data.
 */
import { Bell, FileText, GraduationCap, Inbox, Wallet, Wifi } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface EmptyState {
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
  title: string;
  body: string;
}

const STATES: readonly EmptyState[] = [
  { icon: Inbox, title: 'Chưa có lớp nào', body: 'Liên hệ giáo viên chủ nhiệm để được thêm vào lớp.' },
  { icon: FileText, title: 'Chưa có bài tập', body: 'Kiểm tra lại sau hoặc xem các môn khác.' },
  { icon: GraduationCap, title: 'Chưa có điểm', body: 'Điểm sẽ xuất hiện sau khi giáo viên chấm bài.' },
  { icon: Bell, title: 'Hộp thư trống', body: 'Khi có thông báo mới, em sẽ nhận được tại đây.' },
  { icon: Wallet, title: 'Chưa có hoá đơn', body: 'Phụ huynh đang giúp em đóng học phí.' },
  { icon: Wifi, title: 'Đang offline', body: 'Kiểm tra kết nối mạng và thử lại.' },
];

export default function StudentEmptyStatesPage() {
  return (
    <StudentMobileShell title="Empty states" subtitle="Bộ trạng thái trống">
      <ul className="space-y-3">
        {STATES.map((s, idx) => (
          <li key={idx}>
            <Card>
              <CardContent className="flex flex-col items-center gap-2 p-6 text-center">
                <s.icon className="h-10 w-10 text-muted-foreground" aria-hidden />
                <h3 className="text-sm font-semibold">{s.title}</h3>
                <p className="text-xs text-muted-foreground">{s.body}</p>
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>
    </StudentMobileShell>
  );
}
