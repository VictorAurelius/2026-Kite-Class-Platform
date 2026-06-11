/**
 * Tests for the per-tenant contact form — VN validation + submit wiring (GAP-274 phase-2).
 *
 * @since 2026-06-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor, fireEvent } from '@/test/utils';
import { ContactForm } from '../contact-form';

// NOTE: clicking a submit button does not reliably fire form submit in this jsdom
// setup (see existing `it.skip('[SKIP: jsdom validation timing]')` tests), so the
// form is submitted via fireEvent.submit on the <form> element directly.
function submitForm() {
  const form = document.querySelector('form');
  if (!form) throw new Error('contact form not rendered');
  fireEvent.submit(form);
}

const submitContactForm = vi.fn().mockResolvedValue({ success: true });
vi.mock('@/lib/api/public', () => ({
  publicApi: { submitContactForm: (...args: unknown[]) => submitContactForm(...args) },
}));

describe('ContactForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const nameField = () => screen.getByRole('textbox', { name: /họ và tên/i });
  const phoneField = () => screen.getByRole('textbox', { name: /số điện thoại/i });
  const emailField = () => screen.getByRole('textbox', { name: /^email/i });
  const messageField = () => screen.getByRole('textbox', { name: /lời nhắn/i });
  

  it('shows required errors on empty submit and does not call the API', async () => {
    render(<ContactForm />);
    submitForm();

    const alerts = await screen.findAllByRole('alert');
    expect(alerts.length).toBeGreaterThanOrEqual(3);
    expect(submitContactForm).not.toHaveBeenCalled();
  });

  it('rejects an invalid VN phone number', async () => {
    const user = userEvent.setup();
    render(<ContactForm />);
    await user.type(nameField(), 'Trần Thị Hồng');
    await user.type(phoneField(), '12345'); // not 0 + 10 digits
    await user.type(messageField(), 'Nhờ cô tư vấn lớp cho con ạ.');
    submitForm();

    expect(await screen.findByText(/số điện thoại phải gồm đúng 10 chữ số/i)).toBeInTheDocument();
    expect(submitContactForm).not.toHaveBeenCalled();
  });

  it('submits successfully with email omitted (email optional) and shows success panel', async () => {
    const user = userEvent.setup();
    render(<ContactForm />);
    await user.type(nameField(), 'Trần Thị Hồng');
    await user.type(phoneField(), '0912345678');
    await user.type(messageField(), 'Cháu học lớp 5, nhờ cô tư vấn lớp phù hợp ạ.');
    submitForm();

    await waitFor(() => expect(submitContactForm).toHaveBeenCalledTimes(1));
    expect(submitContactForm).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Trần Thị Hồng', phone: '0912345678', email: undefined })
    );
    expect(await screen.findByText(/đã gửi thành công/i)).toBeInTheDocument();
  });

  it('rejects a malformed email when provided', async () => {
    const user = userEvent.setup();
    render(<ContactForm />);
    await user.type(nameField(), 'Trần Thị Hồng');
    await user.type(phoneField(), '0912345678');
    await user.type(emailField(), 'khong-hop-le');
    await user.type(messageField(), 'Nhờ cô tư vấn lớp cho con ạ.');
    submitForm();

    expect(await screen.findByText(/email không hợp lệ/i)).toBeInTheDocument();
    expect(submitContactForm).not.toHaveBeenCalled();
  });
});
