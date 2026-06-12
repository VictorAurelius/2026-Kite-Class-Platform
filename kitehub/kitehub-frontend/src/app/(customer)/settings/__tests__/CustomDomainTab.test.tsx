/**
 * CustomDomainTab Tests — SAAS-16
 *
 * Tests for custom domain settings UI.
 * - Locked state cho FREE/BASIC tier
 * - Form cho PREMIUM tier
 * - Submit form → gọi API đúng
 * - Status display (PENDING_VERIFY, VERIFIED)
 *
 * @since SAAS-16
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { CustomDomainTab } from '../components/CustomDomainTab';
import type { Instance } from '@/types/instance';

// Mock domain hooks
vi.mock('@/hooks/use-domain', () => ({
  useDomainStatus: vi.fn(),
  useInitiateDomain: vi.fn(),
  useVerifyDomain: vi.fn(),
  useRemoveDomain: vi.fn(),
}));

import {
  useDomainStatus,
  useInitiateDomain,
  useVerifyDomain,
  useRemoveDomain,
} from '@/hooks/use-domain';

// Base instance fixture
const baseInstance: Instance = {
  id: 'inst-premium-1',
  organizationName: 'Test School',
  subdomain: 'test-school',
  ownerId: 'owner-1',
  contactEmail: 'owner@test.com',
  status: 'ACTIVE',
  tier: 'PREMIUM',
  trialStartedAt: null,
  trialExpiresAt: null,
  trialDaysLeft: null,
  subscriptionId: 'sub-1',
  subscriptionExpiresAt: null,
  isActive: true,
  isOnTrial: false,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

// Default mock returns for hooks
const mockMutateFn = vi.fn();

const defaultDomainStatusReturn = {
  data: { customDomain: null, verifyToken: null, verifyRecord: null, status: 'NONE' as const, verifiedAt: null, backupUrl: 'https://test-school.kitehub.me' },
  isLoading: false,
  refetch: vi.fn(),
};

const defaultMutationReturn = {
  mutate: mockMutateFn,
  isPending: false,
  isError: false,
  error: null,
};

describe('CustomDomainTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (useDomainStatus as ReturnType<typeof vi.fn>).mockReturnValue(defaultDomainStatusReturn);
    (useInitiateDomain as ReturnType<typeof vi.fn>).mockReturnValue(defaultMutationReturn);
    (useVerifyDomain as ReturnType<typeof vi.fn>).mockReturnValue(defaultMutationReturn);
    (useRemoveDomain as ReturnType<typeof vi.fn>).mockReturnValue(defaultMutationReturn);
  });

  // ── Locked state tests ──────────────────────────────────────────────────

  it('renders locked state for FREE tier', () => {
    const freeInstance: Instance = { ...baseInstance, tier: 'FREE' };
    render(<CustomDomainTab instance={freeInstance} />);

    expect(screen.getByTestId('domain-locked')).toBeInTheDocument();
    expect(screen.getByText(/Tính năng Premium/i)).toBeInTheDocument();
    expect(screen.getByText(/Nâng cấp gói/i)).toBeInTheDocument();
  });

  it('renders locked state for BASIC tier', () => {
    const basicInstance: Instance = { ...baseInstance, tier: 'BASIC' };
    render(<CustomDomainTab instance={basicInstance} />);

    expect(screen.getByTestId('domain-locked')).toBeInTheDocument();
  });

  it('shows current tier in locked state', () => {
    const freeInstance: Instance = { ...baseInstance, tier: 'FREE' };
    render(<CustomDomainTab instance={freeInstance} />);

    expect(screen.getByText(/FREE/)).toBeInTheDocument();
  });

  // ── PREMIUM tier — form state ───────────────────────────────────────────

  it('renders domain form for PREMIUM tier with no domain set', () => {
    render(<CustomDomainTab instance={baseInstance} />);

    expect(screen.getByTestId('domain-form')).toBeInTheDocument();
    expect(screen.getByTestId('input-domain')).toBeInTheDocument();
    expect(screen.getByTestId('btn-submit-domain')).toBeInTheDocument();
  });

  it('renders domain form for ENTERPRISE tier', () => {
    const enterpriseInstance: Instance = { ...baseInstance, tier: 'ENTERPRISE' };
    render(<CustomDomainTab instance={enterpriseInstance} />);

    expect(screen.getByTestId('domain-form')).toBeInTheDocument();
  });

  it('submit button disabled when input is empty', () => {
    render(<CustomDomainTab instance={baseInstance} />);

    const submitBtn = screen.getByTestId('btn-submit-domain');
    expect(submitBtn).toBeDisabled();
  });

  it('submit button enabled when domain is entered', () => {
    render(<CustomDomainTab instance={baseInstance} />);

    const input = screen.getByTestId('input-domain');
    fireEvent.change(input, { target: { value: 'school.example.com' } });

    const submitBtn = screen.getByTestId('btn-submit-domain');
    expect(submitBtn).not.toBeDisabled();
  });

  it('calls initiate mutation with correct domain on submit', () => {
    const mutateFn = vi.fn();
    (useInitiateDomain as ReturnType<typeof vi.fn>).mockReturnValue({
      ...defaultMutationReturn,
      mutate: mutateFn,
    });

    render(<CustomDomainTab instance={baseInstance} />);

    const input = screen.getByTestId('input-domain');
    fireEvent.change(input, { target: { value: 'school.example.com' } });

    const submitBtn = screen.getByTestId('btn-submit-domain');
    fireEvent.click(submitBtn);

    expect(mutateFn).toHaveBeenCalledWith('school.example.com');
  });

  it('trims whitespace from domain input on submit', () => {
    const mutateFn = vi.fn();
    (useInitiateDomain as ReturnType<typeof vi.fn>).mockReturnValue({
      ...defaultMutationReturn,
      mutate: mutateFn,
    });

    render(<CustomDomainTab instance={baseInstance} />);

    const input = screen.getByTestId('input-domain');
    fireEvent.change(input, { target: { value: '  school.example.com  ' } });

    fireEvent.click(screen.getByTestId('btn-submit-domain'));

    expect(mutateFn).toHaveBeenCalledWith('school.example.com');
  });

  // ── PENDING_VERIFY state ────────────────────────────────────────────────

  it('renders pending state with TXT record instructions', () => {
    (useDomainStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      ...defaultDomainStatusReturn,
      data: {
        customDomain: 'school.example.com',
        verifyToken: 'kitehub-verify=abc-123-def',
        verifyRecord: 'Add TXT record: @ kitehub-verify=abc-123-def',
        status: 'PENDING_VERIFY' as const,
        verifiedAt: null,
        backupUrl: 'https://test-school.kitehub.me',
      },
    });

    render(<CustomDomainTab instance={baseInstance} />);

    expect(screen.getByTestId('domain-pending')).toBeInTheDocument();
    expect(screen.getByText('kitehub-verify=abc-123-def')).toBeInTheDocument();
    expect(screen.getByTestId('btn-verify')).toBeInTheDocument();
  });

  it('calls verify mutation when "Kiểm tra lại" button clicked', () => {
    const verifyFn = vi.fn();
    (useDomainStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      ...defaultDomainStatusReturn,
      data: {
        customDomain: 'school.example.com',
        verifyToken: 'kitehub-verify=abc-123',
        verifyRecord: null,
        status: 'PENDING_VERIFY' as const,
        verifiedAt: null,
        backupUrl: 'https://test-school.kitehub.me',
      },
    });
    (useVerifyDomain as ReturnType<typeof vi.fn>).mockReturnValue({
      ...defaultMutationReturn,
      mutate: verifyFn,
    });

    render(<CustomDomainTab instance={baseInstance} />);

    fireEvent.click(screen.getByTestId('btn-verify'));
    expect(verifyFn).toHaveBeenCalled();
  });

  // ── VERIFIED state ──────────────────────────────────────────────────────

  it('renders verified state with success message', () => {
    (useDomainStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      ...defaultDomainStatusReturn,
      data: {
        customDomain: 'school.example.com',
        verifyToken: 'kitehub-verify=abc-123',
        verifyRecord: null,
        status: 'VERIFIED' as const,
        verifiedAt: '2026-03-23T10:00:00Z',
        backupUrl: 'https://test-school.kitehub.me',
      },
    });

    render(<CustomDomainTab instance={baseInstance} />);

    expect(screen.getByTestId('domain-verified')).toBeInTheDocument();
    expect(screen.getByText(/Tên miền đã hoạt động/i)).toBeInTheDocument();
    expect(screen.getByText(/school\.example\.com/)).toBeInTheDocument();
  });
});
