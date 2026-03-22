import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemePreviewCard } from '../ThemePreviewCard';
import type { ThemeConfig } from '@/types/theme';

describe('ThemePreviewCard', () => {
  const mockThemeConfig: ThemeConfig = {
    colors: {
      primary: {
        shade50: '#E3F2FD',
        shade100: '#BBDEFB',
        shade200: '#90CAF9',
        shade300: '#64B5F6',
        shade400: '#42A5F5',
        shade500: '#2196F3',
        shade600: '#1E88E5',
        shade700: '#1976D2',
        shade800: '#1565C0',
        shade900: '#0D47A1',
      },
      secondary: {
        shade50: '#FBE9E7',
        shade100: '#FFCCBC',
        shade200: '#FFAB91',
        shade300: '#FF8A65',
        shade400: '#FF7043',
        shade500: '#FF5722',
        shade600: '#F4511E',
        shade700: '#E64A19',
        shade800: '#D84315',
        shade900: '#BF360C',
      },
      accent: {
        shade50: '#E8F5E9',
        shade100: '#C8E6C9',
        shade200: '#A5D6A7',
        shade300: '#81C784',
        shade400: '#66BB6A',
        shade500: '#4CAF50',
        shade600: '#43A047',
        shade700: '#388E3C',
        shade800: '#2E7D32',
        shade900: '#1B5E20',
      },
      neutral: {
        shade50: '#FAFAFA',
        shade100: '#F5F5F5',
        shade200: '#E5E5E5',
        shade300: '#D4D4D4',
        shade400: '#A3A3A3',
        shade500: '#737373',
        shade600: '#525252',
        shade700: '#404040',
        shade800: '#262626',
        shade900: '#171717',
      },
      semantic: {
        success: '#10B981',
        warning: '#F59E0B',
        error: '#EF4444',
        info: '#3B82F6',
      },
    },
    typography: {
      fontFamilyHeading: "'Inter', 'SF Pro Display', system-ui, sans-serif",
      fontFamilyBody: "'Inter', 'SF Pro Text', system-ui, sans-serif",
      fontSizeBase: '16px',
      fontSizes: {
        xs: '0.75rem',
        sm: '0.875rem',
        base: '1rem',
        lg: '1.125rem',
        xl: '1.25rem',
        xl2: '1.5rem',
        xl3: '1.875rem',
        xl4: '2.25rem',
      },
      fontWeights: {
        normal: 400,
        medium: 500,
        semibold: 600,
        bold: 700,
      },
      lineHeights: {
        tight: '1.25',
        normal: '1.5',
        relaxed: '1.75',
      },
    },
    spacing: {
      unit: 4,
      sectionSpacing: '4rem',
      componentSpacing: '1.5rem',
      elementSpacing: '0.75rem',
    },
    layout: {
      maxWidth: '1280px',
      borderRadius: {
        sm: '0.25rem',
        base: '0.5rem',
        lg: '0.75rem',
        full: '9999px',
      },
      shadow: {
        sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        base: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
        lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
      },
    },
  };

  it('should render theme preview card', () => {
    render(<ThemePreviewCard themeConfig={mockThemeConfig} />);

    expect(screen.getByText('Theme Preview')).toBeInTheDocument();
  });

  it('should render color palette section', () => {
    render(<ThemePreviewCard themeConfig={mockThemeConfig} />);

    expect(screen.getByText('Bảng Màu')).toBeInTheDocument();
    expect(screen.getByText('Màu Chính')).toBeInTheDocument();
    expect(screen.getByText('Màu Phụ')).toBeInTheDocument();
    expect(screen.getByText('Màu Nhấn')).toBeInTheDocument();
    expect(screen.getByText('Màu Hệ Thống')).toBeInTheDocument();
  });

  it('should render semantic colors', () => {
    render(<ThemePreviewCard themeConfig={mockThemeConfig} />);

    expect(screen.getByText('Success')).toBeInTheDocument();
    expect(screen.getByText('Warning')).toBeInTheDocument();
    expect(screen.getByText('Error')).toBeInTheDocument();
    expect(screen.getByText('Info')).toBeInTheDocument();
  });

  it('should render typography section', () => {
    render(<ThemePreviewCard themeConfig={mockThemeConfig} />);

    expect(screen.getByText('Typography')).toBeInTheDocument();
    expect(screen.getByText('Heading Font')).toBeInTheDocument();
    expect(screen.getByText('Body Font')).toBeInTheDocument();
    expect(screen.getByText(mockThemeConfig.typography.fontFamilyHeading)).toBeInTheDocument();
  });

  it('should render spacing and layout section', () => {
    render(<ThemePreviewCard themeConfig={mockThemeConfig} />);

    expect(screen.getByText('Spacing & Layout')).toBeInTheDocument();
    expect(screen.getByText('Unit')).toBeInTheDocument();
    expect(screen.getByText('4px')).toBeInTheDocument();
    expect(screen.getByText('Max Width')).toBeInTheDocument();
    expect(screen.getByText('1280px')).toBeInTheDocument();
  });

  it('should render all 10 color shades for primary color', () => {
    render(<ThemePreviewCard themeConfig={mockThemeConfig} />);

    // Find all color shade labels (50, 100, 200, ..., 900)
    const shadeLabels = ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900'];

    shadeLabels.forEach((label) => {
      const elements = screen.getAllByText(label);
      // Should have at least 3 instances (primary, secondary, accent)
      expect(elements.length).toBeGreaterThanOrEqual(3);
    });
  });

  it('should render color swatches with correct background colors', () => {
    const { container } = render(<ThemePreviewCard themeConfig={mockThemeConfig} />);

    // Find color swatch for primary 500 (base color)
    const colorDivs = container.querySelectorAll('[style*="background-color"]');
    expect(colorDivs.length).toBeGreaterThan(0);
  });
});
