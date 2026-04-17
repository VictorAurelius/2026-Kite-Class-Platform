import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import { AdminInstancesTable } from './AdminInstancesTable';
import { mockInstances, mockEmptyInstances } from '@/test/mocks/admin-data';

// Mock the hooks
vi.mock('@/hooks/use-admin', () => ({
  useSuspendInstance: () => ({
    mutateAsync: vi.fn().mockResolvedValue({}),
    isPending: false,
  }),
  useActivateInstance: () => ({
    mutateAsync: vi.fn().mockResolvedValue({}),
    isPending: false,
  }),
}));

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    back: vi.fn(),
  }),
}));

// Mock sonner
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('AdminInstancesTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders table with instances', () => {
      render(<AdminInstancesTable instances={mockInstances} />);

      // Check header
      expect(screen.getByText('Tổ chức')).toBeInTheDocument();
      expect(screen.getByText('Subdomain')).toBeInTheDocument();
      expect(screen.getByText('Trạng thái')).toBeInTheDocument();

      // Check data rows
      expect(screen.getByText('Trung tâm ABC')).toBeInTheDocument();
      expect(screen.getByText('abc')).toBeInTheDocument();
      expect(screen.getByText('Học viện XYZ')).toBeInTheDocument();
    });

    it('renders empty state when no instances', () => {
      render(<AdminInstancesTable instances={mockEmptyInstances} />);

      expect(screen.getByText('Không tìm thấy instance nào')).toBeInTheDocument();
    });

    it('displays correct status badges', () => {
      render(<AdminInstancesTable instances={mockInstances} />);

      expect(screen.getByText('Hoạt động')).toBeInTheDocument();
      expect(screen.getByText('Dùng thử')).toBeInTheDocument();
      expect(screen.getByText('Tạm ngưng')).toBeInTheDocument();
    });

    it('displays tier labels', () => {
      render(<AdminInstancesTable instances={mockInstances} />);

      expect(screen.getByText('Premium')).toBeInTheDocument();
      expect(screen.getByText('Basic')).toBeInTheDocument();
      expect(screen.getByText('Free')).toBeInTheDocument();
    });

    it('displays owner emails', () => {
      render(<AdminInstancesTable instances={mockInstances} />);

      expect(screen.getByText('admin@abc.com')).toBeInTheDocument();
      expect(screen.getByText('owner@xyz.edu')).toBeInTheDocument();
    });

    it('shows instance count in summary', () => {
      render(<AdminInstancesTable instances={mockInstances} />);

      expect(screen.getByText(/3 \/ 3 instances/)).toBeInTheDocument();
    });
  });

  describe('Search', () => {
    it('filters by organization name', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      const searchInput = screen.getByPlaceholderText(/Tìm theo tên/i);
      await user.type(searchInput, 'ABC');

      expect(screen.getByText('Trung tâm ABC')).toBeInTheDocument();
      expect(screen.queryByText('Học viện XYZ')).not.toBeInTheDocument();
    });

    it('filters by subdomain', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      const searchInput = screen.getByPlaceholderText(/Tìm theo tên/i);
      await user.type(searchInput, 'xyz');

      expect(screen.getByText('Học viện XYZ')).toBeInTheDocument();
      expect(screen.queryByText('Trung tâm ABC')).not.toBeInTheDocument();
    });

    it('filters by email', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      const searchInput = screen.getByPlaceholderText(/Tìm theo tên/i);
      await user.type(searchInput, 'contact@def');

      expect(screen.getByText('Trường DEF')).toBeInTheDocument();
      expect(screen.queryByText('Trung tâm ABC')).not.toBeInTheDocument();
    });

    it('shows no results when search has no match', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      const searchInput = screen.getByPlaceholderText(/Tìm theo tên/i);
      await user.type(searchInput, 'nonexistent');

      expect(screen.getByText('Không tìm thấy instance nào')).toBeInTheDocument();
    });

    it('is case-insensitive', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      const searchInput = screen.getByPlaceholderText(/Tìm theo tên/i);
      await user.type(searchInput, 'TRUNG TÂM');

      expect(screen.getByText('Trung tâm ABC')).toBeInTheDocument();
    });
  });

  describe('Status Filter', () => {
    it('filters by ACTIVE status', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      // Open status select
      const statusSelect = screen.getAllByRole('combobox')[0]!;
      await user.click(statusSelect);

      // Select ACTIVE
      const activeOption = screen.getByRole('option', { name: 'Hoạt động' });
      await user.click(activeOption);

      expect(screen.getByText('Trung tâm ABC')).toBeInTheDocument();
      expect(screen.queryByText('Học viện XYZ')).not.toBeInTheDocument();
      expect(screen.queryByText('Trường DEF')).not.toBeInTheDocument();
    });

    it('filters by TRIAL status', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      const statusSelect = screen.getAllByRole('combobox')[0]!;
      await user.click(statusSelect);

      const trialOption = screen.getByRole('option', { name: 'Dùng thử' });
      await user.click(trialOption);

      expect(screen.getByText('Học viện XYZ')).toBeInTheDocument();
      expect(screen.queryByText('Trung tâm ABC')).not.toBeInTheDocument();
    });

    it('filters by SUSPENDED status', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      const statusSelect = screen.getAllByRole('combobox')[0]!;
      await user.click(statusSelect);

      const suspendedOption = screen.getByRole('option', { name: 'Tạm ngưng' });
      await user.click(suspendedOption);

      expect(screen.getByText('Trường DEF')).toBeInTheDocument();
      expect(screen.queryByText('Trung tâm ABC')).not.toBeInTheDocument();
    });
  });

  describe('Tier Filter', () => {
    it('filters by PREMIUM tier', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      // Second combobox is tier filter
      const tierSelect = screen.getAllByRole('combobox')[1]!;
      await user.click(tierSelect);

      const premiumOption = screen.getByRole('option', { name: 'Premium' });
      await user.click(premiumOption);

      expect(screen.getByText('Trung tâm ABC')).toBeInTheDocument();
      expect(screen.queryByText('Học viện XYZ')).not.toBeInTheDocument();
    });
  });

  describe('Combined Filters', () => {
    it('filters by search and status together', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      // Search for "Trung"
      const searchInput = screen.getByPlaceholderText(/Tìm theo tên/i);
      await user.type(searchInput, 'Trung');

      // Filter by SUSPENDED (Trường DEF is suspended but doesn't match "Trung")
      const statusSelect = screen.getAllByRole('combobox')[0]!;
      await user.click(statusSelect);
      const suspendedOption = screen.getByRole('option', { name: 'Tạm ngưng' });
      await user.click(suspendedOption);

      // Should show no results since "Trung tâm ABC" is ACTIVE, not SUSPENDED
      expect(screen.getByText('Không tìm thấy instance nào')).toBeInTheDocument();
    });
  });

  describe('Row Actions', () => {
    it('opens dropdown menu on action button click', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      // Find the first action button (MoreHorizontal icon)
      const actionButtons = screen.getAllByRole('button');
      const moreButton = actionButtons.find(btn => btn.querySelector('svg'));

      if (moreButton) {
        await user.click(moreButton);

        // Should show dropdown items
        await waitFor(() => {
          expect(screen.getByText('Xem chi tiết')).toBeInTheDocument();
        });
      }
    });
  });

  describe('Summary', () => {
    it('updates count when filtering', async () => {
      const user = userEvent.setup();
      render(<AdminInstancesTable instances={mockInstances} />);

      expect(screen.getByText(/3 \/ 3 instances/)).toBeInTheDocument();

      const searchInput = screen.getByPlaceholderText(/Tìm theo tên/i);
      await user.type(searchInput, 'ABC');

      expect(screen.getByText(/1 \/ 3 instances/)).toBeInTheDocument();
    });
  });
});
