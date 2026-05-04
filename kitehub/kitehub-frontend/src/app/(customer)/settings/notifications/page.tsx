'use client';

import { useMemo, useState } from 'react';
import { Bell, Lock } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import {
  useNotificationPreferences,
  useUpdateNotificationPreference,
} from '@/hooks/use-notification-preferences';
import {
  CHANNEL_META,
  NOTIFICATION_TYPE_LABELS,
  type NotificationChannelType,
  type NotificationPreferenceDto,
  type NotificationType,
} from '@/types/notification-preference';

/**
 * Notification preferences page (Wave 18a Bucket B — GAP-063 Phase 1).
 *
 * Phase 1 wires only the EMAIL channel. SMS / Zalo / Push toggles render
 * as disabled with "Sắp ra mắt — GAP-063b" tooltip per BR-NOTIF-002.
 */
export default function NotificationPreferencesPage() {
  const { data, isLoading, error } = useNotificationPreferences();
  const update = useUpdateNotificationPreference();
  const [errorByType, setErrorByType] = useState<Partial<Record<NotificationType, string>>>({});

  const preferences = useMemo(() => data?.preferences ?? [], [data]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorAlert message="Không thể tải tùy chọn thông báo. Vui lòng thử lại." />;
  }

  const handleToggleChannel = async (
    pref: NotificationPreferenceDto,
    channel: NotificationChannelType,
    enabled: boolean
  ) => {
    setErrorByType((prev) => ({ ...prev, [pref.notificationType]: undefined }));

    const next = new Set<NotificationChannelType>(pref.enabledChannels);
    if (enabled) next.add(channel);
    else next.delete(channel);

    try {
      await update.mutateAsync({
        notificationType: pref.notificationType,
        payload: { enabledChannels: Array.from(next) },
      });
    } catch (err: any) {
      // Map server errorCode → Vietnamese toast.
      const code = err?.response?.data?.errorCode;
      const msg =
        code === 'MANDATORY_TYPE_CANNOT_BE_DISABLED'
          ? 'Loại thông báo bắt buộc không thể tắt.'
          : 'Không thể cập nhật. Vui lòng thử lại.';
      setErrorByType((prev) => ({ ...prev, [pref.notificationType]: msg }));
    }
  };

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-primary/10 p-3 text-primary">
            <Bell className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Tùy chọn thông báo</h1>
            <p className="text-muted-foreground">
              Chọn kênh nhận thông báo cho từng loại sự kiện. Phase 1 chỉ hỗ trợ Email.
            </p>
          </div>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Kênh thông báo</CardTitle>
          <CardDescription>
            SMS, Zalo và đẩy thông báo (push) sẽ ra mắt trong giai đoạn 2 (theo dõi GAP-063b).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {preferences.map((pref) => (
              <PreferenceRow
                key={pref.notificationType}
                pref={pref}
                disabled={update.isPending}
                onToggle={(channel, enabled) => handleToggleChannel(pref, channel, enabled)}
                errorMessage={errorByType[pref.notificationType]}
              />
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

interface PreferenceRowProps {
  pref: NotificationPreferenceDto;
  disabled: boolean;
  onToggle: (channel: NotificationChannelType, enabled: boolean) => void;
  errorMessage?: string;
}

function PreferenceRow({ pref, disabled, onToggle, errorMessage }: PreferenceRowProps) {
  const channels: NotificationChannelType[] = ['EMAIL', 'SMS', 'ZALO', 'PUSH'];
  const isMandatory = pref.mandatory;
  const enabledSet = new Set(pref.enabledChannels);

  return (
    <div className="rounded-lg border p-4">
      <div className="flex items-center gap-2 mb-3">
        <span className="font-medium">{NOTIFICATION_TYPE_LABELS[pref.notificationType]}</span>
        {isMandatory && (
          <Badge variant="secondary" className="gap-1">
            <Lock className="h-3 w-3" />
            <span>Bắt buộc</span>
          </Badge>
        )}
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {channels.map((channel) => {
          const meta = CHANNEL_META[channel];
          const checked = enabledSet.has(channel);
          // EMAIL on mandatory types: disable toggle (cannot turn off).
          // Other channels: disable in Phase 1 (BR-NOTIF-002).
          const disableEmailMandatory = channel === 'EMAIL' && isMandatory;
          const isDisabled = disabled || !meta.available || disableEmailMandatory;

          return (
            <div
              key={channel}
              className="flex items-center justify-between gap-2 rounded-md border p-2"
              title={
                !meta.available
                  ? 'Sắp ra mắt — GAP-063b'
                  : disableEmailMandatory
                  ? 'Loại thông báo bắt buộc — Email không thể tắt'
                  : undefined
              }
              data-testid={`pref-${pref.notificationType}-${channel}`}
            >
              <span className={`text-sm ${!meta.available ? 'text-muted-foreground' : ''}`}>
                {meta.label}
                {!meta.available && (
                  <span className="ml-1 text-xs text-muted-foreground">(Sắp có)</span>
                )}
              </span>
              <Switch
                checked={checked}
                disabled={isDisabled}
                onCheckedChange={(value) => onToggle(channel, value)}
                aria-label={`${meta.label} toggle for ${pref.notificationType}`}
              />
            </div>
          );
        })}
      </div>

      {errorMessage && (
        <div className="mt-2 text-sm text-destructive" role="alert">
          {errorMessage}
        </div>
      )}
    </div>
  );
}
