/**
 * MSW handlers for Authentication 2FA endpoints (Wave 72b Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/kitehub/auth/api-contract.md`
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Wave 72b Bucket B (GAP-518/519) FE 2FA wizard + recovery codes UI will consume
 * these handlers in unit tests before BE Bucket A (GAP-516 TwoFactorController) lands.
 *
 * Endpoints covered:
 *   - POST /api/auth/2fa/enroll-init           (UC-AUTH-002 — generates secret + 10 recovery codes)
 *   - POST /api/auth/2fa/enroll-confirm        (UC-AUTH-002 — first TOTP code submit)
 *   - POST /api/auth/2fa/verify                (UC-AUTH-003 + UC-AUTH-004 — TOTP or recovery code)
 *   - POST /api/auth/2fa/recovery-codes/regenerate  (regenerate all 10 codes)
 *   - POST /api/auth/2fa/disable               (disable 2FA — blocked for PLATFORM_ADMIN)
 *
 * Login endpoint shape extension (POST /api/auth/login) also stubbed here for completeness —
 * production login lives elsewhere; this stub demonstrates the 3 response shapes Bucket B FE
 * MUST handle (success-no-2fa, requires-2fa, requires-enrollment).
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 72b Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

// ---------------------------------------------------------------
// Test fixtures (deterministic for snapshot stability)
// ---------------------------------------------------------------

// Stub TOTP secret — base32 of "JBSWY3DPEHPK3PXP" (RFC 6238 test vector).
const STUB_TOTP_SECRET = 'JBSWY3DPEHPK3PXP';

// Stub recovery codes — 8-char alphanumeric, no 0/o/1/l per BR-AUTH-007 alphabet.
const STUB_RECOVERY_CODES = [
  'ab23cd45',
  'ef67gh89',
  'ij2kmnp3',
  'qr4st5uv',
  'wx6yzab7',
  'cd8efgh9',
  'ij2kmnp4',
  'qr5st6uv',
  'wx7yzab8',
  'cd9efgh2',
];

const STUB_REGENERATED_CODES = [
  'p3qrst45',
  'uv6wxyz7',
  'ab8cde29',
  'fg2hij3k',
  'mn4pqr5s',
  'tu6vwx7y',
  'za8bc29d',
  'ef2gh3ij',
  'km4np5qr',
  'st6uv7wx',
];

// Stub user profile post-2FA-confirm
const STUB_USER_ADMIN = {
  id: 'user-admin-1',
  email: 'admin@kitehub.me',
  role: 'PLATFORM_ADMIN',
  totp_enrolled_at: '2026-05-14T10:23:00Z',
};

// Accept any 6-digit code matching this regex as "valid TOTP" for stub purposes.
const TOTP_CODE_RE = /^\d{6}$/;
// Recovery code regex: 8 alphanumeric chars from BR-AUTH-007 alphabet.
const RECOVERY_CODE_RE = /^[abcdefghijkmnpqrstuvwxyz23456789]{8}$/;

interface EnrollConfirmPayload {
  first_totp_code?: string;
}

interface VerifyPayload {
  challenge_token?: string;
  totp_code?: string;
  recovery_code?: string;
}

interface RegeneratePayload {
  current_totp_code?: string;
}

interface DisablePayload {
  current_totp_code?: string;
  password_reconfirm?: string;
}

interface LoginPayload {
  email?: string;
  password?: string;
}

export const authHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // POST /api/auth/2fa/enroll-init (UC-AUTH-002)
  // ---------------------------------------------------------------
  http.post('*/api/auth/2fa/enroll-init', ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth || !auth.toLowerCase().startsWith('bearer ')) {
      return HttpResponse.json(
        { error: 'INVALID_CHALLENGE', message: 'Missing or malformed Bearer token.' },
        { status: 401 }
      );
    }

    const token = auth.slice(7);

    // Stub: token starting with "expired-" simulates 410 path.
    if (token.startsWith('expired-')) {
      return HttpResponse.json(
        { error: 'CHALLENGE_EXPIRED', message: 'Challenge token expired (>5 min).' },
        { status: 410 }
      );
    }

    // Stub: token starting with "already-enrolled-" simulates 409 path.
    if (token.startsWith('already-enrolled-')) {
      return HttpResponse.json(
        { error: 'ALREADY_ENROLLED', message: 'User already has 2FA enrolled.' },
        { status: 409 }
      );
    }

    return HttpResponse.json({
      secret: STUB_TOTP_SECRET,
      qr_uri: `otpauth://totp/KiteHub:admin@kitehub.me?secret=${STUB_TOTP_SECRET}&issuer=KiteHub&algorithm=SHA1&digits=6&period=30`,
      recovery_codes: STUB_RECOVERY_CODES,
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/auth/2fa/enroll-confirm (UC-AUTH-002 step 5)
  // ---------------------------------------------------------------
  http.post('*/api/auth/2fa/enroll-confirm', async ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth || !auth.toLowerCase().startsWith('bearer ')) {
      return HttpResponse.json(
        { error: 'INVALID_CHALLENGE', message: 'Missing Bearer token.' },
        { status: 401 }
      );
    }

    let body: EnrollConfirmPayload;
    try {
      body = (await request.json()) as EnrollConfirmPayload;
    } catch {
      return HttpResponse.json(
        { error: 'INVALID_REQUEST', message: 'Malformed JSON.' },
        { status: 400 }
      );
    }

    // Stub: code "000000" simulates wrong-TOTP path for testing 401.
    if (!body.first_totp_code || body.first_totp_code === '000000') {
      return HttpResponse.json(
        { error: 'INVALID_TOTP', message: 'First TOTP code does not match generated secret.' },
        { status: 401 }
      );
    }

    if (!TOTP_CODE_RE.test(body.first_totp_code)) {
      return HttpResponse.json(
        { error: 'INVALID_REQUEST', message: 'first_totp_code must be 6 digits.' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      enrolled: true,
      totp_enrolled_at: '2026-05-14T10:23:00Z',
      access_token: 'stub-access-token-2fa-enrolled',
      refresh_token: 'stub-refresh-token-2fa-enrolled',
      user: STUB_USER_ADMIN,
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/auth/2fa/verify (UC-AUTH-003 TOTP path + UC-AUTH-004 recovery path)
  // ---------------------------------------------------------------
  http.post('*/api/auth/2fa/verify', async ({ request }) => {
    let body: VerifyPayload;
    try {
      body = (await request.json()) as VerifyPayload;
    } catch {
      return HttpResponse.json(
        { error: 'INVALID_REQUEST', message: 'Malformed JSON.' },
        { status: 400 }
      );
    }

    if (!body.challenge_token) {
      return HttpResponse.json(
        { error: 'INVALID_CHALLENGE', message: 'challenge_token required.' },
        { status: 401 }
      );
    }

    // Stub: challenge_token starting with "expired-" simulates 410 path.
    if (body.challenge_token.startsWith('expired-')) {
      return HttpResponse.json(
        { error: 'CHALLENGE_EXPIRED', message: 'Challenge token expired (>5 min).' },
        { status: 410 }
      );
    }

    // Validate exactly one of totp_code OR recovery_code provided.
    const hasTotp = !!body.totp_code;
    const hasRecovery = !!body.recovery_code;
    if ((hasTotp && hasRecovery) || (!hasTotp && !hasRecovery)) {
      return HttpResponse.json(
        {
          error: 'INVALID_REQUEST',
          message: 'Exactly one of totp_code or recovery_code must be provided.',
        },
        { status: 400 }
      );
    }

    if (hasTotp) {
      // TOTP path — UC-AUTH-003
      if (body.totp_code === '000000' || !TOTP_CODE_RE.test(body.totp_code!)) {
        return HttpResponse.json(
          { error: 'INVALID_TOTP', message: 'TOTP code does not match.' },
          { status: 401 }
        );
      }

      return HttpResponse.json({
        access_token: 'stub-access-token-2fa-verified',
        refresh_token: 'stub-refresh-token-2fa-verified',
        user: STUB_USER_ADMIN,
      });
    }

    // Recovery code path — UC-AUTH-004
    if (!RECOVERY_CODE_RE.test(body.recovery_code!) || !STUB_RECOVERY_CODES.includes(body.recovery_code!)) {
      return HttpResponse.json(
        { error: 'INVALID_RECOVERY_CODE', message: 'Recovery code invalid or already used.' },
        { status: 401 }
      );
    }

    return HttpResponse.json({
      access_token: 'stub-access-token-recovery-used',
      refresh_token: 'stub-refresh-token-recovery-used',
      user: STUB_USER_ADMIN,
      regenerate_recommended: true,
      codes_remaining: 9,
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/auth/2fa/recovery-codes/regenerate
  // ---------------------------------------------------------------
  http.post('*/api/auth/2fa/recovery-codes/regenerate', async ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth || !auth.toLowerCase().startsWith('bearer ')) {
      return HttpResponse.json(
        { error: 'UNAUTHORIZED', message: 'Missing access token.' },
        { status: 401 }
      );
    }

    let body: RegeneratePayload;
    try {
      body = (await request.json()) as RegeneratePayload;
    } catch {
      return HttpResponse.json(
        { error: 'INVALID_REQUEST', message: 'Malformed JSON.' },
        { status: 400 }
      );
    }

    // Stub: code "000000" simulates wrong-TOTP path; "111111" simulates precondition-failed (TOTP valid but stale).
    if (!body.current_totp_code || body.current_totp_code === '000000') {
      return HttpResponse.json(
        { error: 'INVALID_TOTP', message: 'TOTP code does not match.' },
        { status: 401 }
      );
    }
    if (body.current_totp_code === '111111') {
      return HttpResponse.json(
        {
          error: 'TOTP_PRECONDITION_FAILED',
          message: 'TOTP code must be from the last 5 minutes (replay defence).',
        },
        { status: 412 }
      );
    }

    return HttpResponse.json({
      new_recovery_codes: STUB_REGENERATED_CODES,
      previous_codes_invalidated: 10,
      message: 'All previous recovery codes are now invalid. Save these new codes — they will not be shown again.',
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/auth/2fa/disable
  // ---------------------------------------------------------------
  http.post('*/api/auth/2fa/disable', async ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth || !auth.toLowerCase().startsWith('bearer ')) {
      return HttpResponse.json(
        { error: 'UNAUTHORIZED', message: 'Missing access token.' },
        { status: 401 }
      );
    }

    let body: DisablePayload;
    try {
      body = (await request.json()) as DisablePayload;
    } catch {
      return HttpResponse.json(
        { error: 'INVALID_REQUEST', message: 'Malformed JSON.' },
        { status: 400 }
      );
    }

    // Stub: bearer token containing "admin" simulates PLATFORM_ADMIN — disable blocked per BR-AUTH-005.
    if (auth.includes('admin')) {
      return HttpResponse.json(
        {
          error: 'CANNOT_DISABLE_2FA_FOR_ADMIN',
          message:
            'PLATFORM_ADMIN role cannot disable 2FA per BR-AUTH-005. Contact security@kitehub.me for emergency access.',
        },
        { status: 403 }
      );
    }

    if (!body.current_totp_code || body.current_totp_code === '000000') {
      return HttpResponse.json(
        { error: 'INVALID_TOTP', message: 'TOTP code does not match.' },
        { status: 401 }
      );
    }

    if (!body.password_reconfirm || body.password_reconfirm === 'wrong') {
      return HttpResponse.json(
        { error: 'INVALID_PASSWORD', message: 'Password reconfirm does not match.' },
        { status: 401 }
      );
    }

    return HttpResponse.json({
      disabled: true,
      disabled_at: '2026-05-14T10:30:00Z',
    });
  }),

  // ---------------------------------------------------------------
  // POST /api/auth/login — STUB demonstrating 3 response shapes Bucket B FE must handle.
  // Note: production login endpoint lives elsewhere (kitehub-subscription auth controller).
  // This stub exists so Bucket B FE wizard tests can exercise the 2FA branching paths.
  // ---------------------------------------------------------------
  http.post('*/api/auth/login', async ({ request }) => {
    let body: LoginPayload;
    try {
      body = (await request.json()) as LoginPayload;
    } catch {
      return HttpResponse.json(
        { error: 'INVALID_REQUEST', message: 'Malformed JSON.' },
        { status: 400 }
      );
    }

    // Stub email matchers control which response shape is returned.
    //   - locked@example.com → 423 ACCOUNT_LOCKED (per BR-AUTH-002)
    //   - admin@example.com → requires2fa: true (existing enrolled admin)
    //   - admin-first@example.com → requires2fa_enrollment: true (first-time PLATFORM_ADMIN)
    //   - any other valid email → success-no-2fa
    if (!body.email || !body.password) {
      return HttpResponse.json(
        { error: 'INVALID_CREDENTIALS', message: 'Email and password required.' },
        { status: 401 }
      );
    }

    if (body.email === 'locked@example.com') {
      return new HttpResponse(
        JSON.stringify({
          error: 'ACCOUNT_LOCKED',
          lockedUntil: '2026-05-14T10:38:00Z',
          attemptsRemaining: 0,
        }),
        {
          status: 423,
          headers: {
            'Content-Type': 'application/json',
            'Retry-After': '900',
          },
        }
      );
    }

    if (body.email === 'admin@example.com') {
      return HttpResponse.json({
        requires2fa: true,
        challenge_token: 'stub-challenge-token-2fa-required',
      });
    }

    if (body.email === 'admin-first@example.com') {
      return HttpResponse.json({
        requires2fa_enrollment: true,
        challenge_token: 'stub-challenge-token-enrollment-required',
      });
    }

    if (body.password === 'wrong') {
      return HttpResponse.json(
        { error: 'INVALID_CREDENTIALS', message: 'Email or password incorrect.' },
        { status: 401 }
      );
    }

    // Success — no 2FA enrolled
    return HttpResponse.json({
      access_token: 'stub-access-token-no-2fa',
      refresh_token: 'stub-refresh-token-no-2fa',
      user: {
        id: 'user-1',
        email: body.email,
        role: 'OWNER',
      },
    });
  }),
];
