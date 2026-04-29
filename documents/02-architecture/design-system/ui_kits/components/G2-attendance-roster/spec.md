# G2 — Attendance Roster

**Component gap:** G2 per `documents/02-architecture/design-system/dossier/04-component-gaps.md` §G2
**Flow ref:** `dossier/05-business-flows.md` Flow #3 (Daily attendance — teacher)
**Used by screens:** KiteClass `/classes/[id]/attendance`, Teacher dashboard daily view, Parent attendance history (read-only variant)
**Persona:** Teacher (homeroom + subject) — used on tablet at center, desktop in office

---

## Purpose

Mark daily attendance for a class session. Teacher toggles per-student status: **P** (Present, có mặt) / **V** (Vắng có phép, excused absence) / **M** (Vắng không phép, unexcused) / **L** (Late, đi trễ). Optimistic UI; saves on bar action.

Replaces `attendance-form-list.tsx` repo pattern with a design-system-grade roster: bigger touch targets (44×44 minimum), inline edit history after save-lock, color-coded chips (green/blue/red/amber).

---

## Props (TypeScript-ish)

```ts
interface AttendanceRosterProps {
  classSession: {
    id: string;
    className: string;        // "Lớp 10A2 - Toán nâng cao"
    sessionNumber: number;    // 12 (12th session of semester)
    date: Date;               // 2026-04-15T14:00:00+07:00
    durationMinutes: number;  // 90
    teacherName: string;      // "Cô Nguyễn Thị Lan"
  };
  students: Array<{
    id: string;
    fullName: string;          // "Nguyễn Văn An"
    studentCode: string;       // MST — student ID
    avatarUrl?: string;
    currentRate: number;       // 0..1, e.g. 0.92 for 92% attendance YTD
    status: "P" | "V" | "M" | "L" | null;  // null = not yet marked
    lateMinutes?: number;      // when status = "L"
    note?: string;             // excuse note, when V
  }>;
  state: "default" | "marking" | "saving" | "saved" | "error" | "empty" | "loading";
  onChange: (studentId: string, status: AttendanceStatus, opts?: { lateMinutes?: number; note?: string }) => void;
  onSave: () => Promise<void>;
  onMarkAllPresent: () => void;
}

type AttendanceStatus = "P" | "V" | "M" | "L";
```

---

## States

| State | When | UI |
|-------|------|-----|
| `loading` | Initial fetch | Skeleton: header + 5 row skeletons |
| `empty` | No students enrolled | "Chưa có học sinh nào trong lớp" + "Thêm học sinh" CTA |
| `default` | Loaded, none marked | All rows show 4-button group, save bar hidden |
| `marking` | User toggling | Optimistic UI: row chip color updates immediately, save bar shows "N thay đổi" + "Lưu" button |
| `saving` | onSave in flight | Save bar disabled with spinner |
| `saved` | Save complete | Toggle locked (read-only chips), edit history visible inline (`Đã lưu lúc 14:35` + `[Sửa]` link) |
| `error` | Save failed | Inline banner "Không tải được danh sách" + retry CTA |

---

## Accessibility

- Touch targets ≥ 44×44 (tablet primary surface)
- 4-button toggle group uses `role="radiogroup"` with `aria-label="Trạng thái cho [tên]"`
- Each button has `aria-checked` + visible label (`P` glyph + "Có mặt" sr-only)
- Color is NOT the only signal — letter glyph in chip + sr-only text per status
- Save bar `role="region" aria-label="Thanh thao tác"` sticky at bottom
- Keyboard: arrow keys cycle within radio group; `Enter` to confirm; `Tab` to next student
- Focus indicator ≥2px ring with 3:1 contrast on amber buttons
- `prefers-reduced-motion`: optimistic chip color change uses opacity instead of slide

---

## Vietnamese UX considerations

- Status codes per `dossier/02-vietnamese-ux-musts.md` §3:
  - **P** green = "Có mặt" (Present)
  - **V** blue = "Vắng có phép" (Excused)
  - **M** red = "Vắng không phép" (Unexcused)
  - **L** amber = "Đi trễ" (Late)
- Vietnamese names: surname-first sort (`dossier/02` §1) — `Nguyễn Văn An` sorted under N, not A
- Date format `dd/MM/yyyy` short, `dd 'tháng' MM 'năm' yyyy` long
- Time 24h `HH:mm` — `14:00 - 15:30`
- "Lớp 10A2" naming — never "Class 1"
- Class metadata: 25 students typical (not 7 or 500); attendance rate inline as `92%` chip
- Late minutes input: `5` / `10` / `15` quick-pills + free input
- Excuse note popover: 1-line input, common reasons preset (`Ốm`, `Việc gia đình`, `Học thi`)
- Save bar copy: `5 thay đổi` (NOT `5 changes`)
- Empty state copy: `Chưa có học sinh nào trong lớp này. [Thêm học sinh]` (per `02` §9 informal `bạn`)
- Saved state: `Đã lưu lúc 14:35 — [Lê Thị Hà]` (teacher name)

---

## Component reuse (per `dossier/09-tech-constraints.md`)

- shadcn `Button` with variants for chip — `default` for active, `outline` for inactive
- shadcn `Avatar` for student photos (KC has it)
- lucide icons: `Check`, `XCircle`, `AlertCircle`, `Clock`, `MoreHorizontal`
- TanStack Table NOT required — list is simpler with flex rows
- shadcn `Dialog` for excuse note popover
- shadcn `Toast` (KC `toaster`) for save confirm
- date-fns `format(date, "EEEE 'ngày' dd/MM/yyyy")` with `vi` locale

---

## Touch UX rules

- Roster is primary tablet surface (8" iPads, Android 10" tablets at center)
- Buttons in 4-button toggle: `min-w-[44px] min-h-[44px]` — actual size 48×48 with 4px gap
- Sticky save bar at bottom on mobile/tablet — desktop: fixed at top of viewport
- Avoid hover-only — long-press shows excuse note popover; tap shows status menu

---

## Performance signals

- Virtualize when ≥50 students (React Virtual or TanStack Virtual)
- Debounce `onSave` 500ms after last toggle (auto-save can be opt-in)
- Optimistic update local state immediately; reconcile on server response
- Estimated bundle impact: ~12 KB gzipped (no Framer Motion needed)

---

## Self-score (each state file)

See per-screen score comments in `default.html`, `loading.html`, `empty.html`, `error.html`, `success.html`. Aggregate target ≥105/128 per state.
