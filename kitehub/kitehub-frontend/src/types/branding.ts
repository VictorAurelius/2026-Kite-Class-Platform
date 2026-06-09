export interface LogoAnalysis {
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  theme: 'MODERN' | 'CLASSIC' | 'PLAYFUL' | 'MINIMAL';
  brandPersonality: string[];
}

export interface BrandingJob {
  id: string; // UUID
  instanceId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress: number; // 0-100
  currentStep: string; // "Analyzing logo", "Generating profiles", etc.
  analysis: LogoAnalysis;
  createdAt: string;
  completedAt?: string;
}

export interface BrandingAsset {
  id: string;
  instanceId: string;
  // GAP-1116 — PORTRAIT: teacher/staff headshots uploaded in the wizard
  // portrait step; feed the banner-compose layer (1..N per instance).
  type: 'PROFILE' | 'HERO' | 'LOGO' | 'BANNER' | 'OG_IMAGE' | 'PORTRAIT';
  url: string;
  s3Key: string;
  createdAt: string;
}

// GAP-1108 — post-deploy status summary for the /branding page.
// Wire shape from GET /api/v1/branding/instances/{id}/deploy-status.
export interface BrandingDeployStatus {
  instanceId: string;
  /** LifecycleState name, e.g. "DEPLOYED" / "GENERATING" (null when no state row). */
  state: string | null;
  /** True when state === "DEPLOYED". */
  deployed: boolean;
  /** Placeholder landing URL from the latest deploy marker (null when never deployed). */
  frontendUrl: string | null;
  templateId: string | null;
  slug: string | null;
  brandingVersion: number | null;
  /** ISO timestamp of the latest deploy-completed marker (null when never deployed). */
  deployedAt: string | null;
}

export interface MarketingContent {
  title: string;
  subtitle: string;
  tagline: string;
  aboutUs: string;
}

// SAAS-8: Template gallery types
export interface BrandingTemplate {
  id: string;
  name: string;
  category: string;
  thumbnailUrl?: string;
  themeConfig: string;
  active: boolean;
  createdAt: string;
}

export interface ThemeConfig {
  colors: {
    primary: string;
    secondary: string;
    accent: string;
  };
  fonts: {
    heading: string;
    body: string;
  };
  style: string;
}
