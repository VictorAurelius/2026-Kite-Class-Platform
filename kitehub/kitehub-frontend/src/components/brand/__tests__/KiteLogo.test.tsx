/**
 * Component tests for KiteLogo.
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { KiteLogo } from '../KiteLogo';

describe('KiteLogo', () => {
  describe('rendering', () => {
    it('renders SVG icon', () => {
      render(<KiteLogo />);
      const svg = document.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('renders text by default', () => {
      render(<KiteLogo />);
      expect(screen.getByText('Kite')).toBeInTheDocument();
      expect(screen.getByText('Hub')).toBeInTheDocument();
    });

    it('hides text when showText is false', () => {
      render(<KiteLogo showText={false} />);
      expect(screen.queryByText('Kite')).not.toBeInTheDocument();
      expect(screen.queryByText('Hub')).not.toBeInTheDocument();
    });

    it('applies flex container styles', () => {
      const { container } = render(<KiteLogo />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('flex', 'items-center', 'gap-2');
    });
  });

  describe('sizes', () => {
    it('renders small size (24px icon)', () => {
      render(<KiteLogo size="sm" />);
      const svg = document.querySelector('svg');
      expect(svg).toHaveAttribute('width', '24');
      expect(svg).toHaveAttribute('height', '24');
    });

    it('renders medium size (32px icon) by default', () => {
      render(<KiteLogo />);
      const svg = document.querySelector('svg');
      expect(svg).toHaveAttribute('width', '32');
      expect(svg).toHaveAttribute('height', '32');
    });

    it('renders large size (48px icon)', () => {
      render(<KiteLogo size="lg" />);
      const svg = document.querySelector('svg');
      expect(svg).toHaveAttribute('width', '48');
      expect(svg).toHaveAttribute('height', '48');
    });

    it('applies correct text size for small', () => {
      render(<KiteLogo size="sm" />);
      const textSpan = screen.getByText('Kite').parentElement;
      expect(textSpan).toHaveClass('text-lg');
    });

    it('applies correct text size for medium', () => {
      render(<KiteLogo size="md" />);
      const textSpan = screen.getByText('Kite').parentElement;
      expect(textSpan).toHaveClass('text-xl');
    });

    it('applies correct text size for large', () => {
      render(<KiteLogo size="lg" />);
      const textSpan = screen.getByText('Kite').parentElement;
      expect(textSpan).toHaveClass('text-3xl');
    });
  });

  describe('customization', () => {
    it('applies custom className', () => {
      const { container } = render(<KiteLogo className="custom-class" />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('custom-class');
    });

    it('merges custom className with default classes', () => {
      const { container } = render(<KiteLogo className="my-custom" />);
      const wrapper = container.firstChild;
      expect(wrapper).toHaveClass('flex', 'items-center', 'gap-2', 'my-custom');
    });
  });

  describe('accessibility', () => {
    it('renders SVG with proper viewBox', () => {
      render(<KiteLogo />);
      const svg = document.querySelector('svg');
      expect(svg).toHaveAttribute('viewBox', '0 0 48 48');
    });

    it('includes kite shape elements', () => {
      render(<KiteLogo />);
      // Diamond shape path
      const diamondPath = document.querySelector('path[d="M24 4L40 24L24 44L8 24L24 4Z"]');
      expect(diamondPath).toBeInTheDocument();
      // Center circle
      const centerCircle = document.querySelector('circle[cx="24"][cy="24"][r="4"]');
      expect(centerCircle).toBeInTheDocument();
    });
  });
});
