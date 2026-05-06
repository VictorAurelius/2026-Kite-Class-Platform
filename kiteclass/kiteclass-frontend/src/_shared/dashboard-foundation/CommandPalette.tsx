/**
 * CommandPalette — ⌘K dialog with fuzzy filter + keyboard navigation.
 *
 * Source spec: kiteclass-pro-v2/screens/command-palette.html.
 *
 * Features:
 *   - Fuzzy match on label + keywords (case-insensitive substring; cheap +
 *     deterministic — no Fuse.js dependency).
 *   - Keyboard ↑/↓ to navigate, Enter to activate, Esc to close.
 *   - Section grouping in the rendered list.
 *   - Routes via `next/navigation` when a command has `href`; otherwise
 *     invokes `onSelect`.
 *
 * Mounted once per layout (consumer provides `commands` + uses
 * `useCommandPalette()` for open state).
 */

'use client';

import { useRouter } from 'next/navigation';
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
} from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import type { DashboardCommand } from './types';

export interface CommandPaletteProps {
  open: boolean;
  onOpenChange: (next: boolean) => void;
  commands: DashboardCommand[];
  /** Optional placeholder for the search input. */
  placeholder?: string;
}

interface RankedCommand {
  command: DashboardCommand;
  score: number;
}

function fuzzyMatch(query: string, command: DashboardCommand): number {
  if (!query) return 1; // empty query → keep all
  const q = query.toLowerCase().trim();
  const haystacks = [command.label, ...(command.keywords ?? [])].map((s) =>
    s.toLowerCase(),
  );
  let best = 0;
  for (const hay of haystacks) {
    if (hay.includes(q)) {
      // Earlier substring match → higher score.
      const idx = hay.indexOf(q);
      const score = 1 - idx / Math.max(hay.length, 1);
      if (score > best) best = score;
    }
  }
  return best;
}

export function CommandPalette({
  open,
  onOpenChange,
  commands,
  placeholder = 'Tìm hoặc gõ lệnh...',
}: CommandPaletteProps) {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const ranked = useMemo<RankedCommand[]>(() => {
    return commands
      .map((command) => ({ command, score: fuzzyMatch(query, command) }))
      .filter((entry) => entry.score > 0)
      .sort((a, b) => b.score - a.score);
  }, [commands, query]);

  // Reset transient state on open/close.
  useEffect(() => {
    if (open) {
      setQuery('');
      setActiveIndex(0);
      // Focus the input shortly after open animation.
      const timer = setTimeout(() => inputRef.current?.focus(), 50);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [open]);

  // Clamp activeIndex when results change.
  useEffect(() => {
    if (activeIndex >= ranked.length) {
      setActiveIndex(Math.max(0, ranked.length - 1));
    }
  }, [activeIndex, ranked.length]);

  const activate = useCallback(
    (command: DashboardCommand) => {
      onOpenChange(false);
      if (command.onSelect) {
        command.onSelect();
        return;
      }
      if (command.href) {
        router.push(command.href);
      }
    },
    [onOpenChange, router],
  );

  const onKeyDown = useCallback(
    (event: KeyboardEvent<HTMLInputElement>) => {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        setActiveIndex((idx) => Math.min(idx + 1, Math.max(0, ranked.length - 1)));
        return;
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault();
        setActiveIndex((idx) => Math.max(0, idx - 1));
        return;
      }
      if (event.key === 'Enter') {
        event.preventDefault();
        const selected = ranked[activeIndex];
        if (selected) activate(selected.command);
      }
    },
    [activate, activeIndex, ranked],
  );

  // Group ranked commands by section while preserving rank order within each.
  const grouped = useMemo(() => {
    const sections = new Map<string, DashboardCommand[]>();
    for (const { command } of ranked) {
      const list = sections.get(command.section);
      if (list) {
        list.push(command);
      } else {
        sections.set(command.section, [command]);
      }
    }
    return Array.from(sections.entries());
  }, [ranked]);

  // Build flat ordering to align activeIndex with rendered list items.
  const flatOrder = useMemo(() => ranked.map((r) => r.command.id), [ranked]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-xl gap-0 overflow-hidden p-0">
        <DialogTitle className="sr-only">Command palette</DialogTitle>
        <DialogDescription className="sr-only">
          Tìm và mở các trang trong dashboard bằng bàn phím.
        </DialogDescription>
        <div className="border-b border-border px-4 py-3">
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setActiveIndex(0);
            }}
            onKeyDown={onKeyDown}
            placeholder={placeholder}
            aria-label="Command search"
            className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            data-testid="command-palette-input"
          />
        </div>
        <div
          className="max-h-[60vh] overflow-y-auto"
          role="listbox"
          aria-label="Available commands"
          data-testid="command-palette-list"
        >
          {grouped.length === 0 ? (
            <div className="px-4 py-6 text-center text-sm text-muted-foreground">
              Không tìm thấy lệnh phù hợp.
            </div>
          ) : (
            grouped.map(([section, items]) => (
              <div key={section} className="px-2 py-2">
                <div className="px-3 py-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  {section}
                </div>
                {items.map((command) => {
                  const flatIdx = flatOrder.indexOf(command.id);
                  const isActive = flatIdx === activeIndex;
                  const Icon = command.icon;
                  return (
                    <button
                      key={command.id}
                      type="button"
                      role="option"
                      aria-selected={isActive}
                      onClick={() => activate(command)}
                      onMouseEnter={() => setActiveIndex(flatIdx)}
                      className={cn(
                        'flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm transition-colors',
                        isActive
                          ? 'bg-accent text-accent-foreground'
                          : 'text-foreground hover:bg-accent/60',
                      )}
                      data-testid="command-palette-item"
                    >
                      {Icon ? (
                        <Icon className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <span className="h-4 w-4" aria-hidden="true" />
                      )}
                      <span className="flex-1 truncate">{command.label}</span>
                      {command.hint ? (
                        <kbd className="rounded border border-border bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
                          {command.hint}
                        </kbd>
                      ) : null}
                    </button>
                  );
                })}
              </div>
            ))
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
