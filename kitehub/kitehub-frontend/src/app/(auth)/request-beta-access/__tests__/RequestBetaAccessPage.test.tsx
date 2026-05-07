/**
 * Smoke test for RequestBetaAccessPage (GAP-372 Wave 33).
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/test-utils';
import RequestBetaAccessPage from '../page';

vi.mock('@/lib/api/client', () => ({
  default: { post: vi.fn() },
}));

describe('RequestBetaAccessPage', () => {
  it('renders the heading + form + login link', () => {
    render(<RequestBetaAccessPage />);
    expect(screen.getByRole('heading', { name: /Đăng ký dùng thử Beta/i })).toBeInTheDocument();
    expect(screen.getByRole('form', { name: /beta-request-form/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Đăng nhập/i })).toBeInTheDocument();
  });
});
