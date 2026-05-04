/**
 * @kite/shared-ui — public API
 *
 * Cross-cutting UI components shared between kiteclass-frontend and
 * kitehub-frontend (per ADR-024).
 *
 * Phase 1 (this commit): empty scaffolding — only types + styles published.
 * Phase 2+: G1..G12 + D1..D10 components added per Track 2 wave-pack
 * (GAP-273 + GAP-279).
 *
 * See:
 * - documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md
 * - documents/02-architecture/design-system/dossier/04-component-gaps.md
 * - documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md
 */

// Re-export types as components are added
// Example (Phase 2+):
//   export { ConfirmDialog } from './components/D1-confirm-dialog';
//   export type { ConfirmDialogProps } from './components/D1-confirm-dialog';

// Phase 1 stub — keeps package importable but empty
export const SHARED_UI_VERSION = '0.1.0';
