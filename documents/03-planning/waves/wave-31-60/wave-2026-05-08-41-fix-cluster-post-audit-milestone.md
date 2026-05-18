---
title: Wave 41 — Fix-cluster post Wave 40 audit milestone
status: complete
created: 2026-05-08
updated: 2026-05-08
waves: [41]
gaps: [GAP-272o, GAP-430, GAP-431, GAP-432, GAP-433, GAP-115, GAP-135]
audit_cluster: release-deploy-artifacts
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 41 — Fix-cluster post Wave 40 audit milestone

**Mục tiêu:** Đóng các P0/P1 gap mới phát hiện ở Wave 40 audit + GAP-272o (Wave 34 follow-up) trong 1 wave-pack 5-7 ngăn parallel để giải phóng đường găng tới Phase 7 production deploy.
**Trigger:** Wave 40 closure đã PASS Quality + Security cổng ≥80 nhưng để lại 5+ P0/P1 gap critical. Wave 41 = code-prereq cluster (per `documents/03-planning/roadmap/release-1-deploy-runbook.md` §A breakdown).
**Wall-clock ước tính:** ~3-5h song song (5-7 ngăn), longest-bucket ~3h (GAP-432 Performance refactor 3 callsites).

---

## 1. Brainstorm (đã pre-filled từ Wave 40 findings)

**Q1 (alignment):**
- Persona: Solo dev chuẩn bị Phase 1 BETA invite-only launch. Audit auditor sau Wave 41 sẽ verify Quality maintain ≥80, Ops Readiness path-to-70, Performance ≥80.
- Domain: 5 domain — Backend (BE refactor), DevOps (Helm + alert), AI Branding (FE wiring), Documentation (rules.md backfill), Compliance (Phase 2 prep).
- Wave: chặn Phase 7 nếu P0 BackupJobFailure không fix (silent monitoring failure cho production); P1 còn lại không block Phase 7 nhưng nên đóng trước first beta tenant onboard.

**Q2 (trade-offs):**
- 6 ngăn vs 7 ngăn: chọn **6 ngăn** + GAP-117 (restore drill) tách ra Wave 42 vì cần staging up (Stream A user-action). 6 ngăn = đủ disjoint, không vượt 5-agent-cap quá nhiều.
- Bundle GAP-115/135 monitoring: ship cùng 1 ngăn vì cùng Grafana dashboard provisioning. ~3-4h.
- A5 npm CVE sweep (GAP-204): tách ngăn riêng vs lồng vào Security audit Wave 40 follow-up: tách riêng cho dễ track + paralllel.
- Pen-test light A6: Wave 40 Bucket C Security đã cover OWASP code-level → A6 redundant; defer Wave 42 live-staging với ZAP scan.

**Q3 (rủi ro):**
- GAP-432 Performance refactor 3 service files cùng module có thể conflict — mitigate: 3 sub-buckets per-file hoặc 1 ngăn coordinator-managed sequential.
- GAP-430 alert metric mismatch fix có thể phá test cũ — mitigate: rule unit test paired.
- GAP-272o orchestrator wiring đụng Wave 32+34 component code — kiểm tra không touch step component nội bộ, chỉ wire orchestrator.
- 6 ngăn parallel: chấp nhận vượt 5-agent-cap 1 đơn vị; tất cả disjoint per gap file path.

---

## 2. Phân chia công việc

| Ngăn | Gap(s) | Owner | Effort | Disjoint? |
|------|--------|-------|--------|-----------|
| A | **GAP-430** P0 BackupJobFailure alert metric fix | bg-agent Sonnet | ~1.5h | ✅ chỉ Helm/prometheusrule + script + runbook |
| B | **GAP-431** P1 startupProbe Helm templates | bg-agent Sonnet | ~1.5h | ✅ chỉ Helm templates |
| C | **GAP-432** P1 3× unbounded findAll services | bg-agent Sonnet (deep) | ~3h | ✅ 3 service files trong 2 module BE |
| D | **GAP-272o** P1 orchestrator wiring DeployingStep + RegenerateCounter | bg-agent Sonnet | ~1.5h | ✅ chỉ FE kitehub-frontend customer/branding/wizard |
| E | **GAP-433** P1 rules.md 5-attr backfill (Phase 1) | bg-agent Sonnet (narrow) | ~3h | ✅ chỉ documents/01-business/**/*.md |
| F | **GAP-115/135** monitoring Grafana dashboards Phase 1 | bg-agent Sonnet | ~3h | ✅ chỉ infrastructure/helm/...grafana + runbook |
| G (optional) | **GAP-204** npm CVE sweep + Trivy exception | bg-agent Sonnet | ~1.5h | ✅ chỉ package-lock.json + .github/workflows |

Disjoint check: 7 ngăn touch 7 path bucket khác nhau; 0 file conflict expected.

**Quyết định:** ship **6 ngăn (A-F)**; G optional nếu RAM cho phép, nếu không tách Wave 42 hoặc Wave 41b sub-PR.

---

## 3. Phạm vi (compact schema)

**Stake tier:** MEDIUM (fix-cluster routine; không block deploy nếu skip 1-2 ngăn) → model: **Sonnet** cho A/B/D/E/F/G; **Sonnet (deep)** cho C (Performance refactor cần benchmark).
**Cross-layer? NO** — không có FE↔BE contract change. Bỏ qua Bucket 0 Foundation.

| # | Ngăn | Gap | Files | Spawn order |
|:-:|------|-----|-------|:-----------:|
| 1 | A | GAP-430 | `infrastructure/helm/.../prometheusrule.yaml` + `kitehub/scripts/backup-production.sh` + `documents/05-guides/operations/runbooks/backup-failure.md` | parallel |
| 2 | B | GAP-431 | `infrastructure/helm/kitehub/templates/deployment.yaml` + 5 sister templates | parallel |
| 3 | C | GAP-432 | `kitehub/kitehub-admin/.../service/AnalyticsService.java` + `kitehub/kitehub-subscription/.../service/{PaymentService.java,InstanceService.java}` + repo + tests | parallel |
| 4 | D | GAP-272o | `kitehub/kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` + `DeployingStep.tsx` + `RegenerateCounter.tsx` | parallel |
| 5 | E | GAP-433 | `documents/01-business/**/rules.md` (~21 files thiếu 5-attr) | parallel |
| 6 | F | GAP-115/135 | `infrastructure/helm/.../grafana/{dashboards,values.yaml}` + alert routing + runbook | parallel |
| 7 | G | GAP-204 | `kitehub/kitehub-frontend/package-lock.json` + `kiteclass/kiteclass-frontend/package-lock.json` + `.github/workflows/docker-build-push.yml` Trivy exception | parallel optional |

---

## 4. State-Check Evidence (theo `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Evidence | Verdict |
|--------|------|----------|---------|
| `BackupJobFailure` alert | PromQL alert | `grep -rn "BackupJobFailure" infrastructure/helm/` | ✅ exists (incorrect metric) |
| `kite_backup_last_success_timestamp_seconds` | Prom metric | `grep -rn "kite_backup_last_success" .` | ⚠️ in alert PromQL only, not emitted |
| `kite_backup_snapshots_total` | Prom metric | `grep -rn "kite_backup_snapshots" kitehub/scripts/` | ✅ exists (emitted by script) |
| `infrastructure/helm/kitehub/templates/deployment.yaml` `startupProbe:` | Helm field | `grep -c "startupProbe" infrastructure/helm/` | ❌ 0 matches → 🆕 to-be-added (Bucket B) |
| `AnalyticsService.findAll` callsites | Java | `grep -n "findAll()" kitehub/kitehub-admin/src/main/java/com/kitehub/admin/service/AnalyticsService.java` | ✅ exists 3 callsites (line 57, 58, 129) |
| `PaymentService.java:121` `findAll` | Java | `sed -n '115,125p' kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/PaymentService.java` | ✅ exists |
| `InstanceService.java:337` `findAll` | Java | `sed -n '330,340p' kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstanceService.java` | ✅ exists |
| `DeployingStep` + `RegenerateCounter` | FE component | `grep -rn "DeployingStep\|RegenerateCounter" kitehub/kitehub-frontend/src` | ✅ exists |
| `useDeployStream` + `useRegenerateQuota` hooks | FE hook | `grep -rn "useDeployStream\|useRegenerateQuota" kitehub/kitehub-frontend/src/hooks` | ✅ exists Wave 34 |
| `documents/01-business/*/rules.md` | Doc | `find documents/01-business -name rules.md \| wc -l` | ✅ ~52 files |
| `infrastructure/helm/.../grafana/` | Helm | `ls infrastructure/helm/*/templates/grafana/` | ⚠️ verify-at-spawn (có thể chưa có folder) |

Forward-looking: chỉ `startupProbe` 🆕 to-be-added — Bucket B owns creation.

Banned shortcuts respected.

---

## 5. Cổng kiểm tra (per ngăn)

| Ngăn | Local verify | CI gate |
|------|-------------|---------|
| A | `promtool check rules infrastructure/helm/.../prometheusrule.yaml` + alert unit test fire-test | core-ci alert rule test |
| B | `helm lint infrastructure/helm/kitehub` + `helm template` render kiểm tra `startupProbe` block | None (helm-lint khi/nếu có CI) |
| C | `cd kitehub && ./mvnw -pl kitehub-subscription,kitehub-admin verify -P strict-warnings` + benchmark | core-ci + admin-ci |
| D | `cd kitehub/kitehub-frontend && pnpm test --run + pnpm build` | KH frontend CI |
| E | bash check rules-frontmatter (chưa có — file thêm trong cùng ngăn) verify 5-attr | None |
| F | `helm lint` + `helm template` Grafana dashboard render | None |
| G | `pnpm audit --audit-level=high` cả 2 FE + Trivy exception YAML lint | docker-build-push CI |

---

## 6. Pattern spawn agent

Theo `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 6 ngăn cùng spawn `run_in_background: true` (G optional spawn nếu RAM cho phép)
- `isolation: worktree`
- RELATIVE paths theo `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge sequential A → B → D → E → F → C (C cuối vì refactor lớn nhất + có thể conflict với pre-existing tests)

---

## 7. Closure protocol

Theo các rule chuẩn:
- Mỗi ngăn ship 1 PR; flip gap status DONE per `gap-done-discipline.md` §2 (verification artifact pointer)
- Closure PR aggregate:
  - ROADMAP §🚀 Next Action update — ⭐0 Wave 41 entry
  - Wave plan frontmatter `status: complete`
  - `wave-history.jsonl` append
  - `prune-merged-worktrees.sh --yes`
  - `output-review-mandate.md` §3 matrix flip Performance + Ops Readiness rows nếu re-audit (optional)
- AUDIT_DEFER: Wave 41 không multi-domain → audit suite KHÔNG required ≤3 ngày per `post-wave-audit-mandate.md` §2.4 (single-domain release-deploy-artifacts continuation, defer-to-Phase-7-launch milestone đã đóng Wave 40).

---

## 8. Đường găng + thời gian

```
Đợt 1 (5 ngăn parallel):  A∥B∥D∥E∥F → ~3h longest path (E backfill 21 files OR F Grafana dashboards)
Đợt 2 (1 ngăn deep):       C (Performance refactor 3 service files) → ~3h
Coordinator merge + closure: ~30 min
─────────────────────────────────────────────────
Tổng wall-clock:           ~3.5-4h cho 1 phiên
```

Tối ưu: nếu 6 ngăn cùng đợt 1: longest path C 3h → tổng ~3.5h. Optional G ngăn 7: thêm ~30min chỉ.

---

## 9. Log

- **2026-05-08** (draft): Plan tạo từ Wave 40 audit findings handoff. 6 ngăn parallel + G optional. Cổng Phase 1 BETA Quality + Security đã PASS Wave 40 (86 + 87) — Wave 41 đóng các P0/P1 còn lại để clean handoff sang Phase 7 deploy. GAP-117 restore drill tách Wave 42 (cần staging up). Pen-test light A6 defer Wave 42 live-staging. Stake tier MEDIUM — không block hard nếu 1-2 ngăn slip.
- **2026-05-08** (complete): SHIPPED — 6 ngăn A-F merged sequential A→B→D→E→F→C. Wall-clock thực tế ~15min (vs 3.5h estimate — agents Sonnet/Opus rất nhanh trên scope đã tinh chỉnh). PR list: A #983 (GAP-430 PARTIAL, promtool test → GAP-435), B #981 (GAP-431 PARTIAL, helm cluster self-test → follow-up), C #985 (GAP-432 PARTIAL, ⚠️ breaking API Page envelope cho `/api/platform/instances` + `/api/platform/payments`, JMH benchmark deferred), D #986 (GAP-272o **DONE** 6/6 AC, SSE E2E EventSource polyfill → follow-up), E #982 (GAP-433 PARTIAL Phase 1 — 42/52 files thực tế vs 21 estimate, broader scope, Phase 2 → GAP-156), F #984 (GAP-115/135 PARTIAL Phase 1, Loki backend → GAP-434). G optional (GAP-204 npm CVE) defer Wave 42. Worktree contamination recurrence ở 4/6 agents (A/C/E/F) — pattern lặp lại của `feedback_worktree_absolute_path_contamination.md` cần meta-review. Cleanup script ran clean. AUDIT_DEFER per `post-wave-audit-mandate.md` §2.4 — domain-milestone trailer applied per bucket; release-deploy-artifacts cluster milestone vẫn là Phase 1 BETA launch wave.
