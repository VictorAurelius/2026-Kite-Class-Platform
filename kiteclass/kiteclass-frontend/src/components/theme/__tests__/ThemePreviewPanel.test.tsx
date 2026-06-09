/**
 * ThemePreviewPanel Component Tests
 *
 * Tests for theme preview panel behavior.
 *
 * @since PR-Q8
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@/contexts/ThemeContext';
import { ThemePreviewPanel } from '../ThemePreviewPanel';

// Mock useSearchParams
const mockGet = vi.fn();
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({
    get: mockGet,
  }),
  usePathname: () => '/',
}));

describe('ThemePreviewPanel', () => {
  beforeEach(() => {
    // Enable preview mode
    mockGet.mockReturnValue('theme');

    // Mock localStorage
    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: vi.fn(),
        setItem: vi.fn(),
        removeItem: vi.fn(),
        clear: vi.fn(),
      },
      writable: true,
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders when preview=theme query param is present', () => {
    render(
      <ThemeProvider>
        <ThemePreviewPanel />
      </ThemeProvider>
    );

    expect(screen.getByText('Theme Preview')).toBeInTheDocument();
  });

  it('does not render when preview param is missing', () => {
    mockGet.mockReturnValue(null);

    render(
      <ThemeProvider>
        <ThemePreviewPanel />
      </ThemeProvider>
    );

    expect(screen.queryByText('Theme Preview')).not.toBeInTheDocument();
  });

  it('applies theme changes to CSS variables when Apply is clicked', async () => {
    const user = userEvent.setup();

    const { container } = render(
      <ThemeProvider>
        <ThemePreviewPanel />
      </ThemeProvider>
    );

    // Find Primary color input (first color input)
    const colorInputs = container.querySelectorAll('input[type="color"]');
    const primaryInput = colorInputs[0] as HTMLInputElement;

    // Change color to red
    fireEvent.change(primaryInput, { target: { value: '#FF0000' } });

    // Click Apply button
    const applyButton = screen.getByRole('button', { name: /Áp dụng/i });
    await user.click(applyButton);

    // Verify CSS variable is set (RGB format: 255 0 0)
    await waitFor(() => {
      const primaryVar = document.documentElement.style.getPropertyValue('--theme-primary');
      expect(primaryVar).toBe('255 0 0');
    });
  });

  it('persists theme to localStorage when Apply is clicked', async () => {
    const user = userEvent.setup();

    const { container } = render(
      <ThemeProvider>
        <ThemePreviewPanel />
      </ThemeProvider>
    );

    // Change color to green
    const colorInputs = container.querySelectorAll('input[type="color"]');
    const primaryInput = colorInputs[0] as HTMLInputElement;
    fireEvent.change(primaryInput, { target: { value: '#00FF00' } });

    // Click Apply
    const applyButton = screen.getByRole('button', { name: /Áp dụng/i });
    await user.click(applyButton);

    // Verify localStorage.setItem was called with theme data
    await waitFor(() => {
      expect(localStorage.setItem).toHaveBeenCalled();
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const calls = (localStorage.setItem as any).mock.calls;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const themeCall = calls.find((call: any) => call[0] === 'kiteclass_theme');
      expect(themeCall).toBeDefined();

      const savedTheme = JSON.parse(themeCall[1]);
      expect(savedTheme.colors.primary.toLowerCase()).toBe('#00ff00');
    });
  });

  it('resets theme to default when Reset is clicked', async () => {
    const user = userEvent.setup();

    const { container } = render(
      <ThemeProvider>
        <ThemePreviewPanel />
      </ThemeProvider>
    );

    // Change color first
    const colorInputs = container.querySelectorAll('input[type="color"]');
    const primaryInput = colorInputs[0] as HTMLInputElement;
    fireEvent.change(primaryInput, { target: { value: '#FF00FF' } });

    const applyButton = screen.getByRole('button', { name: /Áp dụng/i });
    await user.click(applyButton);

    // Click Reset
    const resetButton = screen.getByRole('button', { name: /Reset/i });
    await user.click(resetButton);

    // Verify CSS variables reset to default (Blue: #3B82F6 → 59 130 246)
    await waitFor(() => {
      const primaryVar = document.documentElement.style.getPropertyValue('--theme-primary');
      expect(primaryVar).toBe('59 130 246');
    });

    // Verify localStorage cleared
    expect(localStorage.removeItem).toHaveBeenCalledWith('kiteclass_theme');
  });

  it('does not apply theme until Apply is clicked', async () => {
    const { container } = render(
      <ThemeProvider>
        <ThemePreviewPanel />
      </ThemeProvider>
    );

    // Get initial CSS variable value
    const initialPrimary = document.documentElement.style.getPropertyValue('--theme-primary');

    // Change color but don't click Apply
    const colorInputs = container.querySelectorAll('input[type="color"]');
    const primaryInput = colorInputs[0] as HTMLInputElement;
    fireEvent.change(primaryInput, { target: { value: '#123456' } });

    // Wait a bit to ensure no async updates
    await new Promise((resolve) => setTimeout(resolve, 100));

    // CSS variable should NOT change yet
    const currentPrimary = document.documentElement.style.getPropertyValue('--theme-primary');
    expect(currentPrimary).toBe(initialPrimary);
  });
});
