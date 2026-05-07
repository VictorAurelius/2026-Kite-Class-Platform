import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import {
  QualityGateWidget,
  type QualityGateReport,
} from '../QualityGateWidget';

const PASS_REPORT: QualityGateReport = {
  score: 95,
  checks: [
    { id: 'WCAG_CONTRAST', detail: 'Đo: 4.7:1 — chuẩn AA cần ≥4.5:1', passed: true },
    { id: 'CSS_VARIABLES', detail: '24/24 biến theme đã áp dụng', passed: true },
    { id: 'ASSET_404', detail: '0 link gãy / 12 assets', passed: true },
    { id: 'VISUAL_REGRESSION', detail: '12% diff — cần ≤20%', passed: true },
    { id: 'LOGO_PLACEMENT', detail: 'Logo 32px · góc trái · không bị crop', passed: true },
  ],
};

const FAIL_REPORT: QualityGateReport = {
  score: 65,
  checks: [
    { id: 'WCAG_CONTRAST', detail: 'Đo: 3.2:1 — text đỏ trên nền cam', passed: false },
    { id: 'CSS_VARIABLES', detail: '24/24 biến theme đã áp dụng', passed: true },
    { id: 'ASSET_404', detail: '0 link gãy / 12 assets', passed: true },
    { id: 'VISUAL_REGRESSION', detail: 'Diff: 34% — hero crop logo', passed: false },
    { id: 'LOGO_PLACEMENT', detail: 'Logo bị crop 14% bên trái', passed: false },
  ],
};

describe('QualityGateWidget — pass variant', () => {
  it('renders 5 checks all PASS + PASS badge + deploy CTA', () => {
    const onDeploy = vi.fn();
    render(<QualityGateWidget report={PASS_REPORT} onDeploy={onDeploy} />);

    // Score
    expect(screen.getByTestId('quality-gate-score')).toHaveTextContent('95');
    // PASS badge
    expect(screen.getByTestId('quality-gate-pass-badge')).toBeInTheDocument();
    // No fail badge
    expect(screen.queryByTestId('quality-gate-fail-badge')).not.toBeInTheDocument();
    // 5 checks rendered
    const checks = screen.getAllByTestId(/^quality-check-/);
    expect(checks).toHaveLength(5);
    checks.forEach((el) => {
      expect(el).toHaveAttribute('data-passed', 'true');
    });
    // Deploy CTA
    const deployButton = screen.getByTestId('quality-gate-deploy-button');
    fireEvent.click(deployButton);
    expect(onDeploy).toHaveBeenCalledTimes(1);
  });
});

describe('QualityGateWidget — fail variant', () => {
  it('renders 65/100 + FAIL badge + auto-regenerate button', () => {
    const onAutoRegenerate = vi.fn();
    const onEditManually = vi.fn();

    render(
      <QualityGateWidget
        report={FAIL_REPORT}
        onAutoRegenerate={onAutoRegenerate}
        onEditManually={onEditManually}
        regenerateQuotaText="3"
      />
    );

    expect(screen.getByTestId('quality-gate-score')).toHaveTextContent('65');
    expect(screen.getByTestId('quality-gate-fail-badge')).toBeInTheDocument();
    // No pass badge
    expect(screen.queryByTestId('quality-gate-pass-badge')).not.toBeInTheDocument();
    // 3 fail + 2 pass = 5
    const checks = screen.getAllByTestId(/^quality-check-/);
    expect(checks).toHaveLength(5);
    const failedChecks = checks.filter(
      (el) => el.getAttribute('data-passed') === 'false'
    );
    expect(failedChecks).toHaveLength(3);
    // Auto-regenerate works
    const autoButton = screen.getByTestId('quality-gate-auto-regenerate-button');
    expect(autoButton).toHaveTextContent(/Tự động tạo lại/);
    fireEvent.click(autoButton);
    expect(onAutoRegenerate).toHaveBeenCalledTimes(1);
    // Edit manually works
    fireEvent.click(screen.getByTestId('quality-gate-edit-button'));
    expect(onEditManually).toHaveBeenCalledTimes(1);
  });

  it('sorts FAIL checks first when overall failed', () => {
    render(<QualityGateWidget report={FAIL_REPORT} />);
    const checks = screen.getAllByTestId(/^quality-check-/);
    // First three should be the failures (WCAG_CONTRAST, VISUAL_REGRESSION, LOGO_PLACEMENT)
    expect(checks[0]).toHaveAttribute('data-passed', 'false');
    expect(checks[1]).toHaveAttribute('data-passed', 'false');
    expect(checks[2]).toHaveAttribute('data-passed', 'false');
    expect(checks[3]).toHaveAttribute('data-passed', 'true');
    expect(checks[4]).toHaveAttribute('data-passed', 'true');
  });
});
