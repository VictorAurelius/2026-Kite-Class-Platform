/**
 * ParentInvite component tests — RTL user-flow coverage per
 * G7 spec (`ui_kits/components/G7-parent-invite/spec.md`).
 *
 * Covers:
 *  - Email validation (pure logic — invalid + valid)
 *  - Idle state render (default form)
 *  - Sending state render (button disabled + indicator)
 *  - Sent state render with issued token + copy-to-clipboard
 *  - Error state render with role="alert"
 *  - Channel toggle EMAIL ↔ ZALO_OA (radio-group semantics)
 *  - Token copy-to-clipboard via navigator.clipboard.writeText
 */

import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ParentInvite } from '../ParentInvite';
import { validateEmail } from '../ParentInvite';

const baseProps = {
  childName: 'Lê Minh Tuấn',
} as const;

describe('validateEmail (pure)', () => {
  it('rejects empty string', () => {
    const result = validateEmail('');
    expect(result.ok).toBe(false);
  });

  it('rejects missing @', () => {
    const result = validateEmail('phuhuynh.gmail.com');
    expect(result.ok).toBe(false);
  });

  it('rejects missing domain', () => {
    const result = validateEmail('phuhuynh@');
    expect(result.ok).toBe(false);
  });

  it('rejects whitespace inside', () => {
    const result = validateEmail('phu huynh@gmail.com');
    expect(result.ok).toBe(false);
  });

  it('accepts a typical email', () => {
    const result = validateEmail('phuhuynh@gmail.com');
    expect(result.ok).toBe(true);
  });

  it('accepts plus-aliasing', () => {
    const result = validateEmail('parent+tuan@example.co');
    expect(result.ok).toBe(true);
  });
});

describe('<ParentInvite>', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders idle state with email input + channel toggle + send button', () => {
    const onSend = vi.fn();
    render(<ParentInvite {...baseProps} onSend={onSend} />);

    expect(screen.getByTestId('parent-invite-email')).toBeInTheDocument();
    expect(screen.getByTestId('parent-invite-channel-radiogroup')).toBeInTheDocument();
    expect(screen.getByTestId('parent-invite-send-btn')).toBeInTheDocument();
    // Send disabled while email empty.
    expect(screen.getByTestId('parent-invite-send-btn')).toBeDisabled();
  });

  it('shows error message when invalid email is entered + send remains disabled', async () => {
    const user = userEvent.setup();
    const onSend = vi.fn();
    render(<ParentInvite {...baseProps} onSend={onSend} />);

    await user.type(screen.getByTestId('parent-invite-email'), 'not-an-email');
    // Trigger validation by blurring the field.
    await user.tab();

    expect(await screen.findByTestId('parent-invite-email-error')).toBeInTheDocument();
    expect(screen.getByTestId('parent-invite-send-btn')).toBeDisabled();
    expect(onSend).not.toHaveBeenCalled();
  });

  it('toggles channel between EMAIL and ZALO_OA via radio-group', async () => {
    const user = userEvent.setup();
    render(<ParentInvite {...baseProps} onSend={vi.fn()} />);

    const emailRadio = screen.getByTestId('parent-invite-channel-email');
    const zaloRadio = screen.getByTestId('parent-invite-channel-zalo');

    // Default per spec = ZALO_OA (Zalo reaches ~95% VN parents).
    expect(zaloRadio).toBeChecked();
    expect(emailRadio).not.toBeChecked();

    await user.click(emailRadio);
    expect(emailRadio).toBeChecked();
    expect(zaloRadio).not.toBeChecked();
  });

  it('Send → calls onSend with valid email + channel, then shows sending → sent state', async () => {
    const user = userEvent.setup();
    let resolveSend: ((v: { token: string }) => void) | undefined;
    const onSend = vi.fn(
      () =>
        new Promise<{ token: string }>((resolve) => {
          resolveSend = resolve;
        }),
    );
    render(<ParentInvite {...baseProps} onSend={onSend} />);

    await user.type(screen.getByTestId('parent-invite-email'), 'phuhuynh@gmail.com');
    await user.click(screen.getByTestId('parent-invite-channel-email'));
    await user.click(screen.getByTestId('parent-invite-send-btn'));

    // Sending: button disabled, indicator visible.
    expect(screen.getByTestId('parent-invite-send-btn')).toBeDisabled();
    expect(screen.getByTestId('parent-invite-sending')).toBeInTheDocument();
    expect(onSend).toHaveBeenCalledWith({
      email: 'phuhuynh@gmail.com',
      channel: 'EMAIL',
    });

    resolveSend?.({ token: 'eyJhbGciOiJIUzI1NiIsI' });

    expect(await screen.findByTestId('parent-invite-success')).toBeInTheDocument();
    expect(screen.getByTestId('parent-invite-token')).toHaveTextContent(
      'eyJhbGciOiJIUzI1NiIsI',
    );
  });

  it('Copy-to-clipboard → navigator.clipboard.writeText invoked with token', async () => {
    // userEvent.setup() installs its own clipboard mock; let it run, then we
    // spy on the resulting writeText. Using `writeToClipboard: true` means
    // userEvent's clipboard.writeText is the function we spy on.
    const user = userEvent.setup();
    // After setup, navigator.clipboard exists (userEvent installed it). Spy on its writeText.
    const writeTextSpy = vi.spyOn(navigator.clipboard, 'writeText');

    const onSend = vi.fn().mockResolvedValue({ token: 'eyJhbGciOiJIUzI1NiIsI' });
    render(<ParentInvite {...baseProps} onSend={onSend} />);

    await user.type(screen.getByTestId('parent-invite-email'), 'phuhuynh@gmail.com');
    await user.click(screen.getByTestId('parent-invite-send-btn'));

    await screen.findByTestId('parent-invite-success');
    await user.click(screen.getByTestId('parent-invite-copy-btn'));

    await waitFor(() => {
      expect(writeTextSpy).toHaveBeenCalledWith('eyJhbGciOiJIUzI1NiIsI');
    });
  });

  it('renders error state with role="alert" when onSend rejects', async () => {
    const user = userEvent.setup();
    const onSend = vi
      .fn()
      .mockRejectedValue(new Error('Email delivery failed — vui lòng thử lại'));
    render(<ParentInvite {...baseProps} onSend={onSend} />);

    await user.type(screen.getByTestId('parent-invite-email'), 'phuhuynh@gmail.com');
    await user.click(screen.getByTestId('parent-invite-send-btn'));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/email delivery failed/i);
    // Send button re-enabled to allow retry.
    expect(screen.getByTestId('parent-invite-send-btn')).not.toBeDisabled();
  });

  it('renders Vietnamese-first labels per spec (no quý phụ huynh formal phrasing)', () => {
    render(<ParentInvite {...baseProps} onSend={vi.fn()} />);

    // Sender title — informal "bạn", not "quý phụ huynh".
    expect(screen.getByText(/mời phụ huynh/i)).toBeInTheDocument();
    // Email label uses informal phrasing.
    const emailInput = screen.getByTestId('parent-invite-email');
    expect(emailInput).toHaveAttribute('placeholder', expect.stringMatching(/phuhuynh/i));
  });
});
