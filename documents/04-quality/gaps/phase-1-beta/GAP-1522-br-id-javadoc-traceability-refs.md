# GAP-1522: BR-ID javadoc traceability refs trong kitehub Java services

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta (code ↔ business-rule traceability hygiene)
**Found:** 2026-06-21 (tách từ GAP-666 — README index sync DONE, javadoc half deferred)
**Affects:** `kitehub/kitehub-email` + `kitehub/kitehub-platform` + `kitehub/kitehub-subscription` Java services / Living Docs verification chain `BR-xxx → @Mapping`

## Problem

GAP-666 (Wave 98 BL audit) gồm 2 deliverable: (1) README index sync — **DONE 2026-06-21**; (2) BR-ID javadoc refs — code-heavy, tách thành gap này.

`grep -rnE "BR-(EMAIL|SEED|PREFERENCES)-[0-9]+" kitehub/ --include="*.java"` = **0 hits** (verified 2026-06-21). Business rules đã định nghĩa trong `documents/01-business/kitehub/{email,seed,preferences}/rules.md` (IDs BR-EMAIL-001..007, BR-SEED-001..010, BR-PREFERENCES-*) nhưng các Java service class KHÔNG javadoc reference ngược lại → Living Docs verification chain `BR-xxx → UC-xxx → endpoint → @Mapping → @Test` đứt ở mắt xích `BR-xxx → @Mapping` (manual-only). Future grep "find code implementing BR-X" trả về rỗng.

Tách khỏi GAP-666 vì: annotation spans 3 services (kitehub-email + kitehub-platform + kitehub-subscription) → biến PR docs-only thành code PR (cần full CI java test). README sync (docs-only, reader-facing) đã ship riêng.

## Proposed Fix

Annotate javadoc với `{@code BR-XXX-NNN}` block + `@see documents/01-business/kitehub/{domain}/rules.md` cho các class:

| Class | File | BR-IDs |
|-------|------|--------|
| `EmailTemplateRenderer` | `kitehub-email/.../service/EmailTemplateRenderer.java` | BR-EMAIL-001/002/005 |
| `ResendEmailService` | `kitehub-email/.../service/ResendEmailService.java` | BR-EMAIL-003 (Resend channel) |
| `SESEmailService` | `kitehub-email/.../service/SESEmailService.java` | BR-EMAIL-003 (SES channel) |
| `VietnamSampleDataGenerator` | `kitehub-platform/.../seed/` (hiện chỉ có `VietnamSampleDataGeneratorTest`) | BR-SEED-001/002/003/004 |
| `SeedWorkerService` | kitehub-platform seed (consumer wiring) | BR-SEED-005 (defer per GAP-658 nếu chưa wired) |
| `PreferencesController` | `kitehub-subscription/.../preferences/controller/PreferencesController.java` | BR-PREFERENCES-* |

Trước khi annotate: verify BR-IDs thực tế tồn tại trong từng `rules.md` (tránh ref ID không có thật).

## Acceptance Criteria

- [ ] ≥5 Java class annotated với BR-ID javadoc references (verify BR-IDs khớp rules.md thực tế)
- [ ] Grep `BR-(EMAIL|SEED|PREFERENCES)-[0-9]+` trả về ≥10 hits trong `kitehub/` Java
- [ ] Verification chain: chọn 1 BR-ID (vd BR-EMAIL-001) → grep code → tìm thấy class implementing
- [ ] Java vẫn compile (`./mvnw -pl <module> compile` cho 3 module touched)

## Related

- **Parent gap:** GAP-666 (README sync half DONE; javadoc half = gap này)
- **Parent audit:** `documents/04-quality/audits/business-logic/2026-05-19-wave-98-new-domains.md`
- **Rule:** CLAUDE.md §"Business Logic Documents 3-Layer Structure" verification chain
- **Rule:** `audit-to-gap-pipeline.md` Living Docs verification chain
- **Note:** `VietnamSampleDataGenerator` hiện chỉ có dạng `*Test` trong `kitehub-platform/src/test/` — confirm production class location trước khi annotate; `SeedWorkerService` defer per GAP-658 nếu consumer chưa wired.
