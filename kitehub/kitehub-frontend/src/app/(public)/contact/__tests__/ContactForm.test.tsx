/**
 * Tests for ContactForm (GAP-1101) — KiteHub PLATFORM sales lead form.
 *
 * 4 cases:
 *  1. Renders VN-localized fields
 *  2. Valid submit → POST /api/platform/sales-leads with planInterest → success state
 *  3. Invalid email → Vietnamese error, no POST
 *  4. Missing required (org) → Vietnamese error, no POST
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { ContactForm } from '../ContactForm';

const postMock = vi.fn();
vi.mock('@/lib/api/client', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
  },
}));

describe('ContactForm', () => {
  beforeEach(() => {
    postMock.mockReset();
    postMock.mockResolvedValue({ data: {} });
  });

  it('renders VN-localized fields', () => {
    render(<ContactForm planInterest="ENTERPRISE" />);
    expect(screen.getByLabelText('Họ và tên')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Số điện thoại')).toBeInTheDocument();
    expect(screen.getByLabelText('Tên trung tâm')).toBeInTheDocument();
    expect(screen.getByTestId('contact-submit')).toHaveTextContent('Gửi yêu cầu tư vấn');
  });

  it('submits a valid lead with planInterest and shows success state', async () => {
    const user = userEvent.setup();
    render(<ContactForm planInterest="ENTERPRISE" />);

    await user.type(screen.getByLabelText('Họ và tên'), 'Nguyễn Văn An');
    await user.type(screen.getByLabelText('Email'), 'an.nguyen@skyedu.vn');
    await user.type(screen.getByLabelText('Số điện thoại'), '0901234567');
    await user.type(screen.getByLabelText('Tên trung tâm'), 'Trung tâm Anh ngữ Sky Education');
    await user.click(screen.getByTestId('contact-submit'));

    await waitFor(() => expect(postMock).toHaveBeenCalledTimes(1));
    const [url, body] = postMock.mock.calls[0] as [string, Record<string, unknown>];
    expect(url).toBe('/api/platform/sales-leads');
    expect(body.planInterest).toBe('ENTERPRISE');
    expect(body.email).toBe('an.nguyen@skyedu.vn');
    expect(await screen.findByText('Đã gửi yêu cầu!')).toBeInTheDocument();
  });

  it('rejects invalid email without posting', async () => {
    const user = userEvent.setup();
    render(<ContactForm planInterest="ENTERPRISE" />);

    await user.type(screen.getByLabelText('Họ và tên'), 'Nguyễn Văn An');
    await user.type(screen.getByLabelText('Email'), 'not-an-email');
    await user.type(screen.getByLabelText('Số điện thoại'), '0901234567');
    await user.type(screen.getByLabelText('Tên trung tâm'), 'Trung tâm Sky Education');
    await user.click(screen.getByTestId('contact-submit'));

    expect(await screen.findByText('Email không hợp lệ.')).toBeInTheDocument();
    expect(postMock).not.toHaveBeenCalled();
  });

  it('rejects missing organization name without posting', async () => {
    const user = userEvent.setup();
    render(<ContactForm planInterest="ENTERPRISE" />);

    await user.type(screen.getByLabelText('Họ và tên'), 'Nguyễn Văn An');
    await user.type(screen.getByLabelText('Email'), 'an@skyedu.vn');
    await user.type(screen.getByLabelText('Số điện thoại'), '0901234567');
    await user.click(screen.getByTestId('contact-submit'));

    expect(await screen.findByText('Vui lòng nhập tên trung tâm.')).toBeInTheDocument();
    expect(postMock).not.toHaveBeenCalled();
  });
});
