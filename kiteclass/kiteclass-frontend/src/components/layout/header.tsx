/**
 * Header component with user menu, notifications, and mobile hamburger.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { Bell, Search, Settings, LogOut, User, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from '@/components/ui/sheet';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/hooks/useAuth';
import { SidebarNav } from './sidebar';

interface HeaderProps {
  mobileSidebarOpen?: boolean;
  onMobileSidebarToggle?: () => void;
  onMobileSidebarClose?: () => void;
}

export function Header({
  mobileSidebarOpen = false,
  onMobileSidebarToggle,
  onMobileSidebarClose,
}: HeaderProps) {
  const { logout, user } = useAuth();

  // Map the canonical role token → Vietnamese label for the user menu. The header
  // previously hardcoded "Chủ trung tâm / owner@example.com" (GAP-1168) which hid
  // the real signed-in identity regardless of who logged in.
  const roleLabels: Record<string, string> = {
    ADMIN: 'Quản trị viên',
    OWNER: 'Chủ trung tâm',
    TEACHER: 'Giáo viên',
    PARENT: 'Phụ huynh',
    STUDENT: 'Học viên',
  };
  const roleLabel = user?.userType
    ? roleLabels[String(user.userType)] ?? String(user.userType)
    : 'Khách';
  const displayEmail = user?.email ?? 'Chưa đăng nhập';
  const avatarText = (user?.email ?? 'KC').slice(0, 2).toUpperCase();

  return (
    <>
      <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b bg-background px-4 md:px-6">
        {/* Mobile hamburger */}
        <Button
          variant="ghost"
          size="icon"
          // h-11 w-11 (44px): WCAG 2.5.5 touch target on mobile; md:hidden keeps desktop untouched
          className="h-11 w-11 md:hidden"
          onClick={onMobileSidebarToggle}
          aria-label="Mở menu điều hướng"
        >
          <Menu className="h-5 w-5" />
        </Button>

        {/* Search Bar */}
        <div className="hidden flex-1 items-center gap-2 sm:flex">
          <Search className="h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="Tìm kiếm học viên, khóa học, giáo viên..."
            className="max-w-md border-none bg-muted/50 focus-visible:ring-0"
          />
        </div>
        {/* Spacer on mobile to push actions right */}
        <div className="flex-1 md:hidden" />

        {/* Actions */}
        <div className="flex items-center gap-2">
          {/* Notifications */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="relative">
                <Bell className="h-5 w-5" />
                <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-red-500" />
                <span className="sr-only">Thông báo</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-80">
              <DropdownMenuLabel>Thông báo</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <div className="p-4 text-center text-sm text-muted-foreground">
                Không có thông báo mới
              </div>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* Visible role chip (GAP — role was hidden inside dropdown only; show
              signed-in role at-a-glance so users/walkers see OWNER/TEACHER/STUDENT). */}
          <div className="hidden flex-col items-end leading-tight sm:flex">
            <span className="text-sm font-medium">{roleLabel}</span>
            <span className="text-xs text-muted-foreground">{displayEmail}</span>
          </div>

          {/* User Menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className="relative h-10 w-10 rounded-full"
                aria-label={`Tài khoản: ${roleLabel} (${displayEmail})`}
              >
                <Avatar>
                  <AvatarFallback>{avatarText}</AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium leading-none">{roleLabel}</p>
                  <p className="text-xs leading-none text-muted-foreground">
                    {displayEmail}
                  </p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem>
                <User className="mr-2 h-4 w-4" />
                Hồ sơ
              </DropdownMenuItem>
              <DropdownMenuItem>
                <Settings className="mr-2 h-4 w-4" />
                Cài đặt
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => logout()} className="text-red-600">
                <LogOut className="mr-2 h-4 w-4" />
                Đăng xuất
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      {/* Mobile sidebar Sheet */}
      <Sheet open={mobileSidebarOpen} onOpenChange={(open) => !open && onMobileSidebarClose?.()}>
        <SheetContent side="left" className="w-64 p-0">
          <SheetTitle className="sr-only">Menu điều hướng</SheetTitle>
          <SidebarNav onNavigate={onMobileSidebarClose} />
        </SheetContent>
      </Sheet>
    </>
  );
}
