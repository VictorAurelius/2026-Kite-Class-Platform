# Contract-First for Cross-Layer Waves

**Priority:** 🟠 MANDATORY — governance khi wave touch cả Frontend + Backend
**Version:** 1.0.0
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Mọi wave plan có scope chạm CẢ FE (`*frontend/**`) VÀ BE (`*-core/**`, `your-product-a-*/**` non-frontend) trong cùng một bucket OR khác bucket cùng wave

---

## 1. The Rule

> **Trước khi spawn FE bucket trong cross-layer wave, `documents/01-business/{domain}/api-contract.md` PHẢI được updated TRONG cùng wave plan PR (hoặc một foundation bucket ship FIRST). FE bucket reference contract chứ không tự design endpoint shape.**

Cross-layer wave (FE+BE đều có mặt) không có api-contract sẽ tạo ra "FE-first endpoint proliferation":
- FE agent design endpoint shape theo phỏng đoán → mock theo phỏng đoán → ship
- BE agent (cùng wave hoặc sau) phải tái-design endpoint match FE giả định → hoặc force FE refactor sau
- Mỗi GAP cha sinh ra 6+ sub-endpoints ad-hoc → cluster của các sub-gap follow-up

Wave 32 v1 (2026-05-06) là worked example: 1 GAP-272 → 6+ sub-letters c/d/e/i/j/k/l vì FE-first không có contract foundation. Rework Opus (2026-05-07) ship được 4/4 buckets nhưng vẫn để lại 8 sub-gap follow-up cho BE catch up.

---

## 2. Khi áp dụng — Cross-Layer Wave Definition

Một wave được coi là **cross-layer** khi thỏa MỘT trong các điều kiện sau:

| Điều kiện | Ví dụ |
|---|---|
| Có ≥1 bucket touch `*-frontend/**` AND ≥1 bucket touch `*-core/**` | Wave 32 (FE wizard + BE endpoint clusters) |
| Một bucket touch CẢ `*frontend/**` lẫn `*-core/**` | Single-bucket wave với full-stack feature |
| FE bucket consume API endpoint mà BE bucket cùng wave tạo | Wave 34 (Phase D) — FE refactor consumes 6 BE endpoints |
| FE bucket dùng mock/fixture cho endpoint chưa tồn tại trong code | Wave 32 inline mocks → contract violation |

Wave **không** cross-layer (rule này không áp dụng):
- Pure docs / runbook wave
- Pure FE wave (no new BE contract — chỉ consume endpoint đã có api-contract.md ship trước)
- Pure BE wave (no FE consumer trong cùng wave)
- Frontend-only kit ports (UI redesign không thay đổi API shape)

---

## 3. Required Artifacts cho Cross-Layer Wave Plan

Wave plan PR cho cross-layer wave PHẢI có:

### 3.1 Foundation Bucket (Bucket 0)

Bucket đầu tiên trong §3 Scope, ship FIRST trước các bucket khác:

```markdown
### Bucket 0 — Foundation (Contract + Mock Infrastructure)

- Files: `documents/01-business/{domain}/api-contract.md` (CREATE/UPDATE)
  - Liệt kê mọi endpoint mà FE/BE bucket trong wave này consume
  - Mỗi endpoint: method + path + request/response schema + error codes
- Mock infrastructure (nếu wave dùng MSW handlers): `{frontend}/src/test/msw/handlers/{domain}.ts` setup
- Acceptance: `documents/01-business/{domain}/api-contract.md` tồn tại và list đủ endpoints
- Spawn order: **MERGE FIRST**, sau đó FE+BE buckets parallel
```

### 3.2 State-Check Evidence Row cho api-contract.md

§4 State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6) PHẢI có row riêng cho api-contract:

```markdown
| `documents/01-business/{domain}/api-contract.md` | API contract doc | `ls documents/01-business/{domain}/api-contract.md` | <result> | ✅ exists / 🆕 to-be-created (Bucket 0 Foundation) |
```

Verdict `🆕 to-be-created` HỢP LỆ chỉ khi Bucket 0 Foundation tồn tại trong §3 Scope. Verdict ✅ exists yêu cầu file hiện có endpoint relevant — nếu file có nhưng thiếu endpoint mới, treat như 🆕 partial-update với Bucket 0 Foundation owning the update.

### 3.3 FE Bucket Acceptance Criteria phải reference contract

Mỗi FE bucket trong cross-layer wave có AC line:

```markdown
- [ ] Endpoint consumption tuân thủ schema trong `documents/01-business/{domain}/api-contract.md` (Bucket 0 ship trước)
- [ ] Không hard-code endpoint shape khác với contract; nếu cần extension, update contract trong cùng PR
```

### 3.4 BE Bucket Acceptance Criteria phải implement contract

Mỗi BE bucket trong cross-layer wave có AC line:

```markdown
- [ ] Controller signature + DTO match `documents/01-business/{domain}/api-contract.md` schema
- [ ] Integration test verify response shape match contract
```

---

## 4. Decision Flow khi Plan Cross-Layer Wave

```
1. Identify scope — wave touch FE+BE? Yes → cross-layer
2. List endpoints — wave này tạo/consume endpoint nào?
3. Check api-contract — `ls documents/01-business/{domain}/api-contract.md`
   - Exists + đủ endpoint → Bucket 0 không cần (skip foundation)
   - Exists nhưng thiếu endpoint → Bucket 0 Foundation = update contract
   - Missing → Bucket 0 Foundation = create contract
4. Add Bucket 0 vào §3 Scope với spawn order = FIRST
5. Add api-contract.md row vào §4 State-Check Evidence
6. Mỗi FE/BE bucket reference contract trong AC
7. Spawn order: Bucket 0 merge → THEN spawn FE+BE buckets parallel
```

---

## 5. Anti-Patterns

| ❌ Don't | ✅ Do |
|---|---|
| Spawn FE bucket trước khi api-contract.md ship | Bucket 0 Foundation merge first |
| FE agent design endpoint shape "phỏng đoán" theo BE | Contract là source of truth; FE đọc contract |
| Inline mock endpoint trong FE bucket mà không có MSW handler reference | MSW handler setup trong Bucket 0; FE bucket consume handler |
| BE bucket implement endpoint "match FE giả định" sau khi FE ship | BE bucket implement match contract; FE ship sau hoặc cùng wave |
| 1 GAP cha → 6+ sub-letters ad-hoc do FE design lệch BE | Contract định trước → FE+BE bucket cùng follow → ≤1-2 sub-gap follow-up |
| Skip rule này vì "wave nhỏ, nhanh hơn nếu không contract" | Tốc độ ngắn-hạn = nợ kỹ thuật dài-hạn (Wave 32 worked example) |

---

## 6. Enforcement

### 6.1 Reviewer-checklist (light-parity per `rule-change-process.md` §6.5)

Mỗi wave plan PR có cross-layer scope, reviewer xác nhận:

- [ ] Wave có cross-layer scope (FE+BE) per §2 definition? Nếu CÓ:
  - [ ] §3 Scope có Bucket 0 Foundation (api-contract + mock infra) HOẶC api-contract.md đã ship trước trong wave plan trước
  - [ ] §4 State-Check Evidence có row cho `documents/01-business/{domain}/api-contract.md`
  - [ ] FE bucket AC reference contract
  - [ ] BE bucket AC implement contract
  - [ ] Spawn order: Bucket 0 merge FIRST trước khi FE+BE parallel

### 6.2 PR template hook (deferred — follow-up gap)

Detector wiring (regex check trong `scripts/check-docs.sh` Rule N: nếu wave plan có FE+BE scope nhưng thiếu Bucket 0 Foundation → WARN/BLOCK) deferred to follow-up gap. Reviewer-checklist + worked example trong rule này đủ enforcement parity cho v1.0.0 per `rule-change-process.md` §6.5 light-parity option (paired với follow-up tracking).

### 6.3 Wave-pack-planner skill integration

`.claude/skills/quality/wave-pack-planner/SKILL.md` §Step 4.5 "Cross-layer check (api-contract first)" reference rule này trong process — same PR.

### 6.4 Override mechanism

Trường hợp genuine exception (vd: cross-layer wave nhưng endpoint shape đã được lock bởi external API contract bất biến — như payment gateway):

```
git commit -m "...
CONTRACT_FIRST_OVERRIDE: <reason — e.g. external payment gateway API locked, no internal contract needed>"
```

Trailer logged. Pattern frequency >5% per quarter triggers meta-review.

---

## 7. Self-test (Worked Example)

### 7.1 Wave 32 v1 (2026-05-06) — VIOLATED rule

State at plan time:
- Scope: FE wizard 6 steps + BE endpoint cluster
- `documents/01-business/ai-branding/api-contract.md` — không exist hoặc thiếu wizard endpoints
- Wave plan §3 Scope: 4 buckets (Steps 1-2, Steps 3-4, Step 5, Step 6) — **không có Bucket 0 Foundation**
- §4 State-Check Evidence: không có row cho api-contract.md

Outcome:
- FE agents design 6+ endpoint shapes "phỏng đoán" trong inline mocks (`MOCK_TAKEN_SLUGS`, `TEMPLATE_TO_COLORS`, etc.)
- BE chưa có endpoint match FE giả định
- Cluster 8 sub-gap follow-up GAP-272c/d/e/h/i/j/k/l filed sau closure để BE catch up
- Rework session 2026-05-07 phải ship 4/4 buckets riêng để recover

→ Rule fires correctly: nếu rule này áp dụng tại plan time, sẽ block FE bucket spawn cho đến khi Bucket 0 Foundation ship api-contract.md với 6+ endpoints + MSW handler infra. ✅

### 7.2 Wave 34 (Phase D — sẽ ship sau Phase A meta update) — SẼ SATISFY rule

State at plan time (per `project_post_wave_32_sequence_plan.md` Phase D):
- Scope: 8 sub-gap (1 P0 + 7 P1) — BE Bộ 1, BE Bộ 2, BE Service, FE Refactor
- Wave plan §3 Scope: 5 buckets — **Bucket 0 Foundation FIRST** (api-contract.md + MSW infra) → A BE Bộ 1 + B BE Bộ 2 + C BE Service parallel → D FE Refactor LAST
- §4 State-Check Evidence: row cho `documents/01-business/ai-branding/api-contract.md` với verdict `🆕 to-be-created (Bucket 0 Foundation)`

Outcome (planned):
- Bucket 0 ship api-contract.md với 6+ endpoint schema + MSW handler folder setup → merge first
- BE buckets implement endpoint match contract
- FE bucket consume MSW handlers + real endpoints (post-merge)
- ≤1-2 sub-gap follow-up expected (vs 8 ad-hoc của Wave 32)

→ Rule fires correctly: cross-layer wave → Bucket 0 Foundation prerequisite → endpoint proliferation eliminated. ✅

---

## 8. Open Items / Follow-ups

- [ ] **Detector wiring** (`scripts/check-docs.sh` Rule N: cross-layer wave plan validation) — file follow-up gap khi v1.0.0 stabilizes (~7 ngày sau merge per `incident-to-rule-pipeline.md` premature-rule guard)
- [ ] Cross-link update từ các rule liên quan (`audit-to-gap-pipeline.md` §2.6, `wave-pack-planner/SKILL.md`) — ship cùng PR theo task spec; rule scope không bao gồm sửa các rule liên quan ngược lại

---

## 9. Related

- `audit-to-gap-pipeline.md` §2.6 Wave-Plan Pre-Flight State-Check — rule này extend §4 State-Check Evidence với mandatory api-contract row
- `wave-pack-planner/SKILL.md` §Step 4.5 — process step reference rule này khi plan cross-layer wave
- `rule-change-process.md` §6.5 Enforcement Parity Mandate — rule này ship với reviewer-checklist + worked example per light-parity option
- `incident-to-rule-pipeline.md` — rule này là direct output của Wave 32 v1 endpoint proliferation incident (5-stage applied)
- `gap-done-discipline.md` §3 PARTIAL exit ramp — wave plan không có Bucket 0 Foundation cho cross-layer scope = wave PARTIAL không DONE, sub-gap follow-up filed
- Memory `feedback_fe_first_endpoint_proliferation.md` (paired same PR) — Wave 32 lessons-learned
- Memory `feedback_frontend_msw_missing.md` (paired same PR) — MSW infra gap context

---

## 10. Log

- **2026-05-07 (v1.0.0):** Rule created sau Wave 32 v1 endpoint proliferation incident (1 GAP → 6+ sub-letters ad-hoc do FE-first không có contract). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged 4 questions audit 2026-05-07) → Classify ✓ (no existing rule cover cross-layer contract-first; `audit-to-gap-pipeline.md` §2.6 cover state-check generic, không enforce contract-first specifically) → Rule+Enforce ✓ (this rule + reviewer-checklist + State-Check Evidence row + paired memories `feedback_fe_first_endpoint_proliferation.md` + `feedback_frontend_msw_missing.md` + `wave-pack-planner/SKILL.md` §Step 4.5 update + template compact — all same PR per `rule-change-process.md` §6.5 light-parity option) → Self-Test ✓ (§7 worked examples Wave 32 violated + Wave 34 satisfy) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint, no constraint loosening; existing pure-FE và pure-BE waves grandfathered, rule áp dụng cho cross-layer waves từ Wave 33 trở đi). Detector wiring deferred to follow-up gap (~7 ngày stabilization period); enforcement = reviewer-checklist + worked-example self-test sufficient for v1.0.0 per light-parity option.
