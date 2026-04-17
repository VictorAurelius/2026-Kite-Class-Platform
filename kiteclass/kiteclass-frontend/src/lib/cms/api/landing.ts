import axios from 'axios';

// API client with authentication
const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
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
