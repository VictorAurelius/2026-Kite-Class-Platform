/**
 * ChildCard smoke test — initials + meta + link to transcript.
 *
 * Wave 49 Bucket A (GAP-267).
 */

import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ChildCard } from '../child-card';

describe('ChildCard', () => {
  it('links to transcript page (Wave 18b1 logic preserved)', () => {
    render(
      <ChildCard
        child={{
          studentId: 42,
          studentName: 'Lê Minh Tuấn',
          className: '10A2',
          grade: 'Lớp 10',
          linkType: 'PRIMARY',
        }}
      />,
    );

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/parent/transcript/42');
    expect(link).toHaveTextContent('Con Lê Minh Tuấn');
    expect(link).toHaveTextContent('Lớp 10 · 10A2');
  });

  it('produces VN-style 2-letter initials from full name', () => {
    render(
      <ChildCard
        child={{ studentId: 1, studentName: 'Nguyễn Văn An' }}
      />,
    );
    // First-letter-of-family + first-letter-of-given = N + A
    expect(screen.getByText('NA')).toBeInTheDocument();
  });
});
