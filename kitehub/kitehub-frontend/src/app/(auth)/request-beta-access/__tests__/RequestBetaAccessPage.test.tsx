/**
 * Smoke test for RequestBetaAccessPage (GAP-372 Wave 33).
 *
 * Wave beta-prep-1 Bucket F+G — BetaRequestForm now uses useRouter() for
 * multi-branch redirect (per ADR-036). Page test mocks router để satisfy
 * app router invariant khi render BetaRequestForm.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/test-utils';
import RequestBetaAccessPage from '../page';

vi.mock('@/lib/api/client', () => ({
  default: { post: vi.fn() },
}));

// BetaRequestForm calls useRouter() at module render. Mock to avoid
// "invariant expected app router to be mounted" error in vitest jsdom.
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  }),
}));

describe('RequestBetaAccessPage', () => {
  it('renders the heading + form + login link', () => {
    render(<RequestBetaAccessPage />);
    expect(screen.getByRole('heading', { name: /Đăng ký dùng thử Beta/i })).toBeInTheDocument();
    expect(screen.getByRole('form', { name: /beta-request-form/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Đăng nhập/i })).toBeInTheDocument();
  });
});
