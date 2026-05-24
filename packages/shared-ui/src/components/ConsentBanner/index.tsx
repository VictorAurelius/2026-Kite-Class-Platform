export { ConsentBanner } from './ConsentBanner';
export { useConsent } from './useConsent';
export type { UseConsentOptions, UseConsentResult } from './useConsent';
export {
  DEFAULT_STORAGE_KEY,
  VISITOR_ID_STORAGE_KEY,
  getOrCreateVisitorId,
  readConsent,
  writeConsent,
  clearConsent,
  validate,
} from './storage';
export {
  buildPayload,
  getConsent as apiGetConsent,
  recordConsent as apiRecordConsent,
  revokeConsent as apiRevokeConsent,
} from './api';
export type { ConsentApiPayload, ConsentApiRecord } from './api';
export { applyAnalyticsConsent } from './analytics';
export type { ConsentMap } from './analytics';
export type {
  ConsentBannerProps,
  ConsentCategory,
  ConsentState,
  PartialCategories,
} from './types';
