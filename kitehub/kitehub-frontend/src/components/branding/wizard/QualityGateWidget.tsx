/**
 * QualityGateWidget — Step 6 §5 Quality Gate widget for AI Branding Wizard v2.
 *
 * Wave 32 Bucket D — Direction C 6-step refactor.
 *
 * Per `ai-branding-guidelines.md` §5: 5 automated checks before DEPLOY transition.
 *   1. WCAG contrast ratio ≥ 4.5:1
 *   2. CSS variables applied
 *   3. No broken asset URLs (404)
 *   4. Visual regression vs baseline ≤ 20% diff
 *   5. Logo placement (size, crop, ratio)
 *
 * SCAFFOLD STATUS — real measurement deferred to follow-up gaps:
 *   - // TODO(GAP-226): wire WCAG contrast measurement to real automated tooling
 *     (currently the score + breakdown comes from the parent — caller passes the
 *     mocked-or-real measurement; this component is presentational only).
 *   - // TODO(GAP-227): wire visual regression diff to real automated baseline
 *     comparison (Playwright + pixelmatch or similar).
 *   - // TODO(GAP-228): wire ML classifier scoring for logo-placement &
 *     overall composition once the classifier service ships.
 *
 * The component itself is FULLY presentational. It accepts a `report` prop
 * containing the score + per-check verdicts. Mock generation is the parent's
 * responsibility (and is itself flagged TODO until backend `/quality-gate/score`
 * endpoint ships — see GAP-272c).
 *
 * Threshold per `ai-branding-guidelines.md` §5: score < 70 = FAIL = block DEPLOY.
 */

'use client';

import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  AlertTriangle,
  CheckCircle2,
  Contrast,
  Image as ImageIcon,
  Info,
  Layout,
  Link as LinkIcon,
  Palette,
  RefreshCw,
  Rocket,
  Settings as SettingsIcon,
  Sparkles,
  XCircle,
} from 'lucide-react';

/** Threshold under which DEPLOY must be blocked (per `ai-branding-guidelines.md` §5). */
export const QUALITY_GATE_PASS_THRESHOLD = 70;

/** The 5 mandated checks per `ai-branding-guidelines.md` §5. */
export type QualityCheckId =
  | 'WCAG_CONTRAST'
  | 'CSS_VARIABLES'
  | 'ASSET_404'
  | 'VISUAL_REGRESSION'
  | 'LOGO_PLACEMENT';

export interface QualityCheck {
  id: QualityCheckId;
  /** Human-readable status text — e.g. "Đo: 4.7:1 — chuẩn AA cần ≥4.5:1". */
  detail: string;
  /** Whether this check passed. */
  passed: boolean;
}

export interface QualityGateReport {
  /** Overall score 0..100 — under `QUALITY_GATE_PASS_THRESHOLD` = FAIL = block deploy. */
  score: number;
  /** Per-check verdicts. Order independent — component sorts FAILs first when failing. */
  checks: readonly QualityCheck[];
}

export interface QualityGateWidgetProps {
  /** Score + per-check breakdown. Caller is responsible for sourcing this from backend. */
  report: QualityGateReport;
  /** Click handler when score < threshold and user opts to auto-regenerate. */
  onAutoRegenerate?: () => void;
  /** Click handler when user opts to edit manually after a fail. */
  onEditManually?: () => void;
  /** Click handler when user clicks regenerate (PASS variant). */
  onRegenerate?: () => void;
  /** Click handler for the deploy CTA (PASS variant). */
  onDeploy?: () => void;
  /** Remaining regenerate quota — display string only, e.g. "3/3" or "Không giới hạn". */
  regenerateQuotaText?: string;
}

/**
 * Per-check label + icon, language-stable lookup table (per `design-patterns.md` §3.3
 * "no switch cascade"). Adding a 6th check = add a row; no logic branches change.
 */
const CHECK_META: Readonly<Record<QualityCheckId, { label: string; Icon: typeof Contrast }>> =
  Object.freeze({
    WCAG_CONTRAST: { label: 'WCAG Contrast', Icon: Contrast },
    CSS_VARIABLES: { label: 'CSS Variables', Icon: Palette },
    ASSET_404: { label: 'Asset 404', Icon: LinkIcon },
    VISUAL_REGRESSION: { label: 'Visual Regression', Icon: ImageIcon },
    LOGO_PLACEMENT: { label: 'Logo Placement', Icon: Layout },
  }) as Record<QualityCheckId, { label: string; Icon: typeof Contrast }>;

export function QualityGateWidget(props: QualityGateWidgetProps) {
  const {
    report,
    onAutoRegenerate,
    onEditManually,
    onRegenerate,
    onDeploy,
    regenerateQuotaText,
  } = props;

  const passed = report.score >= QUALITY_GATE_PASS_THRESHOLD;
  const sortedChecks = passed
    ? report.checks
    : // FAILs first when overall failed — easier to spot what broke
      [...report.checks].sort((a, b) => Number(a.passed) - Number(b.passed));

  return (
    <Card className="p-6 md:p-8 max-w-2xl w-full mx-auto" data-testid="quality-gate-widget">
      {/* Header */}
      <div className="mb-6">
        <p
          className="text-sm font-semibold uppercase tracking-wide mb-1"
          style={{ color: passed ? 'hsl(var(--primary))' : 'hsl(var(--destructive))' }}
        >
          {passed
            ? 'Quality gate · 5 kiểm tra · Bước 5'
            : 'Quality gate · 5 kiểm tra · Cần xử lý'}
        </p>
        <h2 className="text-2xl font-bold text-foreground mb-2">
          {passed
            ? `Trang web đạt ${report.score}/100 — sẵn sàng triển khai`
            : `Trang web đạt ${report.score}/100 — chưa thể triển khai`}
        </h2>
        <p className="text-muted-foreground text-sm">
          {passed
            ? 'AI đã chạy 5 kiểm tra tự động. Tất cả pass. Bạn có thể triển khai trang web.'
            : `${sortedChecks.filter((c) => !c.passed).length}/5 kiểm tra fail. Hệ thống sẽ tự tạo lại để cải thiện. Bạn có thể chấp nhận hoặc chỉnh thủ công.`}
        </p>
      </div>

      {/* Score block */}
      <div
        className="rounded-lg border p-4"
        style={{
          background: passed ? 'hsl(142 71% 45% / 0.05)' : 'hsl(0 84% 60% / 0.05)',
          borderColor: passed ? 'hsl(142 71% 45% / 0.3)' : 'hsl(0 84% 60% / 0.3)',
        }}
        data-testid="quality-gate-score-block"
      >
        <div className="flex items-start justify-between mb-3">
          <div>
            <p className="text-xs uppercase tracking-wider text-muted-foreground font-semibold">
              Điểm chất lượng tổng
            </p>
            <p className="text-4xl font-extrabold leading-none mt-1">
              <span data-testid="quality-gate-score">{report.score}</span>
              <span className="text-base text-muted-foreground font-medium">/100</span>
            </p>
          </div>
          {passed ? (
            <Badge
              data-testid="quality-gate-pass-badge"
              variant="default"
              className="bg-emerald-100 text-emerald-800 hover:bg-emerald-100 flex items-center gap-1.5 px-3 py-1.5"
            >
              <CheckCircle2 className="w-4 h-4" />
              ĐẠT YÊU CẦU
            </Badge>
          ) : (
            <Badge
              data-testid="quality-gate-fail-badge"
              variant="destructive"
              className="flex items-center gap-1.5 px-3 py-1.5"
            >
              <AlertTriangle className="w-4 h-4" />
              CHƯA ĐẠT (cần ≥{QUALITY_GATE_PASS_THRESHOLD})
            </Badge>
          )}
        </div>

        {/* Score progress bar */}
        <div
          className="h-2 rounded-full overflow-hidden bg-muted/30 mb-4"
          aria-hidden="true"
        >
          <div
            className="h-full transition-all"
            style={{
              width: `${Math.max(0, Math.min(100, report.score))}%`,
              background: passed ? 'hsl(142 71% 45%)' : 'hsl(0 84% 60%)',
            }}
          />
        </div>

        {/* 5-check breakdown */}
        <div className="grid sm:grid-cols-2 gap-2" data-testid="quality-gate-checks">
          {sortedChecks.map((check, idx) => {
            const meta = CHECK_META[check.id];
            const isLastOdd =
              idx === sortedChecks.length - 1 && sortedChecks.length % 2 === 1;
            return (
              <div
                key={check.id}
                className={`p-3 rounded-md border ${
                  check.passed
                    ? 'bg-emerald-50 border-emerald-200'
                    : 'bg-red-50 border-red-300'
                } ${isLastOdd ? 'sm:col-span-2' : ''}`}
                data-testid={`quality-check-${check.id.toLowerCase()}`}
                data-passed={check.passed}
              >
                <div
                  className={`flex items-center gap-2 font-semibold text-sm mb-1 ${
                    check.passed ? 'text-emerald-900' : 'text-red-900'
                  }`}
                >
                  {check.passed ? (
                    <CheckCircle2 className="w-4 h-4" />
                  ) : (
                    <XCircle className="w-4 h-4" />
                  )}
                  <meta.Icon className="w-4 h-4" />
                  <span>{meta.label}</span>
                  <span className="ml-auto text-xs font-bold">
                    {check.passed ? 'PASS' : 'FAIL'}
                  </span>
                </div>
                <p
                  className={`text-xs ${
                    check.passed ? 'text-emerald-800' : 'text-red-800'
                  }`}
                >
                  {check.detail}
                </p>
              </div>
            );
          })}
        </div>

        {/* Inline notice */}
        <div
          className="mt-4 p-3 rounded-md text-xs flex gap-2"
          style={
            passed
              ? {
                  background: 'hsl(199 89% 48% / 0.08)',
                  borderColor: 'hsl(199 89% 48% / 0.3)',
                  border: '1px solid hsl(199 89% 48% / 0.3)',
                  color: 'hsl(199 89% 32%)',
                }
              : {
                  background: 'hsl(38 92% 50% / 0.1)',
                  border: '1px solid hsl(38 92% 50% / 0.4)',
                }
          }
        >
          {passed ? (
            <>
              <Info className="w-4 h-4 mt-0.5 shrink-0" />
              <p>
                Quality gate chạy lại mỗi lần bạn bấm <strong>Tạo lại</strong>. Điểm dưới {QUALITY_GATE_PASS_THRESHOLD} sẽ chặn deploy — auto-regenerate hoặc đánh dấu FAILED.
              </p>
            </>
          ) : (
            <>
              <Sparkles
                className="w-5 h-5 mt-0.5 shrink-0"
                style={{ color: 'hsl(38 92% 50%)' }}
              />
              <div>
                <p className="font-semibold mb-1">Đề xuất tự động</p>
                <p className="text-muted-foreground">
                  AI sẽ thử lại với bảng màu khác + reposition logo. Mất ~30 giây.
                </p>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 mt-5">
        {passed ? (
          <>
            <Button
              type="button"
              variant="secondary"
              onClick={onRegenerate}
              data-testid="quality-gate-regenerate-button"
            >
              <RefreshCw className="w-4 h-4" />
              Tạo lại{regenerateQuotaText ? ` (còn ${regenerateQuotaText})` : ''}
            </Button>
            <Button
              type="button"
              className="flex-1"
              onClick={onDeploy}
              data-testid="quality-gate-deploy-button"
            >
              <Rocket className="w-4 h-4" />
              Triển khai trang web
            </Button>
          </>
        ) : (
          <>
            <Button
              type="button"
              variant="secondary"
              onClick={onEditManually}
              data-testid="quality-gate-edit-button"
            >
              <SettingsIcon className="w-4 h-4" />
              Chỉnh thủ công
            </Button>
            <Button
              type="button"
              className="flex-1"
              onClick={onAutoRegenerate}
              data-testid="quality-gate-auto-regenerate-button"
            >
              <Sparkles className="w-4 h-4" />
              Tự động tạo lại{regenerateQuotaText ? ` (dùng 1/${regenerateQuotaText})` : ''}
            </Button>
          </>
        )}
      </div>
    </Card>
  );
}
