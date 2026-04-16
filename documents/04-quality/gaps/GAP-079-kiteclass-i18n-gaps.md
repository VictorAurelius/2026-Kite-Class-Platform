# GAP-079: KiteClass i18n Inconsistencies

**Status:** 🔵 OPEN
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

- [ ] 0 English strings trong user-facing UI (trừ technical terms: email, URL)
- [ ] Date format dd/mm/yyyy trên tất cả forms
- [ ] File upload buttons styled (không browser default)
- [ ] All status badges tiếng Việt
