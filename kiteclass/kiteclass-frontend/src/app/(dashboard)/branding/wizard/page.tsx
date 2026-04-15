'use client';

import { BrandingWizard } from '@/components/branding/wizard/BrandingWizard';

/**
 * Branding wizard page at {@code /branding/wizard}.
 *
 * Tier + tenant metadata are hard-coded for the scaffold; real version will read them
 * from the auth/session context (see {@code contexts/AuthContext}).
 *
 * @since Wave 3 Sub-PR 3.7 (GAP-013 + GAP-031 + GAP-069)
 */
export default function BrandingWizardPage() {
  return <BrandingWizard tier="PRO" tenantId="current-tenant" slug="my-school" />;
}
