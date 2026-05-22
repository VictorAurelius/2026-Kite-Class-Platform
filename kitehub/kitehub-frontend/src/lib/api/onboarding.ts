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
    title: 'Hoàn tất hồ sơ trung tâm',
    description: 'Logo, tên trung tâm, và vai trò người dùng đã được xác nhận.',
  },
  INVITE_TEAM: {
    title: 'Mời thành viên đầu tiên',
    description: 'Thêm Giáo viên chủ nhiệm, Quản lý, hoặc bỏ qua nếu chưa cần.',
  },
  IMPORT_DATA: {
    // Wave 105 Bucket B: dual-mode label phục vụ Owner persona (Hằng - 160 học viên sẵn có)
    // VÀ Solo/curious persona (demo data). Bulk-import ƯU TIÊN — đứng trước CREATE_FIRST_CLASS.
    title: 'Nhập danh sách học viên',
    description:
      'Tải lên danh sách học viên qua file Excel (.xlsx) — phù hợp khi bạn đã có dữ liệu từ Misa/Excel. Hoặc bật dữ liệu mẫu để khám phá thử nếu chưa có sẵn.',
  },
  CREATE_FIRST_CLASS: {
    title: 'Tạo lớp học đầu tiên',
    description: 'Tạo lớp và gán học viên đã nhập ở bước trước vào lớp.',
  },
  EXPLORE_FEATURES: {
    title: 'Khám phá tính năng',
    description: 'Tour ngắn các tính năng chính của KiteHub: điểm danh, chấm điểm, thông báo phụ huynh.',
  },
};
