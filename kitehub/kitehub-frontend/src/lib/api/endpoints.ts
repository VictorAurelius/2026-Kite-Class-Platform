const API_BASE = '/api/platform';

export const endpoints = {
  // Instance
  instances: {
    list: `${API_BASE}/instances`,
    byId: (id: string) => `${API_BASE}/instances/${id}`,
    byOwner: (ownerId: string) => `${API_BASE}/instances/owner/${ownerId}`,
    create: `${API_BASE}/instances`,
    register: `${API_BASE}/instances/register`,
    update: (id: string) => `${API_BASE}/instances/${id}`,
    delete: (id: string) => `${API_BASE}/instances/${id}`,
    trialStatus: (id: string) => `${API_BASE}/instances/${id}/trial-status`,
    extendTrial: (id: string) => `${API_BASE}/instances/${id}/extend-trial`,
    // SAAS-16: Custom domain endpoints
    domain: (id: string) => `/api/instances/${id}/domain`,
    domainVerify: (id: string) => `/api/instances/${id}/domain/verify`,
  },

  // Subscription
  subscriptions: {
    active: (instanceId: string) => `${API_BASE}/subscriptions/instance/${instanceId}/active`,
    byInstance: (instanceId: string) => `${API_BASE}/subscriptions/instance/${instanceId}`,
    create: `${API_BASE}/subscriptions`,
    upgrade: (id: string) => `${API_BASE}/subscriptions/${id}/upgrade`,
    downgrade: (id: string) => `${API_BASE}/subscriptions/${id}/downgrade`,
    cancel: (id: string) => `${API_BASE}/subscriptions/${id}`,
  },

  // Payment
  payments: {
    create: `${API_BASE}/payments`,
    byId: (id: string) => `${API_BASE}/payments/${id}`,
    qrCode: (id: string) => `${API_BASE}/payments/${id}/qr-code`,
    bySubscription: (subId: string) => `${API_BASE}/payments/subscription/${subId}`,
  },

  // Branding
  branding: {
    uploadAsset: (instanceId: string, type: string) => `${API_BASE}/branding/assets/${instanceId}/${type}`,
    analyzeLogo: `${API_BASE}/branding/ai/analyze-logo`,
    jobs: `${API_BASE}/branding/jobs`,
    jobById: (id: string) => `${API_BASE}/branding/jobs/${id}`,
    jobAssets: (id: string) => `${API_BASE}/branding/jobs/${id}/assets`,
    listAssets: (instanceId: string) => `${API_BASE}/branding/assets/${instanceId}`,
    generateContent: `${API_BASE}/branding/content/generate`,
    // SAAS-8: Template gallery
    templates: `${API_BASE}/branding/templates`,
    templateById: (id: string) => `${API_BASE}/branding/templates/${id}`,
    applyTemplate: (id: string) => `${API_BASE}/branding/templates/${id}/apply`,
  },

  // Wave 34 — AI Branding Wizard v1 endpoints (GAP-272 cluster).
  // Schema source-of-truth: documents/01-business/kitehub/ai-branding/api-contract.md
  brandingV1: {
    slugAvailability: '/api/v1/branding/slug-availability',
    regenerateQuota: '/api/v1/branding/regenerate-quota',
    jobById: (jobId: string) => `/api/v1/branding/jobs/${jobId}`,
    jobRegenerate: (jobId: string) => `/api/v1/branding/jobs/${jobId}/regenerate`,
    jobDeployStream: (jobId: string) => `/api/v1/branding/jobs/${jobId}/deploy-stream`,
    jobQualityScore: (jobId: string) => `/api/v1/branding/jobs/${jobId}/quality-score`,
    jobPreview: (jobId: string) => `/api/v1/branding/jobs/${jobId}/preview`,
    instanceLifecycleEvents: (instanceId: string) =>
      `/api/v1/branding/instances/${instanceId}/lifecycle/events`,
  },

  // Email
  email: {
    send: `${API_BASE}/emails/send`,
  },

  // Notification preferences (Wave 18a Bucket B — GAP-063 Phase 1)
  notificationPreferences: {
    list: `/api/v1/notification-preferences`,
    update: (notificationType: string) => `/api/v1/notification-preferences/${notificationType}`,
  },

  // Admin
  admin: {
    dashboard: `${API_BASE}/admin/dashboard`,
    revenue: `${API_BASE}/admin/revenue`,
    instances: `${API_BASE}/admin/instances`,
    instanceById: (id: string) => `${API_BASE}/admin/instances/${id}`,
    suspend: (id: string) => `${API_BASE}/admin/instances/${id}/suspend`,
    activate: (id: string) => `${API_BASE}/admin/instances/${id}/activate`,
    // GAP-953 (Wave provisioning-1 Bucket E) — admin retry provisioning for a failed/stuck instance.
    retryProvisioning: (id: string) => `${API_BASE}/admin/instances/${id}/retry-provisioning`,
    extendTrial: (id: string) => `${API_BASE}/instances/${id}/extend-trial`,
    subscriptions: `${API_BASE}/admin/subscriptions`,
    pendingPayments: `${API_BASE}/admin/payments/pending`,
    confirmPayment: (id: string) => `${API_BASE}/admin/payments/${id}/confirm`,
    rejectPayment: (id: string) => `${API_BASE}/admin/payments/${id}/reject`,
  },

  // Auth
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    refresh: '/api/auth/refresh',
    verifyEmail: '/api/auth/verify-email',
    resendVerification: '/api/auth/resend-verification',
    profile: '/api/auth/profile',
    changePassword: '/api/auth/change-password',
    // GAP-372 Wave 33 — Phase 1 BETA invite mechanism
    requestBetaAccess: '/api/v1/auth/request-beta-access',
    validateBetaToken: '/api/v1/auth/beta-signup/validate',
    completeBetaSignup: '/api/v1/auth/beta-signup',
    // GAP-609 Wave 91 — claim code exchange (alternate path when email link unavailable)
    exchangeClaimCode: '/api/v1/auth/beta-signup/exchange-claim-code',
  },

  // Admin coordinator endpoints (GAP-372 Wave 33)
  betaRequests: {
    list: '/api/v1/admin/beta-requests',
    approve: (id: number) => `/api/v1/admin/beta-requests/${id}/approve`,
    reject: (id: number) => `/api/v1/admin/beta-requests/${id}/reject`,
  },

  // Staff invitations (GAP-561b Wave 80 — Owner→Staff invite flow)
  staffInvitations: {
    list: '/api/v1/staff-invitations',
    create: '/api/v1/staff-invitations',
    byToken: (token: string) => `/api/v1/staff-invitations/by-token/${token}`,
    accept: (token: string) => `/api/v1/staff-invitations/${token}/accept`,
    resend: (id: string) => `/api/v1/staff-invitations/${id}/resend`,
    revoke: (id: string) => `/api/v1/staff-invitations/${id}`,
  },
} as const;
