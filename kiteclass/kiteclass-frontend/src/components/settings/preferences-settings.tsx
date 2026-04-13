/**
 * User preferences settings form component.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { usePreferences, useUpdatePreferences } from '@/hooks/use-preferences';
import { Language, Theme } from '@/types/preferences';
import { Globe, Moon, Bell } from 'lucide-react';

const TIMEZONES = [
  { value: 'Asia/Ho_Chi_Minh', label: 'Việt Nam (GMT+7)' },
  { value: 'Asia/Bangkok', label: 'Bangkok (GMT+7)' },
  { value: 'Asia/Singapore', label: 'Singapore (GMT+8)' },
  { value: 'Asia/Tokyo', label: 'Tokyo (GMT+9)' },
  { value: 'UTC', label: 'UTC (GMT+0)' },
];

const schema = z.object({
  language: z.nativeEnum(Language),
  timezone: z.string(),
  theme: z.nativeEnum(Theme),
  notificationPreferences: z.object({
    email: z.boolean(),
    push: z.boolean(),
    sms: z.boolean(),
    enrollmentUpdates: z.boolean(),
    paymentReminders: z.boolean(),
    classReminders: z.boolean(),
    attendanceAlerts: z.boolean(),
  }),
});

type FormData = z.infer<typeof schema>;

export function PreferencesSettings() {
  const { data: preferences, isLoading, isError } = usePreferences();
  const updateMutation = useUpdatePreferences();

  const {
    handleSubmit,
    watch,
    setValue,
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    values: preferences
      ? {
          language: preferences.language,
          timezone: preferences.timezone,
          theme: preferences.theme,
          notificationPreferences: preferences.notificationPreferences,
        }
      : undefined,
  });

  const language = watch('language');
  const timezone = watch('timezone');
  const theme = watch('theme');
  const notifications = watch('notificationPreferences');

  const onSubmit = (data: FormData) => {
    updateMutation.mutate(data);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Globe className="h-5 w-5" />
              Ngôn ngữ & Múi giờ
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="h-10 animate-pulse rounded bg-muted" />
            <div className="h-10 animate-pulse rounded bg-muted" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Moon className="h-5 w-5" />
              Giao diện
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-10 animate-pulse rounded bg-muted" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell className="h-5 w-5" />
              Thông báo
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-8 animate-pulse rounded bg-muted" />
            ))}
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
        Không thể tải tùy chọn. Vui lòng thử lại.
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Language & Timezone */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Globe className="h-5 w-5" />
            Ngôn ngữ & Múi giờ
          </CardTitle>
          <CardDescription>Cấu hình ngôn ngữ hiển thị và múi giờ</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <Label htmlFor="language">Ngôn ngữ</Label>
            <Select value={language} onValueChange={(value) => setValue('language', value as Language)}>
              <SelectTrigger id="language">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={Language.VI}>Tiếng Việt</SelectItem>
                <SelectItem value={Language.EN}>English</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div>
            <Label htmlFor="timezone">Múi giờ</Label>
            <Select value={timezone} onValueChange={(value) => setValue('timezone', value)}>
              <SelectTrigger id="timezone">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {TIMEZONES.map((tz) => (
                  <SelectItem key={tz.value} value={tz.value}>
                    {tz.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Theme */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Moon className="h-5 w-5" />
            Giao diện
          </CardTitle>
          <CardDescription>Chọn chế độ hiển thị giao diện</CardDescription>
        </CardHeader>
        <CardContent>
          <Select value={theme} onValueChange={(value) => setValue('theme', value as Theme)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={Theme.LIGHT}>Sáng</SelectItem>
              <SelectItem value={Theme.DARK}>Tối</SelectItem>
              <SelectItem value={Theme.AUTO}>Tự động</SelectItem>
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {/* Notifications */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bell className="h-5 w-5" />
            Thông báo
          </CardTitle>
          <CardDescription>Cấu hình các kênh nhận thông báo</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div>
            <h3 className="mb-3 font-medium">Kênh thông báo</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label htmlFor="email" className="cursor-pointer">
                  Email
                </Label>
                <Switch
                  id="email"
                  checked={notifications?.email}
                  onCheckedChange={(checked) =>
                    setValue('notificationPreferences.email', checked)
                  }
                />
              </div>

              <div className="flex items-center justify-between">
                <Label htmlFor="push" className="cursor-pointer">
                  Push notification
                </Label>
                <Switch
                  id="push"
                  checked={notifications?.push}
                  onCheckedChange={(checked) =>
                    setValue('notificationPreferences.push', checked)
                  }
                />
              </div>

              <div className="flex items-center justify-between">
                <Label htmlFor="sms" className="cursor-pointer">
                  SMS
                </Label>
                <Switch
                  id="sms"
                  checked={notifications?.sms}
                  onCheckedChange={(checked) =>
                    setValue('notificationPreferences.sms', checked)
                  }
                />
              </div>
            </div>
          </div>

          <div>
            <h3 className="mb-3 font-medium">Loại thông báo</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label htmlFor="enrollmentUpdates" className="cursor-pointer">
                  Cập nhật ghi danh
                </Label>
                <Switch
                  id="enrollmentUpdates"
                  checked={notifications?.enrollmentUpdates}
                  onCheckedChange={(checked) =>
                    setValue('notificationPreferences.enrollmentUpdates', checked)
                  }
                />
              </div>

              <div className="flex items-center justify-between">
                <Label htmlFor="paymentReminders" className="cursor-pointer">
                  Nhắc nhở thanh toán
                </Label>
                <Switch
                  id="paymentReminders"
                  checked={notifications?.paymentReminders}
                  onCheckedChange={(checked) =>
                    setValue('notificationPreferences.paymentReminders', checked)
                  }
                />
              </div>

              <div className="flex items-center justify-between">
                <Label htmlFor="classReminders" className="cursor-pointer">
                  Nhắc nhở lớp học
                </Label>
                <Switch
                  id="classReminders"
                  checked={notifications?.classReminders}
                  onCheckedChange={(checked) =>
                    setValue('notificationPreferences.classReminders', checked)
                  }
                />
              </div>

              <div className="flex items-center justify-between">
                <Label htmlFor="attendanceAlerts" className="cursor-pointer">
                  Cảnh báo điểm danh
                </Label>
                <Switch
                  id="attendanceAlerts"
                  checked={notifications?.attendanceAlerts}
                  onCheckedChange={(checked) =>
                    setValue('notificationPreferences.attendanceAlerts', checked)
                  }
                />
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button type="submit" disabled={updateMutation.isPending}>
          {updateMutation.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
        </Button>
      </div>
    </form>
  );
}
