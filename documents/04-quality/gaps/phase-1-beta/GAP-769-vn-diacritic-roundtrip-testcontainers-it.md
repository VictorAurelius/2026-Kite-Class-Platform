---
audience: dev
---

# GAP-769 — Testcontainers IT VN diacritic roundtrip for BetaAccessService

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (test)
**Found:** 2026-05-27 (Wave 106 GAP-764 fix follow-up)
**Affects:** Beta access flow regression-guard cho VN diacritic roundtrip — currently no IT covers UTF-8 preservation through sanitization layer
**Phase:** phase-1-beta

## Problem

Wave 106 GAP-764 fix shipped code (`HtmlUtils.htmlEscape(stripped, "UTF-8")`) + Flyway V57 backfill + META rule `vn-localization-audit-checklist.md` v1.1.0 §5 (mandate VN diacritic roundtrip test for sanitization). Nhưng PR scope không include integration test cho rule §5 — IT deferred do scope budget.

Per `vn-localization-audit-checklist.md` §5: every PR adding/modifying input sanitization touching tenant-facing field MUST include integration test covering VN diacritic roundtrip. Wave 106 GAP-764 fix is a fix-PR (not new sanitization) — but rule applies prospectively cho future sanitization changes.

Adding IT now provides regression-guard:
- Catch regression nếu future PR reverts `HtmlUtils.htmlEscape(stripped, "UTF-8")` về single-arg
- Validate rule §5 enforcement via concrete test
- Self-test fixture cho rule §5 worked example

## Proposed Fix

Add new test class `BetaAccessServicePostgresIT.java` (sister to existing `BetaAccessRequestRepositoryPostgresIT.java`):

```java
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class BetaAccessServicePostgresIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired BetaAccessService service;
    @Autowired BetaAccessRequestRepository repository;

    @Test void vn_diacritic_roundtrip_org_name() {
        BetaAccessRequest req = service.requestBetaAccess(
            "Trần Thị Hồng",
            "hong+rt1@test.vn",
            "0901234567",
            "Trung tâm Anh ngữ Sky Education",
            "P2_CENTER_OWNER",
            null,
            true,
            ""
        );
        repository.flush();
        BetaAccessRequest reloaded = repository.findById(req.getId()).orElseThrow();
        assertThat(reloaded.getOrgName()).isEqualTo("Trung tâm Anh ngữ Sky Education");
        assertThat(reloaded.getName()).isEqualTo("Trần Thị Hồng");
    }

    @Test void vn_diacritic_full_set_preserve() {
        // Test ALL 7 VN-frequent diacritics: â ê ô ữ ồ ằ ấ
        String fullDiacritic = "âÂêÊôÔữỮồỒằẰấẤ";
        BetaAccessRequest req = service.requestBetaAccess(
            fullDiacritic, "rt2@test.vn", "0901234567",
            "Trung tâm " + fullDiacritic, "P2_CENTER_OWNER",
            null, true, ""
        );
        repository.flush();
        BetaAccessRequest reloaded = repository.findById(req.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo(fullDiacritic);
        assertThat(reloaded.getOrgName()).contains(fullDiacritic);
    }

    @Test void xss_chars_still_escaped() {
        // Verify XSS sanitization still works (regression-guard against §5 fix)
        BetaAccessRequest req = service.requestBetaAccess(
            "Test <script>alert('xss')</script>",
            "rt3@test.vn", "0901234567",
            "Org & \"Quoted\"", "P2_CENTER_OWNER",
            null, true, ""
        );
        repository.flush();
        BetaAccessRequest reloaded = repository.findById(req.getId()).orElseThrow();
        assertThat(reloaded.getName()).doesNotContain("<script>");
        assertThat(reloaded.getOrgName()).contains("&amp;").contains("&quot;");
    }
}
```

## Acceptance Criteria

- [ ] `BetaAccessServicePostgresIT.java` created với 3 test methods (VN diacritic + full set + XSS regression)
- [ ] Tests run trong CI via Testcontainers (already wired cho subscription module — see existing `BetaAccessRequestRepositoryPostgresIT.java`)
- [ ] All 3 tests PASS locally + CI

## Related

- Parent: GAP-764 (DONE) — fix that introduced need for this regression-guard
- META rule: `.claude/rules/vn-localization-audit-checklist.md` v1.1.0 §5
- Sister rule: `.claude/rules/postgres-specific-type-testcontainers.md` v1.0.0 (same test layer pattern)
- Existing pattern: `BetaAccessRequestRepositoryPostgresIT.java`
