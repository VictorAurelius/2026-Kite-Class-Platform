/**
 * Component tests for VettingDocumentUploadPage (Wave 18b3 — GAP-322b).
 *
 * Verifies:
 * - Renders form with file input + submit button (initially disabled)
 * - File >10MB rejected client-side without API call
 * - Successful upload renders the storage key + size banner
 * - API error surfaces ErrorAlert message
 * - Invalid vettingId in route params → ErrorAlert (no form rendered)
 *
 * @since 2.18.1 (Wave 18b3 — GAP-322b Phase 1B remainder)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '@/test/utils';
import VettingDocumentUploadPage from '../page';

const mockUseParams = vi.fn();
vi.mock('next/navigation', () => ({
  useParams: () => mockUseParams(),
  usePathname: () => '/admin/vetting',
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn(), prefetch: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockUpload = vi.fn();
vi.mock('@/lib/api/vetting', () => ({
  vettingApi: {
    uploadDocument: (...args: unknown[]) => mockUpload(...args),
  },
}));

describe('VettingDocumentUploadPage', () => {
  beforeEach(() => {
    mockUseParams.mockReset();
    mockUpload.mockReset();
  });

  it('renders the form when vettingId is valid', () => {
    mockUseParams.mockReturnValue({ vettingId: '7' });
    render(<VettingDocumentUploadPage />);

    expect(
      screen.getByRole('heading', { name: /Tải lên tài liệu xác minh/i }),
    ).toBeInTheDocument();
    expect(screen.getByTestId('vetting-file-input')).toBeInTheDocument();
    const submit = screen.getByTestId('vetting-upload-submit') as HTMLButtonElement;
    expect(submit).toBeInTheDocument();
    // Submit disabled until a file is chosen
    expect(submit.disabled).toBe(true);
  });

  it('renders ErrorAlert when vettingId is missing/invalid', () => {
    mockUseParams.mockReturnValue({ vettingId: 'abc' });
    render(<VettingDocumentUploadPage />);

    expect(
      screen.getByText(/ID hồ sơ không hợp lệ/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId('vetting-file-input'),
    ).not.toBeInTheDocument();
  });

  it('rejects files over 10MB client-side without calling API', async () => {
    mockUseParams.mockReturnValue({ vettingId: '7' });
    const user = userEvent.setup();
    render(<VettingDocumentUploadPage />);

    const input = screen.getByTestId('vetting-file-input') as HTMLInputElement;
    // 11MB
    const big = new File([new Uint8Array(11 * 1024 * 1024)], 'huge.pdf', {
      type: 'application/pdf',
    });
    await user.upload(input, big);

    expect(screen.getByRole('alert')).toHaveTextContent(/vượt quá 10MB/i);
    expect(mockUpload).not.toHaveBeenCalled();
  });

  it('uploads the file on submit and shows success banner', async () => {
    mockUseParams.mockReturnValue({ vettingId: '7' });
    mockUpload.mockResolvedValue({
      vettingId: 7,
      storageKey: 'vetting/7/lltp.pdf',
      sizeBytes: 14,
      contentType: 'application/pdf',
    });
    const user = userEvent.setup();
    render(<VettingDocumentUploadPage />);

    const input = screen.getByTestId('vetting-file-input') as HTMLInputElement;
    const file = new File(['fake-pdf-bytes'], 'lltp.pdf', {
      type: 'application/pdf',
    });
    await user.upload(input, file);

    const submit = screen.getByTestId(
      'vetting-upload-submit',
    ) as HTMLButtonElement;
    expect(submit.disabled).toBe(false);
    // Submit form directly (jsdom + userEvent.click on submit button can be flaky)
    fireEvent.submit(submit.closest('form')!);

    await waitFor(() => expect(mockUpload).toHaveBeenCalledTimes(1));
    expect(mockUpload).toHaveBeenCalledWith(7, file);
    await waitFor(() =>
      expect(screen.getByTestId('vetting-upload-success')).toBeInTheDocument(),
    );
    expect(
      screen.getByText(/vetting\/7\/lltp\.pdf/),
    ).toBeInTheDocument();
  });

  it('surfaces server error message when upload fails', async () => {
    mockUseParams.mockReturnValue({ vettingId: '7' });
    mockUpload.mockRejectedValue(new Error('Server says no'));
    const user = userEvent.setup();
    render(<VettingDocumentUploadPage />);

    const input = screen.getByTestId('vetting-file-input') as HTMLInputElement;
    const file = new File(['x'], 'lltp.pdf', { type: 'application/pdf' });
    await user.upload(input, file);
    const submit = screen.getByTestId(
      'vetting-upload-submit',
    ) as HTMLButtonElement;
    fireEvent.submit(submit.closest('form')!);

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Server says no/i),
    );
    expect(
      screen.queryByTestId('vetting-upload-success'),
    ).not.toBeInTheDocument();
  });
});
