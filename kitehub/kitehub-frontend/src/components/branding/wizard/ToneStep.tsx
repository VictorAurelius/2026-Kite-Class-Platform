'use client';

// ---------------------------------------------------------------------------
// ToneStep — Step 4 of AI Branding Wizard v2 (Wave 32 Bucket B)
//
// 4 constrained-preset tone cards (per ai-branding-guidelines.md §2.1) +
// per-card tiny rendered preview + reasoning preview after select.
//
// Spec source:
//   ui_kits/ai-branding-wizard-v2/screens/step4-tone-default.html
//   ui_kits/ai-branding-wizard-v2/screens/step4-tone-selected.html
//
// Persistence note: tone is stored client-side in WizardState and sent to
// `POST /branding/jobs` on Step 6 generate. No dedicated tone-persistence
// endpoint exists — by design per the original plan. See PR body §"Mocks
// deferred" for the full state-check.
// ---------------------------------------------------------------------------

import { useState } from 'react';
import { ArrowLeft, ArrowRight, Info, Palette } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ToneCard, type ToneOption } from './ToneCard';
import { AUDIENCE_OPTIONS } from './AudienceStep';

// Bucket B-local types — coordinator will reconcile with Bucket A's
// `wizard-shared.tsx` exports post-merge if signatures differ.
// Matches original plan §3 Bucket A signature: WizardState.tone: string | null.
export interface ToneStepWizardState {
  audience: string | null;
  tone: string | null;
}

export interface ToneStepProps {
  wizardState: ToneStepWizardState;
  onNext: (tone: string) => void;
  onBack: () => void;
}

// 4 tone presets per original plan §3 Bucket B + step4 spec.
// Each tone has WCAG AA-engineered swatch (per spec HTML comment).
export const TONE_OPTIONS: readonly ToneOption[] = [
  {
    id: 'professional',
    emoji: '💼',
    title: 'Chuyên nghiệp',
    description: 'Tin cậy, định hướng kết quả, sạch.',
    sampleHeadline: 'Khoá luyện thi THPT 2026',
    sampleButtonLabel: 'Đăng ký ngay',
    fitHint: 'Phù hợp lớp luyện thi, trường THCS-THPT',
    swatch: {
      cardBg: '#F8FAFC',
      cardBorder: '#E2E8F0',
      previewBg: '#F1F5F9',
      previewHeadingColor: '#0F172A', // slate-900 on slate-100 ≈ 16:1 AAA
      previewBtnBg: '#1E293B', // slate-800
      previewBtnColor: '#FFFFFF',
      previewBtnRadius: '4px',
    },
  },
  {
    id: 'friendly',
    emoji: '😊',
    title: 'Thân thiện',
    description: 'Ấm áp, gần gũi, dành cho gia đình.',
    sampleHeadline: 'Học vui mỗi ngày tại Hoa Sen',
    sampleButtonLabel: 'Tham quan lớp',
    fitHint: 'Phù hợp mầm non, tiểu học',
    swatch: {
      cardBg: '#FFFBEB',
      cardBorder: '#FDE68A',
      previewBg: '#FEF9E7',
      previewHeadingColor: '#78350F', // amber-900 on amber-50 ≈ 9:1 AAA
      previewBtnBg: '#F59E0B', // amber-500
      previewBtnColor: '#1F2937', // gray-800 on amber ≈ 4.7:1 AA
      previewBtnRadius: '999px',
    },
  },
  {
    id: 'energetic',
    emoji: '⚡',
    title: 'Năng động',
    description: 'Nhiệt huyết, hiện đại, quốc tế.',
    sampleHeadline: 'Speak English Fluently',
    sampleButtonLabel: 'Bắt đầu',
    fitHint: 'Phù hợp tiếng Anh, kỹ năng mềm',
    swatch: {
      cardBg: '#FFF1F2',
      cardBorder: '#FECDD3',
      previewBg: '#FEE2E2',
      previewHeadingColor: '#7F1D1D', // red-900 on red-100 ≈ 9:1 AAA
      previewBtnBg: '#DC2626', // red-600
      previewBtnColor: '#FFFFFF',
      previewBtnRadius: '8px',
    },
  },
  {
    id: 'luxury',
    emoji: '✨',
    title: 'Sang trọng',
    description: 'Cao cấp, kén chọn, đẳng cấp.',
    sampleHeadline: 'Premium 1-1 Coaching',
    sampleButtonLabel: 'Đặt lịch',
    fitHint: 'Phù hợp lớp 1-1, quốc tế',
    swatch: {
      cardBg: '#F5F0E6',
      cardBorder: '#D6C9A6',
      previewBg: '#2A1F0F',
      previewHeadingColor: '#F5E6C9', // cream on dark-brown ≈ 12:1 AAA
      previewBtnBg: '#D4AF37', // gold
      previewBtnColor: '#2A1F0F', // dark on gold ≈ 6.8:1 AA
      previewBtnRadius: '2px',
    },
  },
] as const;

// Tone reasoning preview after selection — describes how AI will combine
// the chosen tone with the previously-chosen audience (see step4-selected spec).
interface ToneReasoning {
  shortHeadline: string;
  body: string;
}

export const TONE_REASONING: Record<string, ToneReasoning> = {
  professional: {
    shortHeadline: 'Phong cách "Chuyên nghiệp"',
    body:
      'AI sẽ chọn font Sans-serif (Inter), bo tròn nhẹ (4px), bảng màu navy + đỏ kết quả. Sẽ thấy ở Bước 5.',
  },
  friendly: {
    shortHeadline: 'Phong cách "Thân thiện"',
    body:
      'AI sẽ chọn font tròn (Quicksand), bo tròn lớn, bảng màu pastel ấm. Sẽ thấy ở Bước 5.',
  },
  energetic: {
    shortHeadline: 'Phong cách "Năng động"',
    body:
      'AI sẽ chọn font hiện đại (Plus Jakarta Sans), gradient sống động, bảng màu tím-cam. Sẽ thấy ở Bước 5.',
  },
  luxury: {
    shortHeadline: 'Phong cách "Sang trọng"',
    body:
      'AI sẽ chọn font serif (Playfair Display) cho heading + sans-serif cho body, bảng màu vàng kim trên nền tối. Sẽ thấy ở Bước 5.',
  },
};

export function ToneStep({ wizardState, onNext, onBack }: ToneStepProps) {
  const [selected, setSelected] = useState<string | null>(wizardState.tone);

  const handleSelect = (id: string) => {
    setSelected(id);
  };

  const handleContinue = () => {
    if (selected) {
      onNext(selected);
    }
  };

  const reasoning = selected ? TONE_REASONING[selected] : null;
  const selectedTone = TONE_OPTIONS.find((t) => t.id === selected);
  const audienceTitle = AUDIENCE_OPTIONS.find((a) => a.id === wizardState.audience)?.title;

  return (
    <div className="mx-auto w-full max-w-3xl space-y-6">
      <div className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-primary">Phong cách thương hiệu</p>
        <h1 className="text-2xl font-bold text-foreground">Chọn phong cách thương hiệu</h1>
        <p className="text-muted-foreground">
          Mỗi thẻ hiển thị xem trước nhỏ — bấm để cảm nhận tone trước khi chọn.
        </p>
      </div>

      <div
        role="radiogroup"
        aria-label="Phong cách thương hiệu"
        className="grid grid-cols-1 gap-4 sm:grid-cols-2"
      >
        {TONE_OPTIONS.map((option) => (
          <ToneCard
            key={option.id}
            option={option}
            selected={selected === option.id}
            onSelect={handleSelect}
          />
        ))}
      </div>

      <div className="flex max-w-2xl items-start gap-2 text-sm text-muted-foreground">
        <Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
        <p>
          Phong cách ảnh hưởng cả màu sắc, font chữ, độ tròn nút bấm, và ngôn ngữ marketing. Đổi
          sau ở Settings → Branding.
        </p>
      </div>

      {reasoning && selectedTone && (
        <div
          data-testid="tone-reasoning"
          className="max-w-2xl rounded-lg border border-primary/25 bg-primary/[0.06] p-4"
        >
          <div className="flex items-start gap-3">
            <Palette className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
            <div className="text-sm">
              <p className="mb-1 font-semibold">
                {reasoning.shortHeadline}
                {audienceTitle ? ` + ${audienceTitle}` : ''}
              </p>
              <p className="text-muted-foreground">{reasoning.body}</p>
            </div>
          </div>
        </div>
      )}

      <div className="flex items-center justify-between border-t pt-4">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground">
          Phong cách ·{' '}
          {selectedTone ? `Đã chọn: ${selectedTone.title}` : 'Chọn 1 phong cách'}
        </p>
        <Button onClick={handleContinue} disabled={!selected}>
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
