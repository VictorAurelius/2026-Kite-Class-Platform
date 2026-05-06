'use client';

/**
 * QualityGateWidget — Wave 32 Bucket D (GAP-272)
 *
 * Renders the Step 6 quality gate result (§5 quality gate per
 * ai-branding-guidelines.md):
 *   1. WCAG contrast ≥ 4.5:1
 *   2. CSS variables applied (no defaults remaining)
 *   3. No broken asset URLs (404)
 *   4. Visual regression vs baseline ≤ 20% diff
 *   5. Logo placement (not cropped, size appropriate)
 *
 * Pass variant: score ≥ 70, all checks green, deploy CTA.
 * Fail variant: score < 70, failed checks highlighted, auto-regen button.
 *
 * TODO(GAP-226): Replace mock with real WCAG measurement.
 * TODO(GAP-227): Replace mock with real visual regression diff.
 * TODO(GAP-228): Replace mock with real ML classifier scoring.
 */

import React from 'react';
import { CheckCircle, XCircle, AlertCircle, Contrast, Palette, Link, ScanEye, Image as ImageIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';

export interface QualityCheck {
  id: string;
  label: string;
  detail: string;
  passed: boolean;
  icon: React.ReactNode;
}

export interface QualityGateWidgetProps {
  /** Score 0–100. Pass threshold = 70 (per ai-branding-guidelines.md §5). */
  score: number;
  /** Individual check results. If omitted, mock data is used (dev mode). */
  checks?: QualityCheck[];
  /** Called when user clicks "Tạo lại tự động" on fail variant. */
  onAutoRegenerate?: () => void;
  /** Called when user clicks "Triển khai" on pass variant. */
  onDeploy?: () => void;
  /** Whether deploy is currently in progress. */
  isDeploying?: boolean;
}

const PASS_THRESHOLD = 70;

function mockChecks(score: number): QualityCheck[] {
  // Simulate passing/failing based on score bucket
  const allPass = score >= PASS_THRESHOLD;
  return [
    {
      id: 'wcag',
      label: 'WCAG Contrast',
      detail: allPass ? 'Đo: 4.7:1 — chuẩn AA cần ≥4.5:1' : 'Đo: 3.2:1 — thấp hơn ngưỡng AA (≥4.5:1)',
      passed: allPass || score >= 80,
      icon: <Contrast className="w-4 h-4" aria-hidden />,
    },
    {
      id: 'css-vars',
      label: 'CSS Variables',
      detail: allPass ? '24/24 biến theme đã áp dụng' : '18/24 biến — 6 biến vẫn là giá trị mặc định',
      passed: allPass || score >= 75,
      icon: <Palette className="w-4 h-4" aria-hidden />,
    },
    {
      id: 'asset-404',
      label: 'Asset 404',
      detail: allPass ? '0 link gãy / 12 assets' : '2 link gãy / 12 assets',
      passed: allPass || score >= 85,
      icon: <Link className="w-4 h-4" aria-hidden />,
    },
    {
      id: 'visual-regression',
      label: 'Visual Regression',
      detail: allPass ? 'Diff 8% — ngưỡng ≤20%' : 'Diff 34% — vượt ngưỡng 20%',
      passed: allPass || score >= 65,
      icon: <ScanEye className="w-4 h-4" aria-hidden />,
    },
    {
      id: 'logo-placement',
      label: 'Logo Placement',
      detail: allPass ? 'Logo 128×128 px — không bị cắt' : 'Logo bị cắt ở cạnh trái 12 px',
      passed: allPass || score >= 90,
      icon: <ImageIcon className="w-4 h-4" aria-hidden />,
    },
  ];
}

export function QualityGateWidget({
  score,
  checks,
  onAutoRegenerate,
  onDeploy,
  isDeploying = false,
}: QualityGateWidgetProps) {
  const resolvedChecks = checks ?? mockChecks(score);
  const passed = score >= PASS_THRESHOLD;
  const barWidth = Math.min(100, Math.max(0, score));

  return (
    <div
      className={`rounded-xl border p-5 space-y-4 ${
        passed
          ? 'bg-emerald-50 border-emerald-200'
          : 'bg-red-50 border-red-200'
      }`}
      role="region"
      aria-label={`Quality gate: ${passed ? 'đạt' : 'không đạt'} — ${score}/100`}
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-wider text-muted-foreground font-semibold mb-1">
            Điểm chất lượng tổng
          </p>
          <p className="text-4xl font-black leading-none">
            {score}
            <span className="text-xl font-normal text-muted-foreground">/100</span>
          </p>
        </div>
        <span
          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-bold ${
            passed
              ? 'bg-emerald-100 text-emerald-800'
              : 'bg-red-100 text-red-800'
          }`}
          role="status"
          aria-label={passed ? 'Đạt yêu cầu' : 'Không đạt'}
        >
          {passed ? (
            <CheckCircle className="w-4 h-4" aria-hidden />
          ) : (
            <XCircle className="w-4 h-4" aria-hidden />
          )}
          {passed ? 'ĐẠT YÊU CẦU' : 'CHƯA ĐẠT'}
        </span>
      </div>

      {/* Progress bar */}
      <div
        className="w-full h-2 rounded-full bg-white/60 overflow-hidden"
        aria-hidden
      >
        <div
          className={`h-full rounded-full transition-all duration-700 ${
            passed ? 'bg-emerald-500' : 'bg-red-500'
          }`}
          style={{ width: `${barWidth}%` }}
        />
      </div>

      {/* Check rows */}
      <div className="grid sm:grid-cols-2 gap-2">
        {resolvedChecks.map((check) => (
          <div
            key={check.id}
            className={`p-3 rounded-md border text-sm ${
              check.passed
                ? 'bg-emerald-50 border-emerald-200'
                : 'bg-red-50 border-red-200'
            }`}
          >
            <div
              className={`flex items-center gap-2 font-semibold mb-1 ${
                check.passed ? 'text-emerald-900' : 'text-red-900'
              }`}
            >
              {check.icon}
              {check.label}
              {check.passed ? (
                <CheckCircle className="w-3.5 h-3.5 ml-auto text-emerald-600" aria-label="Đạt" />
              ) : (
                <XCircle className="w-3.5 h-3.5 ml-auto text-red-600" aria-label="Không đạt" />
              )}
            </div>
            <p
              className={`text-xs ${
                check.passed ? 'text-emerald-800' : 'text-red-800'
              }`}
            >
              {check.detail}
            </p>
          </div>
        ))}
      </div>

      {/* Threshold info */}
      {!passed && (
        <div
          className="flex items-start gap-2 p-3 rounded-md bg-amber-50 border border-amber-200 text-amber-900 text-sm"
          role="alert"
          aria-live="polite"
        >
          <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" aria-hidden />
          <p>
            Điểm cần đạt ≥ {PASS_THRESHOLD}/100 để triển khai. Hiện tại {score}/100.
            Nhấn &ldquo;Tạo lại tự động&rdquo; để AI thử lại với tham số khác.
          </p>
        </div>
      )}

      {/* CTAs */}
      <div className="flex gap-3 pt-1">
        {passed ? (
          <Button
            onClick={onDeploy}
            disabled={isDeploying}
            className="bg-emerald-600 hover:bg-emerald-700 text-white"
          >
            {isDeploying ? 'Đang triển khai…' : 'Triển khai trang web'}
          </Button>
        ) : (
          <Button
            onClick={onAutoRegenerate}
            variant="destructive"
            className="bg-red-600 hover:bg-red-700"
          >
            Tạo lại tự động
          </Button>
        )}
      </div>
    </div>
  );
}
