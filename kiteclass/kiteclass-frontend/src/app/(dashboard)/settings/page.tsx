/**
 * Settings page with tabs navigation.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useState } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { BrandingSettings } from '@/components/settings/branding-settings';
import { PreferencesSettings } from '@/components/settings/preferences-settings';

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState('branding');

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Cài đặt</h1>
        <p className="text-muted-foreground">
          Quản lý thông tin tổ chức và tùy chọn cá nhân
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="branding">Branding</TabsTrigger>
          <TabsTrigger value="preferences">Tùy chọn</TabsTrigger>
        </TabsList>

        <TabsContent value="branding" className="mt-6">
          <BrandingSettings />
        </TabsContent>

        <TabsContent value="preferences" className="mt-6">
          <PreferencesSettings />
        </TabsContent>
      </Tabs>
    </div>
  );
}
