/**
 * Template Gallery Page Tests
 *
 * Tests for the branding template gallery page.
 *
 * @since SAAS-8
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '@/__tests__/test-utils';
import TemplateGalleryPage from '../branding/templates/page';

// Mock stores and hooks
vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/hooks/use-instances', () => ({
  useOwnerInstances: vi.fn(),
}));

vi.mock('@/hooks/use-templates', () => ({
  useTemplates: vi.fn(),
  useApplyTemplate: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Import mocked modules
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useTemplates, useApplyTemplate } from '@/hooks/use-templates';

const mockTemplates = [
  {
    id: '1',
    name: 'Modern Education',
    category: 'education',
    themeConfig: '{"colors":{"primary":"#3B82F6","secondary":"#1E40AF","accent":"#F59E0B"},"fonts":{"heading":"Inter","body":"Inter"},"style":"modern"}',
    active: true,
    createdAt: '2026-03-24T00:00:00Z',
  },
  {
    id: '2',
    name: 'Professional Training',
    category: 'business',
    themeConfig: '{"colors":{"primary":"#1F2937","secondary":"#374151","accent":"#3B82F6"},"fonts":{"heading":"Roboto","body":"Roboto"},"style":"professional"}',
    active: true,
    createdAt: '2026-03-24T00:00:00Z',
  },
];

describe('TemplateGalleryPage', () => {
  const mockMutateAsync = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    (useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      id: 'user-1',
      email: 'test@example.com',
    });

    (useOwnerInstances as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [{ id: 'instance-1' }],
    });

    (useTemplates as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockTemplates,
      isLoading: false,
    });

    (useApplyTemplate as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      mutateAsync: mockMutateAsync,
      isPending: false,
    });
  });

  it('renders page header', () => {
    render(<TemplateGalleryPage />);

    expect(screen.getByText('Template Gallery')).toBeInTheDocument();
    expect(screen.getByText(/Chọn template để tạo branding ngay lập tức/)).toBeInTheDocument();
  });

  it('renders category filter buttons', () => {
    render(<TemplateGalleryPage />);

    expect(screen.getAllByText('Tất cả').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Giáo dục').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Doanh nghiệp').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Tổng hợp').length).toBeGreaterThanOrEqual(1);
  });

  it('renders template cards', () => {
    render(<TemplateGalleryPage />);

    expect(screen.getByText('Modern Education')).toBeInTheDocument();
    expect(screen.getByText('Professional Training')).toBeInTheDocument();
  });

  it('shows loading state', () => {
    (useTemplates as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
    });

    render(<TemplateGalleryPage />);

    // Should show loading spinner (no template cards)
    expect(screen.queryByText('Modern Education')).not.toBeInTheDocument();
  });

  it('shows empty state when no templates', () => {
    (useTemplates as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [],
      isLoading: false,
    });

    render(<TemplateGalleryPage />);

    expect(screen.getByText('Không có template nào trong danh mục này')).toBeInTheDocument();
  });

  it('displays template style and font info', () => {
    render(<TemplateGalleryPage />);

    expect(screen.getByText('Style: modern')).toBeInTheDocument();
    expect(screen.getByText('Inter')).toBeInTheDocument();
  });

  it('calls apply template on button click', async () => {
    const user = userEvent.setup();
    mockMutateAsync.mockResolvedValue({ themeConfig: '{}', status: 'applied' });

    render(<TemplateGalleryPage />);

    const applyButtons = screen.getAllByText('Áp dụng Template');
    await user.click(applyButtons[0]!);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith({
        templateId: '1',
        instanceId: 'instance-1',
      });
    });
  });
});
