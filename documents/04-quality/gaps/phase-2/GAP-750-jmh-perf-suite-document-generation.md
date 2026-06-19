# GAP-750: JMH micro-benchmark suite for document generators (true p95 SLO measurement)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Testing / Performance
**Detected:** 2026-05-26
**Related PRs:** (TBD — Wave 109+ JMH suite ship PR)
**Related Docs:**
- `documents/01-business/kiteclass/document-generation/rules.md` BR-DOC-PDF-007
- `documents/04-quality/gaps/phase-1-beta/closed/GAP-216-pdf-p95-micro-benchmark.md` (parent — soft-cap canary Wave br-7 Bucket B)
- `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/document/` (3 generator test packages)

## Current State (verified 2026-05-26)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Soft-cap canary tests (Wave br-7 Bucket B / GAP-216) | `PdfGeneratorTest.invoice_render_under_soft_cap_for_regression_canary` (line 183, cap 6s) | ✅ shipped |
| Soft-cap canary tests | `XlsxGeneratorTest.attendance_render_under_soft_cap_for_regression_canary` (line 183, cap 2s) | ✅ shipped |
| Soft-cap canary tests | `DocxGeneratorTest.contract_render_under_soft_cap_for_regression_canary` (line 161, cap 2s) | ✅ shipped |
| BR-DOC-PDF-007 soft-cap clarification | `documents/01-business/kiteclass/document-generation/rules.md` line 32 | ✅ shipped Wave br-7 Bucket B |
| JMH suite (`src/test/java/.../perf/`) | — | ❌ MISSING |
| JMH `@Benchmark` cho `InvoiceGenerationBenchmark` | — | ❌ MISSING |
| JMH `@Benchmark` cho `AttendanceGenerationBenchmark` | — | ❌ MISSING |
| JMH `@Benchmark` cho `TeacherContractBenchmark` | — | ❌ MISSING |
| Weekly scheduled GitHub Actions workflow `perf-bench.yml` | — | ❌ MISSING |
| JMH dependency `org.openjdk.jmh:jmh-core` trong `pom.xml` | — | ❌ MISSING |

**Grep commands run:**
```bash
find kiteclass/kiteclass-core/src/test/java -path "*/perf/*"
# (no output — perf/ folder does not exist)
grep -l "openjdk.jmh\|@Benchmark" kiteclass/kiteclass-core/pom.xml
# (no output)
grep -l "perf-bench\|jmh" .github/workflows/*.yml
# (no output)
```

## Problem

GAP-216 Wave br-7 Bucket B đã ship soft-cap canary (Option B per parent gap §Proposed Fix). Canary là **regression guardrail**, KHÔNG phải SLO measurement đúng nghĩa:
- Single-run System.nanoTime() trên CI runner = noisy (cold JVM, shared executor, GC variance)
- Không capture warmup, mean, p50, p95, p99 distribution
- Không reproducible measurement cross-environment (production hardware vs CI)

BR-DOC-PDF-007 declares p95 budget <2s cho 1-page invoice. Hiện không có cơ chế đo p95 thực sự — chỉ có "single render < 6s soft cap" guard.

## Context

- Wave 5 audit (2026-04-25) flagged GAP-216 P0 — "no code enforces or measures BR-DOC-PDF-007"
- Sub-PR 5.6b shipped Option B soft-cap canary nhanh (Wave 5 closure 2026-04-25 era)
- Wave br-7 Bucket B (2026-05-26) extended canary từ PDF-only sang cả 3 formats + clarified BR-DOC-PDF-007 rule text + filed follow-up này
- True p95 measurement = Wave 7 ops-readiness scope per parent gap §Proposed Fix Option A — NOT Wave 5/br-7 closure blocker
- Impact khi defer: regression detection vẫn hoạt động (canary fires ≥3× SLO); SLO compliance evidence cho audit suite còn weak (rely on production smoke + CloudWatch metrics)

## Evidence

Parent gap GAP-216 (now closed Wave br-7 Bucket B per soft-cap ship):
```
documents/04-quality/gaps/phase-1-beta/closed/GAP-216-pdf-p95-micro-benchmark.md
```

Parent §Proposed Fix Option A reference:
> Add `kiteclass-core/src/test/java/com/kiteclass/core/module/document/perf/`:
> - `InvoiceGenerationBenchmark.java` — JMH `@Benchmark` annotated...
> - `AttendanceGenerationBenchmark.java` — same pattern for XLSX...
> - `TeacherContractBenchmark.java` — DOCX...
> Run via `mvn -Dtest=*Benchmark surefire:test` or dedicated profile. Not in default CI loop (too slow, JMH wants warmup + measurement iterations); run weekly via scheduled workflow.

## Proposed Fix

1. **Add JMH dependency** to `kiteclass/kiteclass-core/pom.xml`:
   - `org.openjdk.jmh:jmh-core:1.37`
   - `org.openjdk.jmh:jmh-generator-annprocess:1.37` (annotation processor)
   - Scope `test` (perf suite is test-only)

2. **Create `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/document/perf/`:**
   - `InvoiceGenerationBenchmark.java` — JMH `@Benchmark` cho `PdfGenerator.generate(invoice)`
     - `@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)`
     - `@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)`
     - `@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})` — capture mean + p50/p95/p99
     - `@OutputTimeUnit(TimeUnit.MILLISECONDS)`
   - `AttendanceGenerationBenchmark.java` — same pattern cho `XlsxGenerator`
   - `TeacherContractBenchmark.java` — same pattern cho `DocxGenerator`
   - Shared base class `DocumentGenerationBenchmarkBase` cho sample data + setup

3. **Maven profile `perf-bench`** trong `kiteclass/kiteclass-core/pom.xml`:
   - Excluded from default `mvn verify`
   - Activated via `mvn verify -P perf-bench`
   - Surefire-only execution (no integration tests)

4. **Scheduled GitHub Actions workflow** `.github/workflows/perf-bench.yml`:
   - Cron schedule: weekly (e.g., `0 2 * * 0` Sunday 02:00 UTC)
   - Runs `mvn verify -P perf-bench` trong `kiteclass-core`
   - Parses JMH JSON output → asserts p95 < SLO (2s for PDF/XLSX/DOCX scaled per BR-DOC-PDF-007)
   - Posts results to GitHub issue OR Slack/Discord webhook
   - Fails workflow nếu p95 > 1.5× SLO (3s) cho 2 consecutive weeks (regression alarm)

5. **Update BR-DOC-PDF-007** trong `rules.md` once JMH lands:
   - Replace "True p95 SLO measurement requires JMH... GAP-750" với reference to actual perf-bench workflow + last-week-measured p95 number

## Acceptance Criteria

- [ ] JMH dependency added to `kiteclass/kiteclass-core/pom.xml` test scope
- [ ] 3 `@Benchmark` classes created (Invoice / Attendance / TeacherContract) với warmup + measurement annotations
- [ ] Maven profile `perf-bench` activates JMH execution (excluded from default `mvn verify`)
- [ ] Scheduled workflow `.github/workflows/perf-bench.yml` runs weekly
- [ ] Workflow asserts p95 < 2s (PDF) / p95 < 1s (XLSX, DOCX) on production-equivalent runner spec
- [ ] BR-DOC-PDF-007 updated post-JMH-landing với measured p95 numbers
- [ ] First weekly run produces baseline JSON artifact archived under `documents/04-quality/audits/performance/YYYY-MM-DD-jmh-baseline.json`

## Related

- **Parent:** GAP-216 (Wave 5 audit P0-2 finding, closed Wave br-7 Bucket B via soft-cap canary Option B)
- **Related performance gaps:** GAP-210 (async queue trigger), GAP-135 (SLO targets), GAP-115 (monitoring dashboards)
- **Rule:** `documents/01-business/kiteclass/document-generation/rules.md` BR-DOC-PDF-007
- **Audit scope:** Wave 7+ ops-readiness category per `.claude/skills/quality/ops-readiness-audit/SKILL.md`
- **Audit:** `documents/04-quality/audits/performance/performance-audit-2026-04-25-wave5.md` (original P0-2)

## Log

- 2026-05-26 — Filed Wave br-7 Bucket B (per GAP-216 §Proposed Fix Option A defer + Acceptance criterion 4 "File follow-up gap for full JMH suite"). State-check verified perf/ folder absent + JMH dep absent + workflow absent. Soft-cap canary parent (GAP-216) shipped same wave; this gap tracks Option A future scope.
