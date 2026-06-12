'use client';

// ---------------------------------------------------------------------------
// AudienceStep — Step 3 of AI Branding Wizard v2 (Wave 32 Bucket B)
//
// 4 constrained-preset audience cards (per ai-branding-guidelines.md §2.1) +
// "không thuộc nhóm?" details disclosure + AI reasoning preview after select.
//
// Spec source:
//   ui_kits/ai-branding-wizard-v2/screens/step3-audience-default.html
//   ui_kits/ai-branding-wizard-v2/screens/step3-audience-selected.html
//
// Persistence note: audience is stored client-side in WizardState and sent to
// `POST /branding/jobs` on Step 6 generate (BrandingGenerationRequest.targetAudience).
// No dedicated audience-persistence endpoint exists — this is by design per
// the original plan. See PR body §"Mocks deferred" for the full state-check.
// ---------------------------------------------------------------------------

import { useState } from 'react';
import { ArrowLeft, ArrowRight, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { AudienceCard, type AudienceOption } from './AudienceCard';

// Bucket B-local types — coordinator will reconcile with Bucket A's
// `wizard-shared.tsx` exports post-merge if signatures differ.
// Matches original plan §3 Bucket A signature: WizardState.audience: string | null.
export interface AudienceStepWizardState {
  audience: string | null;
}

export interface AudienceStepProps {
  wizardState: AudienceStepWizardState;
  onNext: (audience: string) => void;
  onBack: () => void;
}

// 4 audience presets per original plan §3 Bucket B + step3 spec.
// Stable IDs match the BrandingGenerationRequest.targetAudience contract
// (free text on backend; FE supplies preset slug).
// GAP-1231 — relabelled to the kit v3 (`personality.html`) concern-focused
// voice ("phụ huynh quan tâm gì"). IDs + count kept stable: each id carries a
// distinct AI reasoning map + theme (AUDIENCE_REASONING) and is sent to the BE
// as BrandingGenerationRequest.targetAudience, so a 4→3 remap is deferred. The
// kit's 3 choices map to the closest existing ids:
//   "Phụ huynh tiểu học (an toàn, lớp nhỏ)"  → preschool
//   "Luyện thi / THCS-THPT (điểm số, đầu ra)" → secondary / exam-prep
//   "Người đi làm (linh hoạt giờ học)"        → english-center
export const AUDIENCE_OPTIONS: readonly AudienceOption[] = [
  {
    id: 'preschool',
    emoji: '👨‍👩‍👧',
    title: 'Phụ huynh mầm non / tiểu học',
    description: 'Quan tâm an toàn, tiến bộ, lớp nhỏ. Tone ấm áp, nhiều hình ảnh.',
    example: 'VD: Mầm non Hoa Sen, Tiểu học Xanh',
  },
  {
    id: 'secondary',
    emoji: '🎓',
    title: 'Luyện thi / THCS-THPT',
    description: 'Quan tâm điểm số, cam kết đầu ra. Tone học thuật, đáng tin cậy.',
    example: 'VD: THCS-THPT EduPlus, Toán Master',
  },
  {
    id: 'english-center',
    emoji: '💼',
    title: 'Người đi làm / tiếng Anh giao tiếp',
    description: 'Linh hoạt giờ học, mục tiêu rõ. Tone hiện đại, năng động.',
    example: 'VD: ELC Hà Nội, Speak Up',
  },
  {
    id: 'exam-prep',
    emoji: '📊',
    title: 'Lớp luyện thi chuyên sâu',
    description: 'Quan tâm điểm số, cam kết đầu ra. Tone tập trung, kỷ luật.',
    example: 'VD: Hocmai 1-1, luyện thi IELTS',
  },
] as const;

// AI reasoning previews — short summary of how the AI will tailor branding
// based on audience. Shown after a card is selected (matches step3-selected spec).
export const AUDIENCE_REASONING: Record<string, { headline: string; bullets: readonly string[] }> = {
  preschool: {
    headline: 'Với Trường mầm non, AI sẽ chọn:',
    bullets: [
      'Bảng màu chính: pastel ấm, xanh lá nhạt, hồng dịu',
      'Hình ảnh: đồ chơi, bé học, lớp học sáng',
      'Ngôn ngữ: thân thiện với phụ huynh, vui tươi',
      'Mẫu thiết kế: bo tròn, font friendly, nhiều khoảng trắng',
    ],
  },
  secondary: {
    headline: 'Với Trường THCS / THPT, AI sẽ chọn:',
    bullets: [
      'Bảng màu chính: xanh dương đậm, trắng, vàng nhấn',
      'Hình ảnh: lớp học, học sinh tự tin, giáo viên',
      'Ngôn ngữ: học thuật, chuyên nghiệp, đáng tin cậy',
      'Mẫu thiết kế: cấu trúc rõ ràng, đồng bộ với phong cách bạn chọn ở Bước 4',
    ],
  },
  'english-center': {
    headline: 'Với Trung tâm tiếng Anh, AI sẽ chọn:',
    bullets: [
      'Bảng màu chính: tím/xanh điện, cam tươi, trắng sáng',
      'Hình ảnh: học viên đa quốc tịch, slide tiếng Anh, phòng học hiện đại',
      'Ngôn ngữ: năng động, song ngữ, hướng kết quả',
      'Mẫu thiết kế: hiện đại, gradient, font sans-serif tròn',
    ],
  },
  'exam-prep': {
    headline: 'Với Lớp luyện thi, AI sẽ chọn:',
    bullets: [
      'Bảng màu chính: xanh navy, đỏ kết quả, vàng huy chương',
      'Hình ảnh: bảng điểm, sơ đồ, học sinh tập trung',
      'Ngôn ngữ: tự tin, định hướng kết quả, dữ liệu',
      'Mẫu thiết kế: đồng bộ với phong cách bạn chọn ở Bước 4',
    ],
  },
};

export function AudienceStep({ wizardState, onNext, onBack }: AudienceStepProps) {
  // Local UI selection mirrors WizardState — useState seeded from prop allows
  // the user to revisit Step 3 with previous selection still highlighted.
  const [selected, setSelected] = useState<string | null>(wizardState.audience);

  const handleSelect = (id: string) => {
    setSelected(id);
  };

  const handleContinue = () => {
    if (selected) {
      onNext(selected);
    }
  };

  const reasoning = selected ? AUDIENCE_REASONING[selected] : null;
  const selectedOption = AUDIENCE_OPTIONS.find((o) => o.id === selected);

  return (
    <div className="mx-auto w-full max-w-3xl space-y-6">
      <div className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-primary">Đối tượng học viên</p>
        <h1 className="text-2xl font-bold text-foreground">Trung tâm bạn dạy ai?</h1>
        <p className="text-muted-foreground">
          AI sẽ chọn màu sắc, hình ảnh, và ngôn ngữ phù hợp với đối tượng học viên. Chọn 1 — có thể đổi sau.
        </p>
      </div>

      <div
        role="radiogroup"
        aria-label="Đối tượng học viên"
        className="grid grid-cols-1 gap-4 sm:grid-cols-2"
      >
        {AUDIENCE_OPTIONS.map((option) => (
          <AudienceCard
            key={option.id}
            option={option}
            selected={selected === option.id}
            onSelect={handleSelect}
          />
        ))}
      </div>

      <details className="max-w-2xl">
        <summary className="cursor-pointer text-sm font-medium text-primary">
          Trung tâm tôi không thuộc 4 nhóm trên?
        </summary>
        <div className="mt-3 rounded-lg border border-sky-200 bg-sky-50 p-4 text-sm text-sky-900">
          Chọn nhóm gần nhất — bạn vẫn tinh chỉnh được màu sắc và mẫu thiết kế ở các bước sau. Nếu cần
          tuỳ biến sâu, hãy nâng cấp lên gói <strong>ENTERPRISE</strong> để mở Advanced Mode.
        </div>
      </details>

      {reasoning && selectedOption && (
        <div
          data-testid="audience-reasoning"
          className="max-w-2xl rounded-lg border border-primary/25 bg-primary/[0.06] p-4"
        >
          <div className="flex items-start gap-3">
            <Sparkles className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
            <div className="text-sm">
              <p className="mb-2 font-semibold">AI đã hiểu hướng đi</p>
              <p className="text-muted-foreground">{reasoning.headline}</p>
              <ul className="mt-2 space-y-1 text-muted-foreground">
                {reasoning.bullets.map((line) => (
                  <li key={line}>• {line}</li>
                ))}
              </ul>
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
          Đối tượng ·{' '}
          {selectedOption ? `Đã chọn: ${selectedOption.title}` : 'Chọn 1 đối tượng để tiếp tục'}
        </p>
        <Button onClick={handleContinue} disabled={!selected}>
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
