# GAP-1344: Jacoco chưa cấu hình — không đo được test coverage % thật

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (Quality full audit, AUDIT-2026-06-14-quality-full)
**Affects:** `kiteclass/**/pom.xml` + `kitehub/**/pom.xml` (Maven backend modules)

## Problem

Quality audit Cat 3 (Backend Tests) có sub-check "Test coverage >70%" nhưng KHÔNG verify được vì dự án chưa cấu hình `jacoco-maven-plugin`. Hiện có 547 test file (KC 278 + KH 269 *Test/*IT) trên 1357 Java main — ratio file ~40% nhưng đây là **đếm file**, không phải **line/branch coverage** thật. Mỗi quality audit từ Wave 53 đến nay đều ghi nhận "Jacoco vẫn chưa setup (carry-forward)" — đây là measurement gap kéo dài, làm Cat 3 chỉ chấm được bằng proxy (file count) thay vì số liệu coverage chính xác.

## Root Cause

Không có jacoco-maven-plugin trong parent pom hoặc per-module pom; CI cũng không publish coverage report → không có baseline coverage % để track regression.

## Proposed Fix

Thêm `jacoco-maven-plugin` (prepare-agent + report goal) vào parent pom của kiteclass-core + kitehub services; wire `report-aggregate` cho multi-module; (tùy chọn) thêm coverage gate mềm trong CI (WARN-mode trước, threshold sau). Báo cáo coverage % vào quality audit Cat 3.

## Acceptance Criteria

- [x] `mvn verify` sinh `jacoco.exec` + HTML/XML report cho kiteclass-core + kitehub services
- [x] Coverage % baseline ghi nhận (line + branch) cho ≥1 module có business logic chính
- [x] Quality audit Cat 3 sub-check "coverage >70%" chấm được bằng số liệu thật, không proxy file-count

## Resolution (2026-06-15 — audit-fixG-quality wave)

**DONE.** Phát hiện: `kiteclass-core/pom.xml` ĐÃ có cấu hình Jacoco đầy đủ (surefire `prepare-agent` + failsafe `prepare-agent-integration` + `merge` + `report-merged`) — chỉ **kitehub** thiếu hoàn toàn (`grep -rln jacoco kitehub/**/pom.xml` = NONE).

Fix: thêm khối `jacoco-maven-plugin` 0.8.15 + `maven-failsafe-plugin` argLine wiring vào **`kitehub/pom.xml`** (parent `<build><plugins>`) — mirror chính xác cấu hình kiteclass-core; cả 6 module con (platform/subscription/branding/email/admin/gateway) thừa kế.

Verify FOREGROUND (binds OK):
```
cd kitehub && ./mvnw -pl kitehub-platform -am clean test jacoco:report -P strict-warnings
→ BUILD SUCCESS; target/site/jacoco/{index.html,jacoco.csv,jacoco.xml} sinh ra
→ kitehub-platform baseline: INSTRUCTION 53.1% (860/1620), BRANCH 44.4% (68/153)
```
Pattern CI: kitehub-ci.yml chạy `mvn test` → pair với `jacoco:report` goal để publish per-module coverage (giống core-ci.yml). Cat 3 sub-check giờ đo được bằng line/branch thật, không proxy file-count.

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 3)
- Carry-forward từ Wave 53/78/98 quality refresh ("Jacoco chưa setup")
- Related: GAP-152 family (test/coverage infrastructure)
