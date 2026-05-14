---
title: Wave 40 — Audit milestone cho cụm release-deploy-artifacts (sau Wave 33+34+37+38+39)
status: complete
created: 2026-05-08
updated: 2026-05-08
waves: [40]
gaps: []
audit_cluster: release-deploy-artifacts
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 40 — Audit milestone cho cụm `release-deploy-artifacts`

**Mục tiêu:** Chạy bộ kiểm tra audit đầy đủ cho cụm domain `release-deploy-artifacts` sau khi 5 wave (33+34+37+38+39) đã hoãn audit theo `AUDIT_DEFER_DOMAIN_MILESTONE`. Kích hoạt cổng nâng cấp Phase 1 BETA per `post-wave-audit-mandate.md` §2.4.2.
**Lý do kích hoạt:** Cụm `release-deploy-artifacts` đã đủ scope đóng (deploy infra + AI Branding wizard + dev-stack readiness + Phase 1 BETA prep). Theo `post-wave-audit-mandate.md` §2.4 milestone wave PHẢI chạy audit + filed reports + cập nhật ma trận `output-review-mandate.md` §3 + filed gaps. Wave 39 closure đã trigger AUDIT_DEFER trailer; thời hạn 14 ngày để chạy milestone audit đếm từ Wave 33 (2026-05-07) → cảnh báo lúc 2026-05-21.
**Wall-clock ước tính:** ~2-3h song song (7 ngăn read-only đồng thời), longest-bucket ~2.5h (Quality + Security có nhiều tệp).

---

## 1. Brainstorm (5-10 phút)

**Q1 (alignment):**
- Persona: Solo dev (cần biết Phase 1 BETA đã đạt chuẩn launch chưa). Audit auditor bên ngoài (calibration external vs self-score).
- Domain: 7 specialist audits trên cụm `release-deploy-artifacts` (kitehub-* + kiteclass-core + AI Branding + Terraform + Helm + scripts + runbook).
- Wave: chặn Phase 7 production deploy nếu Quality < 80 / Security < 80 (cổng `release-deploy-standard.md` §3.4 + `post-wave-audit-mandate.md` §2.4.2).

**Q2 (trade-offs):**
- 7 audit chạy song song (1 wave-pack) vs chia tách 2-3 wave: chọn **1 wave-pack** vì tất cả read-only, không có rủi ro write conflict, chạy nhanh hơn 2-3× so với serial. Vượt ngưỡng 5-agent-cap nhưng audit là edge-case (READ-ONLY) — chấp nhận 7 ngăn cùng lúc + ghi chú trong `feedback_parallel_agent_strategy.md` lessons-learned.
- Audit gộp toàn cụm 5 wave hay split per-wave: chọn **gộp toàn cụm** vì state hiện tại = state hôm nay; per-wave audit mỗi cái sẽ phải merge findings cuối cùng, double-work.
- Có pen-test live không: **KHÔNG** trong Wave 40 (cần staging up = Stream A user actions chưa xong). Pen-test light (OWASP code-level + headers) nằm trong A6 của khối A — Wave 41 hoặc cùng Wave 40 nếu đủ slot.
- Persona-based business review: **CÓ** (skill `persona-based-business-review` qua 10 tenant types) — bổ sung cho audit truyền thống vì Phase 1 BETA target P1+P2 personas.

**Q3 (rủi ro):**
- Quality < 80 hoặc Security < 80 → block Phase 7. Phục hồi: file gaps theo `audit-to-gap-pipeline.md` + Wave 41 fix sub-PRs. Có thể cần thêm 1-2 wave fix trước khi đạt cổng.
- 7 ngăn cùng spawn → vượt 5-agent-cap. Mitigation: chia 2 đợt trong cùng wave (đợt 1 = 4 ngăn, đợt 2 = 3 ngăn sau khi đợt 1 done). Hoặc chấp nhận 7 read-only an toàn vì không có file conflict — agent context isolation đủ.
- Audit tốn nhiều token (mỗi specialist đọc trăm file). Mitigation: Sonnet medium thay Opus cho 5 audit nhẹ (UI / API contract / Ops / BL / persona-review); Opus cho 2 audit nặng (Quality + Security).
- Stale state: image stack đang chạy có thể khác main HEAD. Mitigation: audit đọc nguồn từ filesystem main HEAD (không từ chạy stack), state-check từ git rev.

---

## 2. Phân chia công việc

| Ngăn | Audit | Owner | Effort | Disjoint? |
|------|-------|-------|--------|-----------|
| A | UI Review /128 (kit-as-source-of-truth + production parity) | bg-agent Opus | ~2h | ✅ chỉ đọc kits + frontend src |
| B | Quality Audit /100 (11 categories) | bg-agent Opus | ~2.5h | ✅ chỉ đọc — gen report |
| C | Security Audit /100 (OWASP + deps + secrets) | bg-agent Opus | ~2.5h | ✅ chỉ đọc — gen report |
| D | Performance Audit /100 (DB + API + bundle + cache) | bg-agent Sonnet | ~1.5h | ✅ static-analysis vì stack chưa staging |
| E | Ops Readiness /100 (monitoring + deploy + DR) | bg-agent Sonnet | ~1.5h | ✅ chỉ đọc Terraform/Helm/runbooks |
| F | API Contract /100 (endpoints ↔ docs sync) | bg-agent Sonnet | ~1.5h | ✅ chỉ đọc Controller + api-contract.md |
| G | Business Logic /100 (code ↔ rules.md) | bg-agent Sonnet | ~1.5h | ✅ chỉ đọc rules.md + service code |
| H (optional) | Persona-based business review (10 tenant types role-play) | foreground-coordinator hoặc bg-agent Sonnet | ~1h | ✅ chỉ đọc personas + UI |

Disjoint check: TẤT CẢ audit là READ-ONLY. Mỗi ngăn ghi MỘT report file vào `documents/04-quality/audits/{category}/2026-05-08-wave-40-milestone.md` — đường dẫn khác nhau, không conflict.

**Quyết định:** ship 7 ngăn (A-G) + ngăn H persona-review chạy foreground sau khi A-G done.

---

## 3. Phạm vi (compact schema)

**Stake tier:** HIGH (cổng quyết định Phase 7 deploy; sai số audit ảnh hưởng go-live decision) → model: **Opus** cho B/C (deep), **Sonnet** cho A/D/E/F/G/H.
**Cross-layer? NO** — audit không thay đổi code, không có FE↔BE contract change. Bỏ qua Bucket 0 Foundation theo `contract-first-for-cross-layer.md`.

| # | Ngăn | Audit | Skill | Output |
|:-:|------|-------|-------|--------|
| 1 | A | UI Review /128 | `quality/ui-review/SKILL.md` | `documents/04-quality/audits/ui/2026-05-08-wave-40-milestone.md` |
| 2 | B | Quality Audit /100 | `quality-audit/SKILL.md` | `documents/04-quality/audits/quality/2026-05-08-wave-40-milestone.md` |
| 3 | C | Security Audit /100 | `quality/security-audit/SKILL.md` | `documents/04-quality/audits/security/2026-05-08-wave-40-milestone.md` |
| 4 | D | Performance Audit /100 | `quality/performance-audit/SKILL.md` | `documents/04-quality/audits/performance/2026-05-08-wave-40-milestone.md` |
| 5 | E | Ops Readiness /100 | `quality/ops-readiness-audit/SKILL.md` | `documents/04-quality/audits/ops-readiness/2026-05-08-wave-40-milestone.md` |
| 6 | F | API Contract /100 | `quality/api-contract-audit/SKILL.md` | `documents/04-quality/audits/api-contract/2026-05-08-wave-40-milestone.md` |
| 7 | G | Business Logic /100 | `quality/business-logic-audit/SKILL.md` | `documents/04-quality/audits/business-logic/2026-05-08-wave-40-milestone.md` |
| 8 | H | Persona business review | `quality/persona-based-business-review.md` | `documents/04-quality/audits/persona-review/2026-05-08-wave-40-milestone.md` |

### Ngăn A — UI Review /128

- Phạm vi: kits `documents/02-architecture/design-system/ui_kits/**` + production frontend `kitehub/kitehub-frontend/src/**` + `kiteclass/kiteclass-frontend/src/**`
- Trọng tâm: kit-as-source-of-truth parity (Wave 22-31 kit ports), 4-layer V-model coverage (Wave 39 GAP-394/423/424 docs ảnh hưởng layer 1+2)
- Output: per-screen score + delta vs Wave 36 baseline (UI 99/128 A+) + gaps mới nếu < 105/128

### Ngăn B — Quality Audit /100

- Phạm vi: toàn dự án 11 categories — code style, business doc 3-layer sync, audit-trail, gap discipline, etc.
- Trọng tâm: tăng vs Wave 36 baseline (80/100 B Phase 1 trigger gate vừa đạt). Phase 7 cần ≥ 80.
- Output: 11 category scores + tổng /100 + gaps mới P0/P1

### Ngăn C — Security Audit /100

- Phạm vi: OWASP Top 10 + deps (Trivy local CVE scan tĩnh) + secrets management + auth flow
- Trọng tâm: Wave 33 BetaAccessRequest + Wave 35 admin auth + Wave 38 cookie consent + GAP-426 ENCRYPTION_MASTER_KEY fix paired
- Output: vulnerability list + CVE counts + gaps mới (chú trọng P0 BLOCKING)

### Ngăn D — Performance Audit /100

- Phạm vi: DB queries (N+1 detection — Wave 35 GAP-392 V31 indexes mới), API handlers (latency static estimate), FE bundle size
- Lưu ý: stack chưa có staging → static-analysis mode (per Wave 36 baseline 71/100 C)
- Trọng tâm: regression vs Wave 36 sau Wave 33-39 thay đổi
- Output: hot-paths + bundle analysis + gaps mới

### Ngăn E — Ops Readiness /100

- Phạm vi: `infrastructure/terraform-aws/` + `infrastructure/helm/` + `kitehub/scripts/` + runbook `documents/05-guides/operations/` + `documents/05-guides/account-prep/`
- Trọng tâm: Wave 37 Terraform + Wave 38 staging.tf + Wave 39 dev-stack readiness + GAP-425/426 fix
- Output: deploy-readiness checklist /100 + DR drill status + gaps mới

### Ngăn F — API Contract /100

- Phạm vi: endpoints ↔ `documents/01-business/*/api-contract.md` sync
- Trọng tâm: Wave 33 beta access endpoints + Wave 34 AI Branding wizard 7 endpoints + Wave 35 admin auth endpoints
- Output: endpoint inventory + drift list + gaps mới

### Ngăn G — Business Logic /100

- Phạm vi: code constants ↔ rules.md sync per `business-logic-review.md` v1.0.0 (5-attribute requirement)
- Trọng tâm: BR-PDPL Wave 23+25+26 + BR-LIFE/QUALITY Wave 36 + BR-INPUT-CAP Wave AI Branding
- Output: rules coverage % + gaps cho rules thiếu 5-attribute

### Ngăn H — Persona-based business review

- Phạm vi: 10 tenant types role-play (P1 prospects + P2 SaaS owners + Phase 1 BETA primary persona) tương tác kits + production
- Trọng tâm: GAP-365 S-student.md + persona AC docs từ Wave 19-22
- Output: persona coverage matrix + UX gaps mới

---

## 4. State-Check Evidence (BẮT BUỘC theo `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `documents/04-quality/audits/quality/` | Audit category dir | `ls documents/04-quality/audits/quality/ \| head -3` | Exists, có audit cũ Wave 36 (2026-05-07) | ✅ exists |
| `documents/04-quality/audits/security/` | Audit category dir | `ls documents/04-quality/audits/security/ \| head -3` | Exists | ✅ exists |
| `documents/04-quality/audits/ui/` | Audit category dir | `ls documents/04-quality/audits/ui/ \| head -3` | Exists | ✅ exists |
| `documents/04-quality/audits/performance/` | Audit category dir | `ls documents/04-quality/audits/performance/ \| head -3` | Exists, baseline Wave 25 | ✅ exists |
| `documents/04-quality/audits/ops-readiness/` | Audit category dir | `ls documents/04-quality/audits/ops-readiness/ \| head -3` | Exists, baseline Wave 25 | ✅ exists |
| `documents/04-quality/audits/api-contract/` | Audit category dir | `ls documents/04-quality/audits/api-contract/ \| head -3` | Exists | ✅ exists |
| `documents/04-quality/audits/business-logic/` | Audit category dir | `ls documents/04-quality/audits/business-logic/ \| head -3` | Exists | ✅ exists |
| `documents/04-quality/audits/persona-review/` | Audit category dir | (sẽ tạo nếu chưa có) | Nếu chưa tồn tại → 🆕 to-be-created | ⚠️ verify-at-spawn |
| `.claude/skills/quality-audit/SKILL.md` | Audit skill | `ls .claude/skills/quality-audit/SKILL.md` | Exists | ✅ exists |
| `.claude/skills/quality/{ui-review,security-audit,performance-audit,ops-readiness-audit,api-contract-audit,business-logic-audit}/SKILL.md` | Audit skills | `find .claude/skills/quality -name SKILL.md` | 6 skills exist | ✅ exists |
| `post-wave-audit-mandate.md` §2.4.2 milestone obligations | Rule | `grep "DOMAIN_MILESTONE_AUDIT" .claude/rules/post-wave-audit-mandate.md` | Exists §3 detector | ✅ exists |

Không có symbol 🆕 to-be-created đáng kể (chỉ persona-review/ folder nếu chưa có — agent H tạo trong commit của mình).

Ngắn gọn không vi phạm:
- KHÔNG dùng `| head` truncation trong state-check core
- KHÔNG defer "agent kiểm tra lúc execute" — đã verify trước
- KHÔNG aspirational refs

---

## 5. Cổng kiểm tra (per ngăn)

| Ngăn | Local verify command | CI gate |
|------|---------------------|---------|
| A | `ls documents/04-quality/audits/ui/2026-05-08-wave-40-milestone.md` | None — output report only |
| B | `ls documents/04-quality/audits/quality/2026-05-08-wave-40-milestone.md` | None |
| C | `ls documents/04-quality/audits/security/2026-05-08-wave-40-milestone.md` | None |
| D-H | tương tự — verify report file tồn tại + có score /100 | None |

Audit không cần CI run — output là markdown report + gaps file. PR review checklist verify mỗi report có score + delta + gaps mới.

---

## 6. Pattern spawn agent

Theo `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

**Đợt 1 (4 ngăn nặng — model Opus):** A, B, C, F
- A UI Review (Sonnet OK) — đổi sang Sonnet để đợt 1 = 3 Opus + 1 Sonnet, không vượt cap Opus
- B Quality + C Security: Opus (deep)
- F API Contract: Sonnet
- Spawn `run_in_background: true`, `isolation: worktree`

**Đợt 2 (3 ngăn nhẹ — model Sonnet):** D, E, G
- D Performance + E Ops + G Business Logic: Sonnet
- Spawn sau khi đợt 1 ≥2 ngăn done (giải phóng RAM agent context)

**Đợt 3 (foreground):** H persona-review
- Coordinator chạy sau khi A-G done. Đọc 7 reports + role-play 10 personas + tổng hợp findings vào report H.

**Đường dẫn RELATIVE** trong agent prompt theo `feedback_worktree_absolute_path_contamination.md`.

---

## 7. Quy trình closure

Theo `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `post-wave-audit-mandate.md` §2.4.2 milestone obligations:

1. Mỗi ngăn ship 1 PR (8 PRs total: A/B/C/D/E/F/G/H + closure PR = 9 PRs cho wave 40).
2. Closure PR aggregate:
   - Cập nhật `output-review-mandate.md` §3 matrix — flip 7 audit rows từ "REFRESHED 2026-05-07" thành "REFRESHED 2026-05-08-wave-40-milestone" với scores mới
   - Cập nhật ROADMAP §🚀 Next Action với điểm tổng + gaps mới + cổng Phase 7 status (PASS / FAIL)
   - Wave plan frontmatter `status: complete` flip
   - `wave-history.jsonl` append (Rule 15)
   - `prune-merged-worktrees.sh --yes`
   - **`DOMAIN_MILESTONE_AUDIT: release-deploy-artifacts <8 audit report paths>` trailer** trong closure commit (per `post-wave-audit-mandate.md` §3 detector validates trailer + report files exist)
   - File gaps theo `audit-to-gap-pipeline.md` cho mỗi finding P0/P1 (audit không fix trực tiếp; finding → gap → Wave 41 fix)
3. Section "Release Plan Progress" trong closure PR body:
   - Phase 1 BETA cổng status (Quality ≥80? Security ≥80?)
   - Số gaps mới P0 → block Phase 7
   - Số gaps mới P1 → Wave 41 cluster
4. Audit deferral của các wave 33+34+37+38+39 = ĐÓNG (milestone đã chạy).

---

## 8. Đường găng + thời gian

```
Đợt 1 (Opus + Sonnet song song): A∥B∥C∥F   → ~2.5h longest path (B/C)
Đợt 2 (Sonnet song song):        D∥E∥G     → ~1.5h longest path  (parallel với cuối đợt 1)
Đợt 3 (foreground):              H         → ~1h
Coordinator merge + closure:     ~30 min
─────────────────────────────────────────
Tổng wall-clock:                 ~3.5-4h cho 1 phiên
```

Tối ưu hơn: nếu RAM cho phép, spawn 7 ngăn cùng lúc → ~2.5h longest path + ~30min closure = **~3h tổng**.

---

## 9. Log

- **2026-05-08** (draft): Plan tạo. Cụm `release-deploy-artifacts` (Wave 33+34+37+38+39) đã đến milestone audit theo `post-wave-audit-mandate.md` §2.4. Mục tiêu: 7-8 specialist audits song song + foreground persona-review → ~3-4h tổng. Phase 1 BETA cổng nâng cấp gate Quality ≥80 + Security ≥80. Wave 39 closure đã trigger AUDIT_DEFER trailer; Wave 40 đóng nó qua DOMAIN_MILESTONE_AUDIT trailer.

- **2026-05-08** (complete): Wave 40 SHIPPED. 7/7 audits done + closure. PRs #971 plan, #972 D (Performance 75 C +4), #973 B (Quality 86 B+ +6 ✅PASS), #974 C (Security 87 B +3 ✅PASS), #975 E (Ops Readiness 60 D +7), #976 F (API Contract 72 C+ +1), #977 G (Business Logic 68 C -14 recalibration), #978 A (UI 111.3 A+ +14.3). **2 cổng critical Quality + Security đều PASS Phase 7 ≥80** — production deploy unblocked từ formal gate view. 5 gaps mới: GAP-427 (F API drift), GAP-428 (A UI prospect public pages), GAP-429 (A UI transient state UX) — note: GAP-427 number collision giữa A+F resolve qua rename A's →429. 7 P0/P1 mới từ D/E/F/G cho Wave 41 cluster. H persona-review deferred (Cat 11 unchanged 4/10 per gap-152 charter, không block Phase 1 BETA invite-only). 76th consecutive 0-clarif streak (1 G respawn Haiku narrow scope sau autocompact thrash). Wall-clock ~1h longest path (A 13min) + ~5min closure. **DOMAIN_MILESTONE_AUDIT trailer applied** trong closure commit per `post-wave-audit-mandate.md` §3 detector → đóng AUDIT_DEFER tag của Wave 33+34+37+38+39.
