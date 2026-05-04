/**
 * RecurrenceForm — UI for entering an RFC 5545 RRULE (subset) for a class.
 *
 * GAP-290 Wave 18a — Phase 1: WEEKLY only with multi-day picker, start/end
 * times, mandatory until date, and optional exclude-dates list. Submits to
 * `POST /api/v1/classes/{classId}/sessions/generate-from-recurrence` via
 * {@link useGenerateSessionsFromRecurrence}.
 *
 * @since GAP-290 Wave 18a (2026-05-04)
 */

'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common';
import { ICAL_DAY_LABELS, type IcalDay, type RecurrenceRule } from '@/types/class';

const DAY_ORDER: IcalDay[] = ['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU'];

interface RecurrenceFormProps {
  /** Initial rule (when editing). When null, form starts blank. */
  initialRule?: RecurrenceRule | null;
  /** Whether existing sessions on the class will be regenerated. */
  hasExistingSessions?: boolean;
  /** Callback when user submits a valid rule. */
  onSubmit: (rule: RecurrenceRule) => void;
  /** Show loading state on submit button. */
  isSubmitting?: boolean;
  /** Optional cancel handler. */
  onCancel?: () => void;
}

interface FormErrors {
  byDay?: string;
  startTime?: string;
  endTime?: string;
  until?: string;
  excludeDates?: string;
}

export function RecurrenceForm({
  initialRule,
  hasExistingSessions = false,
  onSubmit,
  isSubmitting = false,
  onCancel,
}: RecurrenceFormProps) {
  const [byDay, setByDay] = useState<Set<IcalDay>>(
    () => new Set(initialRule?.byDay ?? [])
  );
  const [startTime, setStartTime] = useState(initialRule?.startTime ?? '19:00');
  const [endTime, setEndTime] = useState(initialRule?.endTime ?? '20:30');
  const [until, setUntil] = useState(initialRule?.until ?? '');
  const [excludeDateInput, setExcludeDateInput] = useState('');
  const [excludeDates, setExcludeDates] = useState<string[]>(
    initialRule?.excludeDates ?? []
  );
  const [errors, setErrors] = useState<FormErrors>({});

  function toggleDay(day: IcalDay) {
    setByDay((prev) => {
      const next = new Set(prev);
      if (next.has(day)) {
        next.delete(day);
      } else {
        next.add(day);
      }
      return next;
    });
  }

  function addExcludeDate() {
    if (!excludeDateInput) return;
    if (!excludeDates.includes(excludeDateInput)) {
      setExcludeDates((prev) => [...prev, excludeDateInput].sort());
    }
    setExcludeDateInput('');
  }

  function removeExcludeDate(date: string) {
    setExcludeDates((prev) => prev.filter((d) => d !== date));
  }

  function validate(): FormErrors {
    const next: FormErrors = {};
    if (byDay.size === 0) {
      next.byDay = 'Phải chọn ít nhất 1 ngày trong tuần';
    }
    if (!startTime) {
      next.startTime = 'Giờ bắt đầu không được để trống';
    }
    if (!endTime) {
      next.endTime = 'Giờ kết thúc không được để trống';
    } else if (startTime && endTime <= startTime) {
      next.endTime = 'Giờ kết thúc phải sau giờ bắt đầu';
    }
    if (!until) {
      next.until = 'Ngày kết thúc lặp không được để trống';
    } else if (until < new Date().toISOString().slice(0, 10)) {
      next.until = 'Ngày kết thúc lặp phải sau ngày hôm nay';
    }
    return next;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length > 0) {
      return;
    }
    const rule: RecurrenceRule = {
      freq: 'WEEKLY',
      byDay: DAY_ORDER.filter((d) => byDay.has(d)),
      startTime,
      endTime,
      until,
      ...(excludeDates.length > 0 ? { excludeDates } : {}),
    };
    onSubmit(rule);
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6" data-testid="recurrence-form">
      {hasExistingSessions && (
        <div className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
          <strong>Lưu ý:</strong> Sửa quy tắc sẽ tạo lại các buổi sắp tới. Buổi
          đã điểm danh và buổi trong quá khứ vẫn giữ nguyên.
        </div>
      )}

      {/* Day-of-week multi-picker */}
      <div>
        <label className="mb-2 block text-sm font-medium">Ngày trong tuần</label>
        <div className="flex flex-wrap gap-2" role="group" aria-label="Day of week">
          {DAY_ORDER.map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => toggleDay(d)}
              aria-pressed={byDay.has(d)}
              data-testid={`day-${d}`}
              className={
                byDay.has(d)
                  ? 'rounded-md border border-primary bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground'
                  : 'rounded-md border bg-background px-3 py-1.5 text-sm font-medium text-foreground hover:bg-accent'
              }
            >
              {ICAL_DAY_LABELS[d].vi}
            </button>
          ))}
        </div>
        {errors.byDay && (
          <p className="mt-1 text-sm text-destructive" role="alert">{errors.byDay}</p>
        )}
      </div>

      {/* Times */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="startTime" className="mb-2 block text-sm font-medium">
            Giờ bắt đầu
          </label>
          <input
            id="startTime"
            type="time"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            className="w-full rounded-md border bg-background px-3 py-2 text-sm"
            data-testid="start-time"
          />
          {errors.startTime && (
            <p className="mt-1 text-sm text-destructive" role="alert">{errors.startTime}</p>
          )}
        </div>
        <div>
          <label htmlFor="endTime" className="mb-2 block text-sm font-medium">
            Giờ kết thúc
          </label>
          <input
            id="endTime"
            type="time"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
            className="w-full rounded-md border bg-background px-3 py-2 text-sm"
            data-testid="end-time"
          />
          {errors.endTime && (
            <p className="mt-1 text-sm text-destructive" role="alert">{errors.endTime}</p>
          )}
        </div>
      </div>

      {/* Until date */}
      <div>
        <label htmlFor="until" className="mb-2 block text-sm font-medium">
          Lặp đến hết ngày
        </label>
        <input
          id="until"
          type="date"
          value={until}
          onChange={(e) => setUntil(e.target.value)}
          className="w-full rounded-md border bg-background px-3 py-2 text-sm"
          data-testid="until-date"
        />
        {errors.until && (
          <p className="mt-1 text-sm text-destructive" role="alert">{errors.until}</p>
        )}
      </div>

      {/* Exclude dates (holidays) */}
      <div>
        <label className="mb-2 block text-sm font-medium">
          Bỏ qua ngày <span className="text-xs text-muted-foreground">(nghỉ lễ, không bắt buộc)</span>
        </label>
        <div className="flex gap-2">
          <input
            type="date"
            value={excludeDateInput}
            onChange={(e) => setExcludeDateInput(e.target.value)}
            className="flex-1 rounded-md border bg-background px-3 py-2 text-sm"
            data-testid="exclude-date-input"
          />
          <Button type="button" onClick={addExcludeDate} variant="outline">
            Thêm
          </Button>
        </div>
        {excludeDates.length > 0 && (
          <ul className="mt-2 space-y-1" data-testid="exclude-dates-list">
            {excludeDates.map((d) => (
              <li key={d} className="flex items-center justify-between rounded-md border px-3 py-1 text-sm">
                <span>{d}</span>
                <button
                  type="button"
                  onClick={() => removeExcludeDate(d)}
                  className="text-xs text-destructive hover:underline"
                >
                  Xóa
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Submit */}
      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel} disabled={isSubmitting}>
            Hủy
          </Button>
        )}
        <Button type="submit" disabled={isSubmitting} data-testid="submit-recurrence">
          {isSubmitting ? <LoadingSpinner /> : 'Tạo các buổi học'}
        </Button>
      </div>
    </form>
  );
}
