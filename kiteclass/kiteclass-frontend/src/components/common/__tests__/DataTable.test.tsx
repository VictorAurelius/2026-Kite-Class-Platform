/**
 * DataTable Component Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { DataTable } from '../data-table';
import { ColumnDef } from '@tanstack/react-table';

interface TestData {
  id: number;
  name: string;
  email: string;
}

const mockData: TestData[] = [
  { id: 1, name: 'John Doe', email: 'john@example.com' },
  { id: 2, name: 'Jane Smith', email: 'jane@example.com' },
  { id: 3, name: 'Bob Johnson', email: 'bob@example.com' },
];

const mockColumns: ColumnDef<TestData>[] = [
  {
    accessorKey: 'name',
    header: 'Name',
  },
  {
    accessorKey: 'email',
    header: 'Email',
  },
];

describe('DataTable', () => {
  it('should render table with data', () => {
    render(<DataTable columns={mockColumns} data={mockData} />);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('john@example.com')).toBeInTheDocument();
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  });

  it('should render empty state when no data', () => {
    render(<DataTable columns={mockColumns} data={[]} />);

    expect(screen.getByText(/no results/i)).toBeInTheDocument();
  });

  it('should render column headers', () => {
    render(<DataTable columns={mockColumns} data={mockData} />);

    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
  });

  it('should handle pagination when pageCount is provided', async () => {
    const user = userEvent.setup();
    const onPaginationChange = vi.fn();

    render(
      <DataTable
        columns={mockColumns}
        data={mockData}
        pageCount={5}
        pageSize={10}
        onPaginationChange={onPaginationChange}
      />
    );

    // Should show pagination controls
    const nextButton = screen.getByRole('button', { name: /next/i });
    expect(nextButton).toBeInTheDocument();

    await user.click(nextButton);

    expect(onPaginationChange).toHaveBeenCalledWith({
      pageIndex: 1,
      pageSize: 10,
    });
  });

  it('should disable previous button on first page', () => {
    render(
      <DataTable
        columns={mockColumns}
        data={mockData}
        pageCount={5}
        pageSize={10}
      />
    );

    const prevButton = screen.getByRole('button', { name: /previous/i });
    expect(prevButton).toBeDisabled();
  });

  it('should disable next button on last page', () => {
    render(
      <DataTable
        columns={mockColumns}
        data={mockData}
        pageCount={1}
        pageSize={10}
      />
    );

    const nextButton = screen.getByRole('button', { name: /next/i });
    expect(nextButton).toBeDisabled();
  });

  it('should render all rows', () => {
    render(<DataTable columns={mockColumns} data={mockData} />);

    const rows = screen.getAllByRole('row');
    // +1 for header row
    expect(rows).toHaveLength(mockData.length + 1);
  });

  it('should render custom cell content', () => {
    const customColumns: ColumnDef<TestData>[] = [
      {
        accessorKey: 'name',
        header: 'Name',
        cell: ({ row }) => <strong data-testid="custom-cell">{row.original.name}</strong>,
      },
    ];

    render(<DataTable columns={customColumns} data={mockData} />);

    const customCells = screen.getAllByTestId('custom-cell');
    expect(customCells).toHaveLength(mockData.length);
    expect(customCells[0]).toHaveTextContent('John Doe');
  });
});
