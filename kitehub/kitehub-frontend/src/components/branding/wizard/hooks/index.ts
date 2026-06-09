/**
 * Wave 34 Bucket D — wizard hooks barrel.
 *
 * Replaces 7 Wave 32 inline mocks (GAP-272 cluster) with real v1
 * endpoint consumers backed by MSW handlers in tests.
 */

export { useSlugAvailability } from './useSlugAvailability';
export type { UseSlugAvailability } from './useSlugAvailability';

export { useBrandingJobV1 } from './useBrandingJobV1';

export {
  useCreateBrandingJobV1,
  useApproveBrandingJob,
} from './useBrandingDeploy';
export type {
  CreateBrandingJobInput,
  ApproveBrandingJobInput,
  ApproveBrandingJobResponse,
} from './useBrandingDeploy';

export { usePreviewBrandColors } from './usePreviewBrandColors';
export type { UsePreviewBrandColorsResult } from './usePreviewBrandColors';

export { useQualityScore } from './useQualityScore';

export { usePreview } from './usePreview';

export { useDeployStream } from './useDeployStream';
export type {
  UseDeployStreamOptions,
  UseDeployStreamResult,
} from './useDeployStream';

export { useLifecycleEvents } from './useLifecycleEvents';
export type { UseLifecycleEventsOptions } from './useLifecycleEvents';

export { useRegenerateQuota } from './useRegenerateQuota';

export type {
  WizardJobStatus,
  WizardBrandColors,
  BrandingJobResponse,
  SlugAvailabilityResponse,
  RegenerateQuotaResponse,
  QualityScoreResponse,
  WizardLifecycleEvent,
  LifecycleEventType,
  LifecycleEventsResponse,
  DeployStreamEvent,
  DeployStreamEventName,
} from './types';
