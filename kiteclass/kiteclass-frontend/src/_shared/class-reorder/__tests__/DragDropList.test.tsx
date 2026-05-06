/**
 * Tests for DragDropList primitive (Wave 30 Bucket B).
 *
 * @since Wave 30 (2026-05-06)
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DragDropList } from '../DragDropList';

type Item = { id: number; name: string };

const items: Item[] = [
  { id: 1, name: 'Lớp Toán 6A' },
  { id: 2, name: 'Lớp Văn 6B' },
  { id: 3, name: 'Lớp Anh 6C' },
];

describe('DragDropList', () => {
  it('renders all items', () => {
    render(
      <DragDropList<Item>
        items={items}
        getId={(it) => it.id}
        renderItem={(it) => <span>{it.name}</span>}
        onReorder={vi.fn()}
        ariaLabel="Class list"
      />,
    );

    expect(screen.getByText('Lớp Toán 6A')).toBeInTheDocument();
    expect(screen.getByText('Lớp Văn 6B')).toBeInTheDocument();
    expect(screen.getByText('Lớp Anh 6C')).toBeInTheDocument();
  });

  it('exposes the provided ariaLabel on the list root', () => {
    render(
      <DragDropList<Item>
        items={items}
        getId={(it) => it.id}
        renderItem={(it) => <span>{it.name}</span>}
        onReorder={vi.fn()}
        ariaLabel="Danh sách lớp"
      />,
    );

    expect(
      screen.getByRole('list', { name: 'Danh sách lớp' }),
    ).toBeInTheDocument();
  });

  it('marks each row as draggable', () => {
    render(
      <DragDropList<Item>
        items={items}
        getId={(it) => it.id}
        renderItem={(it) => <span>{it.name}</span>}
        onReorder={vi.fn()}
      />,
    );

    const rows = screen.getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    rows.forEach((row) => {
      expect(row).toHaveAttribute('draggable', 'true');
    });
  });

  it('fires onReorder with the new order when an item is dropped on a different index', () => {
    const onReorder = vi.fn();
    render(
      <DragDropList<Item>
        items={items}
        getId={(it) => it.id}
        renderItem={(it) => <span>{it.name}</span>}
        onReorder={onReorder}
      />,
    );

    const rows = screen.getAllByRole('listitem');
    // Drag first row (id=1) onto third row (index=2).
    const dataTransfer = {
      effectAllowed: '',
      dropEffect: '',
      setData: vi.fn(),
      getData: vi.fn(() => '1'),
    };
    fireEvent.dragStart(rows[0]!, { dataTransfer });
    fireEvent.dragOver(rows[2]!, { dataTransfer });
    fireEvent.drop(rows[2]!, { dataTransfer });

    expect(onReorder).toHaveBeenCalledTimes(1);
    const next = onReorder.mock.calls[0]![0] as Item[];
    expect(next.map((i) => i.id)).toEqual([2, 3, 1]);
  });

  it('does not fire onReorder when dropping on the same index', () => {
    const onReorder = vi.fn();
    render(
      <DragDropList<Item>
        items={items}
        getId={(it) => it.id}
        renderItem={(it) => <span>{it.name}</span>}
        onReorder={onReorder}
      />,
    );

    const rows = screen.getAllByRole('listitem');
    const dataTransfer = {
      effectAllowed: '',
      dropEffect: '',
      setData: vi.fn(),
      getData: vi.fn(() => '2'),
    };
    fireEvent.dragStart(rows[1]!, { dataTransfer });
    fireEvent.dragOver(rows[1]!, { dataTransfer });
    fireEvent.drop(rows[1]!, { dataTransfer });

    expect(onReorder).not.toHaveBeenCalled();
  });

  it('passes isDragging flag to renderItem during a drag', () => {
    const renderItem = vi.fn((it: Item, isDragging: boolean) => (
      <span data-testid={`row-${it.id}`}>
        {it.name}
        {isDragging ? ' (dragging)' : ''}
      </span>
    ));
    render(
      <DragDropList<Item>
        items={items}
        getId={(it) => it.id}
        renderItem={renderItem}
        onReorder={vi.fn()}
      />,
    );

    const rows = screen.getAllByRole('listitem');
    const dataTransfer = {
      effectAllowed: '',
      dropEffect: '',
      setData: vi.fn(),
      getData: vi.fn(),
    };
    fireEvent.dragStart(rows[0]!, { dataTransfer });

    // After drag start the first row's renderItem should be called with isDragging=true.
    const calls = renderItem.mock.calls.filter(
      (c) => (c[0] as Item).id === 1,
    );
    expect(calls.some((c) => c[1] === true)).toBe(true);
  });
});
