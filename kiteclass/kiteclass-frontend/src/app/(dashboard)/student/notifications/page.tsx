/**
 * Student PWA — Thông báo (notifications inbox).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/notifications.html`.
 *
 * Includes Web Push subscribe/unsubscribe UI (the kit's distinguishing
 * feature — Web Push primary, per dossier S. Student persona spec).
 */
'use client';

import { useEffect, useState } from 'react';
import { Bell, BellOff, Calendar, FileText, GraduationCap } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { StudentMobileShell } from '@/components/student/mobile-shell';
import {
  getSubscriptionState,
  isPushSupported,
  subscribe,
  unsubscribe,
} from '@/lib/web-push';

interface NotifRow {
  id: string;
  icon: 'grade' | 'assignment' | 'class';
  title: string;
  body: string;
  at: string;
  unread: boolean;
}

const NOTIFS: readonly NotifRow[] = [
  { id: 'n1', icon: 'grade', title: 'Có điểm mới', body: 'Toán nâng cao — Kiểm tra 15 phút: 9.0 điểm', at: '15 phút trước', unread: true },
  { id: 'n2', icon: 'assignment', title: 'Bài tập sắp đến hạn', body: 'Bài tập chương 4 — Toán nâng cao · Còn 6 giờ', at: '1 giờ trước', unread: true },
  { id: 'n3', icon: 'class', title: 'Đổi phòng học', body: 'Văn 10 chiều nay chuyển sang phòng 209', at: '3 giờ trước', unread: true },
  { id: 'n4', icon: 'grade', title: 'Đã chấm bài', body: 'Phân tích bài thơ — Văn 10: 9.0 điểm', at: 'Hôm qua', unread: false },
  { id: 'n5', icon: 'class', title: 'Lịch thi sắp đến', body: 'Kiểm tra giữa kỳ Toán — 22/04/2026', at: '2 ngày trước', unread: false },
];

const ICONS = {
  grade: GraduationCap,
  assignment: FileText,
  class: Calendar,
} as const;

export default function StudentNotificationsPage() {
  const { toast } = useToast();
  const [pushState, setPushState] = useState<
    'subscribed' | 'not-subscribed' | 'denied' | 'unsupported' | 'loading'
  >('loading');

  useEffect(() => {
    if (!isPushSupported()) {
      setPushState('unsupported');
      return;
    }
    getSubscriptionState().then(setPushState);
  }, []);

  async function handleSubscribe() {
    setPushState('loading');
    const result = await subscribe();
    if (result.outcome === 'subscribed') {
      setPushState('subscribed');
      toast({ title: 'Đã bật thông báo', description: 'Bạn sẽ nhận được thông báo đẩy mới.' });
    } else if (result.outcome === 'denied') {
      setPushState('denied');
      toast({ title: 'Quyền bị từ chối', description: 'Hãy bật lại trong Cài đặt trình duyệt.' });
    } else {
      setPushState('not-subscribed');
      toast({
        title: 'Không bật được',
        description: result.error ?? 'Web Push chưa cấu hình ở môi trường này.',
      });
    }
  }

  async function handleUnsubscribe() {
    setPushState('loading');
    await unsubscribe();
    setPushState('not-subscribed');
    toast({ title: 'Đã tắt thông báo', description: 'Sẽ không nhận thông báo đẩy nữa.' });
  }

  return (
    <StudentMobileShell title="Thông báo" subtitle={`${NOTIFS.filter((n) => n.unread).length} chưa đọc`}>
      <Card className="border-primary/30 bg-primary/5">
        <CardContent className="flex items-center gap-3 p-4">
          {pushState === 'subscribed' ? (
            <Bell className="h-5 w-5 shrink-0 text-primary" aria-hidden />
          ) : (
            <BellOff className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden />
          )}
          <div className="min-w-0 flex-1">
            <div className="text-sm font-medium">Thông báo đẩy</div>
            <div className="text-xs text-muted-foreground">
              {pushState === 'subscribed' && 'Đang bật — nhận thông báo trên thiết bị này'}
              {pushState === 'not-subscribed' && 'Đang tắt — bật để nhận điểm mới và lịch thi'}
              {pushState === 'denied' && 'Bị từ chối — cần cấp quyền lại trong cài đặt trình duyệt'}
              {pushState === 'unsupported' && 'Trình duyệt không hỗ trợ Web Push'}
              {pushState === 'loading' && 'Đang kiểm tra…'}
            </div>
          </div>
          {pushState === 'subscribed' ? (
            <Button size="sm" variant="outline" onClick={handleUnsubscribe}>
              Tắt
            </Button>
          ) : pushState === 'not-subscribed' ? (
            <Button size="sm" onClick={handleSubscribe}>
              Bật
            </Button>
          ) : null}
        </CardContent>
      </Card>

      <ul className="mt-4 space-y-2">
        {NOTIFS.map((n) => {
          const Icon = ICONS[n.icon];
          return (
            <li key={n.id}>
              <Card className={n.unread ? 'border-primary/30' : ''}>
                <CardContent className="flex gap-3 p-3">
                  <div
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${n.unread ? 'bg-primary/15 text-primary' : 'bg-muted text-muted-foreground'}`}
                  >
                    <Icon className="h-4 w-4" aria-hidden />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="truncate text-sm font-medium">{n.title}</h3>
                      {n.unread ? <Badge variant="default" className="h-4 px-1 text-[10px]">Mới</Badge> : null}
                    </div>
                    <p className="text-xs text-muted-foreground">{n.body}</p>
                    <p className="mt-1 text-[10px] text-muted-foreground">{n.at}</p>
                  </div>
                </CardContent>
              </Card>
            </li>
          );
        })}
      </ul>
    </StudentMobileShell>
  );
}
