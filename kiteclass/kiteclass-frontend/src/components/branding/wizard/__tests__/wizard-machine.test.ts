import { describe, expect, it } from 'vitest';
import { initialState, reducer } from '../wizard-machine';
import { DEFAULT_BRAND_INPUTS } from '../types';

describe('wizard-machine', () => {
  it('starts at welcome step with empty inputs', () => {
    const s = initialState('PRO');
    expect(s.name).toBe('welcome');
    expect(s.context.inputs.audiences).toEqual([]);
    expect(s.context.regenerateLimit).toBe(10);
  });

  it('blocks NEXT from welcome until segment chosen', () => {
    let s = initialState('FREE');

    s = reducer(s, { type: 'NEXT' });

    expect(s.name).toBe('welcome'); // still stuck

    s = reducer(s, { type: 'SET_SEGMENT', segment: 'K12' });
    s = reducer(s, { type: 'NEXT' });
    expect(s.name).toBe('logo');
  });

  it('walks through all 6 steps when minimum inputs met', () => {
    let s = initialState('FREE');
    s = reducer(s, { type: 'SET_SEGMENT', segment: 'K12' });
    s = reducer(s, { type: 'NEXT' });             // logo
    s = reducer(s, { type: 'NEXT' });             // audience (logo is always passable)
    // audience requires selection
    s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'students' });
    s = reducer(s, { type: 'NEXT' });             // tone
    s = reducer(s, { type: 'SET_TONE', tone: 'friendly' });
    s = reducer(s, { type: 'NEXT' });             // template
    s = reducer(s, { type: 'SET_TEMPLATE', templateId: 'k12-warm-v1' });
    s = reducer(s, { type: 'NEXT' });             // preview
    expect(s.name).toBe('preview');

    s = reducer(s, { type: 'NEXT' });             // submit
    expect(s.name).toBe('submitting');
  });

  it('BACK goes to previous step', () => {
    let s = initialState('FREE');
    s = reducer(s, { type: 'SET_SEGMENT', segment: 'K12' });
    s = reducer(s, { type: 'NEXT' }); // logo
    s = reducer(s, { type: 'BACK' });
    expect(s.name).toBe('welcome');
  });

  it('BACK from welcome is a no-op', () => {
    const s = initialState('FREE');
    const next = reducer(s, { type: 'BACK' });
    expect(next.name).toBe('welcome');
  });

  it('TOGGLE_AUDIENCE flips membership', () => {
    let s = initialState('PRO');
    s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'students' });
    s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'parents' });
    expect(s.context.inputs.audiences).toEqual(['students', 'parents']);

    s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'students' });
    expect(s.context.inputs.audiences).toEqual(['parents']);
  });

  it('SUBMIT from preview transitions to submitting', () => {
    let s = initialState('PRO');
    // jump to preview via raw dispatch (test helper)
    s = reducer(s, { type: 'SET_SEGMENT', segment: 'K12' });
    s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'students' });
    s = reducer(s, { type: 'SET_TONE', tone: 'friendly' });
    s = reducer(s, { type: 'SET_TEMPLATE', templateId: 'x' });
    s = { name: 'preview', context: s.context };

    s = reducer(s, { type: 'SUBMIT' });
    expect(s.name).toBe('submitting');
  });

  it('SUBMIT_OK → done with instanceId', () => {
    const submitting = {
      name: 'submitting' as const,
      context: initialState('PRO').context,
    };
    const next = reducer(submitting, { type: 'SUBMIT_OK', instanceId: 42 });
    expect(next.name).toBe('done');
    expect(next.context.instanceId).toBe(42);
  });

  it('SUBMIT_FAIL → error with message', () => {
    const submitting = {
      name: 'submitting' as const,
      context: initialState('PRO').context,
    };
    const next = reducer(submitting, { type: 'SUBMIT_FAIL', message: 'oops' });
    expect(next.name).toBe('error');
    expect(next.context.errorMessage).toBe('oops');
  });

  it('REGENERATE from preview consumes quota', () => {
    let s = initialState('FREE');
    s = { name: 'preview', context: s.context };

    s = reducer(s, { type: 'REGENERATE' });

    expect(s.name).toBe('submitting');
    expect(s.context.regenerateCount).toBe(1);
  });

  it('REGENERATE blocked when quota exhausted', () => {
    let s = initialState('FREE');
    s = {
      name: 'preview',
      context: { ...s.context, regenerateCount: 3 },
    };
    const next = reducer(s, { type: 'REGENERATE' });
    expect(next.name).toBe('preview'); // no-op
    expect(next.context.regenerateCount).toBe(3);
  });

  it('ENTERPRISE tier has unlimited regenerate', () => {
    let s = initialState('ENTERPRISE');
    s = { name: 'preview', context: { ...s.context, regenerateCount: 999 } };
    const next = reducer(s, { type: 'REGENERATE' });
    expect(next.name).toBe('submitting');
    expect(next.context.regenerateCount).toBe(1000);
  });

  it('error state allows retry via SUBMIT', () => {
    const err = {
      name: 'error' as const,
      context: { ...initialState('PRO').context, errorMessage: 'x' },
    };
    const next = reducer(err, { type: 'SUBMIT' });
    expect(next.name).toBe('submitting');
    expect(next.context.errorMessage).toBeUndefined();
  });

  // ─── GAP-287: USE_DEFAULTS escape ramp ───────────────────────────────
  describe('USE_DEFAULTS — Sử dụng mặc định escape ramp (GAP-287)', () => {
    it('USE_DEFAULTS from logo (step 2) → submitting với defaults', () => {
      let s = initialState('FREE');
      s = reducer(s, { type: 'SET_SEGMENT', segment: 'K12' });
      s = reducer(s, { type: 'NEXT' }); // logo

      s = reducer(s, { type: 'USE_DEFAULTS' });

      expect(s.name).toBe('submitting');
      // segment user đã chọn được giữ lại
      expect(s.context.inputs.segment).toBe('K12');
      // unset fields được fill bằng defaults
      expect(s.context.inputs.audiences).toEqual(DEFAULT_BRAND_INPUTS.audiences);
      expect(s.context.inputs.tone).toBe(DEFAULT_BRAND_INPUTS.tone);
      expect(s.context.inputs.templateId).toBe(DEFAULT_BRAND_INPUTS.templateId);
    });

    it('USE_DEFAULTS from audience (step 3) → submitting với user audiences preserved', () => {
      let s = initialState('FREE');
      s = reducer(s, { type: 'SET_SEGMENT', segment: 'CENTER' });
      s = reducer(s, { type: 'NEXT' }); // logo
      s = reducer(s, { type: 'NEXT' }); // audience
      s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'parents' });

      s = reducer(s, { type: 'USE_DEFAULTS' });

      expect(s.name).toBe('submitting');
      expect(s.context.inputs.segment).toBe('CENTER');
      // User-provided audience giữ nguyên (KHÔNG bị override bằng defaults)
      expect(s.context.inputs.audiences).toEqual(['parents']);
      // Unset tone + templateId được fill defaults
      expect(s.context.inputs.tone).toBe(DEFAULT_BRAND_INPUTS.tone);
      expect(s.context.inputs.templateId).toBe(DEFAULT_BRAND_INPUTS.templateId);
    });

    it('USE_DEFAULTS from tone (step 4) → submitting; tone user-chose preserved', () => {
      let s = initialState('PRO');
      s = reducer(s, { type: 'SET_SEGMENT', segment: 'UNIV' });
      s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'teachers' });
      s = reducer(s, { type: 'SET_TONE', tone: 'academic' });

      s = reducer(s, { type: 'USE_DEFAULTS' });

      expect(s.name).toBe('submitting');
      expect(s.context.inputs.tone).toBe('academic'); // user chose → preserved
      expect(s.context.inputs.templateId).toBe(DEFAULT_BRAND_INPUTS.templateId); // unset → default
    });

    it('USE_DEFAULTS from template (step 5) → submitting', () => {
      let s = initialState('FREE');
      s = reducer(s, { type: 'SET_SEGMENT', segment: 'K12' });
      s = reducer(s, { type: 'TOGGLE_AUDIENCE', audience: 'students' });
      s = reducer(s, { type: 'SET_TONE', tone: 'friendly' });

      s = reducer(s, { type: 'USE_DEFAULTS' });

      expect(s.name).toBe('submitting');
      expect(s.context.inputs.templateId).toBe(DEFAULT_BRAND_INPUTS.templateId);
    });

    it('USE_DEFAULTS clears errorMessage (skip after retry path)', () => {
      let s = initialState('FREE');
      s.context.errorMessage = 'previous failure';

      s = reducer(s, { type: 'USE_DEFAULTS' });

      expect(s.context.errorMessage).toBeUndefined();
    });

    it('USE_DEFAULTS fills segment default khi user chưa pick (defensive)', () => {
      const s = initialState('FREE');
      // Note: button hidden trên welcome trong UI nhưng reducer phải an toàn
      const next = reducer(s, { type: 'USE_DEFAULTS' });
      expect(next.name).toBe('submitting');
      expect(next.context.inputs.segment).toBe(DEFAULT_BRAND_INPUTS.segment);
    });
  });

  it('RESET returns to initial state keeping tier', () => {
    let s = initialState('PREMIUM');
    s = reducer(s, { type: 'SET_SEGMENT', segment: 'UNIV' });
    s = reducer(s, { type: 'SET_TONE', tone: 'luxurious' });

    s = reducer(s, { type: 'RESET' });

    expect(s.name).toBe('welcome');
    expect(s.context.tier).toBe('PREMIUM');
    expect(s.context.inputs.segment).toBeUndefined();
  });
});
