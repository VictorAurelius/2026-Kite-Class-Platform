/**
 * DoneStep tests (GAP-1108 FE) — terminal success screen + live landing link.
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DoneStep } from '../DoneStep';

describe('DoneStep', () => {
  it('renders the live landing link from frontendUrl', () => {
    render(
      <DoneStep
        tenantName="Trung tâm Anh ngữ Sky"
        frontendUrl="https://sky.kiteclass.vn"
        slug="sky"
        onManage={() => {}}
      />,
    );
    const link = screen.getByTestId('done-step-open-landing') as HTMLAnchorElement;
    expect(link).toBeInTheDocument();
    expect(link.getAttribute('href')).toBe('https://sky.kiteclass.vn');
    expect(link.getAttribute('target')).toBe('_blank');
    expect(screen.getByText('Trung tâm Anh ngữ Sky')).toBeInTheDocument();
  });

  it('falls back to the slug-computed URL when frontendUrl is empty', () => {
    render(
      <DoneStep tenantName="TT" frontendUrl={null} slug="my-center" onManage={() => {}} />,
    );
    const link = screen.getByTestId('done-step-open-landing') as HTMLAnchorElement;
    // G1 walk 2026-06-12: fallback env-driven — local default = KC :3000 ?tenant=
    expect(link.getAttribute('href')).toBe('http://localhost:3000/?tenant=my-center');
  });

  it('shows the no-url placeholder when neither frontendUrl nor slug present', () => {
    render(<DoneStep tenantName="TT" frontendUrl={null} onManage={() => {}} />);
    expect(screen.getByTestId('done-step-no-url')).toBeInTheDocument();
    expect(screen.queryByTestId('done-step-open-landing')).toBeNull();
  });

  it('invokes onManage when the manage button is clicked', () => {
    const onManage = vi.fn();
    render(
      <DoneStep tenantName="TT" frontendUrl="https://x.kiteclass.vn" onManage={onManage} />,
    );
    fireEvent.click(screen.getByTestId('done-step-manage'));
    expect(onManage).toHaveBeenCalledTimes(1);
  });
});
