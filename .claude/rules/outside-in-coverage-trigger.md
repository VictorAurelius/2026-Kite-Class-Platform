# Outside-In Coverage Trigger — Claude phải tự động đề xuất outside-in audit khi dev brainstorm inside-out

**Priority:** 🔴 CRITICAL — force-multiplier governance preventing inside-out blindspots
**Version:** 1.1.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-18
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.0 MINOR self-approve per `rule-change-process.md` §5; Wave 93 retro — adds §2 row "Architecture-decision keywords trong gap filing" (build/engine/integration/partnership/processor/self-build) + §6 worked self-test 4 Wave 93 incidents (GAP-185/183/259/581 lệch hướng). v1.0.0 (kept): new rule với built-in enforcement (memory auto-load + self-detection checklist + worked self-test on Wave 73 incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — extends trigger pattern to gap-filing time for architecture-decision scope (previously trigger only fired at wave/scope brainstorm; missed individual gap filings proposing architecture decisions, leading to Wave 93 surfacing 4 lệch hướng gaps filed Wave 30-86))
**Applies to:** Mỗi lần dev (user) đề xuất scope mới (gap mới, wave plan, meta-rule, feature list, beta cohort plan, release scope) từ góc nhìn inside-out (liệt kê tính năng / hệ thống có sẵn / cải tiến dev nghĩ tới). Claude PHẢI tự động hỏi user có nên invest outside-in audit không, KHÔNG đợi user nudge. v1.1.0 extends scope to individual gap filings proposing architecture decisions (self-build / engine / integration / partnership / processor) — see §2 row "Architecture-decision keywords".

---

## 1. The Rule

> **Khi dev (user) đề xuất scope mới được phrase như "có sẵn tính năng A, B, C — cần làm thêm gì?", "đây là gaps tôi nghĩ ra", "đề xuất Wave N có buckets X/Y/Z", Claude PHẢI proactively đề xuất bổ sung outside-in audit (persona simulation / external benchmark / failure-mode matrix) BEFORE scope được lock vào wave plan hoặc gap file.**

Inside-out = "dev liệt kê features dev có / dev nghĩ tới" — góc nhìn từ trong system ra ngoài. Outside-in = "user thực sự cần gì + kỳ vọng gì + bị cản ở đâu" — góc nhìn từ ngoài (user, ngành) vào system.

Hai góc nhìn này **bù trừ nhau**, không thay thế nhau. Dev đề xuất giỏi inside-out (biết hệ thống). Dev YẾU outside-in (đã quá quen, blind spot tâm lý user). Claude có lợi thế cross-cutting view + access tới skill `persona-based-business-review` + `simulation-gap-finder` + external benchmark research.

Nếu Claude chỉ execute inside-out scope → ship product mà beta user gặp gaps tâm lý / kỳ vọng / văn hoá → bounce → wave sau phải fix lại retroactively → chi phí cao hơn.

---

## 2. Trigger pattern — khi nào rule này fire

Rule fire khi user message có dấu hiệu **inside-out brainstorm**:

| Pattern | Ví dụ |
|---|---|
| **Liệt kê features/gaps có sẵn + hỏi "đã đủ chưa?"** | "Hệ thống đã có X, Y, Z. Beta đủ chưa?" |
| **Đề xuất wave plan với buckets feature-list** | "Wave 73 sẽ có 4 bucket: email audit + manual + Tally + UI smoke" |
| **File gap mới mô tả feature gaps** | "GAP-XXX: thiếu nút export — implement nút export" |
| **Brainstorm tính năng "cần làm thêm"** | "Cần làm gì nữa cho launch?" |
| **Pre-release readiness check** | "Sẵn sàng invite chưa?" |
| **Comparison với own backlog** | "So với roadmap đã có, còn miss gì?" |
| **🆕 Architecture-decision keywords trong gap filing** (v1.1.0) | "GAP-185 self-build VAT engine", "GAP-NEW-payment-processor-init", "GAP-183 build refund engine", "GAP-XXX integration với <vendor>", "GAP-XXX partnership với <provider>" — any gap proposing build/engine/integration/partnership/processor/broker scope |

### 2.1 Architecture-decision keywords detection (added v1.1.0)

Khi file gap (gap PR draft OR initial commit) chứa `## Proposed Fix` / `## Problem` / title với keywords sau:

- **Build verbs:** "build X engine", "self-build", "implement X processor", "construct X integration"
- **Architecture nouns:** "engine", "processor", "broker", "integration layer", "partnership", "merchant integration"
- **Vendor-specific names:** Stripe / MoMo / VNPay / VietQR / MISA / Casso / SePay / AWS / GCP — when gap proposes ADOPT or BUILD trên specific vendor

→ Rule fire BEFORE gap merge (in same chat session OR pre-merge reviewer-checklist). Outside-in audit có thể surface:
- Industry pattern khác (vd: partnership > self-build — Wave 93 GAP-185 case)
- Compliance constraint (vd: PSP license — Wave 93 GAP-NEW-payment-processor-init cancel)
- DUPLICATE existing gap (vd: GAP-259 ≈ GAP-581 Wave 93 cross-session miss)
- Phase mismatch (vd: GAP-123/124/125 infrastructure assigned phase-1.5-paid without payment dependency)

Architecture-decision keywords là **high-leverage trigger** vì decision rooted trong gap = compounds qua N future implementation waves. Catch lệch hướng ở gap-filing time = 50x cheaper than catch ở implementation time (per Wave 93 retro evidence: 4 gaps filed Wave 30-86 surfaced lệch hướng tại Wave 93 audit).

Rule **KHÔNG** fire khi:
- User đề xuất fix cho gap đã có (đã qua brainstorm phase)
- User execute task cụ thể (deploy, merge, run test)
- User hỏi câu thuần kỹ thuật (debug, lookup file)
- User đã CHÍNH THỨC đóng scope ("đã chốt rồi, không thêm")

---

## 3. Hành động Claude phải làm khi rule fire

### Bước 1: Acknowledge inside-out

> "Đề xuất của bạn là inside-out (liệt kê features có sẵn). Để chắc đủ scope, có nên audit outside-in không?"

### Bước 2: Đề xuất 3 cách outside-in (skill có sẵn)

| Phương pháp | Skill / Tool | Phù hợp khi |
|---|---|---|
| **Mô phỏng nhân vật** | `.claude/skills/quality/persona-based-business-review/SKILL.md` | User-facing scope (signup, onboarding, daily use) |
| **So sánh ngành** | WebSearch external SaaS reference | Pre-launch / beta cohort / business model decisions |
| **Ma trận tìm gap** | `.claude/skills/quality/simulation-gap-finder/SKILL.md` | Complex flows nhiều failure modes |

### Bước 3: AskUserQuestion với 3 options + "tất cả 3 song song"

User confirm scope outside-in trước khi Claude lock wave plan / gap file.

### Bước 4: Nếu user chọn YES — Spawn agents song song, RUN BEFORE wave plan ship

Outside-in findings phải có sẵn TRƯỚC khi:
- Wave plan PR mở (per `feedback_wave_plan_through_pr.md`)
- Gap file Status field set OPEN
- Meta-rule scope chốt

### Bước 5: Tổng hợp inside-out + outside-in findings

Wave plan / gap / rule được build từ HAI nguồn:
- Inside-out (dev brainstorm)
- Outside-in (Claude audit)

Highlight **gap mới** (chỉ outside-in tìm ra) vs **overlap** (cả hai cùng có).

---

## 4. Các trường hợp ngoại lệ (skip rule)

Rule KHÔNG mandate khi:

| Case | Lý do |
|---|---|
| Wave plan PATCH hotfix prod incident | Tốc độ ưu tiên; outside-in audit cho follow-up wave |
| Gap fix cụ thể đã có root cause | Outside-in irrelevant cho bug fix kỹ thuật |
| Wave 100% internal scope (ops, refactor, tech debt) | Không có user-facing change |
| User đã trải qua outside-in (audit gần đây ≤ 30 ngày) | Refresh không cần — nhưng vẫn nên hỏi 1 lần |
| User explicitly say "skip persona audit, just execute" | Tôn trọng quyết định user; ghi nhận lý do |

Khi skip → vẫn note "Outside-in audit skipped: <reason>" trong wave plan / gap file để future reader hiểu.

---

## 5. Banned shortcuts

| ❌ Không được | ✅ Phải làm |
|---|---|
| Im lặng execute inside-out scope mà không nhắc outside-in | Hỏi 1 lần ở Bước 1-2 |
| Chỉ execute outside-in audit khi user nudge ("còn miss gì không?") | Proactively offer BEFORE user nudge |
| Đề xuất outside-in nhưng không spawn agent | Spawn agent ngay khi user OK |
| Coi outside-in là "nice-to-have" | Coi là default cho user-facing scope |
| Outside-in audit sau khi wave plan đã merge | Phải làm BEFORE wave plan merge — findings vào §1 Brainstorm Q1 |
| Spawn 1 agent, bỏ qua 2 cách kia | Đề xuất cả 3 trong AskUserQuestion (user pick); KHÔNG tự quyết định cho user |

---

## 6.0 Worked self-test — Wave 93 architecture-decision drift (2026-05-18, v1.1.0)

**Bối cảnh:** Wave 93 outside-in audit (3-agent) cho user proposal "QR upload payment scope Phase 1.5" surfaced **4 gaps cũ filed Wave 30-86 lệch hướng** so với industry pattern + compliance constraints VN:

| Gap | Filed | Original scope | Outside-in audit verdict (Wave 93) | Lệch hướng cause |
|---|---|---|---|---|
| **GAP-185** | 2026-04-20 | "Self-build VAT/TCT Invoice engine" | Re-scope → MISA MeInvoice partnership | Inside-out "build all" mentality; no industry benchmark fire tại gap filing |
| **GAP-183** | 2026-04-20 | "Build refund + dispute resolution engine" | Re-scope → manual SOP (KiteHub non-PSP) | No compliance check (PSP license) at gap filing |
| **GAP-NEW-payment-processor-init** (planned) | (would be ~2026-05-18) | "VNPay/MoMo merchant integration" | CANCEL Phase 1.5; defer Phase 2 partnership | Would have hit PSP license + KYC barriers if not caught by outside-in audit |
| **GAP-259 ≈ GAP-581** | 2026-04-28 / 2026-05-15 | DUPLICATE "Gateway rate limit by tenant_id" | Merge candidate flagged | No cross-gap duplicate-check fire when GAP-581 filed (5 tuần later) |

**Apply v1.1.0 rule retroactively to gap-filing moments:**

**Bước 1 — Acknowledge inside-out:** When user/dev drafts "GAP-185 self-build VAT engine" → Claude phải nhận thấy "build engine" keyword → fire rule §2 row "Architecture-decision keywords".

**Bước 2 — Đề xuất 3 cách outside-in:** Especially **external benchmark** cho VAT/payment/integration scope — high signal industry pattern.

**Bước 3 — AskUserQuestion:** "Có nên benchmark VN edu SaaS pattern cho VAT eInvoice trước khi lock self-build approach?"

**Bước 4 — Spawn agent:** External benchmark agent surfaces MISA MeInvoice partnership (industry norm).

**Bước 5 — Tổng hợp:** Gap §Proposed Fix scope refined từ "self-build VAT engine" → "MISA MeInvoice partnership integration" BEFORE gap file Status set OPEN.

**Kết quả nếu rule v1.1.0 áp dụng từ Wave 30-86:**
- 4 lệch hướng gaps eliminated at gap-filing time (vs Wave 93 retroactive audit cost)
- ~3-4 wave equivalents engineering effort saved (each lệch hướng gap would have wasted ~1 wave when implementation hit reality)
- Phase 1.5 timeline cleaner — no scope refinement mid-wave (Wave 33-34 Phase 1.5b)

**Cost of v1.1.0 trigger:** ~1 minute per gap filing (AskUserQuestion + 3-agent audit spawn) × 30-50 gap filings per quarter = ~30-50 minutes/quarter trigger cost. Compared to ~3-4 wave equivalents saved per recurring drift cluster = **net 10-50x ROI**.

→ Rule v1.1.0 fires correctly trên 4 originating drift cases Wave 93. Self-test PASS ✅

---

## 6. Worked self-test — Wave 73 incident 2026-05-14

**Bối cảnh:** User hỏi "Mời beta user thì mới chuẩn bị workflow trong hệ thống để user trải nghiệm... các mail template ổn đúng không?"

**Claude trả lời ban đầu (vi phạm rule):**
- Liệt kê 4 phương án (email audit / Premium plan / user manual / feedback channel / UI polish)
- Đề xuất Wave 73 với 4 bucket dựa hoàn toàn trên inside-out
- KHÔNG hỏi user có nên outside-in audit không
- User phải push back: "đề xuất là dev nghĩ đến, nhưng không chắc đủ. Claude cần làm gì để improve?"

**Apply rule retroactively:**

Bước 1: Acknowledge inside-out — Claude phải tự nhận thấy "câu hỏi của user là pre-release readiness check + có dấu hiệu liệt kê features"  → fire rule.

Bước 2: Đề xuất 3 cách — persona + benchmark + matrix.

Bước 3: AskUserQuestion với 4 options (3 cách + "all 3 parallel").

Bước 4: User chọn → spawn 3 agents song song.

Bước 5: Tổng hợp → Wave 73 plan có inside-out (dev) + outside-in (Claude) trong §1 Brainstorm.

**Kết quả nếu rule áp dụng từ đầu:**
- Tiết kiệm 1 vòng push-back của user
- Wave 73 scope chính xác hơn ngay từ lần đầu
- Beta user khi nhận invite sẽ trải nghiệm sản phẩm đã được audit outside-in

→ Rule fires đúng cho incident gốc. Self-test PASS ✅

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Memory auto-load mỗi session

Memory entry `feedback_outside_in_coverage_trigger.md` (paired same-PR). Mỗi session start, Claude đọc memory → checklist:

1. User message này có dấu hiệu inside-out brainstorm không? (§2 patterns)
2. Nếu CÓ → fire rule §3 Bước 1-2 trước khi execute
3. Nếu nghi ngờ → default fire (cost của question 1 message; cost của miss = wave retrospective)

### 7.2 Self-detection mỗi turn

Trước khi Claude phản hồi đề xuất scope:
- Đọc lại user message
- Match với §2 patterns
- Nếu match → bắt buộc Bước 1-2 trước Bước 3 (AskUserQuestion về outside-in)
- Nếu không match → execute bình thường

### 7.3 Reviewer-checklist (manual)

Khi review wave plan PR / new gap PR / meta-rule PR, reviewer hỏi:
- PR này có dấu hiệu inside-out only (không có persona audit / benchmark / matrix reference)?
- Nếu CÓ + scope là user-facing → flag + ask author "đã chạy outside-in audit chưa?"

### 7.4 Override mechanism

Cho phép override khi user explicit từ chối outside-in:

```
OUTSIDE_IN_SKIP: <lý do — vd "hotfix prod incident, defer audit to follow-up wave">
OUTSIDE_IN_FOLLOWUP: <gap link scheduling audit cho wave sau>
```

Pattern frequency > 30% trong 1 tháng → meta-review rule (có thể đang quá nặng).

### 7.5 Detector (deferred)

Future enhancement: scan PR description / commit body for inside-out keywords ("đề xuất", "wave bucket", "có sẵn") without outside-in artifact (persona report / benchmark / matrix) → WARN. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥ 7 ngày; enforcement memory + self-detection + reviewer-checklist đủ cho v1.0.0.

---

## 8. Relationship to other rules

- **`agent-action-bias.md`** §1 Part A — "do it yourself"; rule này extend: "do outside-in audit yourself, đừng đợi user nudge"
- **`meta-gap-priority.md`** §3 — meta gaps ưu tiên cao nhất; rule này là META P0 force-multiplier
- **`incident-to-rule-pipeline.md`** — rule này là direct output của 2026-05-14 user-flagged miss applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory + self-test all ship same PR
- **`feedback_wave_plan_through_pr.md`** — wave plan PR-first; rule này extend với "outside-in audit happens BEFORE wave plan PR mở"
- **`output-review-mandate.md`** §3 — adds row "Inside-out → outside-in trigger" tracking standard
- **`audit-to-gap-pipeline.md`** §2.6 wave-plan state-check — rule này thêm layer state-check thứ 2: ngoài symbol verification còn cần persona walkthrough trước plan merge
- **`feedback_outside_in_coverage_trigger.md`** (memory, paired same-PR)

---

## 9. Log

- **2026-05-18 (v1.1.0):** MINOR — added §2 row "Architecture-decision keywords trong gap filing" + §2.1 detection scope + §6.0 worked self-test 4 Wave 93 drift incidents (GAP-185 self-build VAT / GAP-183 self-build refund / GAP-NEW-payment-processor-init cancel / GAP-259 ≈ GAP-581 DUPLICATE). Triggered by Wave 93 outside-in audit retro user-flagged "tại sao gaps cũ lại lệch hướng, có phải cần update meta để tránh lặp lại không?" Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (Wave 93 retro 2026-05-18 user-flagged) → Classify ✓ (rule v1.0.0 trigger fires tại wave/scope brainstorm thời điểm — KHÔNG fire tại individual gap filing → 4 lệch hướng gaps filed Wave 30-86 escape detection) → Rule+Enforce ✓ (this v1.1.0 extension + paired §6.0 retroactive self-test + memory auto-load update per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6.0 worked example: rule v1.1.0 fires correctly on 4 drift cases) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds trigger pattern row at gap-filing time; no constraint loosening for prior gap filings (grandfathered); existing gaps with architecture-decision scope tracked Wave 93 re-triage audit; rule applies prospectively to gap filings từ Wave 94+ forward).
- **2026-05-14 (v1.0.0):** Rule được tạo trong response trực tiếp user-flagged miss: "thiếu sót rất lớn trong dự án là chưa yêu cầu claude, khi dev đưa ra inside-out có tạo gaps mới/meta mới/... thì claude tự động hỏi nên invest để cover hết outside-in hay không". Triggered bởi Wave 73 pre-invite brainstorm where Claude listed 4 inside-out bucket options without proactively suggesting outside-in audit. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no existing rule mandates auto-suggest outside-in cho inside-out brainstorm; `agent-action-bias.md` covers do-it-yourself nhưng không covers cross-cutting audit triggering) → Rule+Enforce ✓ (this file + paired same-PR memory + worked self-test §6 + cross-link updates per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on the originating Wave 73 session — rule fires correctly + counterfactual shows 1 user push-back round-trip eliminated) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint covering previously-uncovered auto-trigger gap; no constraint loosening for prior work; existing wave plans grandfathered, rule applies prospectively từ next session). Detector wiring (§7.5) deferred per premature-rule guard ≥ 7 ngày; enforcement = memory auto-load + self-detection checklist + reviewer-checklist đủ cho v1.0.0.
