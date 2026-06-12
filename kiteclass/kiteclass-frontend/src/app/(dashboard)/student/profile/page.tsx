/**
 * Student PWA — Cá nhân + cài đặt (profile + settings).
 *
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/profile.html`.
 * Includes social-login indicator stubs (Zalo OA + Google) — backend wiring
 * for new social-login providers is deferred to a follow-up sub-gap;
 * this view only shows the EXISTING auth state.
 */
'use client';

import Link from 'next/link';
import {
  ChevronRight,
  HelpCircle,
  Languages,
  LogOut,
  Mail,
  Moon,
  School,
  Shield,
  User,
} from 'lucide-react';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { useAuthStore } from '@/stores/auth-store';
import { StudentMobileShell } from '@/components/student/mobile-shell';

export default function StudentProfilePage() {
  const user = useAuthStore((state) => state.user);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  const fullName = user?.name ?? 'Nguyễn Minh An';
  const email = user?.email ?? 'an.nguyen@kitehub.me';
  const initials = fullName
    .split(' ')
    .map((s) => s.charAt(0))
    .slice(-2)
    .join('')
    .toUpperCase();

  return (
    <StudentMobileShell title="Cá nhân" subtitle="Hồ sơ và cài đặt">
      <Card>
        <CardContent className="flex items-center gap-4 p-4">
          <Avatar className="h-16 w-16">
            <AvatarFallback className="bg-primary/15 text-lg font-bold text-primary">
              {initials}
            </AvatarFallback>
          </Avatar>
          <div className="min-w-0 flex-1">
            <h2 className="truncate text-lg font-semibold">{fullName}</h2>
            <p className="truncate text-xs text-muted-foreground">{email}</p>
            <div className="mt-1 flex flex-wrap gap-1">
              <Badge variant="outline">
                <School className="mr-1 h-3 w-3" aria-hidden /> Lớp 10A2
              </Badge>
              <Badge variant="outline">Học sinh</Badge>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className="mt-4">
        <CardContent className="p-0">
          <Section title="Tài khoản">
            <Row icon={User} label="Hồ sơ cá nhân" href="/student/profile" />
            <Row icon={Mail} label="Liên kết email & Zalo / Google" href="/student/profile" />
            <Row icon={Shield} label="Bảo mật & mật khẩu" href="/student/profile" />
          </Section>
          <Separator />
          <Section title="Hiển thị">
            <Row icon={Moon} label="Giao diện tối" href="/student/profile" />
            <Row icon={Languages} label="Ngôn ngữ — Tiếng Việt" href="/student/profile" />
          </Section>
          <Separator />
          <Section title="Hỗ trợ">
            <Row icon={HelpCircle} label="Câu hỏi thường gặp" href="/student/profile" />
          </Section>
        </CardContent>
      </Card>

      <Button
        variant="outline"
        className="mt-6 w-full text-destructive hover:bg-destructive/10 hover:text-destructive"
        onClick={() => clearAuth()}
      >
        <LogOut className="mr-2 h-4 w-4" aria-hidden /> Đăng xuất
      </Button>

      <p className="mt-4 text-center text-xs text-muted-foreground">
        KiteClass · Phiên bản beta
      </p>
    </StudentMobileShell>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="px-4 pt-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">
        {title}
      </div>
      <ul>{children}</ul>
    </div>
  );
}

function Row({
  icon: Icon,
  label,
  href,
}: {
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
  label: string;
  href: string;
}) {
  return (
    <li>
      <Link
        href={href}
        className="flex items-center gap-3 px-4 py-3 transition-colors hover:bg-accent/50"
      >
        <Icon className="h-4 w-4 text-muted-foreground" aria-hidden />
        <span className="flex-1 text-sm">{label}</span>
        <ChevronRight className="h-4 w-4 text-muted-foreground" aria-hidden />
      </Link>
    </li>
  );
}
