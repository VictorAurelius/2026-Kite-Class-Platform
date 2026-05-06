/**
 * useCommandPalette — keyboard shortcut hook.
 *
 * Listens for ⌘K (mac) / Ctrl+K (other) and toggles the palette open state.
 * Also handles Escape to close.
 *
 * Returns: { open, setOpen, toggle }.
 *
 * Mirror of Wave 30 KC primitive — verbatim (no app coupling).
 */

'use client';

import { useCallback, useEffect, useState } from 'react';

export interface UseCommandPaletteResult {
  open: boolean;
  setOpen: (next: boolean) => void;
  toggle: () => void;
}

export function useCommandPalette(): UseCommandPaletteResult {
  const [open, setOpen] = useState(false);

  const toggle = useCallback(() => {
    setOpen((prev) => !prev);
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;

    const onKeyDown = (event: KeyboardEvent) => {
      const isToggleShortcut =
        (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k';
      if (isToggleShortcut) {
        event.preventDefault();
        toggle();
        return;
      }
      if (event.key === 'Escape' && open) {
        setOpen(false);
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, toggle]);

  return { open, setOpen, toggle };
}
