import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import {
  wizardReducer,
  INITIAL_WIZARD_STATE,
  type WizardState,
} from '../wizard-shared';
import { formatVnd, buildLandingFactsPayload } from '../facts-landing';
import { BrandPersonalityStep } from '../BrandPersonalityStep';

// ---------------------------------------------------------------------------
// GAP-1234 — Step-2 landing facts: reducer, VND helper, payload builder, UI.
// ---------------------------------------------------------------------------

describe('facts reducer (GAP-1234)', () => {
  it('INITIAL_WIZARD_STATE has empty facts', () => {
    expect(INITIAL_WIZARD_STATE.facts).toEqual({
      address: '',
      contactPhone: '',
      contactEmail: '',
      zaloUrl: '',
      tuitions: [],
    });
  });

  it('SET_FACT updates a scalar fact key without touching others', () => {
    const next = wizardReducer(INITIAL_WIZARD_STATE, {
      type: 'SET_FACT',
      key: 'contactPhone',
      value: '0901234567',
    });
    expect(next.facts.contactPhone).toBe('0901234567');
    expect(next.facts.address).toBe('');
    // immutability — original untouched
    expect(INITIAL_WIZARD_STATE.facts.contactPhone).toBe('');
  });

  it('ADD_TUITION appends an empty row with the supplied id', () => {
    const next = wizardReducer(INITIAL_WIZARD_STATE, { type: 'ADD_TUITION', id: 't1' });
    expect(next.facts.tuitions).toEqual([{ id: 't1', name: '', price: '' }]);
  });

  it('SET_TUITION edits the matching row field only', () => {
    let state = wizardReducer(INITIAL_WIZARD_STATE, { type: 'ADD_TUITION', id: 't1' });
    state = wizardReducer(state, { type: 'ADD_TUITION', id: 't2' });
    state = wizardReducer(state, { type: 'SET_TUITION', id: 't1', field: 'name', value: 'IELTS' });
    state = wizardReducer(state, { type: 'SET_TUITION', id: 't1', field: 'price', value: '1500000' });
    expect(state.facts.tuitions).toEqual([
      { id: 't1', name: 'IELTS', price: '1500000' },
      { id: 't2', name: '', price: '' },
    ]);
  });

  it('REMOVE_TUITION drops the matching row', () => {
    let state = wizardReducer(INITIAL_WIZARD_STATE, { type: 'ADD_TUITION', id: 't1' });
    state = wizardReducer(state, { type: 'ADD_TUITION', id: 't2' });
    state = wizardReducer(state, { type: 'REMOVE_TUITION', id: 't1' });
    expect(state.facts.tuitions.map((t) => t.id)).toEqual(['t2']);
  });
});

describe('formatVnd (vn-localization §1)', () => {
  it('formats raw digits as VN currency with dot separators + đ', () => {
    expect(formatVnd('1500000')).toBe('1.500.000đ');
    expect(formatVnd('850000')).toBe('850.000đ');
    expect(formatVnd('1000')).toBe('1.000đ');
  });

  it('strips non-digits before formatting', () => {
    expect(formatVnd('1.500.000')).toBe('1.500.000đ');
    expect(formatVnd('1,500,000 vnd')).toBe('1.500.000đ');
  });

  it('returns empty string for empty / non-numeric input', () => {
    expect(formatVnd('')).toBe('');
    expect(formatVnd('abc')).toBe('');
  });
});

describe('buildLandingFactsPayload (facts → UpdateLandingPageRequest)', () => {
  const empty = INITIAL_WIZARD_STATE.facts;

  it('returns null when nothing entered', () => {
    expect(buildLandingFactsPayload(empty)).toBeNull();
  });

  it('includes only non-empty contact fields (partial PATCH)', () => {
    const payload = buildLandingFactsPayload({
      ...empty,
      address: '  123 Lê Lợi, Q.1  ',
      contactPhone: '0901234567',
    });
    expect(payload).toEqual({
      address: '123 Lê Lợi, Q.1',
      contactPhone: '0901234567',
    });
    // omitted fields are absent (not null/empty)
    expect(payload).not.toHaveProperty('contactEmail');
    expect(payload).not.toHaveProperty('pricingTiers');
  });

  it('maps complete tuition rows to pricingTiers with formatted VND price', () => {
    const payload = buildLandingFactsPayload({
      ...empty,
      contactEmail: 'lienhe@trungtam.vn',
      zaloUrl: 'zalo.me/0901234567',
      tuitions: [
        { id: 't1', name: 'IELTS 6.5', price: '1500000' },
        { id: 't2', name: 'Giao tiếp', price: '850000' },
      ],
    });
    expect(payload).toEqual({
      contactEmail: 'lienhe@trungtam.vn',
      zaloUrl: 'zalo.me/0901234567',
      pricingTiers: [
        { name: 'IELTS 6.5', price: '1.500.000đ', period: '/tháng' },
        { name: 'Giao tiếp', price: '850.000đ', period: '/tháng' },
      ],
    });
  });

  it('drops tuition rows missing a name or price', () => {
    const payload = buildLandingFactsPayload({
      ...empty,
      tuitions: [
        { id: 't1', name: 'IELTS', price: '' }, // no price → dropped
        { id: 't2', name: '', price: '500000' }, // no name → dropped
      ],
    });
    expect(payload).toBeNull();
  });
});

describe('BrandPersonalityStep facts disclosure (GAP-1234)', () => {
  function renderStep(facts = INITIAL_WIZARD_STATE.facts) {
    const dispatch = vi.fn();
    const state: WizardState = { ...INITIAL_WIZARD_STATE, facts };
    render(
      <BrandPersonalityStep
        wizardState={state}
        dispatch={dispatch}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    return { dispatch };
  }

  it('facts panel is collapsed by default and expands on toggle', () => {
    renderStep();
    expect(screen.getByTestId('facts-toggle')).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByTestId('facts-panel')).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId('facts-toggle'));
    expect(screen.getByTestId('facts-toggle')).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByTestId('facts-panel')).toBeInTheDocument();
  });

  it('typing the address dispatches SET_FACT', () => {
    const { dispatch } = renderStep();
    fireEvent.click(screen.getByTestId('facts-toggle'));
    fireEvent.change(screen.getByLabelText('Địa chỉ trung tâm'), {
      target: { value: '45 Hai Bà Trưng' },
    });
    expect(dispatch).toHaveBeenCalledWith({
      type: 'SET_FACT',
      key: 'address',
      value: '45 Hai Bà Trưng',
    });
  });

  it('"Thêm lớp / mức phí" dispatches ADD_TUITION with an id', () => {
    const { dispatch } = renderStep();
    fireEvent.click(screen.getByTestId('facts-toggle'));
    fireEvent.click(screen.getByTestId('facts-tuition-add'));
    expect(dispatch).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'ADD_TUITION', id: expect.any(String) }),
    );
  });

  it('renders a VND-formatted price preview for an entered tuition row', () => {
    renderStep({
      ...INITIAL_WIZARD_STATE.facts,
      tuitions: [{ id: 't1', name: 'IELTS', price: '1500000' }],
    });
    fireEvent.click(screen.getByTestId('facts-toggle'));
    expect(screen.getByTestId('facts-tuition-price-preview')).toHaveTextContent('1.500.000đ/tháng');
  });
});
