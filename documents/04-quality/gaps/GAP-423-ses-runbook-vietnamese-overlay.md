# GAP-423: AWS SES runbook Vietnamese quick-start overlay

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA — solo dev VN-first; blocks first-deploy execution if user can't follow EN-only doc)
**Domain:** Operations / Documentation / VN-localization
**Found:** 2026-05-07 (post-WSL-migration session — Stream A user-action coverage audit)
**Affects:** Solo dev (VN-first per CLAUDE.md) executing Phase 1 BETA §1.4 SES production access request

---

## Problem

`documents/05-guides/operations/email-ses-setup-runbook.md` (304 dòng, Wave 33 Bucket B) đầy đủ technical nhưng **chỉ 3/304 dòng có Vietnamese diacritic** = effectively 100% English. Solo dev VN-first phải đọc EN khi muốn:

1. Verify domain `kitehub.vn` (DKIM + SPF + DMARC records via Cloudflare DNS)
2. Submit SES production access request (sandbox → production, usually 24-48h)
3. Configure verified sender + bounce/complaint SNS topics

State-check (per `audit-to-gap-pipeline.md` §2.5) 2026-05-07:
```bash
$ grep -c "[àáảãạăâèéẹêìíịòóỏôơùúụừứửữựỳýỹỵđ]" \
    documents/05-guides/operations/email-ses-setup-runbook.md
3                          # 3/304 lines = 0.99% Vietnamese density

$ grep -nE "^#{1,3} " documents/05-guides/operations/email-ses-setup-runbook.md
1:# AWS SES Production Setup Runbook
14:## 1. Overview
30:## 2. Prerequisites
39:## 3. Sender domain verification
85:## 4. Sandbox → Production access request
121:## 5. Bounce + complaint feedback loops
                          # All section headings English
```

Compare: Cloudflare runbook (`cloudflare-setup.md`) đã có Vietnamese đầy đủ (Wave 38 Bucket B) — section headings "Hướng Dẫn Cài Đặt", "§2 Tạo Account Cloudflare", etc. SES không tuân theo cùng standard.

## Root Cause

Wave 33 Bucket B (GAP-370) shipped runbook EN-only. Project communication standard per CLAUDE.md = Vietnamese (technical English terms OK trong code blocks). Runbook bypass standard này tại merge time — chưa có review checklist enforce VN coverage cho user-facing runbooks.

## Proposed Fix

**Option A — Quick-start VI overlay** (~50 dòng, fastest):
Add `## §0 Hướng Dẫn Nhanh (Vietnamese)` section ở đầu file:
- Bối cảnh ngắn (2-3 dòng)
- 6-step Vietnamese summary (mỗi step 1-2 dòng + cross-link tới EN section bên dưới)
- KYC pitfalls VN (timezone, document acceptance VN passport vs CCCD)
- Common reject reasons + remediation

**Option B — Full translation** (~150 dòng touch, slower):
Translate toàn bộ runbook section-by-section. Cleaner long-term nhưng tốn 2-3× thời gian.

**Recommended: Option A** for Phase 1 BETA speed; convert to Option B at Phase 2 if VN dev team grows.

Plus: add "VN-coverage review checklist" entry to `output-review-mandate.md` §3 row "Runbooks (operations)" → enforce next runbook ship VN-first.

## Acceptance Criteria

- [ ] `email-ses-setup-runbook.md` có `## §0 Hướng Dẫn Nhanh` section ở đầu file
- [ ] §0 contains 6-step Vietnamese summary covering: domain verification → DNS records add → sandbox access submit → wait approval → verify production limits → configure bounce/complaint SNS
- [ ] §0 includes KYC pitfalls section (timezone GMT+7 vs AWS UTC default, VN passport vs CCCD acceptance, common reject reasons + remediation)
- [ ] §0 cross-links each step to corresponding EN section §3.x / §4.x / §5.x
- [ ] Total file VN diacritic density ≥15% (vs current 0.99%) — measurable via grep -c
- [ ] No regression in EN section content (Option A is overlay, not rewrite)
- [ ] PR title trong scope `docs(deploy):` per conventional commits

## Related

- `email-ses-setup-runbook.md` (the doc to overlay)
- `cloudflare-setup.md` (✅ Vietnamese — reference standard cho VN runbook)
- GAP-370 (parent — original SES infra ship)
- GAP-394 (sibling — 4 missing runbooks for AWS account / domain / password manager / superadmin)
- GAP-424 (sibling — Statuspage VN overlay, same session)
- CLAUDE.md §"CRITICAL: Communication Language" Vietnamese-first rule
- Wave 39 candidate cluster (consider folding into Wave 39 Bucket E or new Wave 40)

## Log

- **2026-05-07:** Filed during post-WSL-migration session Stream A coverage audit. Found 5 doc gaps in user-action prereqs Phase 1 §1.1-1.5; SES = 1 of 5. Sibling GAP-424 (Statuspage VN overlay) filed same PR. Per `agent-action-bias.md` v1.0.0: agent files gap directly without offloading.
