# Eval Fixture — edge-config-key-renamed.md

# Expected: FAIL — rules.md uses old key, code uses new key (silent drift)

**Skill:** `quality/business-logic-audit`
**Scenario:** Synthetic case where someone renamed a config key in code but
forgot to update `rules.md`. Both keys "work" in some test paths, masking
the inconsistency.
**Which check fires:** Category 2 — Config Accuracy (-4); Category 4 —
Cross-Domain Consistency may also flag if the key is referenced in another
domain.
**Filed:** 2026-04-28 (GAP-253 pilot)

---

## Setup (synthetic)

`rules.md` (stale):

```markdown
- **BR-ATT-002** Late threshold: configurable via
  `kiteclass.attendance.late-threshold-minutes` (default 10).
```

`application.yml` (renamed in PR #555):

```yaml
kiteclass:
  attendance:
    late-grace-minutes: 10   # ← renamed from late-threshold-minutes
```

`AttendanceService.java`:

```java
@Value("${kiteclass.attendance.late-grace-minutes}")
private int lateGraceMinutes; // ← uses new key
```

The old key `late-threshold-minutes` no longer exists in the YAML. The audit
skill must catch this drift — even though `lateGraceMinutes` works at
runtime, `rules.md` lies about what's configurable.

---

## Why this is an "edge" case

Naive grep "find BR-ATT-002 in code" → matches the comment in
`AttendanceService.java`. Pass. But the **config value** in rules.md
doesn't match `application.yml`. Subtle.

Detection requires a 2-step check:
1. Extract config key from rules.md (`late-threshold-minutes`)
2. Grep that key in `application*.yml` — if missing, FAIL Category 2

---

## Expected audit-report excerpt

```
## Category 2: Config Accuracy         16/20  (-4)

### Drift detected:
- `kiteclass.attendance.late-threshold-minutes` named in rules.md (BR-ATT-002)
  but not present in application.yml. Code uses
  `kiteclass.attendance.late-grace-minutes`.

### Root cause:
- PR #555 renamed the config key but did not update rules.md (Living Docs
  rule violation per CLAUDE.md §Business Logic Documents).

### Recommended actions:
1. Decide which name is canonical (likely keep new `late-grace-minutes`)
2. Update rules.md BR-ATT-002 with new key + bump `updated:` in frontmatter
3. File gap if pattern recurs across other domains
```

---

## How to use this fixture

This case echoes a real pattern from `feedback_post_merge_doc_sync.md` — code
and docs drift across the same PR. The audit skill should treat config-key
drift as more severe than missing rules because it's silent (runtime works,
docs lie).

Regression test: when audit skill body is edited, verify this fixture still
fires Category 2 with `-4`. If a future change exempts config drift (e.g.
per the rationale "key is internal"), reviewer must explicitly justify the
loosening.
