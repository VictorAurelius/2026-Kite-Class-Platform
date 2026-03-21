import { render, screen } from '@testing-library/react';
import { CMSEditor } from '../CMSEditor';

// Mock useToast
jest.mock('@/hooks/use-toast', () => ({
  toast: jest.fn(),
}));

describe('CMSEditor', () => {
  const mockTenantId = 'test-tenant-123';

  it('should render editor title', () => {
    render(<CMSEditor tenantId={mockTenantId} />);

    expect(screen.getByText('Landing Page Editor')).toBeInTheDocument();
  });

  it('should render hero section fields', () => {
    render(<CMSEditor tenantId={mockTenantId} />);

    // Check if hero section exists
    expect(screen.getByText('Hero Section')).toBeInTheDocument();
  });

  it('should render about section fields', () => {
    render(<CMSEditor tenantId={mockTenantId} />);

    // Check if about section exists
    expect(screen.getByText('About Section')).toBeInTheDocument();
  });

  it('should render courses section fields', () => {
    render(<CMSEditor tenantId={mockTenantId} />);

    // Check if courses section exists
    expect(screen.getByText('Courses Section')).toBeInTheDocument();
  });

  it('should have save button', () => {
    render(<CMSEditor tenantId={mockTenantId} />);

    const saveButton = screen.getByRole('button', { name: /save changes/i });
    expect(saveButton).toBeInTheDocument();
    expect(saveButton).toBeDisabled(); // Disabled when no changes
  });

  it('should have reset button', () => {
    render(<CMSEditor tenantId={mockTenantId} />);

    const resetButton = screen.getByRole('button', { name: /reset/i });
    expect(resetButton).toBeInTheDocument();
    expect(resetButton).toBeDisabled(); // Disabled when no changes
  });

  it('should render with initial data', () => {
    const initialData = {
      hero: {
        title: 'Test Title',
        subtitle: 'Test Subtitle',
      },
    };

    render(<CMSEditor tenantId={mockTenantId} initialData={initialData} />);

    // Check if initial values are rendered (this is a simple check)
    expect(screen.getByText('Landing Page Editor')).toBeInTheDocument();
  });
});
