/**
 * Smoke test for RegisterPage (GAP-372 Wave 45 closure follow-up #2).
 *
 * Verifies that /register redirects to /request-beta-access during
 * Phase 1 BETA invite-only mode.
 */
import { describe, it, expect, vi } from 'vitest';
import { redirect } from 'next/navigation';
import RegisterPage from '../page';

vi.mock('next/navigation', () => ({
  redirect: vi.fn(() => {
    // next/navigation's redirect throws internally to halt rendering;
    // mock simply records the call.
  }),
}));

describe('RegisterPage', () => {
  it('redirects to /request-beta-access (Phase 1 BETA invite-only)', () => {
    RegisterPage();
    expect(redirect).toHaveBeenCalledWith('/request-beta-access');
  });
});
