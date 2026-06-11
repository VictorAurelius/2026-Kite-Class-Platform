/**
 * Branding wizard — state machine types (GAP-013, GAP-031, GAP-069).
 *
 * A discriminated-union {@link WizardState} drives the whole flow; {@link WizardEvent}
 * captures every user intent. The reducer in `wizard-machine.ts` is the single source
 * of transition truth — 100% pure, 100% testable, no side effects.
 *
 * @since Wave 3 Sub-PR 3.7
 */

export type Segment = 'K12' | 'CENTER' | 'UNIV' | 'CORP' | 'OTHER';

export type Audience = 'students' | 'parents' | 'teachers' | 'staff';

export type Tone =
  | 'professional'
  | 'friendly'
  | 'energetic'
  | 'luxurious'
  | 'academic';

// BASIC = canonical (PricingTier.java, GAP-1228); 'PRO' giữ alias backward-compat JWT cũ
export type Tier = 'FREE' | 'BASIC' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';

export interface TemplateCandidate {
  id: string;
  label: string;
  previewUrl: string;
  segments: Segment[];
}

/**
 * Expanded brand inputs (GAP-031). Tier gates which fields are visible:
 * - FREE: segment + audience + tone + templateId  (4 fields)
 * - PRO: + colorHint + typographyHint            (6)
 * - PREMIUM: + contentDensity + imageryStyle + ctaStyle (9)
 * - ENTERPRISE: all fields below (16)
 */
export interface BrandInputs {
  segment?: Segment;
  audiences: Audience[];
  tone?: Tone;
  templateId?: string;

  // PRO tier
  colorHint?: string;
  typographyHint?: 'serif' | 'sans-serif' | 'rounded';

  // PREMIUM tier
  contentDensity?: 'spacious' | 'balanced' | 'dense';
  imageryStyle?: 'photo' | 'illustration' | 'mixed';
  ctaStyle?: 'button' | 'text-link' | 'banner';

  // ENTERPRISE tier
  customPrompt?: string;
  brandKeywords?: string[];
  bannedKeywords?: string[];
  preferredFonts?: string[];
  accessibilityLevel?: 'AA' | 'AAA';
  supportedLanguages?: string[];
  brandValues?: string[];
}

export type StepName =
  | 'welcome'
  | 'logo'
  | 'audience'
  | 'tone'
  | 'template'
  | 'preview';

export interface WizardContext {
  tier: Tier;
  instanceId?: number;
  inputs: BrandInputs;
  logoFilename?: string;
  regenerateCount: number;
  regenerateLimit: number;
  errorMessage?: string;
}

/**
 * Discriminated union — adding a new state forces exhaustive handling in the reducer
 * + UI because `never` compile error surfaces missed arms.
 */
export type WizardState =
  | { name: 'welcome'; context: WizardContext }
  | { name: 'logo'; context: WizardContext }
  | { name: 'audience'; context: WizardContext }
  | { name: 'tone'; context: WizardContext }
  | { name: 'template'; context: WizardContext }
  | { name: 'preview'; context: WizardContext }
  | { name: 'submitting'; context: WizardContext }
  | { name: 'done'; context: WizardContext }
  | { name: 'error'; context: WizardContext };

export type WizardEvent =
  | { type: 'NEXT' }
  | { type: 'BACK' }
  | { type: 'SET_SEGMENT'; segment: Segment }
  | { type: 'SET_LOGO_FILENAME'; filename?: string }
  | { type: 'TOGGLE_AUDIENCE'; audience: Audience }
  | { type: 'SET_TONE'; tone: Tone }
  | { type: 'SET_TEMPLATE'; templateId: string }
  | { type: 'SET_INPUT'; field: keyof BrandInputs; value: unknown }
  | { type: 'SUBMIT' }
  | { type: 'SUBMIT_OK'; instanceId: number }
  | { type: 'SUBMIT_FAIL'; message: string }
  | { type: 'REGENERATE' }
  | { type: 'USE_DEFAULTS' }
  | { type: 'RESET' };

/**
 * Default branding inputs applied khi user click "Sử dụng mặc định" (GAP-287).
 *
 * Per AC-ONBOARD-002: Solo teacher (FREE tier) cần escape ramp khỏi 6-step wizard.
 * Defaults được chọn an toàn cho mọi segment (templateId universal) và tone trung tính.
 * AI branding pipeline sẽ chạy với inputs này; user có thể quay lại Settings → Branding
 * để re-run wizard với custom inputs sau.
 */
export const DEFAULT_BRAND_INPUTS: Required<Pick<BrandInputs, 'audiences' | 'tone' | 'templateId'>> & {
  segment: Segment;
} = {
  segment: 'OTHER',
  audiences: ['students'],
  tone: 'professional',
  templateId: 'default-template-v1',
};

export const ORDERED_STEPS: StepName[] = [
  'welcome',
  'logo',
  'audience',
  'tone',
  'template',
  'preview',
];

export const REGENERATE_LIMIT_BY_TIER: Record<Tier, number> = {
  FREE: 3,
  BASIC: 10, // canonical (GAP-1228); PRO = alias cũ cùng cap
  PRO: 10,
  PREMIUM: 30,
  ENTERPRISE: Number.POSITIVE_INFINITY,
};

/** Fields visible at each tier (for rendering; reducer accepts all regardless). */
export const VISIBLE_FIELDS_BY_TIER: Record<Tier, (keyof BrandInputs)[]> = {
  FREE: ['segment', 'audiences', 'tone', 'templateId'],
  // BASIC = canonical (GAP-1228) — cùng field set với alias PRO
  BASIC: [
    'segment',
    'audiences',
    'tone',
    'templateId',
    'colorHint',
    'typographyHint',
  ],
  PRO: [
    'segment',
    'audiences',
    'tone',
    'templateId',
    'colorHint',
    'typographyHint',
  ],
  PREMIUM: [
    'segment',
    'audiences',
    'tone',
    'templateId',
    'colorHint',
    'typographyHint',
    'contentDensity',
    'imageryStyle',
    'ctaStyle',
  ],
  ENTERPRISE: [
    'segment',
    'audiences',
    'tone',
    'templateId',
    'colorHint',
    'typographyHint',
    'contentDensity',
    'imageryStyle',
    'ctaStyle',
    'customPrompt',
    'brandKeywords',
    'bannedKeywords',
    'preferredFonts',
    'accessibilityLevel',
    'supportedLanguages',
    'brandValues',
  ],
};
