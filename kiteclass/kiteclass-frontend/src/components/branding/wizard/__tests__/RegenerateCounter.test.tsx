import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RegenerateCounter } from '../RegenerateCounter';

describe('RegenerateCounter', () => {
  it('shows remaining count for finite tier', () => {
    render(<RegenerateCounter tier="FREE" used={1} limit={3} />);
    expect(screen.getByText(/2/)).toBeInTheDocument();
    expect(screen.getByText(/FREE/)).toBeInTheDocument();
  });

  it('shows unlimited for enterprise', () => {
    render(
      <RegenerateCounter tier="ENTERPRISE" used={999} limit={Number.POSITIVE_INFINITY} />,
    );
    expect(screen.getByText(/không giới hạn/i)).toBeInTheDocument();
  });

  it('uses destructive styling when quota exhausted', () => {
    const { container } = render(
      <RegenerateCounter tier="FREE" used={3} limit={3} />,
    );
    expect(container.firstChild).toHaveClass('text-destructive');
  });
});
