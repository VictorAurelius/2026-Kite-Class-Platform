/**
 * useCommandPalette tests — keyboard shortcut + escape-to-close.
 */

import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useCommandPalette } from '../useCommandPalette';

function dispatchKey(key: string, options: KeyboardEventInit = {}) {
  const event = new KeyboardEvent('keydown', { key, bubbles: true, ...options });
  window.dispatchEvent(event);
}

describe('useCommandPalette', () => {
  it('toggles open via Cmd+K', () => {
    const { result } = renderHook(() => useCommandPalette());
    expect(result.current.open).toBe(false);

    act(() => dispatchKey('k', { metaKey: true }));
    expect(result.current.open).toBe(true);

    act(() => dispatchKey('k', { metaKey: true }));
    expect(result.current.open).toBe(false);
  });

  it('toggles open via Ctrl+K (non-mac)', () => {
    const { result } = renderHook(() => useCommandPalette());
    act(() => dispatchKey('k', { ctrlKey: true }));
    expect(result.current.open).toBe(true);
  });

  it('closes on Escape when open', () => {
    const { result } = renderHook(() => useCommandPalette());
    act(() => result.current.setOpen(true));
    expect(result.current.open).toBe(true);

    act(() => dispatchKey('Escape'));
    expect(result.current.open).toBe(false);
  });

  it('Escape is a no-op when closed', () => {
    const { result } = renderHook(() => useCommandPalette());
    act(() => dispatchKey('Escape'));
    expect(result.current.open).toBe(false);
  });
});
