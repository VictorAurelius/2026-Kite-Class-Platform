import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';

import { AssetReusePicker } from '../AssetReusePicker';

const mockUseAssets = vi.fn();
vi.mock('@/hooks/use-branding', () => ({
  useAssets: (id?: string) => mockUseAssets(id),
}));

const ASSETS = [
  { id: 'a1', instanceId: 'i1', type: 'BANNER', url: 'https://cdn/b1.webp', s3Key: 'k1', createdAt: '' },
  { id: 'a2', instanceId: 'i1', type: 'LOGO', url: 'https://cdn/l1.png', s3Key: 'k2', createdAt: '' },
];

describe('AssetReusePicker', () => {
  beforeEach(() => mockUseAssets.mockReset());

  it('shows the empty-state when there are no assets', () => {
    mockUseAssets.mockReturnValue({ data: [], isLoading: false });
    render(<AssetReusePicker instanceId="i1" onSelect={() => {}} />);
    expect(screen.getByTestId('asset-reuse-picker-empty')).toBeInTheDocument();
  });

  it('filters assets by the given type', () => {
    mockUseAssets.mockReturnValue({ data: ASSETS, isLoading: false });
    render(<AssetReusePicker instanceId="i1" type="BANNER" onSelect={() => {}} />);
    expect(screen.getByTestId('asset-reuse-item-a1')).toBeInTheDocument();
    expect(screen.queryByTestId('asset-reuse-item-a2')).not.toBeInTheDocument();
  });

  it('calls onSelect with the asset URL when a thumbnail is clicked', () => {
    mockUseAssets.mockReturnValue({ data: ASSETS, isLoading: false });
    const onSelect = vi.fn();
    render(<AssetReusePicker instanceId="i1" type="BANNER" onSelect={onSelect} />);
    fireEvent.click(screen.getByTestId('asset-reuse-item-a1'));
    expect(onSelect).toHaveBeenCalledWith('https://cdn/b1.webp');
  });

  it('calls onSelect(null) when "Không dùng" is clicked', () => {
    mockUseAssets.mockReturnValue({ data: ASSETS, isLoading: false });
    const onSelect = vi.fn();
    render(
      <AssetReusePicker
        instanceId="i1"
        type="BANNER"
        selectedUrl="https://cdn/b1.webp"
        onSelect={onSelect}
      />,
    );
    fireEvent.click(screen.getByTestId('asset-reuse-clear'));
    expect(onSelect).toHaveBeenCalledWith(null);
  });
});
