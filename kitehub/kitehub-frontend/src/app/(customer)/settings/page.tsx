'use client';

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { AccountTab } from './components/AccountTab';
import { InstanceTab } from './components/InstanceTab';
import { DangerZone } from './components/DangerZone';
import { User, Settings, AlertTriangle } from 'lucide-react';

export default function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const { data: instances, isLoading, error } = useOwnerInstances(user?.id);
  const instance = instances?.[0];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorAlert message="Không thể tải thông tin cài đặt. Vui lòng thử lại." />;
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Cài đặt</h1>
        <p className="text-muted-foreground mt-2">
          Quản lý tài khoản và cấu hình instance của bạn
        </p>
      </div>

      <Tabs defaultValue="account" className="w-full">
        <TabsList className="grid w-full grid-cols-3 lg:w-[400px]">
          <TabsTrigger value="account" className="flex items-center gap-2">
            <User className="h-4 w-4" />
            <span className="hidden sm:inline">Tài khoản</span>
          </TabsTrigger>
          <TabsTrigger value="instance" className="flex items-center gap-2">
            <Settings className="h-4 w-4" />
            <span className="hidden sm:inline">Instance</span>
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

        <TabsContent value="danger" className="mt-6">
          <DangerZone instance={instance} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
