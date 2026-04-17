/**
 * User preferences types.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

export enum Language {
  VI = 'vi',
  EN = 'en',
}

export enum Theme {
  LIGHT = 'light',
  DARK = 'dark',
  AUTO = 'auto',
}

export interface NotificationPreferences {
  email: boolean;
  push: boolean;
  sms: boolean;
  enrollmentUpdates: boolean;
  paymentReminders: boolean;
  classReminders: boolean;
  attendanceAlerts: boolean;
}

export interface UserPreferences {
  id: number;
  userId: string;
  language: Language;
  timezone: string;
  theme: Theme;
  notificationPreferences: NotificationPreferences;
  createdAt: string;
  updatedAt: string;
}

export interface UpdatePreferencesRequest {
  language?: Language;
  timezone?: string;
  theme?: Theme;
  notificationPreferences?: Partial<NotificationPreferences>;
}
