/**
 * Component tests for BetaModeBanner (Wave flow-kh3-2, GAP-977).
 */

import { describe, it, expect, afterEach, vi } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { BetaModeBanner } from '../BetaModeBanner';

describe('BetaModeBanner', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('renders the beta notice when NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE=true', () => {
    vi.stubEnv('NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE', 'true');
    render(<BetaModeBanner />);
    expect(screen.getByTestId('beta-mode-banner')).toBeInTheDocument();
    expect(screen.getByText(/10\.000đ tượng trưng/)).toBeInTheDocument();
  });

  it('renders nothing when the flag is not "true"', () => {
    vi.stubEnv('NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE', 'false');
    const { container } = render(<BetaModeBanner />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when the flag is unset', () => {
    vi.stubEnv('NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE', '');
    const { container } = render(<BetaModeBanner />);
    expect(container).toBeEmptyDOMElement();
  });
});
