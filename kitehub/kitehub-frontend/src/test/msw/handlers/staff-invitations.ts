/**
 * MSW handlers for Staff Invitations endpoints (Wave 79 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/roles/use-cases.md` UC-ROLE-STAFF-INVITE
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Bucket B (GAP-561) FE staff invitation UI will consume these handlers in unit
 * tests before BE module lands (Wave 79 Bucket B target).
 *
 * Endpoints covered:
 *   - POST   /api/v1/staff-invitations            — Owner invite new staff
 *   - GET    /api/v1/staff-invitations/{token}    — Recipient fetch invite details
 *   - POST   /api/v1/staff-invitations/{token}/accept — Recipient accept + set password
 *   - DELETE /api/v1/staff-invitations/{id}       — Owner revoke pending invite or disable staff
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 79 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_MIN_LEN = 12;
const STAFF_MAX_PER_TENANT = 50;

interface InvitePayload {
  email?: string;
  fullName?: string;
}

interface AcceptPayload {
  password?: string;
  fullName?: string;
}

interface StaffInvitation {
  id: string;
  tenantId: string;
  tenantName: string;
  email: string;
  fullName: string;
  role: 'STAFF';
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED';
  invitedBy: string;
  createdAt: string;
  expiresAt: string;
}

// In-memory store for stateful test scenarios (cleared per test via reset)
const invitationStore = new Map<string, StaffInvitation>();
const tokenToIdMap = new Map<string, string>();

// Seeded default invitation for happy-path tests
const DEFAULT_PENDING_INVITATION: StaffInvitation = {
  id: 'invitation-00000000-0000-0000-0000-000000000001',
  tenantId: 'tenant-aaaa-0000-0000-0000-000000000001',
  tenantName: 'Trung tâm Anh ngữ Demo',
  email: 'staff.new@example.edu.vn',
  fullName: 'Nguyễn Văn Mẫu',
  role: 'STAFF',
  status: 'PENDING',
  invitedBy: 'owner-uuid-00001',
  createdAt: '2026-05-14T09:00:00Z',
  expiresAt: '2026-05-21T09:00:00Z',
};

const DEFAULT_INVITATION_TOKEN = 'invite-jwt-token-fixture-default';

invitationStore.set(DEFAULT_PENDING_INVITATION.id, DEFAULT_PENDING_INVITATION);
tokenToIdMap.set(DEFAULT_INVITATION_TOKEN, DEFAULT_PENDING_INVITATION.id);

function errorBody(code: string, message: string) {
  return HttpResponse.json({ error: code, message }, { status: errorStatus(code) });
}

function errorStatus(code: string): number {
  switch (code) {
    case 'EMAIL_ALREADY_INVITED':
    case 'INVITATION_ALREADY_USED':
    case 'CONSENT_ALREADY_RECORDED':
      return 409;
    case 'STAFF_CAP_REACHED':
      return 422;
    case 'INVITATION_EXPIRED':
    case 'CONSENT_ALREADY_WITHDRAWN':
      return 410;
    case 'INVALID_TOKEN':
    case 'UNAUTHORIZED':
      return 401;
    case 'FORBIDDEN':
      return 403;
    case 'WEAK_PASSWORD':
    case 'INVALID_REQUEST':
      return 400;
    case 'NOT_FOUND':
      return 404;
    case 'RATE_LIMITED':
      return 429;
    default:
      return 500;
  }
}

export const staffInvitationsHandlers: HttpHandler[] = [
  /** POST /api/v1/staff-invitations — Owner create invitation */
  http.post('/api/v1/staff-invitations', async ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth || !auth.startsWith('Bearer ')) {
      return errorBody('UNAUTHORIZED', 'Missing access token');
    }
    // Fixture: any Bearer token with `owner-` prefix is treated as OWNER role.
    if (!auth.includes('owner-')) {
      return errorBody('FORBIDDEN', 'Only OWNER role can invite staff');
    }

    const body = (await request.json().catch(() => ({}))) as InvitePayload;
    if (!body.email || !EMAIL_RE.test(body.email)) {
      return errorBody('INVALID_REQUEST', 'Invalid email');
    }
    if (!body.fullName || body.fullName.trim().length < 2) {
      return errorBody('INVALID_REQUEST', 'Full name required');
    }

    // Duplicate check
    for (const inv of invitationStore.values()) {
      if (
        inv.email === body.email &&
        (inv.status === 'PENDING' || inv.status === 'ACCEPTED')
      ) {
        return errorBody('EMAIL_ALREADY_INVITED', 'Email already invited or staff member');
      }
    }

    // Cap check
    const activeCount = [...invitationStore.values()].filter(
      (i) => i.status === 'ACCEPTED'
    ).length;
    if (activeCount >= STAFF_MAX_PER_TENANT) {
      return errorBody('STAFF_CAP_REACHED', `Reached cap of ${STAFF_MAX_PER_TENANT} staff`);
    }

    const id = `invitation-${crypto.randomUUID()}`;
    const token = `invite-jwt-${crypto.randomUUID()}`;
    const now = new Date();
    const expires = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    const invitation: StaffInvitation = {
      id,
      tenantId: 'tenant-aaaa-0000-0000-0000-000000000001',
      tenantName: 'Trung tâm Anh ngữ Demo',
      email: body.email,
      fullName: body.fullName,
      role: 'STAFF',
      status: 'PENDING',
      invitedBy: 'owner-uuid-00001',
      createdAt: now.toISOString(),
      expiresAt: expires.toISOString(),
    };
    invitationStore.set(id, invitation);
    tokenToIdMap.set(token, id);

    return HttpResponse.json(invitation, { status: 201 });
  }),

  /** GET /api/v1/staff-invitations/{token} — Recipient fetch invite details */
  http.get('/api/v1/staff-invitations/:token', ({ params }) => {
    const token = params.token as string;
    const id = tokenToIdMap.get(token);
    if (!id) return errorBody('INVALID_TOKEN', 'Invitation token not found');
    const inv = invitationStore.get(id);
    if (!inv) return errorBody('NOT_FOUND', 'Invitation not found');

    if (new Date(inv.expiresAt) < new Date()) {
      return errorBody('INVITATION_EXPIRED', 'Invitation has expired');
    }
    if (inv.status === 'ACCEPTED') {
      return errorBody('INVITATION_ALREADY_USED', 'Invitation already accepted');
    }
    if (inv.status === 'REVOKED') {
      return errorBody('NOT_FOUND', 'Invitation revoked');
    }

    return HttpResponse.json({
      id: inv.id,
      tenantName: inv.tenantName,
      email: inv.email,
      fullName: inv.fullName,
      role: inv.role,
      invitedBy: inv.invitedBy,
      expiresAt: inv.expiresAt,
    });
  }),

  /** POST /api/v1/staff-invitations/{token}/accept — Recipient accept + set password */
  http.post('/api/v1/staff-invitations/:token/accept', async ({ params, request }) => {
    const token = params.token as string;
    const id = tokenToIdMap.get(token);
    if (!id) return errorBody('INVALID_TOKEN', 'Invitation token not found');
    const inv = invitationStore.get(id);
    if (!inv) return errorBody('NOT_FOUND', 'Invitation not found');

    if (new Date(inv.expiresAt) < new Date()) {
      return errorBody('INVITATION_EXPIRED', 'Invitation has expired');
    }
    if (inv.status === 'ACCEPTED') {
      return errorBody('INVITATION_ALREADY_USED', 'Invitation already accepted');
    }

    const body = (await request.json().catch(() => ({}))) as AcceptPayload;
    if (!body.password || body.password.length < PASSWORD_MIN_LEN) {
      return errorBody('WEAK_PASSWORD', `Password min ${PASSWORD_MIN_LEN} chars`);
    }
    if (!/[A-Z]/.test(body.password) || !/[a-z]/.test(body.password) || !/\d/.test(body.password)) {
      return errorBody('WEAK_PASSWORD', 'Password requires upper + lower + digit');
    }
    if (!body.fullName || body.fullName.trim().length < 2) {
      return errorBody('INVALID_REQUEST', 'Full name required');
    }

    inv.status = 'ACCEPTED';
    inv.fullName = body.fullName;
    invitationStore.set(id, inv);

    return HttpResponse.json(
      {
        accessToken: `fake-access-jwt-staff-${id}`,
        refreshToken: `fake-refresh-jwt-staff-${id}`,
        user: {
          id: `user-${id}`,
          email: inv.email,
          fullName: body.fullName,
          role: 'STAFF',
          tenantId: inv.tenantId,
        },
      },
      { status: 200 }
    );
  }),

  /** DELETE /api/v1/staff-invitations/{id} — Owner revoke invite or disable staff */
  http.delete('/api/v1/staff-invitations/:id', ({ request, params }) => {
    const auth = request.headers.get('Authorization');
    if (!auth || !auth.startsWith('Bearer ')) {
      return errorBody('UNAUTHORIZED', 'Missing access token');
    }
    if (!auth.includes('owner-')) {
      return errorBody('FORBIDDEN', 'Only OWNER role can revoke invitation');
    }

    const id = params.id as string;
    const inv = invitationStore.get(id);
    if (!inv) return errorBody('NOT_FOUND', 'Invitation not found');

    inv.status = 'REVOKED';
    invitationStore.set(id, inv);
    return new HttpResponse(null, { status: 204 });
  }),
];

/** Reset store between tests — call in `beforeEach`. */
export function resetStaffInvitationsStore(): void {
  invitationStore.clear();
  tokenToIdMap.clear();
  invitationStore.set(DEFAULT_PENDING_INVITATION.id, { ...DEFAULT_PENDING_INVITATION });
  tokenToIdMap.set(DEFAULT_INVITATION_TOKEN, DEFAULT_PENDING_INVITATION.id);
}

export { DEFAULT_PENDING_INVITATION, DEFAULT_INVITATION_TOKEN };
