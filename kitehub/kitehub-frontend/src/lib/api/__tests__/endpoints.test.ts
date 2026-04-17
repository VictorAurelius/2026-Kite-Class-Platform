/**
 * Unit tests for API endpoints configuration.
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { endpoints } from '../endpoints';

const API_BASE = '/api/platform';

describe('endpoints', () => {
  describe('instances', () => {
    it('has list endpoint', () => {
      expect(endpoints.instances.list).toBe(`${API_BASE}/instances`);
    });

    it('generates byId endpoint', () => {
      expect(endpoints.instances.byId('uuid-123')).toBe(`${API_BASE}/instances/uuid-123`);
    });

    it('generates byOwner endpoint', () => {
      expect(endpoints.instances.byOwner('owner-uuid-456')).toBe(`${API_BASE}/instances/owner/owner-uuid-456`);
    });

    it('has create endpoint', () => {
      expect(endpoints.instances.create).toBe(`${API_BASE}/instances`);
    });

    it('generates update endpoint', () => {
      expect(endpoints.instances.update('uuid-789')).toBe(`${API_BASE}/instances/uuid-789`);
    });

    it('generates delete endpoint', () => {
      expect(endpoints.instances.delete('uuid-101')).toBe(`${API_BASE}/instances/uuid-101`);
    });

    it('generates trialStatus endpoint', () => {
      expect(endpoints.instances.trialStatus('uuid-111')).toBe(`${API_BASE}/instances/uuid-111/trial-status`);
    });

    it('generates extendTrial endpoint', () => {
      expect(endpoints.instances.extendTrial('uuid-222')).toBe(`${API_BASE}/instances/uuid-222/extend-trial`);
    });
  });

  describe('subscriptions', () => {
    it('generates active endpoint', () => {
      expect(endpoints.subscriptions.active('inst-123')).toBe(`${API_BASE}/subscriptions/instance/inst-123/active`);
    });

    it('generates byInstance endpoint', () => {
      expect(endpoints.subscriptions.byInstance('inst-456')).toBe(`${API_BASE}/subscriptions/instance/inst-456`);
    });

    it('generates upgrade endpoint', () => {
      expect(endpoints.subscriptions.upgrade('sub-789')).toBe(`${API_BASE}/subscriptions/sub-789/upgrade`);
    });

    it('generates downgrade endpoint', () => {
      expect(endpoints.subscriptions.downgrade('sub-101')).toBe(`${API_BASE}/subscriptions/sub-101/downgrade`);
    });

    it('generates cancel endpoint', () => {
      expect(endpoints.subscriptions.cancel('sub-111')).toBe(`${API_BASE}/subscriptions/sub-111`);
    });
  });

  describe('payments', () => {
    it('has create endpoint', () => {
      expect(endpoints.payments.create).toBe(`${API_BASE}/payments`);
    });

    it('generates byId endpoint', () => {
      expect(endpoints.payments.byId('pay-123')).toBe(`${API_BASE}/payments/pay-123`);
    });

    it('generates qrCode endpoint', () => {
      expect(endpoints.payments.qrCode('pay-456')).toBe(`${API_BASE}/payments/pay-456/qr-code`);
    });

    it('generates bySubscription endpoint', () => {
      expect(endpoints.payments.bySubscription('sub-789')).toBe(`${API_BASE}/payments/subscription/sub-789`);
    });
  });

  describe('branding', () => {
    it('generates uploadAsset endpoint', () => {
      expect(endpoints.branding.uploadAsset('inst-123', 'logo')).toBe(`${API_BASE}/branding/assets/inst-123/logo`);
    });

    it('has analyzeLogo endpoint', () => {
      expect(endpoints.branding.analyzeLogo).toBe(`${API_BASE}/branding/ai/analyze-logo`);
    });

    it('has jobs endpoint', () => {
      expect(endpoints.branding.jobs).toBe(`${API_BASE}/branding/jobs`);
    });

    it('generates jobById endpoint', () => {
      expect(endpoints.branding.jobById('job-123')).toBe(`${API_BASE}/branding/jobs/job-123`);
    });

    it('generates jobAssets endpoint', () => {
      expect(endpoints.branding.jobAssets('job-456')).toBe(`${API_BASE}/branding/jobs/job-456/assets`);
    });

    it('generates listAssets endpoint', () => {
      expect(endpoints.branding.listAssets('inst-789')).toBe(`${API_BASE}/branding/assets/inst-789`);
    });

    it('has generateContent endpoint', () => {
      expect(endpoints.branding.generateContent).toBe(`${API_BASE}/branding/content/generate`);
    });
  });

  describe('email', () => {
    it('has send endpoint', () => {
      expect(endpoints.email.send).toBe(`${API_BASE}/emails/send`);
    });
  });

  describe('admin', () => {
    it('has dashboard endpoint', () => {
      expect(endpoints.admin.dashboard).toBe(`${API_BASE}/admin/dashboard`);
    });

    it('has revenue endpoint', () => {
      expect(endpoints.admin.revenue).toBe(`${API_BASE}/admin/revenue`);
    });

    it('has instances endpoint', () => {
      expect(endpoints.admin.instances).toBe(`${API_BASE}/admin/instances`);
    });

    it('generates instanceById endpoint', () => {
      expect(endpoints.admin.instanceById('inst-123')).toBe(`${API_BASE}/admin/instances/inst-123`);
    });

    it('generates suspend endpoint', () => {
      expect(endpoints.admin.suspend('inst-456')).toBe(`${API_BASE}/admin/instances/inst-456/suspend`);
    });

    it('generates activate endpoint', () => {
      expect(endpoints.admin.activate('inst-789')).toBe(`${API_BASE}/admin/instances/inst-789/activate`);
    });

    it('generates extendTrial endpoint', () => {
      expect(endpoints.admin.extendTrial('inst-101')).toBe(`${API_BASE}/instances/inst-101/extend-trial`);
    });

    it('has subscriptions endpoint', () => {
      expect(endpoints.admin.subscriptions).toBe(`${API_BASE}/admin/subscriptions`);
    });

    it('has pendingPayments endpoint', () => {
      expect(endpoints.admin.pendingPayments).toBe(`${API_BASE}/admin/payments/pending`);
    });

    it('generates confirmPayment endpoint', () => {
      expect(endpoints.admin.confirmPayment('pay-123')).toBe(`${API_BASE}/admin/payments/pay-123/confirm`);
    });

    it('generates rejectPayment endpoint', () => {
      expect(endpoints.admin.rejectPayment('pay-456')).toBe(`${API_BASE}/admin/payments/pay-456/reject`);
    });
  });

  describe('auth', () => {
    it('has login endpoint', () => {
      expect(endpoints.auth.login).toBe('/api/auth/login');
    });

    it('has register endpoint', () => {
      expect(endpoints.auth.register).toBe('/api/auth/register');
    });

    it('has refresh endpoint', () => {
      expect(endpoints.auth.refresh).toBe('/api/auth/refresh');
    });
  });
});
