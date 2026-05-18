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
    // Wave 98 B3: support@kitehub.me appears multiple times (main body + PDPL
    // consent rút-đồng-ý mailto). Use getAllByText to assert at least one.
    expect(screen.getAllByText(/support@kitehub.me/).length).toBeGreaterThan(0);
  });

  it('shows link to /beta-status page', () => {
    render(<BetaDisclaimerBanner />);
    const link = screen.getByRole('link', { name: /trạng thái beta/i });
    expect(link).toHaveAttribute('href', '/beta-status');
  });

  // GAP-560 (Wave 79 Bucket D) — specificity check: banner cites 7-day notice + 30-day backup
  // and links to the data-reset policy doc so Owners can verify reset semantics.
  it('cites specific reset cadence + advance notice + backup window', () => {
    render(<BetaDisclaimerBanner />);
    const banner = screen.getByTestId('beta-disclaimer-banner');
    expect(banner).toHaveTextContent(/không tự ý reset/i);
    expect(banner).toHaveTextContent(/7 ngày/i);
    expect(banner).toHaveTextContent(/backup 30 ngày/i);
  });

  it('links to /docs/data-reset-policy for full policy detail (GAP-560)', () => {
    render(<BetaDisclaimerBanner />);
    const policyLink = screen.getByTestId('beta-disclaimer-policy-link');
    expect(policyLink).toBeInTheDocument();
    expect(policyLink).toHaveAttribute('href', '/docs/data-reset-policy');
    expect(policyLink).toHaveTextContent(/Chính sách reset dữ liệu Beta/i);
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

  // Wave 98 Bucket B3 GAP-539 finishing-stroke tests — version chip + PDPL consent.

  it('renders version chip with default app version fallback', () => {
    render(<BetaDisclaimerBanner />);
    const chip = screen.getByTestId('beta-disclaimer-version-chip');
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveTextContent(/v\d+\.\d+\.\d+-beta/i);
  });

  it('honours appVersion prop override for version chip', () => {
    render(<BetaDisclaimerBanner appVersion="v0.9.1-beta" />);
    const chip = screen.getByTestId('beta-disclaimer-version-chip');
    expect(chip).toHaveTextContent('v0.9.1-beta');
  });

  it('renders PDPL consent line citing Luật BVDLCN 2023 Art 9-15', () => {
    render(<BetaDisclaimerBanner />);
    const consent = screen.getByTestId('beta-disclaimer-pdpl-consent');
    expect(consent).toBeInTheDocument();
    expect(consent).toHaveTextContent(/đồng ý với việc xử lý dữ liệu cá nhân/i);
    expect(consent).toHaveTextContent(/Lu(ật|at) B(ả|a)o v(ệ|e) d(ữ|u) li(ệ|e)u c(á|a) nh(â|a)n 2023/i);
    expect(consent).toHaveTextContent(/Điều 9-15|Dieu 9-15/);
  });

  it('PDPL consent line links to /legal/privacy', () => {
    render(<BetaDisclaimerBanner />);
    const privacyLink = screen.getByTestId('beta-disclaimer-privacy-link');
    expect(privacyLink).toBeInTheDocument();
    expect(privacyLink).toHaveAttribute('href', '/legal/privacy');
    expect(privacyLink).toHaveTextContent(/Chính sách Bảo mật/i);
  });
});
