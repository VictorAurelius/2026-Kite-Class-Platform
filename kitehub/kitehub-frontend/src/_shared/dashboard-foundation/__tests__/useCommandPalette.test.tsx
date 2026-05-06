/**
 * useCommandPalette tests — Wave 31 Bucket A.
 */

import { describe, it, expect } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useCommandPalette } from '../useCommandPalette';

describe('useCommandPalette (KH foundation)', () => {
  it('starts closed and toggles open via toggle()', () => {
    const { result } = renderHook(() => useCommandPalette());
    expect(result.current.open).toBe(false);

    act(() => {
      result.current.toggle();
    });
    expect(result.current.open).toBe(true);
  });

  it('opens via Cmd+K keyboard shortcut', () => {
    const { result } = renderHook(() => useCommandPalette());
    expect(result.current.open).toBe(false);

    act(() => {
      const event = new KeyboardEvent('keydown', {
        key: 'k',
        metaKey: true,
        bubbles: true,
        cancelable: true,
      });
      window.dispatchEvent(event);
    });

    expect(result.current.open).toBe(true);
  });

  it('closes via Escape when open', () => {
    const { result } = renderHook(() => useCommandPalette());

    act(() => {
      result.current.setOpen(true);
    });
    expect(result.current.open).toBe(true);

    act(() => {
      const event = new KeyboardEvent('keydown', {
        key: 'Escape',
        bubbles: true,
        cancelable: true,
      });
      window.dispatchEvent(event);
    });

    expect(result.current.open).toBe(false);
  });
});
