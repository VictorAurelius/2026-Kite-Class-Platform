'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import { Globe, Bell, ExternalLink, Copy, CheckCircle2, Info } from 'lucide-react';
import { Instance } from '@/types/instance';

interface InstanceTabProps {
  instance: Instance | undefined;
}

export function InstanceTab({ instance }: InstanceTabProps) {
  const [customDomain, setCustomDomain] = useState(instance?.customDomain || '');
  const [isSaving, setIsSaving] = useState(false);
  const [copied, setCopied] = useState(false);

  // Notification preferences
  const [notifications, setNotifications] = useState({
    emailNotifications: true,
    trialReminders: true,
  });

  const isPremium = instance?.tier === 'PREMIUM';
  const subdomain = instance?.subdomain || 'your-subdomain';
  const fullUrl = `https://${subdomain}.kiteclass.com`;

  const handleCopyUrl = () => {
    navigator.clipboard.writeText(fullUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSaveDomain = async () => {
    setIsSaving(true);
    try {
      if (instance?.id) {
        await apiClient.put(endpoints.instances.update(instance.id), { customDomain });
      }
    } catch {
      alert('Không thể lưu tên miền');
    } finally {
      setIsSaving(false);
    }
  };

  const handleNotificationChange = (key: keyof typeof notifications) => {
    setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
    // TODO: Implement notification settings API call
  };

  return (
    <div className="space-y-6">
      {/* Subdomain Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Globe className="h-5 w-5 text-primary" />
            <CardTitle>Subdomain</CardTitle>
          </div>
          <CardDescription>
            Địa chỉ truy cập KiteClass instance của bạn
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-2">
            <div className="flex-1 flex items-center gap-2 p-3 bg-muted rounded-md">
              <span className="text-sm font-medium">{fullUrl}</span>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={handleCopyUrl}
              >
                {copied ? (
                  <CheckCircle2 className="h-4 w-4 text-green-500" />
                ) : (
                  <Copy className="h-4 w-4" />
                )}
              </Button>
            </div>
            <Button variant="outline" asChild>
              <a href={fullUrl} target="_blank" rel="noopener noreferrer">
                <ExternalLink className="h-4 w-4 mr-2" />
                Mở
              </a>
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Subdomain được tạo khi đăng ký và không thể thay đổi
          </p>
        </CardContent>
      </Card>

      {/* Custom Domain Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Globe className="h-5 w-5 text-primary" />
              <CardTitle>Tên miền riêng</CardTitle>
            </div>
            {isPremium ? (
              <Badge variant="default">PREMIUM</Badge>
            ) : (
              <Badge variant="secondary">Cần nâng cấp</Badge>
            )}
          </div>
          <CardDescription>
            Sử dụng tên miền của riêng bạn để truy cập KiteClass
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {isPremium ? (
            <>
              <div className="space-y-2">
                <Label htmlFor="customDomain">Tên miền</Label>
                <Input
                  id="customDomain"
                  value={customDomain}
                  onChange={(e) => setCustomDomain(e.target.value)}
                  placeholder="classes.your-school.edu.vn"
                />
              </div>

              <Alert>
                <Info className="h-4 w-4" />
                <AlertTitle>Hướng dẫn cấu hình DNS</AlertTitle>
                <AlertDescription className="mt-2">
                  <p className="mb-2">
                    Để sử dụng tên miền riêng, vui lòng tạo CNAME record:
                  </p>
                  <div className="bg-muted p-2 rounded-md font-mono text-sm">
                    <span className="text-muted-foreground">CNAME</span>{' '}
                    <span className="text-primary">{customDomain || 'your-domain.com'}</span>{' '}
                    <span className="text-muted-foreground">→</span>{' '}
                    <span>{subdomain}.kiteclass.com</span>
                  </div>
                </AlertDescription>
              </Alert>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-sm">Trạng thái:</span>
                  {instance?.customDomainVerified ? (
                    <Badge variant="default" className="bg-green-500">
                      <CheckCircle2 className="h-3 w-3 mr-1" />
                      Đã xác minh
                    </Badge>
                  ) : (
                    <Badge variant="secondary">Chưa xác minh</Badge>
                  )}
                </div>
                <Button onClick={handleSaveDomain} disabled={isSaving || !customDomain}>
                  {isSaving ? 'Đang lưu...' : 'Lưu & Xác minh'}
                </Button>
              </div>
            </>
          ) : (
            <div className="text-center py-4">
              <p className="text-muted-foreground mb-4">
                Tính năng này chỉ khả dụng cho gói PREMIUM
              </p>
              <Button asChild>
                <a href="/billing">Nâng cấp ngay</a>
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Notification Preferences */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Bell className="h-5 w-5 text-primary" />
            <CardTitle>Thông báo</CardTitle>
          </div>
          <CardDescription>
            Cấu hình cách bạn nhận thông báo từ KiteClass
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label>Thông báo email</Label>
              <p className="text-sm text-muted-foreground">
                Nhận email về hoạt động của instance
              </p>
            </div>
            <Switch
              checked={notifications.emailNotifications}
              onCheckedChange={() => handleNotificationChange('emailNotifications')}
            />
          </div>

          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label>Nhắc nhở dùng thử</Label>
              <p className="text-sm text-muted-foreground">
                Nhận thông báo khi gần hết thời gian dùng thử
              </p>
            </div>
            <Switch
              checked={notifications.trialReminders}
              onCheckedChange={() => handleNotificationChange('trialReminders')}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
