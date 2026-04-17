/**
 * Component tests for StepIndicator.
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { StepIndicator } from '../StepIndicator';

describe('StepIndicator', () => {
  describe('rendering', () => {
    it('renders step labels', () => {
      render(<StepIndicator currentStep={1} totalSteps={2} />);
      expect(screen.getByText('Chọn gói')).toBeInTheDocument();
      expect(screen.getByText('Xác nhận')).toBeInTheDocument();
    });

    it('renders centered container', () => {
      const { container } = render(<StepIndicator currentStep={1} totalSteps={2} />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('flex', 'items-center', 'justify-center');
    });
  });

  describe('step states', () => {
    it('shows step 1 as current when currentStep is 1', () => {
      render(<StepIndicator currentStep={1} totalSteps={2} />);
      // Step 1 should show number "1" (not checkmark)
      expect(screen.getByText('1')).toBeInTheDocument();
    });

    it('shows checkmark for completed steps', () => {
      render(<StepIndicator currentStep={2} totalSteps={2} />);
      // Step 1 should show checkmark SVG (Check icon)
      const checkIcon = document.querySelector('.lucide-check');
      expect(checkIcon).toBeInTheDocument();
    });

    it('shows step 2 as current when currentStep is 2', () => {
      render(<StepIndicator currentStep={2} totalSteps={2} />);
      // Step 2 should show number "2"
      expect(screen.getByText('2')).toBeInTheDocument();
    });
  });

  describe('visual indicators', () => {
    it('applies ring styles to current step', () => {
      const { container } = render(<StepIndicator currentStep={1} totalSteps={2} />);
      const currentStepCircle = container.querySelector('.ring-4');
      expect(currentStepCircle).toBeInTheDocument();
    });

    it('renders connector line between steps', () => {
      const { container } = render(<StepIndicator currentStep={1} totalSteps={2} />);
      const connectorLine = container.querySelector('.h-0\\.5.w-24');
      expect(connectorLine).toBeInTheDocument();
    });

    it('applies primary color to completed connector line', () => {
      const { container } = render(<StepIndicator currentStep={2} totalSteps={2} />);
      const connectorLine = container.querySelector('.h-0\\.5.w-24.bg-primary');
      expect(connectorLine).toBeInTheDocument();
    });

    it('applies muted color to incomplete connector line', () => {
      const { container } = render(<StepIndicator currentStep={1} totalSteps={2} />);
      const connectorLine = container.querySelector('.h-0\\.5.w-24.bg-muted');
      expect(connectorLine).toBeInTheDocument();
    });
  });
});
