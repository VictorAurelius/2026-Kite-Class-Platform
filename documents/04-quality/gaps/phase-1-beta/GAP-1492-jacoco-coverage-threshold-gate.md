# GAP-1492: Jacoco report chạy nhưng không có coverage threshold gate (silent regress)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Phase:** phase-1-beta
**Found:** 2026-06-21 (quality-audit /110 refresh — phase1-closeout-loop)
**Affects:** `kitehub/pom.xml`, `kiteclass/kiteclass-core/pom.xml`, `.github/workflows/core-ci.yml` (+ các CI workflow chạy test)

## Problem

GAP-1344 đã cấu hình `jacoco-maven-plugin` (kitehub/pom.xml L222, kiteclass-core/pom.xml L457) với execution `prepare-agent` + `report` + `report-merged`, và `core-ci.yml` L62 chạy `./mvnw jacoco:report`. Tuy nhiên:

1. **Không có `jacoco:check` execution với `<rules><rule>` + `<minimum>` threshold** trong bất kỳ pom nào → build KHÔNG fail khi coverage tụt dưới ngưỡng.
2. **Coverage % không được surface/assert trong CI** (không upload Codecov, không in coverage summary, không gate PR) → coverage có thể silent regress qua các wave.

Hệ quả: quality-audit Cat 3 (Backend Tests) sub-check "coverage >70%" vẫn KHÔNG verify được kết quả thật — report được sinh nhưng không ai đọc/enforce. GAP-1344 đóng phần "configure plugin" nhưng phần "đo + enforce" còn hở. Đây là finding NEW (distinct với GAP-1344 đã DONE).

Evidence: `grep jacoco kitehub/pom.xml` cho thấy execution `report` + `report-merged` nhưng KHÔNG có `check`/`<minimum>`/`COVEREDRATIO` rule.

## Proposed Fix

1. Thêm `jacoco:check` execution (bound `verify` phase) với rule baseline khiêm tốn (vd LINE COVEREDRATIO `minimum=0.60`) ở kitehub/pom.xml + kiteclass-core/pom.xml — đặt thấp ban đầu để không block, nâng dần.
2. CI in coverage summary (parse `target/site/jacoco/jacoco.xml`) HOẶC upload artifact để track trend.
3. Document baseline coverage % hiện tại trong audit để các refresh sau đo delta.

## Acceptance Criteria

- [ ] `jacoco:check` rule có `<minimum>` threshold trong cả 2 pom (kitehub + kiteclass-core)
- [ ] CI surface coverage % (summary log hoặc artifact) trên mỗi run
- [ ] Baseline coverage % ghi nhận trong 1 audit/doc để track regress

## Related

- Discovered in: quality-audit `documents/04-quality/audits/quality-audit/2026-06-21-quality-full-audit.md` (Cat 3)
- Builds on: GAP-1344 (Jacoco config — DONE; this is the enforce-layer follow-on)
- Related: GAP-987 (kiteclass-core IT ddl-auto masks migration drift — test fidelity sibling)
