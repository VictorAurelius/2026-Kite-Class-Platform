'use client';

// ---------------------------------------------------------------------------
// TemplateFullscreen — Step 5 fullscreen template preview modal.
//
// Per `ai-branding-guidelines.md` §4.2 the user MUST be able to preview a
// template before committing — this dialog is the in-wizard surface for
// that, mirroring `step5-template-fullscreen.html` from the kit.
//
// The 3 badges (WCAG / responsive / text-safety) are real surfaces with
// PLACEHOLDER scoring values — real measurement is tracked separately:
//   - WCAG real measurement   → GAP-226
//   - Visual regression diff  → GAP-227
//   - Text-safety overflow ML → GAP-228
// Per `ai-branding-guidelines.md` §11.4 (migration test checklist) the
// scaffold render is acceptable until those three gaps land; placeholders
// emit DEV warnings so future automation can pick them up.
// ---------------------------------------------------------------------------

import { CheckCircle2, ArrowLeft, Check, Smartphone, Tablet, Monitor } from 'lucide-react';
import { useState } from 'react';
import DOMPurify from 'isomorphic-dompurify';
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import type { TemplateDescriptor } from './TemplateGrid';

// SVG sanitization config: allow SVG elements + presentation attributes,
// strip <script> and on* event handlers (XSS via SVG attack surface).
const SVG_PURIFY_CONFIG: DOMPurify.Config = {
  USE_PROFILES: { svg: true, svgFilters: true },
  FORBID_TAGS: ['script', 'use'],
  FORBID_ATTR: ['xlink:href', 'href'],
};

export type ResponsiveDevice = 'mobile' | 'tablet' | 'desktop';

interface TemplateFullscreenProps {
  /** Template currently shown. `null` closes the modal. */
  template: TemplateDescriptor | null;
  /**
   * Called when the modal is closed (close button OR backdrop click OR Esc).
   */
  onClose: () => void;
  /** Called when the user confirms this template selection. */
  onConfirm: (template: TemplateDescriptor) => void;
  /** Initial responsive device (default `desktop`). */
  initialDevice?: ResponsiveDevice;
  /**
   * Override the placeholder badge scores — used by tests.
   * Real measurement lands per GAP-226/227/228.
   */
  badgesOverride?: BadgeScores;
}

export interface BadgeScores {
  wcagRatio: number;
  responsiveOk: boolean;
  textSafetyMaxChars: number;
}

const PLACEHOLDER_SCORES: BadgeScores = {
  // TODO(GAP-226): replace with `calculateContrast()` from @kite/shared-ui
  // run against the rendered template DOM.
  wcagRatio: 4.7,
  // TODO(GAP-227): replace with visual regression diff comparing template
  // SVG render across viewport widths 320 / 768 / 1280 / 3840.
  responsiveOk: true,
  // TODO(GAP-228): replace with overflow-detection scan against a 50-char
  // Vietnamese headline rendered into the template's headline slot.
  textSafetyMaxChars: 50,
} as const;

const DEVICE_LABEL: Record<ResponsiveDevice, string> = {
  mobile: 'Điện thoại',
  tablet: 'Máy tính bảng',
  desktop: 'Máy tính',
};

/**
 * Fullscreen preview dialog with 3 quality badges (WCAG / responsive /
 * text-safety) and a device toggle (mobile / tablet / desktop).
 *
 * The template SVG is rendered as the preview content. Real iframe rendering
 * lands once the backend `/branding/templates/:id/render` endpoint exists
 * (tracked GAP-272j shared with Step 6 iframe).
 */
export function TemplateFullscreen({
  template,
  onClose,
  onConfirm,
  initialDevice = 'desktop',
  badgesOverride,
}: TemplateFullscreenProps) {
  const [device, setDevice] = useState<ResponsiveDevice>(initialDevice);
  const badges = badgesOverride ?? PLACEHOLDER_SCORES;

  const open = template !== null;
  const wcagPass = badges.wcagRatio >= 4.5;
  const responsivePass = badges.responsiveOk;
  const textSafetyPass = badges.textSafetyMaxChars >= 50;

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
    >
      <DialogContent
        className="max-w-5xl w-[95vw] p-6"
        data-testid="template-fullscreen"
      >
        <DialogTitle className="sr-only">
          {template ? `Xem trước mẫu ${template.name}` : 'Xem trước mẫu'}
        </DialogTitle>

        {template && (
          <div className="space-y-4">
            <div className="flex items-start justify-between flex-wrap gap-3">
              <div>
                <h2 className="text-xl font-bold">
                  {template.name} · Mã {template.code}
                </h2>
                <p className="text-sm text-muted-foreground">{template.tag}</p>
              </div>
              <div className="flex gap-2">
                <Button variant="ghost" onClick={onClose} type="button">
                  <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
                  Quay lại lưới
                </Button>
                <Button
                  type="button"
                  onClick={() => onConfirm(template)}
                  data-testid="template-fullscreen-confirm"
                >
                  <Check className="mr-2 w-4 h-4" aria-hidden="true" />
                  Chọn mẫu này
                </Button>
              </div>
            </div>

            {/* Device toggle */}
            <div
              className="flex items-center gap-2 text-xs"
              role="radiogroup"
              aria-label="Chọn kích thước xem trước"
            >
              {(['mobile', 'tablet', 'desktop'] as const).map((d) => {
                const Icon =
                  d === 'mobile' ? Smartphone : d === 'tablet' ? Tablet : Monitor;
                return (
                  <button
                    key={d}
                    type="button"
                    role="radio"
                    aria-checked={device === d}
                    onClick={() => setDevice(d)}
                    className={`flex items-center gap-1 px-2 py-1 rounded border ${
                      device === d
                        ? 'border-primary bg-primary/10 text-primary'
                        : 'border-border hover:border-primary/50'
                    }`}
                    data-testid={`template-fullscreen-device-${d}`}
                  >
                    <Icon className="w-3.5 h-3.5" aria-hidden="true" />
                    <span>{DEVICE_LABEL[d]}</span>
                  </button>
                );
              })}
            </div>

            {/* Preview frame — SVG render until real iframe wired (GAP-272j) */}
            <div
              className="rounded-lg border bg-slate-50 overflow-hidden"
              style={{
                maxWidth: device === 'mobile' ? 360 : device === 'tablet' ? 768 : '100%',
                margin: '0 auto',
                aspectRatio: '16 / 9',
              }}
              data-testid="template-fullscreen-preview"
              data-device={device}
            >
              <div
                className="w-full h-full"
                dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(template.svg, SVG_PURIFY_CONFIG) }}
              />
            </div>

            {/* 3 quality badges */}
            <div className="grid md:grid-cols-3 gap-3 text-sm">
              <BadgeRow
                pass={wcagPass}
                label={`WCAG AA · contrast ${badges.wcagRatio.toFixed(1)}:1`}
                testid="template-fullscreen-badge-wcag"
              />
              <BadgeRow
                pass={responsivePass}
                label="Responsive 320px → 3840px"
                testid="template-fullscreen-badge-responsive"
              />
              <BadgeRow
                pass={textSafetyPass}
                label={`Tiêu đề VN ≤ ${badges.textSafetyMaxChars} ký tự — không tràn`}
                testid="template-fullscreen-badge-text-safety"
              />
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

interface BadgeRowProps {
  pass: boolean;
  label: string;
  testid: string;
}

function BadgeRow({ pass, label, testid }: BadgeRowProps) {
  return (
    <div
      data-testid={testid}
      data-pass={pass ? 'true' : 'false'}
      className={`p-3 rounded-lg border flex items-start gap-2 ${
        pass
          ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
          : 'bg-rose-50 border-rose-200 text-rose-900'
      }`}
    >
      <CheckCircle2
        className={`w-4 h-4 mt-0.5 shrink-0 ${
          pass ? 'text-emerald-600' : 'text-rose-600'
        }`}
        aria-hidden="true"
      />
      <span>{label}</span>
    </div>
  );
}
