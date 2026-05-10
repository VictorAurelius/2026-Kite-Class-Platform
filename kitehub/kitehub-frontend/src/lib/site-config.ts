/**
 * Centralized site URL + contact constants.
 *
 * Single source of truth for the public-facing domain. Wire every canonical
 * URL, sitemap entry, JSON-LD `url`, and `mailto:` reference through this
 * module so a future domain change is a one-line edit instead of a sweep.
 *
 * GAP-459 — established 2026-05-10 after AWS Activate denial caught
 * codebase still pointing at the obsolete `kitehub.vn` domain (Path C
 * decision GAP-458 selected `kitehub.me` Free via GitHub Student Pack).
 */

export const SITE_URL = 'https://kitehub.me';
export const SITE_HOST = 'kitehub.me';
export const SUPPORT_EMAIL = 'support@kitehub.me';
export const DPO_EMAIL = 'dpo@kitehub.me';
