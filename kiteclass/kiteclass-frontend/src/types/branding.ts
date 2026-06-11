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
