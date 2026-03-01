/**
 * FeatureGate Component Tests
 *
 * @author KiteClass Team
 * @since 2026-02-23
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FeatureGate } from '../FeatureGate';
import { FeatureName, PricingTier, InstanceStatus } from '@/types/feature-detection';
import * as useFeatureDetectionModule from '@/hooks/useFeatureDetection';

// Mock useFeatureDetection hook
vi.mock('@/hooks/useFeatureDetection');

describe('FeatureGate', () => {
  const mockUseFeatureDetection = vi.mocked(useFeatureDetectionModule.useFeatureDetection);

  describe('Loading State', () => {
    it('should show skeleton when loading', () => {
      mockUseFeatureDetection.mockReturnValue({
        isLoading: true,
        error: null,
        config: undefined,
        hasFeature: vi.fn(),
        requireFeature: vi.fn(),
        getRequiredTier: vi.fn(),
      });

      render(
        <FeatureGate feature={FeatureName.STUDENTS}>
          <div>Protected Content</div>
        </FeatureGate>
      );

      expect(screen.getByRole('status')).toBeInTheDocument();
      expect(screen.getByLabelText('Loading feature availability')).toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    it('should show error alert when error occurs', () => {
      mockUseFeatureDetection.mockReturnValue({
        isLoading: false,
        error: new Error('Failed to load config'),
        config: undefined,
        hasFeature: vi.fn(),
        requireFeature: vi.fn(),
        getRequiredTier: vi.fn(),
      });

      render(
        <FeatureGate feature={FeatureName.STUDENTS}>
          <div>Protected Content</div>
        </FeatureGate>
      );

      expect(screen.getByText('Failed to load feature configuration')).toBeInTheDocument();
      expect(
        screen.getByText('Unable to verify feature availability. Please try again later.')
      ).toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });
  });

  describe('Feature Available', () => {
    it('should show children when feature is available', () => {
      mockUseFeatureDetection.mockReturnValue({
        isLoading: false,
        error: null,
        config: {
          instanceId: 'test-instance',
          tier: PricingTier.PREMIUM,
          features: {
            [FeatureName.STUDENTS]: true,
            [FeatureName.CLASSES]: true,
            [FeatureName.ATTENDANCE]: true,
            [FeatureName.ENGAGEMENT]: true,
            [FeatureName.AI_BRANDING]: true,
            [FeatureName.MEDIA]: true,
            [FeatureName.CUSTOM_DOMAIN]: true,
          },
          limitations: {
            maxStudents: Infinity,
            maxCourses: Infinity,
          },
          status: InstanceStatus.ACTIVE,
        },
        hasFeature: vi.fn(() => true),
        requireFeature: vi.fn(),
        getRequiredTier: vi.fn(),
      });

      render(
        <FeatureGate feature={FeatureName.STUDENTS}>
          <div>Protected Content</div>
        </FeatureGate>
      );

      expect(screen.getByText('Protected Content')).toBeInTheDocument();
      expect(screen.queryByText('Feature Not Available')).not.toBeInTheDocument();
    });
  });

  describe('Feature Not Available', () => {
    it('should show upgrade prompt when feature is not available', () => {
      mockUseFeatureDetection.mockReturnValue({
        isLoading: false,
        error: null,
        config: {
          instanceId: 'test-instance',
          tier: PricingTier.BASIC,
          features: {
            [FeatureName.STUDENTS]: true,
            [FeatureName.CLASSES]: true,
            [FeatureName.ATTENDANCE]: true,
            [FeatureName.ENGAGEMENT]: false,
            [FeatureName.AI_BRANDING]: false,
            [FeatureName.MEDIA]: false,
            [FeatureName.CUSTOM_DOMAIN]: false,
          },
          limitations: {
            maxStudents: 50,
            maxCourses: 10,
          },
          status: InstanceStatus.ACTIVE,
        },
        hasFeature: vi.fn(() => false),
        requireFeature: vi.fn(),
        getRequiredTier: vi.fn(() => PricingTier.PREMIUM),
      });

      render(
        <FeatureGate feature={FeatureName.AI_BRANDING}>
          <div>Protected Content</div>
        </FeatureGate>
      );

      expect(screen.getByText('Feature Not Available')).toBeInTheDocument();
      expect(
        screen.getByText(/is not available on your current plan/i)
      ).toBeInTheDocument();
      expect(screen.getByText(/requires PREMIUM tier or higher/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /upgrade to premium/i })).toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });

    it('should show fallback content when provided', () => {
      mockUseFeatureDetection.mockReturnValue({
        isLoading: false,
        error: null,
        config: {
          instanceId: 'test-instance',
          tier: PricingTier.BASIC,
          features: {
            [FeatureName.STUDENTS]: true,
            [FeatureName.CLASSES]: true,
            [FeatureName.ATTENDANCE]: true,
            [FeatureName.ENGAGEMENT]: false,
            [FeatureName.AI_BRANDING]: false,
            [FeatureName.MEDIA]: false,
            [FeatureName.CUSTOM_DOMAIN]: false,
          },
          limitations: {
            maxStudents: 50,
            maxCourses: 10,
          },
          status: InstanceStatus.ACTIVE,
        },
        hasFeature: vi.fn(() => false),
        requireFeature: vi.fn(),
        getRequiredTier: vi.fn(),
      });

      render(
        <FeatureGate
          feature={FeatureName.AI_BRANDING}
          fallback={<div>Custom Fallback Content</div>}
        >
          <div>Protected Content</div>
        </FeatureGate>
      );

      expect(screen.getByText('Custom Fallback Content')).toBeInTheDocument();
      expect(screen.queryByText('Feature Not Available')).not.toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });

    it('should show upgrade button without tier when getRequiredTier returns null', () => {
      mockUseFeatureDetection.mockReturnValue({
        isLoading: false,
        error: null,
        config: {
          instanceId: 'test-instance',
          tier: PricingTier.BASIC,
          features: {
            [FeatureName.STUDENTS]: true,
            [FeatureName.CLASSES]: true,
            [FeatureName.ATTENDANCE]: true,
            [FeatureName.ENGAGEMENT]: false,
            [FeatureName.AI_BRANDING]: false,
            [FeatureName.MEDIA]: false,
            [FeatureName.CUSTOM_DOMAIN]: false,
          },
          limitations: {
            maxStudents: 50,
            maxCourses: 10,
          },
          status: InstanceStatus.ACTIVE,
        },
        hasFeature: vi.fn(() => false),
        requireFeature: vi.fn(),
        getRequiredTier: vi.fn(() => null),
      });

      render(
        <FeatureGate feature={FeatureName.AI_BRANDING}>
          <div>Protected Content</div>
        </FeatureGate>
      );

      expect(screen.getByRole('button', { name: /upgrade to premium/i })).toBeInTheDocument();
    });
  });
});
