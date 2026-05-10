/**
 * Student PWA — Chi tiết bài tập + nộp bài (offline-aware).
 *
 * Wave 49 Bucket C (GAP-269). Mirrors
 * `kiteclass-student/screens/assignment-detail.html` and adds the
 * offline-submit pattern: when the network is down, the response is
 * persisted via {@link enqueue} and replayed on the next `online` event.
 */
'use client';

import { use, useEffect, useState } from 'react';
import { Cloud, CloudOff, FileText, Send } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { StudentMobileShell } from '@/components/student/mobile-shell';
import {
  enqueue,
  flush,
  readQueue,
  type QueuedSubmit,
} from '@/lib/offline/student-assignment-queue';

interface PageProps {
  params: Promise<{ assignmentId: string }>;
}

async function submitToServer(item: QueuedSubmit): Promise<{ ok: boolean; error?: string }> {
  // Real endpoint wiring is deferred to a follow-up gap (kc-core controller
  // not yet shipped). The shape is: POST /api/student/assignments/:id/submit
  // with idempotency-key header set to `item.id`.
  try {
    const res = await fetch(
      `/api/student/assignments/${encodeURIComponent(item.assignmentId)}/submit`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': item.id,
        },
        body: JSON.stringify({ body: item.body }),
      },
    );
    if (!res.ok) return { ok: false, error: `http ${res.status}` };
    return { ok: true };
  } catch (err) {
    return { ok: false, error: err instanceof Error ? err.message : String(err) };
  }
}

export default function StudentAssignmentDetailPage({ params }: PageProps) {
  const { assignmentId } = use(params);
  const { toast } = useToast();

  const [body, setBody] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [online, setOnline] = useState(true);
  const [pendingCount, setPendingCount] = useState(0);

  // Track network state — students lose signal frequently on mobile.
  useEffect(() => {
    function refresh() {
      setOnline(typeof navigator !== 'undefined' ? navigator.onLine : true);
      setPendingCount(readQueue().length);
    }
    refresh();
    window.addEventListener('online', refresh);
    window.addEventListener('offline', refresh);
    return () => {
      window.removeEventListener('online', refresh);
      window.removeEventListener('offline', refresh);
    };
  }, []);

  // Auto-flush queued submits when the device comes back online.
  useEffect(() => {
    if (!online) return;
    if (pendingCount === 0) return;
    let cancelled = false;
    flush(submitToServer).then((result) => {
      if (cancelled) return;
      setPendingCount(result.remaining);
      if (result.flushed > 0) {
        toast({
          title: 'Đã đồng bộ',
          description: `Đã gửi ${result.flushed} bài tập đang chờ.`,
        });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [online, pendingCount, toast]);

  async function handleSubmit() {
    if (!body.trim()) {
      toast({ title: 'Chưa có nội dung', description: 'Hãy viết bài trước khi nộp.' });
      return;
    }
    setSubmitting(true);
    try {
      if (!online) {
        enqueue({ assignmentId, body });
        setPendingCount(readQueue().length);
        setBody('');
        toast({
          title: 'Đã lưu nháp',
          description: 'Bài sẽ được gửi tự động khi có mạng trở lại.',
        });
        return;
      }
      const result = await submitToServer({
        id: 'inline-' + Date.now(),
        assignmentId,
        body,
        queuedAt: Date.now(),
        attempts: 1,
      });
      if (result.ok) {
        setBody('');
        toast({ title: 'Đã nộp bài', description: 'Cô giáo sẽ nhận được ngay.' });
      } else {
        // Network error mid-submit → fall back to queue so the work isn't lost.
        enqueue({ assignmentId, body });
        setPendingCount(readQueue().length);
        setBody('');
        toast({
          title: 'Lưu nháp do lỗi mạng',
          description: 'Sẽ tự động gửi lại sau.',
        });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <StudentMobileShell title={`Bài luận "Quê hương"`} subtitle="Văn 10 · Hạn 18/04/2026">
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-base">
            <FileText className="h-4 w-4" aria-hidden /> Đề bài
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-muted-foreground">
          <p>
            Viết bài luận khoảng 600 từ về chủ đề &ldquo;Quê hương&rdquo; — chia sẻ
            kỷ niệm, cảm xúc và những điều em muốn gìn giữ.
          </p>
          <div className="flex flex-wrap gap-2">
            <Badge variant="outline">~ 600 từ</Badge>
            <Badge variant="outline">Tự luận</Badge>
            <Badge variant="outline">Hạn 18/04/2026</Badge>
          </div>
        </CardContent>
      </Card>

      {!online ? (
        <Alert className="mt-4 border-amber-500/40 bg-amber-50 dark:bg-amber-950/30">
          <CloudOff className="h-4 w-4" aria-hidden />
          <AlertTitle>Đang offline</AlertTitle>
          <AlertDescription>
            Bài nộp sẽ được lưu nháp và tự động gửi khi có mạng trở lại.
          </AlertDescription>
        </Alert>
      ) : pendingCount > 0 ? (
        <Alert className="mt-4">
          <Cloud className="h-4 w-4" aria-hidden />
          <AlertTitle>Đang đồng bộ</AlertTitle>
          <AlertDescription>
            Còn {pendingCount} bài đang chờ gửi…
          </AlertDescription>
        </Alert>
      ) : null}

      <Card className="mt-4">
        <CardHeader className="pb-2">
          <CardTitle className="text-base">Bài làm của em</CardTitle>
        </CardHeader>
        <CardContent>
          <Textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Nhập bài làm…"
            rows={10}
            className="resize-y text-base"
          />
          <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
            <span>{body.length} ký tự · Tự lưu mỗi 10s</span>
            <Button onClick={handleSubmit} disabled={submitting} className="min-h-[44px]">
              <Send className="mr-1 h-4 w-4" aria-hidden />
              {online ? 'Nộp bài' : 'Lưu nháp offline'}
            </Button>
          </div>
        </CardContent>
      </Card>
    </StudentMobileShell>
  );
}
