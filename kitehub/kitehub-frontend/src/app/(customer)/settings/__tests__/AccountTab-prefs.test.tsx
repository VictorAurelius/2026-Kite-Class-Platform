/**
 * Wave 31 Bucket D — Settings AccountTab notification + locale tests.
 *
 * Verifies the new "Thông báo & Ngôn ngữ" section ports KH pro v2 spec
 * onto production:
 *  - Section renders.
 *  - All 3 notification toggles + locale select wired to state.
 *  - Save button is reachable without errors (client-side persistence
 *    mock — backend prefs endpoint not yet wired).
 */

import { describe, it, expect } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { render } from '@/test/test-utils';
import { AccountTab } from '../components/AccountTab';

const baseUser = {
  id: 'user-1',
  email: 'owner@kite.test',
  name: 'Nguyễn Văn An',
  role: 'OWNER' as const,
};

describe('AccountTab — notification & locale prefs', () => {
  it('renders the notification & locale section', () => {
    render(<AccountTab user={baseUser} organizationName="Kite Test Center" />);

    expect(screen.getByTestId('prefs-section')).toBeInTheDocument();
    expect(screen.getByText('Thông báo & Ngôn ngữ')).toBeInTheDocument();
    expect(screen.getByLabelText('Email thông báo chung')).toBeInTheDocument();
    expect(screen.getByLabelText('Nhắc hết hạn trial')).toBeInTheDocument();
    expect(screen.getByLabelText('Tin tức sản phẩm')).toBeInTheDocument();
  });

  it('default toggles match KH pro v2 spec (email + trial reminders ON; product updates OFF)', () => {
    render(<AccountTab user={baseUser} />);

    const emailSwitch = screen.getByLabelText('Email thông báo chung') as HTMLButtonElement;
    const trialSwitch = screen.getByLabelText('Nhắc hết hạn trial') as HTMLButtonElement;
    const productSwitch = screen.getByLabelText('Tin tức sản phẩm') as HTMLButtonElement;

    expect(emailSwitch.getAttribute('data-state')).toBe('checked');
    expect(trialSwitch.getAttribute('data-state')).toBe('checked');
    expect(productSwitch.getAttribute('data-state')).toBe('unchecked');
  });

  it('toggling a switch updates its visible state', () => {
    render(<AccountTab user={baseUser} />);

    const productSwitch = screen.getByLabelText('Tin tức sản phẩm') as HTMLButtonElement;
    expect(productSwitch.getAttribute('data-state')).toBe('unchecked');

    fireEvent.click(productSwitch);

    expect(productSwitch.getAttribute('data-state')).toBe('checked');
  });

  it('locale select is rendered and defaults to Vietnamese', () => {
    render(<AccountTab user={baseUser} />);

    const localeTrigger = screen.getByTestId('locale-select');
    expect(localeTrigger).toBeInTheDocument();
    expect(localeTrigger).toHaveTextContent('Tiếng Việt');
  });

  it('save button is rendered and clickable', () => {
    render(<AccountTab user={baseUser} />);

    const save = screen.getByTestId('prefs-save');
    expect(save).toBeInTheDocument();
    expect(save).not.toBeDisabled();
    fireEvent.click(save); // should not throw — handler is async + safe
  });
});
