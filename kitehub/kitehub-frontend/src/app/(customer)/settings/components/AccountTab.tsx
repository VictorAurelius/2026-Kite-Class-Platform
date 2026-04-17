'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { User, Building2, Lock, Save } from 'lucide-react';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';

interface AccountTabProps {
  user: {
    id?: string;
    email?: string;
    name?: string;
    role?: 'OWNER' | 'ADMIN';
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
