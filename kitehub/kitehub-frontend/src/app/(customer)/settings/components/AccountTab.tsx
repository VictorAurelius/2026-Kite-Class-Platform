'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Switch } from '@/components/ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { User, Building2, Lock, Save, Bell, Globe } from 'lucide-react';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

interface AccountTabProps {
  user: {
    id?: string;
    email?: string;
    name?: string;
    // GAP-562b (Wave 80 Bucket C): STAFF added to align with auth-store role
    // union; legacy ADMIN / PLATFORM_ADMIN retained until Wave 81 cutoff.
    role?: 'OWNER' | 'STAFF' | 'ADMIN' | 'PLATFORM_ADMIN';
  } | null;
  // Additional fields from instance (to be fetched separately)
  phone?: string;
  organizationName?: string;
}

export function AccountTab({ user, phone, organizationName }: AccountTabProps) {
  const [isProfileSaving, setIsProfileSaving] = useState(false);
  const [isPasswordSaving, setIsPasswordSaving] = useState(false);

  // Profile form state
  const [profileForm, setProfileForm] = useState({
    name: user?.name || '',
    email: user?.email || '',
    phone: phone || '',
  });

  // Organization form state
  const [orgForm, setOrgForm] = useState({
    organizationName: organizationName || '',
  });

  // Password form state
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  // Notification + locale preferences (Wave 31 Bucket D — KH pro v2 port).
  // Persisted client-side only for now; backend endpoint to wire up tracked
  // separately when notification-prefs API ships.
  const [prefsForm, setPrefsForm] = useState({
    emailNotifications: true,
    trialReminders: true,
    productUpdates: false,
    locale: 'vi' as 'vi' | 'en',
  });
  const [isPrefsSaving, setIsPrefsSaving] = useState(false);

  const handlePrefsSave = async () => {
    setIsPrefsSaving(true);
    try {
      // TODO(GAP-1394): pending BE /api/users/{id}/preferences (notification +
      // locale prefs composite). No such endpoint exists yet; the per-type
      // notification-preferences API (/api/v1/notification-preferences) does NOT
      // map to these UI toggles (trialReminders → TRIAL_ENDING is mandatory and
      // cannot be disabled; productUpdates + locale have no backend home).
      // Persisted client-side only until the composite endpoint ships.
      await new Promise((resolve) => setTimeout(resolve, 200));
    } finally {
      setIsPrefsSaving(false);
    }
  };

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsProfileSaving(true);
    try {
      await apiClient.put(endpoints.auth.profile, {
        email: user?.email,
        name: profileForm.name,
        phone: profileForm.phone,
      });
    } catch {
      alert('Không thể cập nhật thông tin');
    } finally {
      setIsProfileSaving(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      alert('Mật khẩu mới không khớp');
      return;
    }
    setIsPasswordSaving(true);
    try {
      await apiClient.post(endpoints.auth.changePassword, {
        email: user?.email,
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
    } catch {
      alert('Mật khẩu hiện tại không đúng');
    } finally {
      setIsPasswordSaving(false);
    }
    setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
  };

  return (
    <div className="space-y-6">
      {/* Profile Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <User className="h-5 w-5 text-primary" />
            <CardTitle>Thông tin cá nhân</CardTitle>
          </div>
          <CardDescription>
            Cập nhật thông tin tài khoản của bạn
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleProfileSubmit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="name">Họ và tên</Label>
                <Input
                  id="name"
                  value={profileForm.name}
                  onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
                  placeholder="Nhập họ và tên"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={profileForm.email}
                  onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                  placeholder="email@example.com"
                  disabled
                />
                <p className="text-xs text-muted-foreground">
                  Email không thể thay đổi
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Số điện thoại</Label>
                <Input
                  id="phone"
                  type="tel"
                  value={profileForm.phone}
                  onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                  placeholder="0912 345 678"
                />
              </div>
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={isProfileSaving}>
                {isProfileSaving ? (
                  <>Đang lưu...</>
                ) : (
                  <>
                    <Save className="h-4 w-4 mr-2" />
                    Lưu thay đổi
                  </>
                )}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Organization Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Building2 className="h-5 w-5 text-primary" />
            <CardTitle>Thông tin tổ chức</CardTitle>
          </div>
          <CardDescription>
            Thông tin về trung tâm/trường học của bạn
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="orgName">Tên tổ chức</Label>
              <Input
                id="orgName"
                value={orgForm.organizationName}
                onChange={(e) => setOrgForm({ ...orgForm, organizationName: e.target.value })}
                placeholder="Tên trung tâm/trường học"
              />
            </div>
            <div className="flex justify-end">
              <Button type="button" disabled={isProfileSaving}>
                <Save className="h-4 w-4 mr-2" />
                Lưu thay đổi
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Separator />

      {/* Notification & Locale Preferences (Wave 31 Bucket D) */}
      <Card data-testid="prefs-section">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Bell className="h-5 w-5 text-primary" />
            <CardTitle>Thông báo & Ngôn ngữ</CardTitle>
          </div>
          <CardDescription>
            Quản lý email thông báo và ngôn ngữ giao diện
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-5">
            <div className="flex items-center justify-between gap-4">
              <div className="space-y-1 min-w-0">
                <Label htmlFor="emailNotifications" className="text-sm font-medium">
                  Email thông báo chung
                </Label>
                <p className="text-xs text-muted-foreground">
                  Nhận email về sự kiện quan trọng của phiên bản
                </p>
              </div>
              <Switch
                id="emailNotifications"
                checked={prefsForm.emailNotifications}
                onCheckedChange={(checked) =>
                  setPrefsForm({ ...prefsForm, emailNotifications: checked })
                }
              />
            </div>

            <div className="flex items-center justify-between gap-4">
              <div className="space-y-1 min-w-0">
                <Label htmlFor="trialReminders" className="text-sm font-medium">
                  Nhắc hết hạn trial
                </Label>
                <p className="text-xs text-muted-foreground">
                  Thông báo khi trial gần hết hạn (3, 1 ngày trước)
                </p>
              </div>
              <Switch
                id="trialReminders"
                checked={prefsForm.trialReminders}
                onCheckedChange={(checked) =>
                  setPrefsForm({ ...prefsForm, trialReminders: checked })
                }
              />
            </div>

            <div className="flex items-center justify-between gap-4">
              <div className="space-y-1 min-w-0">
                <Label htmlFor="productUpdates" className="text-sm font-medium">
                  Tin tức sản phẩm
                </Label>
                <p className="text-xs text-muted-foreground">
                  Cập nhật tính năng mới và mẹo sử dụng KiteClass
                </p>
              </div>
              <Switch
                id="productUpdates"
                checked={prefsForm.productUpdates}
                onCheckedChange={(checked) =>
                  setPrefsForm({ ...prefsForm, productUpdates: checked })
                }
              />
            </div>

            <Separator />

            <div className="flex items-center justify-between gap-4">
              <div className="space-y-1 min-w-0">
                <Label htmlFor="locale" className="text-sm font-medium flex items-center gap-1.5">
                  <Globe className="h-3.5 w-3.5" />
                  Ngôn ngữ giao diện
                </Label>
                <p className="text-xs text-muted-foreground">
                  Chọn ngôn ngữ hiển thị trong KiteHub
                </p>
              </div>
              <Select
                value={prefsForm.locale}
                onValueChange={(value: 'vi' | 'en') =>
                  setPrefsForm({ ...prefsForm, locale: value })
                }
              >
                <SelectTrigger id="locale" className="w-[160px]" data-testid="locale-select">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="vi">Tiếng Việt</SelectItem>
                  <SelectItem value="en">English</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex justify-end pt-2">
              <Button
                type="button"
                onClick={handlePrefsSave}
                disabled={isPrefsSaving}
                data-testid="prefs-save"
              >
                {isPrefsSaving ? (
                  'Đang lưu...'
                ) : (
                  <>
                    <Save className="h-4 w-4 mr-2" />
                    Lưu cài đặt
                  </>
                )}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Password Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Lock className="h-5 w-5 text-primary" />
            <CardTitle>Đổi mật khẩu</CardTitle>
          </div>
          <CardDescription>
            Cập nhật mật khẩu để bảo mật tài khoản
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="currentPassword">Mật khẩu hiện tại</Label>
              <Input
                id="currentPassword"
                type="password"
                value={passwordForm.currentPassword}
                onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                placeholder="Nhập mật khẩu hiện tại"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="newPassword">Mật khẩu mới</Label>
                <Input
                  id="newPassword"
                  type="password"
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                  placeholder="Nhập mật khẩu mới"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  value={passwordForm.confirmPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                  placeholder="Nhập lại mật khẩu mới"
                />
              </div>
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={isPasswordSaving}>
                {isPasswordSaving ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
