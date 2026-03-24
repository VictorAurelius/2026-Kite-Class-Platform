'use client';

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { AccountTab } from './components/AccountTab';
import { InstanceTab } from './components/InstanceTab';
import { DangerZone } from './components/DangerZone';
import { CustomDomainTab } from './components/CustomDomainTab';
import { User, Settings, AlertTriangle, Globe } from 'lucide-react';

export default function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const { data: instances, isLoading, error } = useOwnerInstances(user?.id);
  const instance = instances?.[0];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorAlert message="Không thể tải thông tin cài đặt. Vui lòng thử lại." />;
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-primary/10 p-3 text-primary">
            <Settings className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Cài đặt</h1>
            <p className="text-muted-foreground">
              Quản lý tài khoản và cấu hình instance của bạn
            </p>
          </div>
        </div>
      </div>

      <Tabs defaultValue="account" className="w-full">
        <TabsList className="grid w-full grid-cols-4 lg:w-[540px]">
          <TabsTrigger value="account" className="flex items-center gap-2">
            <User className="h-4 w-4" />
            <span className="hidden sm:inline">Tài khoản</span>
          </TabsTrigger>
          <TabsTrigger value="instance" className="flex items-center gap-2">
            <Settings className="h-4 w-4" />
            <span className="hidden sm:inline">Instance</span>
          </TabsTrigger>
          <TabsTrigger value="domain" className="flex items-center gap-2">
            <Globe className="h-4 w-4" />
            <span className="hidden sm:inline">Tên miền</span>
          </TabsTrigger>
          <TabsTrigger value="danger" className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            <span className="hidden sm:inline">Nguy hiểm</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="account" className="mt-6">
          <AccountTab
            user={user}
            organizationName={instance?.organizationName}
          />
        </TabsContent>

        <TabsContent value="instance" className="mt-6">
          <InstanceTab instance={instance} />
        </TabsContent>

        <TabsContent value="domain" className="mt-6">
          <CustomDomainTab instance={instance} />
        </TabsContent>

        <TabsContent value="danger" className="mt-6">
          <DangerZone instance={instance} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
