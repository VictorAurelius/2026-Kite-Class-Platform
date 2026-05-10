/**
 * Student PWA — Chi tiết lớp (class detail).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/class-detail.html`.
 */
'use client';

import { use } from 'react';
import Link from 'next/link';
import { BookOpen, Clock, MapPin, MessageCircle, User, Users } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface PageProps {
  params: Promise<{ classId: string }>;
}

const CLASSMATES_PREVIEW = [
  'Nguyễn Minh An',
  'Trần Thị Bích',
  'Lê Hoàng Phúc',
  'Phạm Thu Hà',
  'Vũ Quốc Khánh',
];

export default function StudentClassDetailPage({ params }: PageProps) {
  const { classId } = use(params);

  return (
    <StudentMobileShell title="Toán nâng cao" subtitle={`Mã lớp: ${classId} · Khối 10`}>
      <Card>
        <CardContent className="space-y-3 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/15 text-lg font-bold text-primary">
              T
            </div>
            <div>
              <h2 className="text-base font-semibold">Toán nâng cao — 10A2</h2>
              <p className="text-xs text-muted-foreground">Năm học 2025-2026</p>
            </div>
          </div>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li className="flex items-center gap-2"><User className="h-4 w-4" aria-hidden /> Cô Trần Thị Hương</li>
            <li className="flex items-center gap-2"><Clock className="h-4 w-4" aria-hidden /> Thứ 2, 4, 6 · 10:00 – 11:30</li>
            <li className="flex items-center gap-2"><MapPin className="h-4 w-4" aria-hidden /> Phòng 305 — Toà A</li>
            <li className="flex items-center gap-2"><BookOpen className="h-4 w-4" aria-hidden /> Chương 4 — Hàm số bậc hai (đang học)</li>
          </ul>
          <div className="flex gap-2 pt-2">
            <Button variant="outline" size="sm" className="flex-1">
              <MessageCircle className="mr-1 h-4 w-4" aria-hidden /> Nhắn cô giáo
            </Button>
            <Button asChild variant="outline" size="sm" className="flex-1">
              <Link href="/student/assignments">Bài tập</Link>
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card className="mt-4">
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-sm">
            <Users className="h-4 w-4" aria-hidden /> Bạn cùng lớp ({CLASSMATES_PREVIEW.length}+)
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-0">
          <ul className="flex flex-wrap gap-2">
            {CLASSMATES_PREVIEW.map((name) => (
              <li key={name}>
                <Badge variant="outline" className="font-normal">{name}</Badge>
              </li>
            ))}
            <li>
              <Badge variant="outline" className="font-normal">+ 27 bạn khác</Badge>
            </li>
          </ul>
        </CardContent>
      </Card>
    </StudentMobileShell>
  );
}
