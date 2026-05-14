/**
 * Tests for BetaDisclaimerBanner (Wave 78 GAP-539).
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, act } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { BetaDisclaimerBanner, BETA_DISCLAIMER_COOKIE } from '../BetaDisclaimerBanner';

function clearAllCookies() {
  if (typeof document === 'undefined') return;
  const cookies = document.cookie.split(';');
  for (const c of cookies) {
    const eq = c.indexOf('=');
    const name = (eq > -1 ? c.substring(0, eq) : c).trim();
    if (name) {
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
    }
  }
}

describe('BetaDisclaimerBanner', () => {
  beforeEach(() => {
    clearAllCookies();
  });

  it('renders dismissible banner with Vietnamese disclaimer text', () => {
    render(<BetaDisclaimerBanner />);
    expect(screen.getByTestId('beta-disclaimer-banner')).toBeInTheDocument();
    expect(screen.getByText(/đang trong giai đoạn Beta/i)).toBeInTheDocument();
    expect(screen.getByText(/support@kitehub.me/)).toBeInTheDocument();
  });

  it('shows link to /beta-status page', () => {
    render(<BetaDisclaimerBanner />);
    const link = screen.getByRole('link', { name: /trạng thái beta/i });
    expect(link).toHaveAttribute('href', '/beta-status');
  });

  it('writes dismissal cookie + hides banner when X button clicked', async () => {
    const user = userEvent.setup();
    render(<BetaDisclaimerBanner />);

    const closeBtn = screen.getByRole('button', { name: /đóng/i });
    await act(async () => {
      await user.click(closeBtn);
    });

    expect(screen.queryByTestId('beta-disclaimer-banner')).not.toBeInTheDocument();
    expect(document.cookie).toContain(`${BETA_DISCLAIMER_COOKIE}=1`);
  });

  it('stays hidden on re-mount when cookie already set', () => {
    document.cookie = `${BETA_DISCLAIMER_COOKIE}=1; path=/`;
    render(<BetaDisclaimerBanner />);
    expect(screen.queryByTestId('beta-disclaimer-banner')).not.toBeInTheDocument();
  });

  it('forceShow=true overrides cookie dismissal', () => {
    document.cookie = `${BETA_DISCLAIMER_COOKIE}=1; path=/`;
    render(<BetaDisclaimerBanner forceShow />);
    expect(screen.getByTestId('beta-disclaimer-banner')).toBeInTheDocument();
  });
});
