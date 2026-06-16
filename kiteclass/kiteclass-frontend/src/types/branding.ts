/**
 * Branding types for organization customization.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

export interface Branding {
  id: number;
  instanceId: string;
  logoUrl?: string;
  faviconUrl?: string;
  displayName: string;
  tagline?: string;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  facebookUrl?: string;
  zaloUrl?: string;
  websiteUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateBrandingRequest {
  displayName?: string;
  tagline?: string;
  primaryColor?: string;
  secondaryColor?: string;
  accentColor?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  facebookUrl?: string;
  zaloUrl?: string;
  websiteUrl?: string;
}

export interface UploadLogoResponse {
  logoUrl: string;
  faviconUrl?: string;
}

/**
 * Response of a single landing banner upload (GAP-1211).
 *
 * Each upload stores a new image (no slot overwrite) and returns its renderable
 * URL, which the FE appends to the landing heroImages list.
 */
export interface BannerUploadResponse {
  url: string;
}

/**
 * A single branding version snapshot (GAP-1446).
 *
 * Maps the BE {@code BrandingVersion} entity returned by
 * {@code GET /api/v1/branding/{instanceId}/versions}. {@code snapshotJson} is a
 * serialized copy of the Branding fields at that point in time; the FE only
 * needs the metadata (number / active / timestamps) to render the history list,
 * so the snapshot is kept as a raw string.
 */
export interface BrandingVersion {
  id: number;
  instanceId: string;
  versionNumber: number;
  snapshotJson: string;
  /** Non-null when this row was created by rolling back to an earlier version. */
  rollbackOf?: number | null;
  /** Exactly one version per instance is active (the current branding). */
  active: boolean;
  createdAt: string;
  updatedAt?: string;
}

/**
 * Spring {@code Page<BrandingVersion>} wire shape (GAP-1446).
 *
 * The version endpoints return a raw Spring Data {@code Page} (NOT wrapped in
 * {@code ApiResponse}), so the FE reads {@code content} directly. Only the
 * fields the history list consumes are typed.
 */
export interface BrandingVersionPage {
  content: BrandingVersion[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
