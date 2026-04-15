'use client';

import type { Segment } from './types';

/**
 * GAP-069 segment picker — binds wizard to Vietnamese education segments.
 *
 * @since Wave 3 Sub-PR 3.7
 */
interface Props {
  value?: Segment;
  onChange: (s: Segment) => void;
}

const SEGMENTS: { id: Segment; label: string; description: string; icon: string }[] = [
  {
    id: 'K12',
    label: 'Trường K-12 (tiểu học / THCS / THPT)',
    description: 'Màu ấm, font serif/rounded, hình ảnh phù hợp tuổi',
    icon: '🏫',
  },
  {
    id: 'CENTER',
    label: 'Trung tâm giáo dục',
    description: 'Trẻ trung, năng động, hero banner marketing',
    icon: '🎓',
  },
  {
    id: 'UNIV',
    label: 'Đại học / Cao đẳng',
    description: 'Academic formal, muted palette, serif',
    icon: '🎓',
  },
  {
    id: 'CORP',
    label: 'Training nội bộ doanh nghiệp',
    description: 'Corporate clean, minimal, sans-serif',
    icon: '🏢',
  },
  { id: 'OTHER', label: 'Khác', description: 'Tôi sẽ tự mô tả sau', icon: '✨' },
];

export function SegmentPicker({ value, onChange }: Props) {
  return (
    <div role="radiogroup" aria-label="Loại tổ chức giáo dục" className="grid gap-3 md:grid-cols-2">
      {SEGMENTS.map((s) => (
        <button
          key={s.id}
          type="button"
          role="radio"
          aria-checked={value === s.id}
          onClick={() => onChange(s.id)}
          className={`rounded-xl border p-4 text-left transition ${
            value === s.id
              ? 'border-primary bg-primary/5 ring-2 ring-primary/30'
              : 'border-border hover:border-primary/50 hover:bg-muted/50'
          }`}
        >
          <div className="text-2xl">{s.icon}</div>
          <div className="mt-2 font-medium">{s.label}</div>
          <div className="mt-1 text-sm text-muted-foreground">{s.description}</div>
        </button>
      ))}
    </div>
  );
}
