/**
 * Tests for {@link OfflineSyncStatusBadge} — the small visual indicator that
 * tells the GVCN how many attendance batches are pending / failed offline.
 *
 * @since 4.x.x (Wave 18b3 Bucket A)
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OfflineSyncStatusBadge } from '../../../lib/offline/sync-status-badge';

describe('OfflineSyncStatusBadge', () => {
  it('renders nothing when there is no offline activity', () => {
    const { container } = render(
      <OfflineSyncStatusBadge pending={0} failed={0} synced={0} />,
    );
    // Component returns null in the no-activity case.
    expect(container.firstChild).toBeNull();
  });

  it('shows pending count with a "đang chờ" label when pending>0', () => {
    render(<OfflineSyncStatusBadge pending={3} failed={0} synced={0} />);
    expect(screen.getByText(/3 đang chờ/i)).toBeInTheDocument();
  });

  it('shows failed badge when failed>0', () => {
    render(<OfflineSyncStatusBadge pending={0} failed={2} synced={5} />);
    expect(screen.getByText(/2 thất bại/i)).toBeInTheDocument();
  });
});
