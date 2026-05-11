# GAP-079: KiteClass i18n Inconsistencies

**Status:** ✅ DONE
**Priority:** 🟠 P1
**Domain:** Frontend / Internationalization
**Found:** 2026-04-16 (UI audit)
**Affects:** Multiple KiteClass pages

## Problem

UI chủ yếu tiếng Việt nhưng còn rải rác English strings:

| Location | English String | Should Be |
|----------|---------------|-----------|
| register-student form | "Select an option" (gender dropdown) | "Chọn giới tính" |
| register-student form | Date format `mm/dd/yyyy` | `dd/mm/yyyy` |
| Table pagination | "Rows per page" | "Số hàng mỗi trang" |
| Session status badges | "COMPLETED" | "Hoàn thành" |
| Branding wizard steps | "4. Tone", "5. Template", "6. Preview" | Vietnamese labels |
| File upload buttons | "Choose File" (browser default) | Custom styled button |

## Proposed Fix

1. Search codebase cho hardcoded English strings: `grep -rn "Select an option\|Rows per page\|Choose File" --include="*.tsx"`
2. Replace với Vietnamese equivalents
3. Date format: check `<input type="date">` locale setting hoặc dùng custom date picker
4. "Choose File": replace native `<input type="file">` với styled component
5. Status badges: check enum → label mapping

## Acceptance Criteria

- [x] 0 English strings trong user-facing UI (trừ technical terms: email, URL)
- [ ] Date format dd/mm/yyyy trên tất cả forms (native date input, requires custom date picker — deferred)
- [ ] File upload buttons styled (không browser default — deferred, no file uploads found in current UI)
- [x] All status badges tiếng Việt

## Resolution

Fixed in PR `fix/frontend-p1-gaps`. Files changed:
- `data-table.tsx`: "No results found." → "Không tìm thấy kết quả.", "Rows per page" → "Số hàng mỗi trang", "Previous/Next" → "Trước/Sau", "Page X of Y" → "Trang X / Y"
- `error-alert.tsx`: "Error" → "Lỗi", "Try Again" → "Thử lại", "Dismiss" → "Bỏ qua"
- `loading-spinner.tsx`: "Loading..." → "Đang tải..."
- `search-input.tsx`: "Search..." → "Tìm kiếm...", "Clear search" → "Xóa tìm kiếm"
- `header.tsx`: All menu items, notifications, search placeholder → Vietnamese
- `status-badge.tsx`: Added Vietnamese translation map for all common status enums
- `dashboard/page.tsx`: Invoice status badges display Vietnamese labels
- `FeatureGate.tsx`: All error/upgrade messages → Vietnamese
- `useFeatureDetection.ts`: Error message → Vietnamese
- `WizardProgress.tsx`: "Tone/Template/Preview" → "Phong cách/Mẫu/Xem trước"
- `PreviewStep.tsx`: All labels → Vietnamese
- `ToneStep.tsx`, `TemplateStep.tsx`: aria-labels → Vietnamese
- `CMSEditor.tsx`: "Enter items, one per line" → Vietnamese
- `teacher-columns.tsx`: aria-labels → Vietnamese
- Tests updated to match new Vietnamese strings
