/**
 * Tests for TotpInput component (Wave 72b Bucket B / GAP-516).
 */
import { describe, it, expect, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { TotpInput } from '../TotpInput';

function TotpHarness({ onComplete }: { onComplete?: (v: string) => void }) {
  const [value, setValue] = useState('');
  return <TotpInput value={value} onChange={setValue} onComplete={onComplete} />;
}

describe('TotpInput', () => {
  it('renders 6 input boxes', () => {
    render(<TotpHarness />);
    const inputs = screen.getAllByRole('textbox');
    expect(inputs).toHaveLength(6);
  });

  it('auto-focuses first box on mount', () => {
    render(<TotpHarness />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    expect(document.activeElement).toBe(inputs[0]!);
  });

  it('accepts single digit and auto-advances focus to next box', () => {
    render(<TotpHarness />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    fireEvent.change(inputs[0]!, { target: { value: '3' } });
    expect(inputs[0]!.value).toBe('3');
    expect(document.activeElement).toBe(inputs[1]!);
  });

  it('rejects non-digit input', () => {
    render(<TotpHarness />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    fireEvent.change(inputs[0]!, { target: { value: 'a' } });
    expect(inputs[0]!.value).toBe('');
  });

  it('handles paste of full 6-digit code', () => {
    const onComplete = vi.fn();
    render(<TotpHarness onComplete={onComplete} />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    fireEvent.paste(inputs[0]!, {
      clipboardData: {
        getData: () => '123456',
      },
    });
    expect(inputs[0]!.value).toBe('1');
    expect(inputs[1]!.value).toBe('2');
    expect(inputs[5]!.value).toBe('6');
    expect(onComplete).toHaveBeenCalledWith('123456');
  });

  it('strips non-digits from pasted text', () => {
    render(<TotpHarness />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    fireEvent.paste(inputs[0]!, {
      clipboardData: {
        getData: () => '12-34 56',
      },
    });
    expect(inputs[0]!.value).toBe('1');
    expect(inputs[5]!.value).toBe('6');
  });

  it('backspace on empty box jumps to previous box and clears it', () => {
    render(<TotpHarness />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    fireEvent.change(inputs[0]!, { target: { value: '1' } });
    fireEvent.change(inputs[1]!, { target: { value: '2' } });
    // inputs[2]! is focused and empty
    fireEvent.keyDown(inputs[2]!, { key: 'Backspace' });
    expect(document.activeElement).toBe(inputs[1]!);
    expect(inputs[1]!.value).toBe('');
  });

  it('fires onComplete when 6 digits filled', () => {
    const onComplete = vi.fn();
    render(<TotpHarness onComplete={onComplete} />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    for (let i = 0; i < 6; i += 1) {
      fireEvent.change(inputs[i]!, { target: { value: String(i + 1) } });
    }
    expect(onComplete).toHaveBeenCalledWith('123456');
  });

  it('respects disabled prop', () => {
    function H() {
      const [v, setV] = useState('');
      return <TotpInput value={v} onChange={setV} disabled />;
    }
    render(<H />);
    const inputs = screen.getAllByRole('textbox') as HTMLInputElement[];
    inputs.forEach((i) => expect(i).toBeDisabled());
  });
});
