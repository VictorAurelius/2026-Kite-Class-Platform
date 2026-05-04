/**
 * Notification preference types (Wave 18a Bucket B — GAP-063 Phase 1).
 *
 * Mirrors the API contract documented in
 * documents/01-business/kitehub/notification/api-contract.md.
 */

export type NotificationType =
  | 'ABSENCE'
  | 'FEE_REMINDER'
  | 'EXAM_RESULT'
  | 'TRIAL_ENDING'
  | 'BILLING_INVOICE'
  | 'SECURITY_ALERT'
  | 'GENERAL_ANNOUNCEMENT';

export type NotificationChannelType = 'EMAIL' | 'SMS' | 'ZALO' | 'PUSH';

export interface NotificationPreferenceDto {
  notificationType: NotificationType;
  enabledChannels: NotificationChannelType[];
  mandatory: boolean;
}

export interface NotificationPreferenceListResponse {
  preferences: NotificationPreferenceDto[];
}

export interface UpdateNotificationPreferenceRequest {
  enabledChannels: NotificationChannelType[];
}

/** Vietnamese display label for each notification type. */
export const NOTIFICATION_TYPE_LABELS: Record<NotificationType, string> = {
  ABSENCE: 'Vắng mặt',
  FEE_REMINDER: 'Nhắc học phí',
  EXAM_RESULT: 'Kết quả kiểm tra',
  TRIAL_ENDING: 'Hết hạn dùng thử',
  BILLING_INVOICE: 'Hóa đơn thanh toán',
  SECURITY_ALERT: 'Cảnh báo bảo mật',
  GENERAL_ANNOUNCEMENT: 'Thông báo chung',
};

/** Channel display label + Phase 1 availability flag. */
export const CHANNEL_META: Record<NotificationChannelType, { label: string; available: boolean }> = {
  EMAIL: { label: 'Email', available: true },
  SMS: { label: 'SMS', available: false },
  ZALO: { label: 'Zalo', available: false },
  PUSH: { label: 'Đẩy (Push)', available: false },
};
