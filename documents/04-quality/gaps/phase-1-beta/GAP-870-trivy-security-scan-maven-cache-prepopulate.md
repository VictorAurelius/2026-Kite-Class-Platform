---
id: GAP-870
title: Trivy Security Scan transient Maven 429 — workflow cần pre-populate Maven cache
status: OPEN
priority: P1
phase: phase-1-beta
domain: DevOps
created: 2026-06-02
last_updated: 2026-06-02
---

# GAP-870 — Trivy Security Scan transient Maven 429 (workflow cache pre-populate)

> Surfaced 2026-06-02 Wave local-doable-7 Bucket C closure (PR #2070) + #2064 IT warning fix. Same root cause hits 2 PR cùng session.

## Problem

`.github/workflows/<security-scan>.yml` Trivy `scan-type: fs` cố fetch remote POMs từ Maven Central trực tiếp:

```
2026-06-02T07:09:05Z  FATAL  Error  remote Maven repository returned 429 Too Many Requests
for https://repo.maven.apache.org/maven2/org/springframework/pulsar/spring-pulsar-bom/1.2.16/spring-pulsar-bom-1.2.16.pom.
Retry-After: 1800.
The repository blocks all subsequent requests from this IP until the block clears.
To avoid this, populate the local Maven cache before scanning (e.g. run `mvn dependency:resolve` and cache ~/.m2 in CI).
```

**Impact:** Security Scan fails transient → 2 PR cùng session blocked (PR #2070 Wave 7 Bucket C kc-core RabbitAdmin fix + PR #2064 IT warning suppression).

## Root Cause

Trivy `scan-type: fs` fs scan re-fetches transitive POMs ngay cả khi local có Maven dependencies → repeated requests trigger Maven Central rate-limit (`Retry-After: 1800` = 30 phút block).

## Proposed Fix

Per Trivy error hint — pre-populate Maven cache trong workflow trước khi run Trivy:

```yaml
- name: Pre-populate Maven cache
  run: |
    cd kitehub && ./mvnw dependency:resolve -q
    cd kiteclass && ./mvnw dependency:resolve -q

- name: Cache Maven dependencies
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: maven-${{ hashFiles('**/pom.xml') }}

- name: Run Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    scan-type: fs
    # ... existing config ...
```

Maven cache hit → Trivy reads từ local `~/.m2` thay vì fetch remote → 0 Maven Central requests → 0 rate-limit.

## Acceptance Criteria

- [ ] Workflow `.github/workflows/<security-scan>.yml` (locate via `Glob ".github/workflows/*security*.yml"`) thêm step pre-populate Maven cache TRƯỚC khi Trivy run
- [ ] `actions/cache@v4` wired key `maven-${{ hashFiles('**/pom.xml') }}` per Maven best practice
- [ ] Trivy step runs offline (no Maven Central fetch) — verify via fresh PR test (zero `429` error trong logs)
- [ ] 2 unblocked PR (#2070 + #2064) post-fix CI rerun PASS Security Scan

## Related

- PR #2070 (Wave 7 Bucket C kc-core RabbitAdmin fix) — blocked by this gap; admin-merge với `ADMIN_MERGE_OVERRIDE: GAP-870` trailer
- PR #2064 (IT warning suppression) — same blocking pattern; same override trailer
- Per `release-fix-retry-budget.md` §5 row 4 "Test environment flake (transient network, runner image bug)" — acceptable retry với trailer `RELEASE_RETRY_TRANSIENT`
- Per `admin-merge-discipline.md` §4 override mechanism — `ADMIN_MERGE_OVERRIDE:` trailer + follow-up gap link

## Log

- **2026-06-02:** Filed during Wave 7+8 closure batch. 2 PR cùng session bị Security Scan Maven 429 transient. Root cause Trivy không cache Maven deps; per error hint pre-populate `~/.m2` trong CI. Per `admin-merge-discipline.md` §4 + `release-fix-retry-budget.md` §5 — 2 PR admin-merge với override trailer pointing here.
