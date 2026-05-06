/**
 * @kite/shared-ui — public API
 *
 * Cross-cutting UI components shared between kiteclass-frontend and
 * kitehub-frontend (per ADR-024).
 *
 * Phase 2 (Wave 23 Bucket BC, GAP-353): ConsentBanner — PDPL 2023 Articles 11-13
 * cookie/consent UI for KH + KC public marketing surfaces.
 *
 * Future phases: G1..G12 + D1..D10 components per Track 2 wave-pack
 * (GAP-273 + GAP-279).
 *
 * See:
 * - documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md
 * - documents/02-architecture/design-system/dossier/04-component-gaps.md
 * - documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md
 * - documents/04-quality/gaps/GAP-353-pdpl-cookie-consent-banner-marketing-kits.md
 */

// ConsentBanner — PDPL 2023 cookie/consent (Wave 23 Bucket BC)
export {
  ConsentBanner,
  useConsent,
  DEFAULT_STORAGE_KEY,
  readConsent,
  writeConsent,
  clearConsent,
  validate,
} from './components/ConsentBanner';
export type {
  ConsentBannerProps,
  ConsentCategory,
  ConsentState,
  PartialCategories,
} from './components/ConsentBanner';

// G5 PaymentMethodSelector — VN multi-gateway picker (Wave 27 Bucket C, GAP-273)
export { PaymentMethodSelector } from './components/G5-payment-method-selector';
export type {
  PaymentMethod,
  PaymentMethodOption,
  PaymentMethodSelectorProps,
} from './components/G5-payment-method-selector';

// Phase 1 stub — kept for back-compat consumers reading version.
export const SHARED_UI_VERSION = '0.2.0';
