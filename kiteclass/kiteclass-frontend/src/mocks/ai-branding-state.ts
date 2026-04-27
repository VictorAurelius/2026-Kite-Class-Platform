/**
 * In-memory state for AI Branding MSW handlers — mirrors the kiteclass-core
 * `FrontendInstance` lifecycle state machine (see wave plan §7.2 +
 * `kiteclass-core/.../instance/entity/FrontendInstanceStatus.java`).
 *
 * Tracking: GAP-235 Sub-PR E2.
 */

export type FrontendInstanceStatus =
  | 'NOT_STARTED'
  | 'INITIALIZING'
  | 'GENERATING'
  | 'DEPLOYED'
  | 'REGENERATING'
  | 'FAILED';

export interface MockFrontendInstance {
  id: number;
  tenantId: string;
  slug: string;
  frontendUrl: string;
  status: FrontendInstanceStatus;
  brandingVersion: number;
  initializingAt: string | null;
  generatingAt: string | null;
  deployedAt: string | null;
  lastRegenerateAt: string | null;
  failedAt: string | null;
  retryCount: number;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MockBrandingPackage {
  instanceId: number;
  brandingVersion: number;
  theme: {
    primaryColor: string;
    secondaryColor: string;
    accentColor: string;
    backgroundColor: string;
    textColor: string;
    fontFamily: string;
  };
  assets: Record<string, string>;
  etag: string;
}

export interface MockQualityReport {
  id: number;
  targetInstanceId: number;
  brandingVersion: number;
  score: number;
  passed: boolean;
  contrastScore: number;
  cssVarsScore: number;
  assetUrlsScore: number;
  visualRegressionScore: number;
  logoPlacementScore: number;
  reviewedAt: string;
}

const ALLOWED: Record<FrontendInstanceStatus, FrontendInstanceStatus[]> = {
  NOT_STARTED: ['INITIALIZING', 'FAILED'],
  INITIALIZING: ['GENERATING', 'FAILED'],
  GENERATING: ['DEPLOYED', 'FAILED'],
  DEPLOYED: ['REGENERATING'],
  REGENERATING: ['DEPLOYED', 'FAILED'],
  FAILED: ['INITIALIZING'],
};

export function canTransition(from: FrontendInstanceStatus, to: FrontendInstanceStatus): boolean {
  return ALLOWED[from]?.includes(to) ?? false;
}

interface StateContainer {
  instances: Map<number, MockFrontendInstance>;
  qualityReports: Map<number, MockQualityReport>;
  nextId: number;
}

let state: StateContainer = freshState();

function freshState(): StateContainer {
  return {
    instances: new Map<number, MockFrontendInstance>(),
    qualityReports: new Map<number, MockQualityReport>(),
    nextId: 1,
  };
}

/** Seeds the demo dataset that {@code BrandingDataSeeder} produces server-side. */
function seedDemo(): void {
  const now = new Date();
  const instance: MockFrontendInstance = {
    id: 1,
    tenantId: 'dev-tenant-thanglong',
    slug: 'thanglong',
    frontendUrl: 'https://thanglong.kite.local',
    status: 'DEPLOYED',
    brandingVersion: 1,
    initializingAt: new Date(now.getTime() - 5 * 60_000).toISOString(),
    generatingAt: new Date(now.getTime() - 4 * 60_000).toISOString(),
    deployedAt: new Date(now.getTime() - 3 * 60_000).toISOString(),
    lastRegenerateAt: null,
    failedAt: null,
    retryCount: 0,
    failureReason: null,
    createdAt: new Date(now.getTime() - 6 * 60_000).toISOString(),
    updatedAt: new Date(now.getTime() - 3 * 60_000).toISOString(),
  };
  state.instances.set(instance.id, instance);
  state.qualityReports.set(instance.id, {
    id: 1,
    targetInstanceId: instance.id,
    brandingVersion: 1,
    score: 85,
    passed: true,
    contrastScore: 85,
    cssVarsScore: 90,
    assetUrlsScore: 80,
    visualRegressionScore: 82,
    logoPlacementScore: 88,
    reviewedAt: instance.deployedAt!,
  });
  state.nextId = 2;
}

seedDemo();

export const aiBrandingState = {
  list: (): MockFrontendInstance[] => Array.from(state.instances.values()),
  get: (id: number): MockFrontendInstance | undefined => state.instances.get(id),
  create: (partial: Partial<MockFrontendInstance>): MockFrontendInstance => {
    const id = state.nextId++;
    const now = new Date().toISOString();
    const instance: MockFrontendInstance = {
      id,
      tenantId: partial.tenantId ?? `mock-tenant-${id}`,
      slug: partial.slug ?? `mock-slug-${id}`,
      frontendUrl: partial.frontendUrl ?? `https://mock-${id}.kite.local`,
      status: 'NOT_STARTED',
      brandingVersion: 0,
      initializingAt: null,
      generatingAt: null,
      deployedAt: null,
      lastRegenerateAt: null,
      failedAt: null,
      retryCount: 0,
      failureReason: null,
      createdAt: now,
      updatedAt: now,
    };
    state.instances.set(id, instance);
    return instance;
  },
  update: (id: number, mutator: (i: MockFrontendInstance) => void): MockFrontendInstance | undefined => {
    const instance = state.instances.get(id);
    if (!instance) return undefined;
    mutator(instance);
    instance.updatedAt = new Date().toISOString();
    state.instances.set(id, instance);
    return instance;
  },
  qualityReport: (instanceId: number): MockQualityReport | undefined => state.qualityReports.get(instanceId),
  recordQualityReport: (report: MockQualityReport): void => {
    state.qualityReports.set(report.targetInstanceId, report);
  },
  reset: (): void => {
    state = freshState();
    seedDemo();
  },
};

/** Stamps timestamps + brandingVersion on `transitionTo`-style transitions. */
export function applyTransitionEffects(instance: MockFrontendInstance, target: FrontendInstanceStatus): void {
  const now = new Date().toISOString();
  instance.status = target;
  switch (target) {
    case 'INITIALIZING':
      instance.initializingAt = now;
      instance.failureReason = null;
      break;
    case 'GENERATING':
      instance.generatingAt = now;
      break;
    case 'DEPLOYED':
      instance.deployedAt = now;
      instance.brandingVersion += 1;
      break;
    case 'REGENERATING':
      instance.lastRegenerateAt = now;
      break;
    case 'FAILED':
      instance.failedAt = now;
      instance.retryCount += 1;
      break;
    default:
      break;
  }
}
