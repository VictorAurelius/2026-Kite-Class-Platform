'use client';

// ---------------------------------------------------------------------------
// ResourceToggle — Step 6: per-resource approve toggle.
//
// Per `ai-branding-guidelines.md` §4.2 the wizard MUST let the user approve
// each branding resource individually before deploy ("KHÔNG all-or-nothing
// approve · per-resource approve"). State lives in the WizardState reducer
// (action types `APPROVE_RESOURCE` / `UNAPPROVE_RESOURCE`) so it survives
// re-mount + persists across step navigation. v1 audit caught the prior
// implementation using local `useState`, which is BANNED by rework §4.2.
// ---------------------------------------------------------------------------

import { Switch } from '@/components/ui/switch';
import type { ApprovableResource, WizardAction } from './wizard-shared';

export interface ResourceToggleProps {
  /** Resource identifier — drives which APPROVE_RESOURCE payload fires. */
  resource: ApprovableResource;
  /** User-visible title (e.g. "Logo", "Bảng màu"). */
  title: string;
  /** One-line description (file size, palette hexes, template code, etc.). */
  description: string;
  /** Whether this resource is currently approved (from WizardState). */
  approved: boolean;
  /** Reducer dispatch — REQUIRED. Local state is BANNED per rework §4.2. */
  dispatch: React.Dispatch<WizardAction>;
  /**
   * Optional thumbnail rendered next to the title (data URI, SVG string,
   * or react node). Shown unstyled — caller controls dimensions.
   */
  thumbnail?: React.ReactNode;
}

/**
 * One row in the per-resource approve stack.
 *
 * Toggling fires `APPROVE_RESOURCE` / `UNAPPROVE_RESOURCE` on the dispatch
 * passed in — this component intentionally does NOT keep local state so the
 * approval set persists across step changes (per rework §4.2 + audit-caught
 * v1 bug).
 */
export function ResourceToggle({
  resource,
  title,
  description,
  approved,
  dispatch,
  thumbnail,
}: ResourceToggleProps) {
  function handleToggle(next: boolean) {
    dispatch({
      type: next ? 'APPROVE_RESOURCE' : 'UNAPPROVE_RESOURCE',
      payload: resource,
    });
  }

  return (
    <div
      data-testid={`resource-toggle-${resource}`}
      data-approved={approved ? 'true' : 'false'}
      className={`flex items-center gap-3 p-3 rounded-lg border transition-colors ${
        approved
          ? 'border-emerald-200 bg-emerald-50/40'
          : 'border-border bg-background'
      }`}
    >
      {thumbnail && (
        <div className="shrink-0" aria-hidden="true">
          {thumbnail}
        </div>
      )}
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-sm text-foreground">{title}</p>
        <p className="text-xs text-muted-foreground truncate">{description}</p>
      </div>
      <Switch
        checked={approved}
        onCheckedChange={handleToggle}
        aria-label={`Phê duyệt ${title}`}
        data-testid={`resource-toggle-switch-${resource}`}
      />
    </div>
  );
}
