/**
 * MSW handlers for Support Ticket endpoints (Wave 78 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/kitehub/support/api-contract.md`
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Bucket F (GAP-540) FE support form / footer link will consume this handler
 * in unit tests before BE module lands.
 *
 * Endpoints covered:
 *   - POST /api/v1/support-tickets (in-house route MVP, public submit allowed,
 *     email required)
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 78 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

const ALLOWED_CATEGORIES = new Set([
  'AUTH_ISSUE',
  'BILLING',
  'BUG',
  'FEATURE_REQUEST',
  'DATA_ISSUE',
  'OTHER',
]);
const ALLOWED_PRIORITIES = new Set(['LOW', 'NORMAL', 'HIGH', 'URGENT']);
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SUBJECT_MIN = 5;
const SUBJECT_MAX = 200;
const BODY_MIN = 10;
const BODY_MAX = 5000;

interface SupportTicketPayload {
  subject?: string;
  body?: string;
  email?: string;
  category?: string;
  priority?: string;
  honeypot?: string;
}

let ticketCounter = 41;

function nextTicketNumber(): string {
  ticketCounter += 1;
  return `KH-2026-${String(ticketCounter).padStart(5, '0')}`;
}

export const supportHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // POST /api/v1/support-tickets (in-house MVP, email required)
  // ---------------------------------------------------------------
  http.post('*/api/v1/support-tickets', async ({ request }) => {
    let body: SupportTicketPayload;
    try {
      body = (await request.json()) as SupportTicketPayload;
    } catch {
      return HttpResponse.json(
        { error: 'SUPPORT_INVALID_PAYLOAD', message: 'Malformed JSON' },
        { status: 400 }
      );
    }

    if (body.honeypot && body.honeypot.length > 0) {
      return HttpResponse.json(
        { error: 'SUPPORT_HONEYPOT_FILLED', message: 'Honeypot must be empty.' },
        { status: 400 }
      );
    }

    const subjectTrimmed = (body.subject ?? '').trim();
    if (subjectTrimmed.length < SUBJECT_MIN || subjectTrimmed.length > SUBJECT_MAX) {
      return HttpResponse.json(
        {
          error: 'SUPPORT_INVALID_SUBJECT',
          message: `subject must be ${SUBJECT_MIN}-${SUBJECT_MAX} chars.`,
          field: 'subject',
        },
        { status: 400 }
      );
    }

    const bodyTrimmed = (body.body ?? '').trim();
    if (bodyTrimmed.length < BODY_MIN || bodyTrimmed.length > BODY_MAX) {
      return HttpResponse.json(
        {
          error: 'SUPPORT_INVALID_BODY',
          message: `body must be ${BODY_MIN}-${BODY_MAX} chars.`,
          field: 'body',
        },
        { status: 400 }
      );
    }

    if (!body.email || !EMAIL_RE.test(body.email)) {
      return HttpResponse.json(
        {
          error: 'SUPPORT_INVALID_EMAIL',
          message: 'email is required and must be a valid email address.',
          field: 'email',
        },
        { status: 400 }
      );
    }

    if (body.category && !ALLOWED_CATEGORIES.has(body.category)) {
      return HttpResponse.json(
        {
          error: 'SUPPORT_INVALID_CATEGORY',
          message: `category must be one of: ${Array.from(ALLOWED_CATEGORIES).join(', ')}`,
          field: 'category',
        },
        { status: 400 }
      );
    }

    if (body.priority && !ALLOWED_PRIORITIES.has(body.priority)) {
      return HttpResponse.json(
        {
          error: 'SUPPORT_INVALID_PRIORITY',
          message: `priority must be one of: ${Array.from(ALLOWED_PRIORITIES).join(', ')}`,
          field: 'priority',
        },
        { status: 400 }
      );
    }

    // Happy path
    return HttpResponse.json(
      {
        id: 'ticket-test-uuid',
        ticketNumber: nextTicketNumber(),
        subject: subjectTrimmed,
        category: body.category ?? 'OTHER',
        priority: body.priority ?? 'NORMAL',
        status: 'OPEN',
        createdAt: '2026-05-14T09:30:00Z',
      },
      { status: 201 }
    );
  }),
];

/**
 * Test helper — reset ticket counter between specs to make ticket numbers deterministic.
 */
export function resetSupportHandlerState(): void {
  ticketCounter = 41;
}
