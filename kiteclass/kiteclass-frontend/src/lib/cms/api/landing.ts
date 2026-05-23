import axios from 'axios';

// SSR runs inside Docker container — localhost ≠ host gateway. Use
// INTERNAL_API_URL (Docker network DNS) for server, NEXT_PUBLIC_API_URL
// (browser-visible host port mapping) for client. Surfaced via Wave 105
// RST UI walk 2026-05-23 — kc-frontend SSR landing fetch ECONNREFUSED.
const isServer = typeof window === 'undefined';
// baseURL KHÔNG bao gồm /api/v1 — auth endpoints (/api/auth/*) không có /v1,
// landing endpoints tự thêm /api/v1 prefix tại callsite.
const baseURL = isServer
  ? (process.env.INTERNAL_API_URL || 'http://kite-gateway:9000')
  : (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000');

const apiClient = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Include cookies for authentication
});

export interface LandingPageContent {
  hero?: {
    title?: string;
    subtitle?: string;
    tagline?: string;
    image?: string;
  };
  about?: {
    content?: string;
    mission?: string;
    vision?: string;
  };
  courses?: {
    features?: string[];
  };
  contact?: {
    email?: string;
    phone?: string;
    address?: string;
  };
}

export interface SaveLandingPageRequest {
  heroTitle?: string;
  heroSubtitle?: string;
  heroImageUrl?: string;
  tagline?: string;
  teacherBio?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
}

export const landingApi = {
  /**
   * Get landing page content for a tenant
   */
  getLandingPage: async (tenantId: string) => {
    const response = await apiClient.get(`/api/v1/tenants/${tenantId}/landing`);
    return response.data.data;
  },

  /**
   * Update landing page content
   * Requires ADMIN or TEACHER role
   */
  updateLandingPage: async (tenantId: string, data: SaveLandingPageRequest) => {
    const response = await apiClient.put(`/api/v1/tenants/${tenantId}/landing`, data);
    return response.data.data;
  },
};

/**
 * Convert CMS form data to API request format
 */
export function transformFormDataToApiRequest(
  formData: LandingPageContent
): SaveLandingPageRequest {
  const { hero, about, contact } = formData;

  return {
    // Hero section
    heroTitle: hero?.title,
    heroSubtitle: hero?.subtitle,
    heroImageUrl: hero?.image,
    tagline: hero?.tagline,

    // About section
    teacherBio: about?.content,

    // Contact section
    contactEmail: contact?.email,
    contactPhone: contact?.phone,
    address: contact?.address,
  };
}

/**
 * Convert API response to CMS form data format
 */
export function transformApiResponseToFormData(apiData: SaveLandingPageRequest): LandingPageContent {
  return {
    hero: {
      title: apiData.heroTitle,
      subtitle: apiData.heroSubtitle,
      tagline: apiData.tagline,
      image: apiData.heroImageUrl,
    },
    about: {
      content: apiData.teacherBio,
    },
    contact: {
      email: apiData.contactEmail,
      phone: apiData.contactPhone,
      address: apiData.address,
    },
  };
}
