/**
 * Smoke test for kiteclass-frontend RegisterPage (GAP-372 Wave 45 closure follow-up #2).
 *
 * Verifies the trung tâm card now points to the beta access request page
 * (Phase 1 BETA invite-only) instead of public self-signup.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import RegisterPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/register',
}));

describe('RegisterPage', () => {
  it('renders student + center options with beta access request link for centers', () => {
    render(<RegisterPage />);

    // Student path preserved
    expect(screen.getByRole('heading', { name: /Tạo tài khoản/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Đăng ký học viên/i })).toBeInTheDocument();

    // Center card now shows Beta language + points to request-beta-access
    expect(screen.getByRole('button', { name: /Yêu cầu Beta/i })).toBeInTheDocument();
    expect(screen.getByText(/gửi yêu cầu Beta/i)).toBeInTheDocument();
  });

  it('opens kitehub /auth/request-beta-access (not public /register) on center button click', () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);
    render(<RegisterPage />);

    const button = screen.getByRole('button', { name: /Yêu cầu Beta/i });
    button.click();

    expect(openSpy).toHaveBeenCalled();
    const calledUrl = openSpy.mock.calls[0]?.[0] as string;
    expect(calledUrl).toContain('/auth/request-beta-access');
    expect(calledUrl).not.toContain('/register');

    openSpy.mockRestore();
  });
});
