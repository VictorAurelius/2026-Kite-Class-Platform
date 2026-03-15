const API_BASE = '/api/platform';

export const endpoints = {
  // Instance
  instances: {
    list: `${API_BASE}/instances`,
    byId: (id: number) => `${API_BASE}/instances/${id}`,
    byOwner: (ownerId: number) => `${API_BASE}/instances/owner/${ownerId}`,
    create: `${API_BASE}/instances`,
    update: (id: number) => `${API_BASE}/instances/${id}`,
    delete: (id: number) => `${API_BASE}/instances/${id}`,
    trialStatus: (id: number) => `${API_BASE}/instances/${id}/trial-status`,
    extendTrial: (id: number) => `${API_BASE}/instances/${id}/extend-trial`,
  },

  // Subscription
  subscriptions: {
    active: (instanceId: number) => `${API_BASE}/subscriptions/instance/${instanceId}/active`,
    byInstance: (instanceId: number) => `${API_BASE}/subscriptions/instance/${instanceId}`,
    upgrade: (id: number) => `${API_BASE}/subscriptions/${id}/upgrade`,
    downgrade: (id: number) => `${API_BASE}/subscriptions/${id}/downgrade`,
    cancel: (id: number) => `${API_BASE}/subscriptions/${id}`,
  },

  // Payment
  payments: {
    create: `${API_BASE}/payments`,
    byId: (id: number) => `${API_BASE}/payments/${id}`,
    qrCode: (id: number) => `${API_BASE}/payments/${id}/qr-code`,
    bySubscription: (subId: number) => `${API_BASE}/payments/subscription/${subId}`,
  },

  // Branding
  branding: {
    analyzelogo: `${API_BASE}/branding/ai/analyze-logo`,
    jobs: `${API_BASE}/branding/jobs`,
    jobById: (id: number) => `${API_BASE}/branding/jobs/${id}`,
    jobAssets: (id: number) => `${API_BASE}/branding/jobs/${id}/assets`,
    uploadAsset: (instanceId: number, type: string) => `${API_BASE}/branding/assets/${instanceId}/${type}`,
    listAssets: (instanceId: number) => `${API_BASE}/branding/assets/${instanceId}`,
    generateContent: `${API_BASE}/branding/content/generate`,
  },

  // Email
  email: {
    send: `${API_BASE}/emails/send`,
  },

  // Admin
  admin: {
    dashboard: `${API_BASE}/admin/dashboard`,
    revenue: `${API_BASE}/admin/revenue`,
    instances: `${API_BASE}/admin/instances`,
    suspend: (id: number) => `${API_BASE}/admin/instances/${id}/suspend`,
    activate: (id: number) => `${API_BASE}/admin/instances/${id}/activate`,
    pendingPayments: `${API_BASE}/admin/payments/pending`,
    confirmPayment: (id: number) => `${API_BASE}/admin/payments/${id}/confirm`,
    rejectPayment: (id: number) => `${API_BASE}/admin/payments/${id}/reject`,
  },

  // Auth
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    refresh: '/api/auth/refresh',
  },
} as const;
