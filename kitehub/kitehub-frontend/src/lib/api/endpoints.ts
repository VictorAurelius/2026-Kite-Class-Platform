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
  },

  // Subscription
  subscriptions: {
    active: (instanceId: string) => `${API_BASE}/subscriptions/instance/${instanceId}/active`,
    byInstance: (instanceId: string) => `${API_BASE}/subscriptions/instance/${instanceId}`,
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
    instanceById: (id: string) => `${API_BASE}/admin/instances/${id}`,
    suspend: (id: string) => `${API_BASE}/admin/instances/${id}/suspend`,
    activate: (id: string) => `${API_BASE}/admin/instances/${id}/activate`,
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
  },
} as const;
