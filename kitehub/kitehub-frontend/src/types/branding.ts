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
  type: 'PROFILE' | 'HERO' | 'LOGO' | 'BANNER' | 'OG_IMAGE';
  url: string;
  s3Key: string;
  createdAt: string;
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
