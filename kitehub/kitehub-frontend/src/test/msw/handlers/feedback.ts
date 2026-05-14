/**
 * MSW handlers for Feedback Submission endpoints (Wave 78 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/kitehub/feedback/api-contract.md`
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Bucket F (GAP-542) FE feedback widget will consume these handlers in unit
 * tests before BE module lands.
 *
 * Endpoints covered:
 *   - POST /api/v1/feedback (in-app feedback widget submission, public)
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 78 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

const ALLOWED_CATEGORIES = new Set(['BUG', 'USABILITY', 'FEATURE_REQUEST', 'GENERAL']);
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_COMMENT_CHARS = 5;
const MAX_COMMENT_CHARS = 2000;

interface FeedbackPayload {
  rating?: number;
  comment?: string;
  email?: string;
  pageUrl?: string;
  category?: string;
  honeypot?: string;
}

export const feedbackHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // POST /api/v1/feedback (public submit)
  // ---------------------------------------------------------------
  http.post('*/api/v1/feedback', async ({ request }) => {
    let body: FeedbackPayload;
    try {
      body = (await request.json()) as FeedbackPayload;
    } catch {
      return HttpResponse.json(
        { error: 'FEEDBACK_INVALID_PAYLOAD', message: 'Malformed JSON' },
        { status: 400 }
      );
    }

    // Honeypot first (silent bot trap — surfaced for tests)
    if (body.honeypot && body.honeypot.length > 0) {
      return HttpResponse.json(
        { error: 'FEEDBACK_HONEYPOT_FILLED', message: 'Honeypot must be empty.' },
        { status: 400 }
      );
    }

    // Rating in [1..5]
    if (
      typeof body.rating !== 'number' ||
      !Number.isInteger(body.rating) ||
      body.rating < 1 ||
      body.rating > 5
    ) {
      return HttpResponse.json(
        {
          error: 'FEEDBACK_INVALID_RATING',
          message: 'rating must be integer in [1..5].',
          field: 'rating',
        },
        { status: 400 }
      );
    }

    // Comment length
    const commentTrimmed = (body.comment ?? '').trim();
    if (commentTrimmed.length < MIN_COMMENT_CHARS || commentTrimmed.length > MAX_COMMENT_CHARS) {
      return HttpResponse.json(
        {
          error: 'FEEDBACK_INVALID_COMMENT',
          message: `comment must be ${MIN_COMMENT_CHARS}-${MAX_COMMENT_CHARS} chars.`,
          field: 'comment',
        },
        { status: 400 }
      );
    }

    // Email optional but if provided must be valid
    if (body.email && !EMAIL_RE.test(body.email)) {
      return HttpResponse.json(
        {
          error: 'FEEDBACK_INVALID_EMAIL',
          message: 'email must be a valid email address.',
          field: 'email',
        },
        { status: 400 }
      );
    }

    // Category optional but if provided must be in enum
    if (body.category && !ALLOWED_CATEGORIES.has(body.category)) {
      return HttpResponse.json(
        {
          error: 'FEEDBACK_INVALID_CATEGORY',
          message: `category must be one of: ${Array.from(ALLOWED_CATEGORIES).join(', ')}`,
          field: 'category',
        },
        { status: 400 }
      );
    }

    // Happy path — minimal response (no echo back of comment/email per contract)
    return HttpResponse.json(
      {
        id: 'feedback-test-uuid',
        rating: body.rating,
        category: body.category ?? 'GENERAL',
        createdAt: '2026-05-14T09:00:00Z',
        status: 'RECEIVED',
      },
      { status: 201 }
    );
  }),
];
