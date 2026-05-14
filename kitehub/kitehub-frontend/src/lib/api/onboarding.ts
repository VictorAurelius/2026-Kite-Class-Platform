/**
 * Onboarding Progress API client (Wave 78 GAP-538).
 *
 * Schema source-of-truth:
 * `documents/01-business/kitehub/onboarding/api-contract.md`.
 *
 * @since Wave 78 Bucket B
 */

import { apiClient } from './client';

export const ONBOARDING_STEP_IDS = [
  'PROFILE_SETUP',
  'INVITE_TEAM',
  'IMPORT_DATA',
  'CREATE_FIRST_CLASS',
  'EXPLORE_FEATURES',
] as const;

export type OnboardingStepId = (typeof ONBOARDING_STEP_IDS)[number];

export interface OnboardingStep {
  stepId: OnboardingStepId;
  completed: boolean;
  completedAt: string | null;
}

export interface OnboardingProgressResponse {
  tenantId: string;
  completionPercent: number;
  totalSteps: number;
  completedSteps: number;
  lastUpdatedAt: string;
  steps: OnboardingStep[];
}

export interface OnboardingProgressUpdatePayload {
  stepId: OnboardingStepId;
  completed: boolean;
}

const ENDPOINT = '/api/v1/onboarding-progress';

export async function getOnboardingProgress(): Promise<OnboardingProgressResponse> {
  const { data } = await apiClient.get<OnboardingProgressResponse>(ENDPOINT);
  return data;
}

export async function updateOnboardingStep(
  payload: OnboardingProgressUpdatePayload
): Promise<OnboardingProgressResponse> {
  const { data } = await apiClient.put<OnboardingProgressResponse>(ENDPOINT, payload);
  return data;
}

export const ONBOARDING_STEP_LABELS_VI: Record<OnboardingStepId, { title: string; description: string }> = {
  PROFILE_SETUP: {
    title: 'Hoàn tất hồ sơ tenant',
    description: 'Logo, tên trung tâm, và persona đã được xác nhận.',
  },
  INVITE_TEAM: {
    title: 'Mời thành viên đầu tiên',
    description: 'Thêm ít nhất 1 thành viên khác, hoặc bỏ qua nếu chưa cần.',
  },
  IMPORT_DATA: {
    title: 'Nhập dữ liệu mẫu (tuỳ chọn)',
    description:
      'Bật để KiteHub seed dữ liệu mẫu giúp bạn khám phá nhanh. Có thể bỏ qua nếu muốn bắt đầu với workspace trống.',
  },
  CREATE_FIRST_CLASS: {
    title: 'Tạo lớp học đầu tiên',
    description: 'Thử nghiệm tính năng cốt lõi KiteClass.',
  },
  EXPLORE_FEATURES: {
    title: 'Khám phá tính năng',
    description: 'Tour ngắn các tính năng chính của KiteHub.',
  },
};
