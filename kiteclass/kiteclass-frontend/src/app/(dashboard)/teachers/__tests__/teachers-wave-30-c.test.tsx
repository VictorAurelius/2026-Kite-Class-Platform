/**
 * Wave 30 Bucket C tests — Phase 4 KC pro v2 port (GAP-266).
 *
 * Validates teacher-list integration with @kite/shared-ui BulkActionsBar.
 *
 * @since 2026-05-06
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, within } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import TeachersPage from '../page';

beforeEach(() => {
  vi.spyOn(console, 'info').mockImplementation(() => {});
});

describe('Wave 30 Bucket C — TeachersPage list', () => {
  it('renders the heading + Add button + does not show BulkActionsBar without selection', async () => {
    render(<TeachersPage />);

    expect(
      screen.getByRole('heading', { name: 'Giáo viên' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /thêm giáo viên/i }),
    ).toHaveAttribute('href', '/teachers/new');
    expect(screen.queryByTestId('bulk-actions-bar-root')).not.toBeInTheDocument();
  });

  it('shows BulkActionsBar with selectedCount=1 after selecting a teacher row', async () => {
    const user = userEvent.setup();
    render(<TeachersPage />);

    // Wait until at least one teacher row renders (mock data populates).
    await waitFor(
      () => {
        const checkboxes = screen.queryAllByRole('checkbox', {
          name: /Chọn giáo viên/i,
        });
        expect(checkboxes.length).toBeGreaterThan(0);
      },
      { timeout: 4000 },
    );

    const checkboxes = screen.getAllByRole('checkbox', {
      name: /Chọn giáo viên/i,
    });
    const firstCheckbox = checkboxes[0];
    expect(firstCheckbox).toBeDefined();
    await user.click(firstCheckbox as HTMLElement);

    const bar = await screen.findByTestId('bulk-actions-bar-root');
    expect(within(bar).getByText('Đã chọn 1')).toBeInTheDocument();
  });
});
