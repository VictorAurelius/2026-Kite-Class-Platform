// ---------------------------------------------------------------------------
// facts-landing — pure helpers for the Step-2 landing facts (GAP-1234).
//
// Kept framework-free + side-effect-free so they can be unit-tested without a
// DOM. Two responsibilities:
//   1. `formatVnd` — render a raw price string as VN currency `1.500.000đ`
//      (VN thousands separator = dot; suffix `đ`) per vn-localization §1.
//   2. `buildLandingFactsPayload` — map captured facts → a PARTIAL
//      `UpdateLandingPageRequest` body. Only non-empty fields are included
//      (PATCH semantics; the BE mapper IGNOREs nulls). Returns `null` when the
//      user entered nothing, so the caller can skip the network call entirely.
//
// Field mapping (verified against kiteclass-core LandingPage entity + DTO):
//   address       → address
//   contactPhone  → contactPhone
//   contactEmail  → contactEmail
//   zaloUrl       → zaloUrl
//   tuitions[]    → pricingTiers[] : { name, price: "1.500.000đ", period: "/tháng" }
//                   (NOT `programs` — that field has no `price`; `pricingTiers`
//                    is `[{name, price, period, ...}]`, the correct tuition shape.)
// ---------------------------------------------------------------------------

import type { WizardFacts } from './wizard-shared';

/** Keep only digits from a free-typed price string. */
function digitsOnly(raw: string): string {
  return (raw ?? '').replace(/\D/g, '');
}

/**
 * Format a raw price string as VN currency, e.g. "1500000" → "1.500.000đ".
 * Returns '' for an empty / non-numeric input (no stray "đ").
 */
export function formatVnd(raw: string): string {
  const digits = digitsOnly(raw);
  if (!digits) return '';
  // VN thousands separator is a dot; group from the right.
  const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${grouped}đ`;
}

/** A single pricing-tier item sent to the BE landing. */
export interface LandingPricingTier {
  name: string;
  price: string;
  period: string;
}

/**
 * Partial `UpdateLandingPageRequest` body — only the keys the wizard sets.
 * Every field optional so PATCH semantics hold (BE mapper IGNOREs nulls).
 */
export interface LandingFactsPayload {
  address?: string;
  contactPhone?: string;
  contactEmail?: string;
  zaloUrl?: string;
  pricingTiers?: LandingPricingTier[];
}

/**
 * Build the partial landing payload from captured facts.
 * Returns `null` when nothing was entered (skip the request).
 */
export function buildLandingFactsPayload(
  facts: WizardFacts,
): LandingFactsPayload | null {
  const payload: LandingFactsPayload = {};

  const address = facts.address?.trim();
  if (address) payload.address = address;

  const contactPhone = facts.contactPhone?.trim();
  if (contactPhone) payload.contactPhone = contactPhone;

  const contactEmail = facts.contactEmail?.trim();
  if (contactEmail) payload.contactEmail = contactEmail;

  const zaloUrl = facts.zaloUrl?.trim();
  if (zaloUrl) payload.zaloUrl = zaloUrl;

  // Only rows with BOTH a name and a price become a pricing tier.
  const tiers: LandingPricingTier[] = (facts.tuitions ?? [])
    .map((t) => ({ name: t.name?.trim() ?? '', price: formatVnd(t.price) }))
    .filter((t) => t.name.length > 0 && t.price.length > 0)
    .map((t) => ({ name: t.name, price: t.price, period: '/tháng' }));
  if (tiers.length > 0) payload.pricingTiers = tiers;

  return Object.keys(payload).length > 0 ? payload : null;
}
