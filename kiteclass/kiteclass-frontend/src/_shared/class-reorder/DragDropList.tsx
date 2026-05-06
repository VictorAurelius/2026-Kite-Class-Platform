/**
 * DragDropList — HTML5 drag-and-drop reorder primitive.
 *
 * Wave 30 Bucket B (GAP-266 Phase 4 kit port). Native HTML5 DnD — no extra deps,
 * keeps bundle lean. Designed for class/course reorder lists; generic over item
 * shape via `getId` + `renderItem` props (controlled component pattern matching
 * Wave 27/28/29 G* port style).
 *
 * Limitations (acceptable for v1):
 * - Touch devices: HTML5 DnD has limited mobile support; long-press to drag works
 *   on most modern Android Chrome / iOS Safari but is best-effort.
 * - No keyboard reorder yet (would need ARIA live region + arrow-key handler).
 *   Tracked separately if needed; out-of-scope for kit port.
 *
 * Persistence: parent owns `onReorder` callback — wire it to PUT endpoint.
 *
 * @since Wave 30 (2026-05-06)
 */

'use client';

import { useState, useCallback } from 'react';
import type { DragEvent } from 'react';

export type DragDropListProps<T> = {
  /** Source-of-truth ordered list. Parent owns the array. */
  items: ReadonlyArray<T>;
  /** Stable id extractor for React keys + drag state. */
  getId: (item: T) => string | number;
  /** Render function for one row — caller controls visual chrome. */
  renderItem: (item: T, isDragging: boolean) => React.ReactNode;
  /**
   * Called when user drops an item at a new index. Parent persists the new
   * order (typically via PUT endpoint). New array is provided pre-reordered.
   */
  onReorder: (next: ReadonlyArray<T>) => void;
  /** Optional aria-label for the list root. */
  ariaLabel?: string;
  /** Optional className for the list root. */
  className?: string;
};

export function DragDropList<T>({
  items,
  getId,
  renderItem,
  onReorder,
  ariaLabel,
  className,
}: DragDropListProps<T>) {
  const [draggingId, setDraggingId] = useState<string | number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

  const handleDragStart = useCallback(
    (e: DragEvent<HTMLLIElement>, id: string | number) => {
      setDraggingId(id);
      e.dataTransfer.effectAllowed = 'move';
      // Required for Firefox to actually start the drag.
      e.dataTransfer.setData('text/plain', String(id));
    },
    [],
  );

  const handleDragOver = useCallback(
    (e: DragEvent<HTMLLIElement>, overIndex: number) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      if (dragOverIndex !== overIndex) {
        setDragOverIndex(overIndex);
      }
    },
    [dragOverIndex],
  );

  const handleDragEnd = useCallback(() => {
    setDraggingId(null);
    setDragOverIndex(null);
  }, []);

  const handleDrop = useCallback(
    (e: DragEvent<HTMLLIElement>, dropIndex: number) => {
      e.preventDefault();
      if (draggingId === null) {
        return;
      }
      const fromIndex = items.findIndex((it) => getId(it) === draggingId);
      if (fromIndex === -1 || fromIndex === dropIndex) {
        setDraggingId(null);
        setDragOverIndex(null);
        return;
      }
      const next = [...items];
      const [moved] = next.splice(fromIndex, 1);
      if (moved !== undefined) {
        next.splice(dropIndex, 0, moved);
        onReorder(next);
      }
      setDraggingId(null);
      setDragOverIndex(null);
    },
    [draggingId, items, getId, onReorder],
  );

  return (
    <ul
      role="list"
      aria-label={ariaLabel}
      className={className ?? 'space-y-2'}
      data-testid="drag-drop-list"
    >
      {items.map((item, index) => {
        const id = getId(item);
        const isDragging = draggingId === id;
        const isDragOver = dragOverIndex === index;
        return (
          <li
            key={id}
            draggable
            onDragStart={(e) => handleDragStart(e, id)}
            onDragOver={(e) => handleDragOver(e, index)}
            onDrop={(e) => handleDrop(e, index)}
            onDragEnd={handleDragEnd}
            data-dragging={isDragging || undefined}
            data-drag-over={isDragOver || undefined}
            className={[
              'cursor-grab transition-opacity',
              isDragging ? 'opacity-50' : '',
              isDragOver ? 'ring-2 ring-primary' : '',
            ]
              .filter(Boolean)
              .join(' ')}
          >
            {renderItem(item, isDragging)}
          </li>
        );
      })}
    </ul>
  );
}
