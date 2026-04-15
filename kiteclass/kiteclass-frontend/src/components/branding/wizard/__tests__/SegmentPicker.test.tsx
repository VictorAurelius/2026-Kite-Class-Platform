import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SegmentPicker } from '../SegmentPicker';

describe('SegmentPicker', () => {
  it('renders 5 segments', () => {
    render(<SegmentPicker onChange={() => {}} />);
    expect(screen.getAllByRole('radio')).toHaveLength(5);
  });

  it('fires onChange with segment id on click', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<SegmentPicker onChange={onChange} />);

    await user.click(screen.getByText(/trung tâm giáo dục/i));

    expect(onChange).toHaveBeenCalledWith('CENTER');
  });

  it('marks the chosen option with aria-checked', () => {
    render(<SegmentPicker value="K12" onChange={() => {}} />);
    const chosen = screen.getByRole('radio', { checked: true });
    expect(chosen.textContent?.toLowerCase()).toContain('k-12');
  });
});
