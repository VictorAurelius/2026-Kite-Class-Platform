/**
 * StatusBadge Component Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { StatusBadge } from '../status-badge';

describe('StatusBadge', () => {
  it('should render Vietnamese label for known status', () => {
    render(<StatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText('Đang diễn ra')).toBeInTheDocument();
  });

  it('should auto-detect success variant for active/published/completed statuses', () => {
    const { container: container1 } = render(<StatusBadge status="ACTIVE" />);
    expect(container1.querySelector('.bg-green-100')).toBeInTheDocument();

    const { container: container2 } = render(<StatusBadge status="PUBLISHED" />);
    expect(container2.querySelector('.bg-green-100')).toBeInTheDocument();

    const { container: container3 } = render(<StatusBadge status="COMPLETED" />);
    expect(container3.querySelector('.bg-green-100')).toBeInTheDocument();
  });

  it('should auto-detect warning variant for pending/draft statuses', () => {
    const { container: container1 } = render(<StatusBadge status="PENDING" />);
    expect(container1.querySelector('.bg-yellow-100')).toBeInTheDocument();

    const { container: container2 } = render(<StatusBadge status="DRAFT" />);
    expect(container2.querySelector('.bg-yellow-100')).toBeInTheDocument();
  });

  it('should auto-detect error variant for cancelled/failed statuses', () => {
    const { container: container1 } = render(<StatusBadge status="CANCELLED" />);
    expect(container1.querySelector('.bg-red-100')).toBeInTheDocument();

    const { container: container2 } = render(<StatusBadge status="FAILED" />);
    expect(container2.querySelector('.bg-red-100')).toBeInTheDocument();
  });

  it('should auto-detect info variant for archived statuses', () => {
    const { container } = render(<StatusBadge status="ARCHIVED" />);
    expect(container.querySelector('.bg-blue-100')).toBeInTheDocument();
  });

  it('should use explicit variant when provided', () => {
    const { container } = render(<StatusBadge status="CUSTOM_STATUS" variant="error" />);
    expect(container.querySelector('.bg-red-100')).toBeInTheDocument();
  });

  it('should apply custom className', () => {
    const { container } = render(<StatusBadge status="TEST" className="custom-class" />);
    expect(container.querySelector('.custom-class')).toBeInTheDocument();
  });

  it('should use default variant for unrecognized statuses', () => {
    const { container } = render(<StatusBadge status="UNKNOWN_STATUS" />);
    expect(container.querySelector('.bg-secondary')).toBeInTheDocument();
  });
});
