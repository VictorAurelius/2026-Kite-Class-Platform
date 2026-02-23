/**
 * SearchInput Component Tests
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { SearchInput } from '../search-input';

describe('SearchInput', () => {
  it('should render search input', () => {
    render(<SearchInput onSearch={vi.fn()} />);
    expect(screen.getByPlaceholderText(/search/i)).toBeInTheDocument();
  });

  it('should call onSearch when typing and debounced', async () => {
    const onSearch = vi.fn();
    render(<SearchInput onSearch={onSearch} />);

    const input = screen.getByPlaceholderText(/search/i);

    // Simulate typing (will trigger debounced search)
    await userEvent.type(input, 'test query');

    // Wait for debounce to complete (default is usually 300-500ms)
    await vi.waitFor(() => {
      expect(onSearch).toHaveBeenCalled();
    }, { timeout: 1000 });
  });

  it('should clear input when clear button clicked', async () => {
    const onSearch = vi.fn();
    render(<SearchInput onSearch={onSearch} />);

    const input = screen.getByPlaceholderText(/search/i) as HTMLInputElement;
    await userEvent.type(input, 'test');

    // Wait for clear button to appear
    const clearButton = await screen.findByRole('button', {}, { timeout: 1000 });
    await userEvent.click(clearButton);

    expect(input.value).toBe('');
  });

  it('should use custom placeholder', () => {
    render(<SearchInput onSearch={vi.fn()} placeholder="Search students..." />);
    expect(screen.getByPlaceholderText('Search students...')).toBeInTheDocument();
  });
});
