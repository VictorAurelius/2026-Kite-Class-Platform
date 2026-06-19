# Phase-1 BETA Gap Re-classification Audit — wave-gap-audit-p1-1 (2026-06-19)

**Trigger:** Phase 1 chốt 2026-05-06 đã qua deadline; audit toàn bộ gap Phase-1-BETA đối chiếu 45 ADR (mới nhất ADR-045) + code đã ship.
**Method:** 5 Opus agents (read-only) phân loại 410 gap (197 OPEN + 213 PARTIAL) theo domain cluster, design-first per `design-first-investigation-order`. Mutation gated sau user review. Apply policy: bảo thủ-có-kiểm soát (LOW-confidence giữ Phase 1).

## Aggregate verdict (410 gap)

| Bucket | Verdict count | Applied this wave |
|---|---:|---:|
| KEEP Phase 1 | 274 | — (no-op) |
| MOVE → Phase 2 | 117 | 105 (HIGH+MED); 12 LOW giữ Phase 1 |
| CLOSE-DONE | 18 | sau verify pass (agent) |
| CLOSE-OBSOLETE | 1 | 1 (GAP-894 WONTFIX) |

**Đối chiếu kế hoạch blanket gốc (move toàn bộ 197 OPEN→Phase 2):** sẽ đẩy nhầm ~80 OPEN blocker thật (GAP-1413 multi-tenant P0, GAP-286 OTP P0, GAP-955/1130/1411/1412/1430/1431/896/825). Cách 4-bucket: 0 gap P0 bị move.

## MOVE→Phase 2 applied (105, HIGH+MED confidence)
```
GAP-033 GAP-043 GAP-080 GAP-1007 GAP-1008 GAP-1022 GAP-1027 GAP-1030 GAP-1032 GAP-1033 GAP-1038 GAP-1043 GAP-1046 GAP-1052 GAP-1056 GAP-1059 GAP-1068 GAP-1088 GAP-1094 GAP-1099 GAP-1128 GAP-113 GAP-1147 GAP-1148 GAP-1170 GAP-1219 GAP-1222 GAP-1229 GAP-1243 GAP-1245 GAP-1294 GAP-1320 GAP-1337 GAP-1338 GAP-1345 GAP-1346 GAP-135 GAP-1376 GAP-1379 GAP-1394 GAP-140 GAP-1406 GAP-141 GAP-1417 GAP-142 GAP-1461 GAP-1462 GAP-1464 GAP-1470 GAP-1478 GAP-1483 GAP-197 GAP-239 GAP-246 GAP-247 GAP-248 GAP-256 GAP-269a GAP-405 GAP-408 GAP-461 GAP-469 GAP-517 GAP-546 GAP-579 GAP-589 GAP-597 GAP-598 GAP-665 GAP-667 GAP-720 GAP-733 GAP-750 GAP-762 GAP-763 GAP-766 GAP-767 GAP-768 GAP-769 GAP-779 GAP-780 GAP-781 GAP-812 GAP-873 GAP-883 GAP-888 GAP-892 GAP-897 GAP-902 GAP-903 GAP-907 GAP-912 GAP-917 GAP-918 GAP-920 GAP-923 GAP-930 GAP-944 GAP-967 GAP-968 GAP-969 GAP-970 GAP-982 GAP-984 GAP-987 
```
## LOW-confidence MOVE — giữ Phase 1 (12, revisit sau)
```
GAP-1131 GAP-1149 GAP-1169 GAP-1468 GAP-269b GAP-412 GAP-532 GAP-587 GAP-878 GAP-905 GAP-919 GAP-971 
```
## CLOSE-DONE candidates (18 — verify pass)
```
GAP-213 GAP-346 GAP-429 GAP-438 GAP-774 GAP-775 GAP-884 GAP-887 GAP-889 GAP-891 GAP-895 GAP-899 GAP-900 GAP-901 GAP-904 GAP-908 GAP-909 GAP-910 
```
## CLOSE-OBSOLETE applied (1)
- GAP-894 → WONTFIX (by-design OWASP A09 retention)

## CLOSE-DONE verify pass (Opus agent, read-only — migrations/controllers/RLS DDL structurally confirmed)
- **DONE-OK → flipped DONE (8):** GAP-438, GAP-884, GAP-891, GAP-899, GAP-901, GAP-904, GAP-909, GAP-910 (substantive work shipped + CI-verified via migration-replay/rls-coverage/schema-drift; residual = doc-ref/AC-checkbox cosmetic).
- **NEEDS-FOLLOWUP → held PARTIAL (5):** GAP-887, GAP-889, GAP-895, GAP-900, GAP-908 (DB guarantee enforced+verified; residual = one IT regression-guard / sub-table decision). Per `gap-done-discipline` not flipped without filed follow-up — conservative hold; candidates for a targeted IT-backfill wave.
- **KEEP-PARTIAL (5):** GAP-213 (scope-revise unverified), GAP-346 (~145-test coverage debt → GAP-873), GAP-429 (Phase-2 Suspense → GAP-850), GAP-774 (FE page → GAP-871), GAP-775 (FE dashboard → GAP-865).

## Net applied this wave
- MOVE→phase-2: **105** · CLOSE WONTFIX: **1** (GAP-894) · CLOSE DONE: **8** = **114 gap mutations**.
- Held Phase 1 (no-op): 12 LOW-move + 5 needs-followup + 5 keep-partial + 274 keep = 296.
