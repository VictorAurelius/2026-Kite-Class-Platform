/**
 * GVCN per-tiết attendance route — Phase 1B v1 (Wave 18b2 Bucket A, GAP-323b).
 *
 * URL: `/attendance/period/{classId}/{periodNo}/{date}`
 *   - classId: numeric K-12 class ID
 *   - periodNo: 1..10 (tiết)
 *   - date: ISO YYYY-MM-DD
 *
 * Wires:
 *   - {@link useDailyRoster} → list of period rows for the class on the date
 *   - {@link PeriodTapGrid} → 4-button tap-grid keyed by studentId
 *   - {@link PeriodBulkActions} → "mark-all-present" / "reset" / "save"
 *   - {@link useUpsertAttendancePeriod} → POST batch on save
 *
 * Phase 1B v1 deliberately defers:
 *   - Offline queue / retry-on-reconnect (UC-PERIOD-ATT-UI-002 placeholder)
 *   - Playwright ≤2-min perf assertion (rules.md AC-OPS-001 hard target)
 *   - Multi-period quick-switch + "inherit from previous period" delta entry
 *   - Single-row PATCH merge dialog on optimistic-lock 409
 *
 * Each remains a placeholder in
 * `documents/01-business/kiteclass/period-attendance/use-cases.md`.
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

'use client';

import * as React from 'react';
import { useParams } from 'next/navigation';
import {
  PeriodTapGrid,
  type PeriodTapGridStudent,
} from '@/components/attendance/period-tap-grid';
import { PeriodBulkActions } from '@/components/attendance/period-bulk-actions';
import {
  useDailyRoster,
  useUpsertAttendancePeriod,
} from '@/hooks/use-period-attendance';
import {
  attendancePeriodApi,
  type AttendancePeriodBatchEntry,
  type AttendancePeriodResponse,
  type AttendancePeriodStatus,
} from '@/lib/api/attendance-period';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { useOfflineAttendanceQueue } from '@/lib/offline/use-offline-attendance-queue';
import { OfflineSyncStatusBadge } from '@/lib/offline/sync-status-badge';

interface RouteParams extends Record<string, string | string[] | undefined> {
  classId: string;
  periodNo: string;
  date: string;
}

function parsePositiveInt(value: string): number | null {
  const n = Number.parseInt(value, 10);
  if (!Number.isFinite(n) || n <= 0) return null;
  return n;
}

function isValidIsoDate(value: string): boolean {
  // Cheap validation — the API will reject malformed dates anyway.
  return /^\d{4}-\d{2}-\d{2}$/.test(value);
}

/**
 * Convert the daily-roster fetch result into the `(studentId → status)` map
 * the tap-grid renders, filtered to rows for THIS period only.
 */
function rosterToStatuses(
  rows: readonly AttendancePeriodResponse[],
  periodNo: number,
): Record<number, AttendancePeriodStatus> {
  const out: Record<number, AttendancePeriodStatus> = {};
  for (const row of rows) {
    if (row.periodNo === periodNo) {
      out[row.studentId] = row.status;
    }
  }
  return out;
}

/**
 * Pull the unique student roster out of the daily-roster fetch. The Phase 1B
 * v1 backend does NOT return student names yet (GAP-323b follow-up will add
 * a `/api/v1/parent/children/{classId}/students` shape). Until then we
 * surface "Học sinh #{studentId}" as a placeholder so the UI is functional
 * end-to-end against the real backend.
 */
function rosterToStudents(
  rows: readonly AttendancePeriodResponse[],
): PeriodTapGridStudent[] {
  const seen = new Map<number, PeriodTapGridStudent>();
  for (const row of rows) {
    if (!seen.has(row.studentId)) {
      seen.set(row.studentId, {
        studentId: row.studentId,
        fullName: `Học sinh #${row.studentId}`,
      });
    }
  }
  return Array.from(seen.values()).sort((a, b) => a.studentId - b.studentId);
}

export default function PeriodAttendancePage() {
  const params = useParams<RouteParams>();
  const classIdRaw = params?.classId ?? '';
  const periodNoRaw = params?.periodNo ?? '';
  const dateRaw = params?.date ?? '';

  const classIdParsed = parsePositiveInt(classIdRaw);
  const periodNoParsed = parsePositiveInt(periodNoRaw);
  const dateValid = isValidIsoDate(dateRaw);

  const teacherId = useAuthStore((s) => s.user?.id ?? null);

  const isParamValid =
    classIdParsed !== null &&
    periodNoParsed !== null &&
    periodNoParsed >= 1 &&
    periodNoParsed <= 10 &&
    dateValid;

  const classId = classIdParsed ?? 0;
  const periodNo = periodNoParsed ?? 0;
  const date = dateValid ? dateRaw : '';

  const { data: roster, isLoading: isRosterLoading } = useDailyRoster(
    classId,
    date,
  );

  const upsert = useUpsertAttendancePeriod({
    teacherId: teacherId ?? 0,
    classId,
    date,
  });

  // Offline queue — wraps the same upsertBatch endpoint, persists batches in
  // IndexedDB when offline, and auto-drains on the `online` event. The
  // upsertFn is the network-only path (no toast / cache invalidation noise);
  // those are handled by `upsert.mutate()` for the on-line save path.
  const offlineQueue = useOfflineAttendanceQueue({
    upsertFn: async (item) => {
      await attendancePeriodApi.upsertBatch(
        { entries: item.entries },
        { teacherId: item.teacherId },
      );
    },
  });

  const [localStatuses, setLocalStatuses] = React.useState<
    Record<number, AttendancePeriodStatus>
  >({});

  // Seed local state from the roster the first time it lands so the teacher
  // sees what's already recorded. We deliberately do NOT keep merging on
  // every refetch — once the user starts tapping, their local state wins
  // until they hit Save.
  const seededRef = React.useRef(false);
  React.useEffect(() => {
    if (!seededRef.current && roster && roster.length > 0) {
      setLocalStatuses(rosterToStatuses(roster, periodNo));
      seededRef.current = true;
    }
  }, [roster, periodNo]);

  const students = React.useMemo<PeriodTapGridStudent[]>(
    () => (roster ? rosterToStudents(roster) : []),
    [roster],
  );

  const subjectSectionFromRoster = React.useMemo<number | null>(() => {
    if (!roster) return null;
    for (const row of roster) {
      if (row.periodNo === periodNo) return row.subjectSectionId;
    }
    // Fall back to first row's subjectSection if no row for this period yet —
    // common case on a fresh tiết. The backend will accept whatever we send.
    return roster[0]?.subjectSectionId ?? null;
  }, [roster, periodNo]);

  const handleStatusChange = React.useCallback(
    (studentId: number, status: AttendancePeriodStatus) => {
      setLocalStatuses((prev) => ({ ...prev, [studentId]: status }));
    },
    [],
  );

  const handleMarkAllPresent = React.useCallback(() => {
    setLocalStatuses(() => {
      const next: Record<number, AttendancePeriodStatus> = {};
      for (const s of students) next[s.studentId] = 'PRESENT';
      return next;
    });
  }, [students]);

  const handleReset = React.useCallback(() => {
    setLocalStatuses({});
  }, []);

  const handleSave = React.useCallback(() => {
    if (!isParamValid || subjectSectionFromRoster === null) return;
    if (teacherId === null) return;

    const entries: AttendancePeriodBatchEntry[] = students
      .map((student) => {
        const status = localStatuses[student.studentId];
        if (!status) return null;
        return {
          studentId: student.studentId,
          classId,
          subjectSectionId: subjectSectionFromRoster,
          periodNo,
          date,
          status,
        } satisfies AttendancePeriodBatchEntry;
      })
      .filter((entry): entry is AttendancePeriodBatchEntry => entry !== null);

    if (entries.length === 0) return;

    // Offline-aware save path:
    //   - navigator.onLine === false → enqueue to IDB; the user's badge
    //     turns amber and they can keep working until reconnect
    //   - online → fire the normal mutation; on the rare network failure,
    //     the mutation's onError surfaces a toast and the user can manually
    //     re-save (which will queue at that point)
    if (typeof navigator !== 'undefined' && navigator.onLine === false) {
      void offlineQueue.enqueue({
        teacherId,
        classId,
        date,
        entries,
      });
      return;
    }

    upsert.mutate({ entries });
  }, [
    isParamValid,
    subjectSectionFromRoster,
    teacherId,
    students,
    localStatuses,
    classId,
    periodNo,
    date,
    upsert,
    offlineQueue,
  ]);

  if (!isParamValid) {
    return (
      <div className="mx-auto max-w-2xl space-y-2 p-4">
        <h1 className="text-lg font-semibold">Tham số URL không hợp lệ</h1>
        <p className="text-sm text-muted-foreground">
          URL phải có dạng <code>/attendance/period/[classId]/[periodNo]/[date]</code>{' '}
          với <code>periodNo</code> từ 1..10 và <code>date</code> theo định
          dạng <code>YYYY-MM-DD</code>.
        </p>
      </div>
    );
  }

  if (teacherId === null) {
    return (
      <div className="mx-auto max-w-2xl space-y-2 p-4">
        <h1 className="text-lg font-semibold">Cần đăng nhập</h1>
        <p className="text-sm text-muted-foreground">
          Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.
        </p>
      </div>
    );
  }

  const hasAnyEntry = Object.keys(localStatuses).length > 0;

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-3 p-3 sm:p-4">
      <header className="space-y-1">
        <h1 className="text-lg font-semibold sm:text-xl">
          Điểm danh tiết {periodNo}
        </h1>
        <p className="text-sm text-muted-foreground">
          Lớp #{classId} · {date}
        </p>
      </header>

      <OfflineSyncStatusBadge
        pending={offlineQueue.state.pending}
        failed={offlineQueue.state.failed}
        synced={offlineQueue.state.synced}
        onRetry={() => {
          void offlineQueue.flush();
        }}
      />

      {isRosterLoading ? (
        <div className="flex items-center justify-center p-8">
          <LoadingSpinner />
        </div>
      ) : students.length === 0 ? (
        <div
          data-testid="period-empty-state"
          className="rounded-md border border-dashed p-6 text-center text-sm text-muted-foreground"
        >
          Chưa có dữ liệu học sinh cho lớp này. Hãy yêu cầu bộ phận đào tạo
          khởi tạo bản ghi tiết đầu tiên trước khi điểm danh.
        </div>
      ) : (
        <>
          <PeriodTapGrid
            students={students}
            statuses={localStatuses}
            onStatusChange={handleStatusChange}
            disabled={upsert.isPending}
          />
          <PeriodBulkActions
            onMarkAllPresent={handleMarkAllPresent}
            onReset={handleReset}
            onSave={handleSave}
            isSaving={upsert.isPending}
            isSaveDisabled={!hasAnyEntry || subjectSectionFromRoster === null}
          />
        </>
      )}
    </div>
  );
}
