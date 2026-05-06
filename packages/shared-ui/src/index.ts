/**
 * @kite/shared-ui — public API
 *
 * Cross-cutting UI components shared between kiteclass-frontend and
 * kitehub-frontend (per ADR-024).
 *
 * Phase 2 (Wave 23 Bucket BC, GAP-353): ConsentBanner — PDPL 2023 Articles 11-13
 * cookie/consent UI for KH + KC public marketing surfaces.
 *
 * Phase 2 (Wave 27, GAP-273): G2/G5/G6/G7 component port — Track 2 Phase 2.
 *
 * Phase 2 (Wave 28 Bucket A, GAP-273): G3 Gradebook Entry Grid + VN 10pt validator.
 *
 * Future phases: remaining G1/G4/G8..G12 + D1..D10 components per Track 2 wave-pack.
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

// G2 AttendanceRoster — daily attendance per class session (Wave 27 Bucket A, GAP-273)
export { AttendanceRoster } from './components/G2-attendance-roster';
export type {
  AttendanceRosterProps,
  AttendanceRosterState,
  AttendanceStatus,
  AttendanceChangeOpts,
  ClassSession,
  StudentRecord,
} from './components/G2-attendance-roster';

// G3 Gradebook Entry Grid — VN 10pt validation + Excel paste (Wave 28 Bucket A, GAP-273)
export {
  GradebookEntryGrid,
  validateGrade,
  parseExcelPaste,
} from './components/G3-gradebook-entry-grid';
export type {
  GradeValue,
  GradeWeight,
  GradeColumn,
  GradebookCell,
  GradebookCellState,
  GradebookCellStatus,
  GradebookEntryGridProps,
  GradebookGridState,
  GradebookSession,
  GradebookStudent,
  ValidateGradeResult,
} from './components/G3-gradebook-entry-grid';

// G5 PaymentMethodSelector — VN multi-gateway picker (Wave 27 Bucket C, GAP-273)
export { PaymentMethodSelector } from './components/G5-payment-method-selector';
export type {
  PaymentMethod,
  PaymentMethodOption,
  PaymentMethodSelectorProps,
} from './components/G5-payment-method-selector';

// G6 Invoice Detail — VN tax + currency (Wave 27 Bucket B, GAP-273)
export {
  InvoiceDetail,
  formatVNCurrency,
  formatVNTax,
} from './components/G6-invoice-detail';
export type {
  InvoiceData,
  InvoiceDetailProps,
  InvoiceDiscount,
  InvoiceLineItem,
  InvoiceState,
  InvoiceStatus,
  InvoiceStudent,
  InvoiceTaxBreakdown,
  InvoiceTenant,
} from './components/G6-invoice-detail';

// G7 ParentInvite — email/Zalo OA invite flow (Wave 27 Bucket D, GAP-273)
export { ParentInvite, validateEmail } from './components/G7-parent-invite';
export type {
  EmailValidationResult,
  InviteChannel,
  InviteState,
  ParentInviteProps,
} from './components/G7-parent-invite';

// Phase 1 stub — kept for back-compat consumers reading version.
export const SHARED_UI_VERSION = '0.2.0';
