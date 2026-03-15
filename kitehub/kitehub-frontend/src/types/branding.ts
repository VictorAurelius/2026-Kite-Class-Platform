export type JobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface BrandingJob {
  id: number;
  instanceId: number;
  status: JobStatus;
  progress: number;
  currentStep: string | null;
  logoUrl: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface BrandingAsset {
  id: number;
  instanceId: number;
  type: 'PROFILE' | 'HERO' | 'LOGO' | 'BANNER' | 'OG_IMAGE';
  url: string;
  cdnUrl: string | null;
  createdAt: string;
}

export interface LogoAnalysis {
  primaryColors: string[];
  theme: string;
  brandPersonality: string;
  suggestedFonts: string[];
}
