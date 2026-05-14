---
parent_rule: release-deploy-standard.md
purpose: deferred-load §Self-test + §Worked example for context budget compliance
---

# release-deploy-standard — Examples / Self-test

Companion to `.claude/rules/release-deploy-standard.md`. Body moved here per Wave 76 Bucket E streamline.

## Self-test (worked example — Release 1)

Apply rule §3.4 (MAJOR + first PRODUCTION) to Release Lần 1 v1.0.0:

| Required artifact | Status | Reference |
|---|---|---|
| Deploy plan document linked | ✅ | `release-1-deploy-plan.md` |
| Smoke test script | ⏳ | GAP-377 (P1) |
| Rollback procedure | ⏳ | GAP-378 (P1) |
| Status page | ⏳ | GAP-373 (P1) |
| Secrets management | ⏳ | GAP-379 (P1) |
| HTTPS/TLS | ⏳ | Per Oracle Cloud setup |
| DNS production setup | ⏳ | GAP-369 (P0 BLOCKING) |
| CDN setup | ⏳ | GAP-371 (P1) |
| Email transactional | ⏳ | GAP-370 (P0 BLOCKING) |
| Pen-test light | ⏳ | New gap (mentioned in `release-1-deploy-plan.md` Phase 1.5) |
| Production data seed | ⏳ | GAP-376 (P0 BLOCKING) |
| Monitoring dashboards | ⏳ | GAP-115 (PARTIAL) |
| SLO targets | ⏳ | GAP-135 (PARTIAL) |
| Tag-based release CI | ⏳ | GAP-374 (P1) |
| GitHub Release với changelog | ⏳ | GAP-375 (P2) |
| Staging environment parity | ⏳ | GAP-380 (P1) |
| Beta tenant invite mechanism | ⏳ | GAP-372 (P0 BLOCKING — for v0.9.0-beta) |
| Counsel-reviewed legal docs | ⏳ | GAP-182 + GAP-184 Phase 2 (Phase 3 K-12 trigger) |

→ Rule §3.4 successfully maps Release 1 v1.0.0 readiness state. 12 gaps GAP-369..380 + existing GAP-115/135/182/184 = comprehensive coverage. **Rule fires correctly on existing scope.** ✅

### Meta-lesson

Rule creation itself surfaced 2 misses by current author:
1. State-check vi phạm — không đọc `deployment-strategy.md` (GAP-103 DONE) trước khi file 12 deploy gaps
2. Standard groundwork miss — generated artifacts free-form thay vì cite Well-Architected/Twelve-Factor/DORA/OWASP

Both addressed via this rule §2 (standards reference) + §10 (cross-link GAP-103) + 12 gaps' updates (cross-ref `deployment-strategy.md`).
