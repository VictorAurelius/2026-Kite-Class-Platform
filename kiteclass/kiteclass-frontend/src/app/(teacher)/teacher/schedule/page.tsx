/**
 * Teacher schedule — wraps G4 ClassScheduleManager from `@kite/shared-ui`.
 *
 * Maps to `kiteclass-teacher/schedule-week-view.html` + create-slot +
 * conflict-error variants. G4 natively handles conflict detection via
 * `detectConflicts`.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import { useCallback, useState } from 'react';
import {
  ClassScheduleManager,
  detectConflicts,
  type ClassScheduleManagerProps,
  type ConflictWarning,
  type ScheduleSlot,
  type WeekDay,
} from '@kite/shared-ui';

// ScheduleManagerState is not directly re-exported from `@kite/shared-ui`
// root yet — derive from the props shape which IS re-exported.
type ScheduleManagerState = ClassScheduleManagerProps['state'];
import { TEACHER_PROFILE } from '@/components/teacher/teacher-mock-data';

// Vietnamese week starts MONDAY (T2) per VN convention. WeekDay enum
// uses ISO 3-letter codes (MON..SUN); display labels are component-internal
// (per G4 spec).
const MON: WeekDay = 'MON';
const TUE: WeekDay = 'TUE';
const THU: WeekDay = 'THU';
const FRI: WeekDay = 'FRI';

const INITIAL_SLOTS: ScheduleSlot[] = [
  {
    id: 'sl-1',
    className: 'Lớp 10A2 - Toán nâng cao',
    teacherName: TEACHER_PROFILE.fullName,
    date: '2026-04-13',
    startTime: '08:00',
    endTime: '09:30',
    recurrence: 'WEEKLY',
    daysOfWeek: [MON, TUE, THU],
  },
  {
    id: 'sl-2',
    className: 'Lớp 11B1 - Toán cơ bản',
    teacherName: TEACHER_PROFILE.fullName,
    date: '2026-04-13',
    startTime: '14:00',
    endTime: '15:30',
    recurrence: 'WEEKLY',
    daysOfWeek: [MON, THU],
  },
  {
    id: 'sl-3',
    className: 'Lớp 12C1 - Toán ôn thi',
    teacherName: TEACHER_PROFILE.fullName,
    date: '2026-04-13',
    startTime: '16:00',
    endTime: '17:30',
    recurrence: 'WEEKLY',
    daysOfWeek: [TUE, FRI],
  },
];

export default function TeacherSchedulePage() {
  const [slots, setSlots] = useState<ScheduleSlot[]>(INITIAL_SLOTS);
  const [state, setState] = useState<ScheduleManagerState>('single-class');
  const [editingSlot, setEditingSlot] = useState<ScheduleSlot | undefined>(undefined);
  const [conflicts, setConflicts] = useState<ConflictWarning[] | undefined>(undefined);

  const handleAddSlot = useCallback(() => {
    setEditingSlot(undefined);
    setState('recurring-edit');
  }, []);

  const handleSaveSlot = useCallback(
    (slot: ScheduleSlot) => {
      const others = slots.filter((s) => s.id !== slot.id);
      const newConflicts = detectConflicts([...others, slot]);
      if (newConflicts.length > 0) {
        setConflicts(newConflicts);
        setEditingSlot(slot);
        setState('conflict-warning');
        return;
      }
      setSlots((prev) => {
        const exists = prev.some((s) => s.id === slot.id);
        return exists
          ? prev.map((s) => (s.id === slot.id ? slot : s))
          : [...prev, slot];
      });
      setEditingSlot(undefined);
      setConflicts(undefined);
      setState('saved');
    },
    [slots],
  );

  const handleCancelEdit = useCallback(() => {
    setEditingSlot(undefined);
    setConflicts(undefined);
    setState('single-class');
  }, []);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">Lịch dạy</h1>
        <p className="text-muted-foreground">
          {TEACHER_PROFILE.fullName} · {TEACHER_PROFILE.semester}{' '}
          {TEACHER_PROFILE.schoolYear}
        </p>
      </div>

      <ClassScheduleManager
        className={`${TEACHER_PROFILE.fullName} - Năm học ${TEACHER_PROFILE.schoolYear}`}
        schoolYearLabel={`Năm học ${TEACHER_PROFILE.schoolYear}`}
        slots={slots}
        state={state}
        editingSlot={editingSlot}
        conflicts={conflicts}
        onAddSlot={handleAddSlot}
        onSaveSlot={handleSaveSlot}
        onCancelEdit={handleCancelEdit}
      />
    </div>
  );
}
