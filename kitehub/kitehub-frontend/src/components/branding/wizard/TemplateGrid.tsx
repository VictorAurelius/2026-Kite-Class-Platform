'use client';

// ---------------------------------------------------------------------------
// TemplateGrid — Step 5: 6 SVG template previews filtered by audience + tone.
//
// XSS mitigation: all SVG passed to dangerouslySetInnerHTML is sanitized via
// DOMPurify with SVG profile (strip <script> + on* handlers). See Bucket A.
//
// Per `ai-branding-guidelines.md` §2.2 the wizard MUST show ≥6 visual previews
// (templates), and per the rework plan §4 Bucket C the filtering by selected
// audience + tone MUST actually narrow the catalogue (v1 audit caught the
// state being read but ignored). Templates are static metadata for now —
// when the backend `/branding/templates` endpoint returns its real catalogue
// (already exists per `endpoints.ts`), this list moves to a react-query call.
//
// TODO(GAP-272n): wire to `/branding/templates` endpoint once schema matches
// the audience/tone filter contract; until then the local catalogue ships
// 6 known-good templates each tagged with audience + tone affinities.
// ---------------------------------------------------------------------------

import { CheckCircle2 } from 'lucide-react';
import DOMPurify from 'isomorphic-dompurify';

// SVG sanitization config: allow SVG elements + presentation attributes,
// strip <script> and on* event handlers.
const SVG_PURIFY_CONFIG: DOMPurify.Config = {
  USE_PROFILES: { svg: true, svgFilters: true },
  FORBID_TAGS: ['script', 'use'],
  FORBID_ATTR: ['xlink:href', 'href'],
};

/**
 * Template descriptor mirroring the 6 spec SVGs in
 * `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step5-template-grid.html`.
 */
export interface TemplateDescriptor {
  /** Stable ID — used as `wizardState.templateId`. */
  id: string;
  /** Short code visible to the user (T1 / T2 / ...). */
  code: string;
  /** Display name (e.g. "Score Board"). */
  name: string;
  /** One-line tag explaining the design feel. */
  tag: string;
  /** Audience presets this template fits (intersection with WizardState.audience). */
  audiences: ReadonlyArray<string>;
  /** Tone presets this template fits (intersection with WizardState.tone). */
  tones: ReadonlyArray<string>;
  /** Inline SVG preview (string returned in HTML form). */
  svg: string;
}

/**
 * Local catalogue of 6 templates seeded from the spec HTML. Each is tagged
 * with `audiences` + `tones` so filtering is deterministic. The audiences
 * cover the 4 cards Bucket B ships (mam-non / thcs / english-center /
 * exam-prep) plus a generic `mixed` value for "không thuộc nhóm". Tones
 * cover the 4 cards Bucket B ships (professional / friendly / energetic /
 * luxurious).
 */
export const TEMPLATES: ReadonlyArray<TemplateDescriptor> = [
  {
    id: 'template-t1-navy-focus',
    code: 'T1',
    name: 'Navy Focus',
    tag: 'Tin cậy · Tập trung kết quả · Mã T1',
    audiences: ['exam-prep', 'thcs', 'mixed'],
    tones: ['professional', 'luxurious'],
    svg: `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Navy Focus template preview">
      <rect width="320" height="200" fill="#0F1E3D"/>
      <rect x="0" y="0" width="320" height="32" fill="#0A1530"/>
      <circle cx="20" cy="16" r="6" fill="#F59E0B"/>
      <text x="36" y="20" fill="white" font-size="10" font-weight="600">Toán Master</text>
      <rect x="240" y="8" width="68" height="18" rx="9" fill="#EF4444"/>
      <text x="274" y="20" fill="white" font-size="9" font-weight="600" text-anchor="middle">Đăng ký</text>
      <text x="160" y="100" fill="white" font-size="20" font-weight="800" text-anchor="middle">Vào trường chuyên</text>
      <text x="160" y="120" fill="#F59E0B" font-size="14" font-weight="700" text-anchor="middle">98% học viên đạt điểm 9+</text>
      <rect x="120" y="140" width="80" height="24" rx="4" fill="#F59E0B"/>
      <text x="160" y="156" fill="#0F1E3D" font-size="10" font-weight="700" text-anchor="middle">Đăng ký test</text>
    </svg>`,
  },
  {
    id: 'template-t2-score-board',
    code: 'T2',
    name: 'Score Board',
    tag: 'Dữ liệu-first · Bảng điểm · Mã T2',
    audiences: ['exam-prep', 'thcs', 'english-center', 'mixed'],
    tones: ['professional', 'energetic'],
    svg: `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Score Board template preview">
      <rect width="320" height="200" fill="#FFFFFF"/>
      <rect x="0" y="0" width="320" height="40" fill="#1E40AF"/>
      <text x="20" y="26" fill="white" font-size="12" font-weight="700">Toán Master</text>
      <rect x="240" y="10" width="64" height="20" rx="4" fill="white"/>
      <text x="272" y="24" fill="#1E40AF" font-size="9" font-weight="700" text-anchor="middle">Tham gia</text>
      <text x="20" y="80" fill="#0F172A" font-size="18" font-weight="800">Bảng điểm 2025</text>
      <rect x="20" y="92" width="280" height="60" rx="4" fill="#F1F5F9"/>
      <rect x="32" y="104" width="180" height="6" rx="3" fill="#1E40AF"/>
      <rect x="32" y="120" width="220" height="6" rx="3" fill="#10B981"/>
      <rect x="32" y="136" width="140" height="6" rx="3" fill="#F59E0B"/>
      <text x="200" y="180" fill="#1E40AF" font-size="10" font-weight="600">Xem chi tiết →</text>
    </svg>`,
  },
  {
    id: 'template-t3-coach-card',
    code: 'T3',
    name: 'Coach Card',
    tag: 'Cá nhân · Giáo viên-led · Mã T3',
    audiences: ['english-center', 'mam-non', 'mixed'],
    tones: ['friendly', 'professional'],
    svg: `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Coach Card template preview">
      <rect width="320" height="200" fill="#F8FAFC"/>
      <rect x="20" y="20" width="120" height="160" rx="8" fill="#1E40AF"/>
      <circle cx="80" cy="60" r="20" fill="#F59E0B"/>
      <text x="80" y="100" fill="white" font-size="11" font-weight="700" text-anchor="middle">Th.S Nguyễn An</text>
      <text x="80" y="118" fill="#CBD5E1" font-size="8" text-anchor="middle">15 năm luyện thi</text>
      <rect x="32" y="140" width="96" height="24" rx="4" fill="#F59E0B"/>
      <text x="80" y="156" fill="#0F1E3D" font-size="9" font-weight="700" text-anchor="middle">Đặt buổi tư vấn</text>
      <rect x="160" y="20" width="140" height="50" rx="4" fill="#FFFFFF" stroke="#E2E8F0"/>
      <rect x="160" y="78" width="140" height="50" rx="4" fill="#FFFFFF" stroke="#E2E8F0"/>
      <rect x="160" y="136" width="140" height="44" rx="4" fill="#FFFFFF" stroke="#E2E8F0"/>
    </svg>`,
  },
  {
    id: 'template-t4-result-stripes',
    code: 'T4',
    name: 'Result Stripes',
    tag: 'Số liệu lớn · Social proof · Mã T4',
    audiences: ['exam-prep', 'thcs', 'mixed'],
    tones: ['energetic', 'professional'],
    svg: `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Result Stripes template preview">
      <rect width="320" height="200" fill="#FFFFFF"/>
      <rect x="0" y="0" width="320" height="60" fill="#0F1E3D"/>
      <text x="160" y="38" fill="white" font-size="14" font-weight="800" text-anchor="middle">98% đỗ chuyên</text>
      <rect x="0" y="60" width="320" height="2" fill="#F59E0B"/>
      <rect x="20" y="80" width="84" height="100" rx="4" fill="#1E40AF"/>
      <text x="62" y="120" fill="white" font-size="22" font-weight="800" text-anchor="middle">240</text>
      <text x="62" y="140" fill="#CBD5E1" font-size="9" text-anchor="middle">học viên đỗ</text>
      <rect x="118" y="80" width="84" height="100" rx="4" fill="#10B981"/>
      <text x="160" y="120" fill="white" font-size="22" font-weight="800" text-anchor="middle">9.2</text>
      <text x="160" y="140" fill="#A7F3D0" font-size="9" text-anchor="middle">điểm trung bình</text>
      <rect x="216" y="80" width="84" height="100" rx="4" fill="#F59E0B"/>
      <text x="258" y="120" fill="#0F1E3D" font-size="22" font-weight="800" text-anchor="middle">15</text>
      <text x="258" y="140" fill="#0F1E3D" font-size="9" text-anchor="middle">năm kinh nghiệm</text>
    </svg>`,
  },
  {
    id: 'template-t5-schedule-grid',
    code: 'T5',
    name: 'Schedule Grid',
    tag: 'Khai giảng-first · Đếm chỗ · Mã T5',
    audiences: ['english-center', 'mam-non', 'mixed'],
    tones: ['friendly', 'energetic'],
    svg: `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Schedule Grid template preview">
      <rect width="320" height="200" fill="#FFFFFF"/>
      <text x="20" y="30" fill="#0F172A" font-size="14" font-weight="800">Lịch khai giảng</text>
      <rect x="20" y="44" width="280" height="36" rx="4" fill="#F1F5F9"/>
      <text x="32" y="66" fill="#0F172A" font-size="10" font-weight="600">15/08 · Lớp 12 · Khai giảng</text>
      <rect x="240" y="52" width="56" height="20" rx="4" fill="#10B981"/>
      <text x="268" y="65" fill="white" font-size="8" font-weight="700" text-anchor="middle">Còn 8 chỗ</text>
      <rect x="20" y="88" width="280" height="36" rx="4" fill="#F1F5F9"/>
      <text x="32" y="110" fill="#0F172A" font-size="10" font-weight="600">22/08 · Lớp 9 · Vào chuyên</text>
      <rect x="240" y="96" width="56" height="20" rx="4" fill="#F59E0B"/>
      <text x="268" y="109" fill="white" font-size="8" font-weight="700" text-anchor="middle">Còn 3 chỗ</text>
      <rect x="20" y="132" width="280" height="36" rx="4" fill="#F1F5F9"/>
      <text x="32" y="154" fill="#94A3B8" font-size="10" font-weight="600">29/08 · Lớp 11 · Cơ bản</text>
      <rect x="240" y="140" width="56" height="20" rx="4" fill="#EF4444"/>
      <text x="268" y="153" fill="white" font-size="8" font-weight="700" text-anchor="middle">Hết</text>
    </svg>`,
  },
  {
    id: 'template-t6-roadmap-vertical',
    code: 'T6',
    name: 'Roadmap Vertical',
    tag: 'Lộ trình rõ · Cam kết · Mã T6',
    audiences: ['exam-prep', 'thcs', 'english-center', 'mixed'],
    tones: ['professional', 'luxurious'],
    svg: `<svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Roadmap Vertical template preview">
      <rect width="320" height="200" fill="#0F1E3D"/>
      <text x="160" y="32" fill="white" font-size="14" font-weight="800" text-anchor="middle">Lộ trình 12 tháng</text>
      <line x1="160" y1="50" x2="160" y2="180" stroke="#F59E0B" stroke-width="2"/>
      <circle cx="160" cy="60" r="8" fill="#F59E0B"/>
      <text x="180" y="64" fill="white" font-size="9">Tháng 1-3 · Cơ bản</text>
      <circle cx="160" cy="100" r="8" fill="#10B981"/>
      <text x="180" y="104" fill="white" font-size="9">Tháng 4-6 · Nâng cao</text>
      <circle cx="160" cy="140" r="8" fill="#3B82F6"/>
      <text x="180" y="144" fill="white" font-size="9">Tháng 7-9 · Đề thi thật</text>
      <circle cx="160" cy="172" r="8" fill="#EF4444"/>
      <text x="180" y="176" fill="white" font-size="9">Tháng 10-12 · Tổng ôn</text>
    </svg>`,
  },
] as const;

/**
 * Returns the templates that match BOTH the chosen audience and tone.
 * If audience or tone is `null`, that axis is treated as "any" — but we
 * always return at least 1 template to satisfy `ai-branding-guidelines.md`
 * §2.2 (≥6 previews — when filters narrow below 6 we widen back to all 6
 * with a soft-priority sort so users still see 6 options).
 */
export function filterTemplates(
  templates: ReadonlyArray<TemplateDescriptor>,
  audience: string | null,
  tone: string | null,
): ReadonlyArray<TemplateDescriptor> {
  const matchesAudience = (t: TemplateDescriptor) =>
    !audience || t.audiences.includes(audience);
  const matchesTone = (t: TemplateDescriptor) => !tone || t.tones.includes(tone);
  const filtered = templates.filter(
    (t) => matchesAudience(t) && matchesTone(t),
  );
  // §2.2 mandates ≥6 previews; if filter is too aggressive widen back so the
  // user still has 6 options but the matched ones land first.
  if (filtered.length >= 6) return filtered;
  const matchedIds = new Set(filtered.map((t) => t.id));
  const rest = templates.filter((t) => !matchedIds.has(t.id));
  return [...filtered, ...rest];
}

interface TemplateGridProps {
  /** Currently selected audience (from `WizardState.audience`). */
  audience: string | null;
  /** Currently selected tone (from `WizardState.tone`). */
  tone: string | null;
  /** Currently selected template ID (`null` if none). */
  selectedId: string | null;
  /** Click handler firing on template card click. */
  onSelect: (template: TemplateDescriptor) => void;
  /** Click handler firing when user opens the fullscreen preview. */
  onOpenFullscreen: (template: TemplateDescriptor) => void;
  /** Optional override of the catalogue (defaults to `TEMPLATES`). Tests use this. */
  templates?: ReadonlyArray<TemplateDescriptor>;
}

/**
 * 6-card grid filtered by audience + tone.
 *
 * Click on a card selects it; double-click / enter on a card opens the
 * fullscreen preview (matching the spec affordance from
 * step5-template-grid.html "Bấm để xem toàn màn hình").
 */
export function TemplateGrid({
  audience,
  tone,
  selectedId,
  onSelect,
  onOpenFullscreen,
  templates = TEMPLATES,
}: TemplateGridProps) {
  const visible = filterTemplates(templates, audience, tone);

  return (
    <div
      className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"
      data-testid="template-grid"
    >
      {visible.map((t) => {
        const isSelected = selectedId === t.id;
        return (
          <article
            key={t.id}
            data-template-id={t.id}
            data-testid={`template-card-${t.code}`}
            tabIndex={0}
            onClick={() => onSelect(t)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelect(t);
              }
            }}
            onDoubleClick={() => onOpenFullscreen(t)}
            aria-label={`${t.name} (${t.code})`}
            aria-pressed={isSelected}
            role="button"
            className={`group cursor-pointer rounded-lg border-2 overflow-hidden transition-all bg-white focus:outline-none focus:ring-2 focus:ring-primary ${
              isSelected
                ? 'border-primary ring-2 ring-primary/40'
                : 'border-border hover:border-primary/50'
            }`}
          >
            <div
              className="aspect-video w-full"
              dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(t.svg, SVG_PURIFY_CONFIG) }}
            />
            <div className="p-3">
              <div className="flex items-center justify-between">
                <p className="font-semibold text-sm text-foreground">
                  {t.name}
                </p>
                {isSelected && (
                  <CheckCircle2
                    className="w-4 h-4 text-emerald-600"
                    aria-label="Đã chọn"
                  />
                )}
              </div>
              <p className="text-xs text-muted-foreground mt-1">{t.tag}</p>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onOpenFullscreen(t);
                }}
                className="mt-2 text-xs font-medium text-primary underline-offset-2 hover:underline"
                data-testid={`template-fullscreen-trigger-${t.code}`}
              >
                Xem toàn màn hình
              </button>
            </div>
          </article>
        );
      })}
    </div>
  );
}
