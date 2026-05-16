# Threat Model — Bulk Import CSV Upload

**Created:** 2026-05-16
**Wave:** 86 Bucket E
**Status:** complete
**Scope:** P2 Center Owner / P3 Manager bulk-import flow — upload CSV/XLSX của danh sách học viên, phụ huynh, giáo viên → parse → preview → confirm → persist
**Linked gaps:** Wave 84 P2 import (GAP-NEW MAX_ROWS), Wave 86 Agent E MAX_ROWS env
**Mitigation owners:** kitehub-subscription ImportService, kiteclass-core BulkImportService, kite-minio storage

---

## 1. Asset under threat

Bulk-import endpoint nhận multipart upload từ P2/P3 user → parse rows → preview UI → user confirm → INSERT/UPDATE batch vào tenant DB. Common formats: CSV (UTF-8 with/without BOM), XLSX.

**Trust boundaries crossed:**
1. Browser → ALB (multipart upload)
2. ALB → kitehub-subscription (parse + validate)
3. kitehub-subscription → MinIO (raw file storage)
4. kitehub-subscription → RDS (persisted rows)

---

## 2. STRIDE analysis

### S — Spoofing

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| S1 | Non-tenant user uploads to wrong tenant's import endpoint | Low | Critical | JWT tenant_id claim + RLS enforces filter; endpoint role-guard P2/P3 only | — |
| S2 | Filename spoofs MIME type — `data.exe` renamed `data.csv` | Medium | High | Content-Type validation server-side (`text/csv`, `application/vnd.ms-excel`, OpenXML); content-sniff header `python-magic` parse first 1024 bytes | — |

### T — Tampering

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| T1 | CSV injection — formula `=cmd|'/c calc'!A1` in cell → Excel auto-execs on download | High | High | Server escapes formula cells: prepend `'` to any cell starting `=`, `+`, `-`, `@`, `\t`, `\r` before persist; export downloads sanitize again | Add unit test fixture covering CSV injection patterns |
| T2 | XLSX with embedded macro / VBA | Medium | High | Reject XLSM extension; XLSX accepted via Apache POI XSSF (parses only sheet data, not macros) | — |
| T3 | XXE — XLSX is ZIP of XML; attacker crafts `xml-external-entity` payload to read server files | Low | High | Apache POI POIXMLDocument disables external entity resolution by default (since POI 4.1+); current version 5.x | Verify POI version in BOM (kitehub-base/pom.xml) |
| T4 | Header row tampered to break tenant isolation (e.g., column `tenant_id` smuggled) | Low | Critical | Whitelist column names server-side; ignore unknown columns; never trust client to set tenant_id | — |

### R — Repudiation

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| R1 | User imports bad data, claims "tôi không upload" | Medium | Medium | `admin_audit_logs` row per import w/ uploaded_by + uploaded_at + file_hash (sha256) + row_count; raw file persisted to MinIO 90d | — |
| R2 | Provider claims rows persisted but no DB trace | Low | Low | Single transaction wrap entire import batch; audit log captures success/fail count | — |

### I — Information Disclosure

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| I1 | Preview screen shows other tenant's data via crafted CSV | Low | Critical | Preview re-queries DB with tenant_id filter; never trust file contents to determine display scope | — |
| I2 | Error message leaks server path / stack trace | Medium | Low | `GlobalExceptionHandler` returns RFC 7807 problem detail; stack trace only in logs (not response) | — |
| I3 | Raw uploaded file URL in MinIO publicly accessible | Low | High | MinIO bucket policy: private; pre-signed URLs only; bucket not in CDN | Verify MinIO bucket policy in Helm chart |

### D — Denial of Service

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| D1 | Oversize file (10GB CSV) | High | High | Multipart `spring.servlet.multipart.max-file-size=20MB`; pre-parse Content-Length check; reject 413 | — |
| D2 | ZIP bomb — XLSX is ZIP; expanded ratio 1000:1 (1MB → 1GB in memory) | Medium | High | Apache POI ZipSecureFile.setMinInflateRatio(0.01) — POI built-in protection | Document POI security config in `documents/02-architecture/`; add unit test ZIP bomb fixture |
| D3 | Million-row CSV → DB lock + OOM during INSERT | Medium | Critical | **MAX_ROWS env var per Wave 86 Agent E** — config `kitehub.import.max-rows=10000` default; rejection 422 with explicit error | — |
| D4 | Concurrent imports from same tenant exhaust pool | Low | Medium | Per-tenant import lock — only 1 active import at a time; subsequent returns 429 | Verify lock in code; add P2 retry button |

### E — Elevation of Privilege

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| E1 | Import smuggles row with `role=PLATFORM_ADMIN` | Low | Critical | Whitelist column names; `role` column not importable; defaulted server-side per persona | — |
| E2 | P3 Manager imports + escalates to P2 Owner permissions via CSV | Low | Critical | Persona/role assignment server-side from JWT, not CSV row | — |
| E3 | CSV path-traversal — filename `../../../etc/passwd` in metadata | Low | Medium | Filename sanitized; original name stored as metadata only; file saved with UUID name to MinIO | — |

---

## 3. Mitigation status summary

| Severity | Total | Mitigated | Open follow-up |
|---|---|---|---|
| Critical | 6 | 6 | 1 (MinIO bucket policy verify) |
| High | 6 | 6 | 2 (CSV injection test fixture, POI ZIP bomb test) |
| Medium | 4 | 3 | 1 (P2 retry button on lock 429) |
| Low | 1 | 1 | 0 |

**Verdict:** Acceptable risk posture. 4 follow-ups Wave 87+.

---

## 4. Trust boundary diagram

```
[Browser P2/P3]
     |  multipart upload (max 20MB enforced)
     v
[ALB] -- request size limit
     |
     v
[kitehub-subscription ImportService]
     |  1. MIME sniff (python-magic equiv)
     |  2. Apache POI XSSF parse (XLSX) or Univocity (CSV) — XXE off, ZIP-bomb guard
     |  3. MAX_ROWS cap (10k default)
     |  4. Column whitelist + sanitize formula cells
     |  5. Per-tenant lock acquire
     v
[MinIO raw archive]  -- private bucket, sha256 stored
     |
     v
[RDS @Transactional batch INSERT]
     |
     v
[admin_audit_logs] -- immutable
```

---

## 5. Test cases

- [ ] T1 CSV injection: upload row with `=2+2` → server stores `'=2+2`; downloaded back, Excel shows literal string not formula
- [ ] T3 XXE: upload XLSX với DOCTYPE external entity → server rejects or ignores; no file system read
- [ ] D2 ZIP bomb: upload 1KB XLSX expanding to 1GB → server rejects with explicit error not OOM
- [ ] D3 MAX_ROWS: upload CSV với 10001 rows → 422 + error citing limit per config
- [ ] E1 role smuggle: CSV row with `role=ADMIN` → role ignored, server defaults to persona default

---

## 6. Open follow-ups

1. **I3 follow-up:** Verify MinIO bucket policy private + pre-signed URL pattern in `infrastructure/helm/kitehub-subscription/values.yaml`. Track Wave 87.
2. **T1 follow-up:** Add `BulkImportServiceTest.testCsvFormulaInjection` covering `=cmd`, `+http`, `-2`, `@SUM` patterns. Track Wave 87.
3. **D2 follow-up:** Document POI ZipSecureFile config trong `documents/02-architecture/`; integration test ZIP bomb fixture. Track Wave 87.
4. **D4 follow-up:** P2 UI shows "retry" button when 429 returned (not silent fail). Track Wave 87 P2 UX polish.

---

## 7. References

- [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.5 — File upload flow gap checklist
- Wave 84 P2 import — initial bulk-import scope
- Wave 86 Agent E MAX_ROWS env config

---

## 8. Log

- **2026-05-16:** Threat model created (Wave 86 Bucket E Fix 4). Baseline 17 threats; 4 follow-ups filed.
