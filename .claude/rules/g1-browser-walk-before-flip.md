---
paths:
  - "documents/03-planning/waves/wave-*-flow-*.md"
  - "documents/03-planning/roadmap/flow-verification-campaign.md"
  - "documents/05-guides/operations/*-g2-recipe-*.md"
  - ".claude/rules/g1-browser-walk-before-flip.md"
---

# G1 Browser-Walk Before Flip — browser thật trước khi flip G1 PASS cho FE flow

**Priority:** 🟠 MANDATORY — flow verification campaign G1 gate discipline
**Version:** 1.3.0
**Created:** 2026-06-08
**Last-Reviewed:** 2026-06-16
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.2.0 MINOR self-approve per `rule-change-process.md` §5; adds §3.2 general principle "production access-mode PHẢI local-reproduce-được" generalizing §3.1 subdomain case to any production access/entry mechanism (custom-domain / JWT-claim routing / header-scope / OAuth redirect) + phân tầng functional-parity (G1/G2★ local) vs infra-parity (G3-infra AWS-gated) khớp campaign §1 G2★ merge 2026-06-11; paired same-PR campaign gate redefinition; no constraint loosening — codifies general principle previously implicit chỉ ở subdomain instance. v1.1.0 (kept): adds §3.1 production-accurate domain simulation cho host/subdomain flow (nip.io/etc-hosts mandatory, `?tenant=`/query-override banned as G1/G2 evidence) + §4 banned rows + §7.1 checklist row, paired same-PR worked self-test on GAP-811 recipe `?tenant=` slip 2026-06-08 per §6.5 Enforcement Parity Mandate; no constraint loosening — tightens browser-walk fidelity for host-based flows. v1.0.0 (kept): MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + memory auto-load + worked self-test on Flow Verification Campaign KC-1 G2 session 2026-06-08 — 3/3 bugs GAP-1067/1068/1069 would have surfaced ở G1 nếu có browser-walk) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-implicit "G1 walk cho FE flow phải qua FE :3000 thật, không chỉ curl gắn header tay"; META P1 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi flip status flow user-facing CÓ FE trong `flow-verification-campaign.md` §4 từ chưa-G1 → `🔄 walk-pass-pending-human` (G1 PASS). Out-of-scope: flow API-only không FE surface (vd internal cron / webhook consumer / BE-only side-effect), pure refactor, docs-only PR.

---

## 1. The Rule

> **Trước khi flip flow user-facing có FE từ chưa-G1 → `🔄 walk-pass-pending-human` (G1 PASS), MUST chạy ÍT NHẤT 1 lượt walk trên browser thật (headless browser thật cũng OK, KHÔNG phải curl gắn header thủ công) qua FE `:3000`.** Walk qua browser để FE tự inject auth token + tenant header + route — bắt drift FE↔gateway, credential, routing mà `curl`-with-manual-header che mất. Curl-walk vẫn dùng để verify từng AC/endpoint nhưng KHÔNG đủ một mình cho G1 PASS của flow có FE.

`curl` qua gateway `:9000` với header gắn tay (vd `X-Instance-Subdomain: sky-education`, `Authorization: Bearer <token>`) re-tạo **một** giả định về wire-format. Real browser path khác: FE tự derive tenant header từ JWT claim / subdomain, tự gắn token từ session storage, tự resolve Next.js route. Curl-walk PASS ≠ browser-walk PASS — gap giữa hai = exactly nơi user gặp lỗi ở G2.

Sister rules theo walk lifecycle, khác boundary:
- `pre-walk-persona-simulation-mandate.md` §1 — PRE-walk persona simulation (trước walk)
- `feature-ship-runtime-walk-mandate.md` §3.4 — catalog-then-batch DURING walk
- `pre-handoff-self-test-completeness.md` §3 — POST-FIX re-walk
- `g2-handoff-md-mandate.md` §1 — G2 handoff recipe AFTER G1 PASS
- **rule này** — browser-real verify là điều kiện cần của G1 PASS cho FE flow (khác curl-only)

Force-multiplier: 1 chuẩn browser-walk-before-G1-flip → mọi flow FE subsequent (22-flow campaign) auto-comply → eliminate "curl PASS nhưng G2 lòi hàng loạt" class.

---

## 2. Trigger pattern — khi nào rule fires

Rule fires khi:

| Pattern | Ví dụ |
|---|---|
| Flip campaign §4 row → `🔄 walk-pass-pending-human` cho flow CÓ FE surface | KC-1 tenant settings page, KC-3 course/class UI, KH-3 subscription checkout page |
| Wave plan §5 Verification Gates flip `G1 ⬜ → ✅ PASS` cho flow có FE page | wave-flow-kc1 §5 |
| Closure PR mentions "G1 ✅" / "G1 PASS" cho flow có dashboard/form/portal | feat(wave-flow-X): G1 PASS commit |

Rule **KHÔNG** fires khi:
- Flow API-only KHÔNG có FE surface (vd KH-4 manual VietQR admin-confirm BE-only, internal cron, webhook consumer) — curl-walk đủ cho G1; ghi rõ "API-only, no FE — browser-walk N/A" trong evidence
- G1 ⚠️ PARTIAL (chưa flip pending-human, còn iterate fix loop)
- Re-walk sau fix (covered by `pre-handoff-self-test-completeness.md` §3 post-fix re-walk)
- Flow đã `✅ THÔNG` (G1+G2+G3) — không cần re-flip
- FE page của flow chưa build (contract-first stub) → flow defer, KHÔNG flip G1 cho FE-half (vd KC-9 student portal DEFERRED Phase 2)

---

## 3. Required browser-walk evidence

Browser-walk evidence dán vào wave plan G1 section HOẶC campaign §4 row dưới `## G1 browser-walk evidence (per g1-browser-walk-before-flip.md §3)`:

| Evidence | Tiêu chí PASS |
|---|---|
| **(a) FE entry point thật** | Mở `http://localhost:3000/<route>` trên browser (Chrome DevTools / Playwright headless) — KHÔNG curl |
| **(b) Console clean** | DevTools Console không có uncaught error / ERR_EMPTY_RESPONSE / failed-to-fetch trên happy path |
| **(c) Network tab status** | Request chính tới gateway `:9000` trả 2xx (KHÔNG 400/404/503) — screenshot OR cite từng status |
| **(d) FE-injected header observed** | Network tab Request Headers cho thấy FE TỰ gắn `X-Instance-Subdomain` (HOẶC JWT `tenantId` claim) + `Authorization: Bearer` — KHÔNG do tay |
| **(e) FE route resolves** | Next.js route render đúng page (KHÔNG 404 page / redirect loop / blank), data hiển thị |
| **(f) ≥1 sad path qua browser** | Submit form sai / unauthorized → FE hiển thị error message rõ ràng (KHÔNG silent fail) |

Nếu BẤT KỲ evidence (a)-(e) FAIL → flow KHÔNG đạt G1 PASS, ở lại trạng thái G1 walk-in-progress; catalog blocker + fix per campaign loop §2.

### 3.1 Production-accurate domain simulation cho host/subdomain-based flow (added v1.1.0)

> **Khi flow resolve tenant/context qua Host header (subdomain → tenant landing, custom domain, host-based routing), browser-walk PHẢI dùng subdomain Host THẬT (production-accurate), CẤM dùng query-override (`?tenant=`/`?preview=`) hoặc localhost thuần làm bằng chứng G1/G2.** Query-override đi qua nhánh dev-preview, **BYPASS** chính Host-resolution path mà flow cần verify → pass ở đó KHÔNG chứng minh production hoạt động.

Lý do (incident 2026-06-08): recipe GAP-811 ban đầu chọn `?tenant=sky-education` làm primary vì tiện/no-sudo — nhưng `extractSlug()` ưu tiên `?tenant=` TRƯỚC Host (middleware line 99) → bypass `extractSlugFromHost()`. Pass `?tenant=` không test cơ chế production. Phải push 2 lần mới ra đúng.

**Cách local production-accurate (xếp ưu tiên):**

| Cách | Test Host→resolve thật? | Cần sudo? | Khuyến nghị |
|---|:---:|:---:|---|
| **nip.io / sslip.io wildcard** (`<sub>.127.0.0.1.nip.io:<port>`) | ✅ | ❌ | **DEFAULT** — DNS công cộng resolve 127.0.0.1, Host header thật có subdomain, no sudo |
| `/etc/hosts` + `<sub>.<domain>.local` | ✅ | ✅ | Fallback offline (nip.io cần internet DNS); dev chạy `!`+sudo |
| `curl -H "Host: <sub>..."` | ✅ (BE/middleware) | ❌ | Bổ trợ per-AC, KHÔNG đủ cho browser visual |
| `?tenant=`/query-override | ❌ bypass | ❌ | CHỈ smoke "FE render branding", KHÔNG tính G1/G2 evidence |

Khoảng cách nip.io ↔ production thật (port/TLS/LB/wildcard-cert) = **infra parity (G3 territory)**, không phải G1/G2 functional. nip.io exercise đúng 100% resolution logic.

Evidence (d) "FE-injected header observed" cho flow host-based PHẢI cho thấy Host header chứa subdomain thật (không phải `?tenant=` query param).

### 3.2 Nguyên tắc tổng quát — production access-mode PHẢI local-reproduce-được (added v1.2.0)

> **Tổng quát hóa §3.1: bất kỳ cơ chế access/entry production nào quyết định behavior (Host→tenant subdomain, custom-domain, JWT-claim routing, header-derived scope, OAuth redirect URI) — local walk PHẢI tái hiện CHÍNH cơ chế đó, KHÔNG thay bằng đường tắt dev bypass nó.** Production truy cập bằng X → local test bằng X (vd subdomain → nip.io subdomain), không phải bằng dev-shortcut Y (vd `?tenant=`) đi nhánh khác. Test qua Y rồi flip "thông" = bằng chứng giả: Y không đi qua path mà production dùng.

Force-multiplier: mọi flow trong campaign có 1 access-mode production riêng; 1 chuẩn "local reproduce access-mode" → mọi G1/G2★ subsequent verify đúng chuỗi production thật, eliminate class "local-shortcut-pass nhưng production-path chưa từng chạy" (landing-100 GAP-811 `?tenant=` slip + KC-1 GAP-1067/1068/1069).

Phân tầng với campaign gate (per `flow-verification-campaign.md` §1 mô hình G2★ 2026-06-11):
- **Functional access-mode parity** (subdomain Host resolution, gateway auth chain, RLS, prod-profile) = **local-reproduce-được** (nip.io/etc-hosts) → thuộc **G1 + G2★** (mandatory trước flip THÔNG-local).
- **Infra-only parity** (TLS thật, ALB/LB, wildcard-cert, real-DNS) = **không local-reproduce-được** → thuộc **G3-infra** (AWS-gated, không block THÔNG-local).

Ranh giới: nếu một parity dimension CÓ thể tái hiện local (kể cả qua nip.io/etc-hosts/docker-prod-profile) thì nó là G1/G2★ mandatory, KHÔNG được đẩy sang G3-infra để né.

### 3.3 Canonical KC tenant access recipe — env local (WSL2 + Windows browser) (added v1.3.0)

KiteClass (`:3000`) multi-tenant resolve qua **Host subdomain**. Recipe author + walker PHẢI dùng đúng pattern sau (ghi vào mọi KC G2 recipe):

| # | Bước | Chi tiết |
|---|---|---|
| 1 | **Slug = tenant `subdomain`, KHÔNG phải `instances.slug`** | Verify resolve TRƯỚC khi ghi recipe: `curl :9000/api/v1/public/tenants/by-subdomain/<slug>` → có `"subdomain"` (không `TENANT_NOT_FOUND`). Vd `sky-education` ✅ ; `g2-test-center-5` (instances.slug) ✗ TENANT_NOT_FOUND. |
| 2 | **Host ≥3 phần** (middleware `parts.length < 3 → null`) | `<slug>.127.0.0.1.nip.io` (4+ phần) ✅ ; `<slug>.kiteclass.local` (3 phần) ✅ ; `<slug>.localhost` (2 phần) ✗ không resolve tenant. |
| 3a | **nip.io (default, không sửa hosts)** | `http://<slug>.127.0.0.1.nip.io:3000/...` — cần internet-DNS resolve nip.io (WSL DNS có thể fail nhưng Windows Chrome thường OK). |
| 3b | **hosts file (fallback khi nip.io DNS chặn)** | Windows admin: `C:\Windows\System32\drivers\etc\hosts` thêm `127.0.0.1 <slug>.kiteclass.local` → `ipconfig /flushdns` → `http://<slug>.kiteclass.local:3000/...`. Domain quy ước = `.kiteclass.local`. |
| 4 | **POST-REBUILD GOTCHA (GAP-1067)** | Sau `docker-compose build/up` recreate FE container → port-forward `:3000` Windows↔container **stale** → mọi truy cập `:3000` từ Windows **timeout/ERR_EMPTY_RESPONSE** (kể cả nip.io đúng). Fix: `docker restart kiteclass-frontend` (+ `kitehub-frontend` nếu rebuild), chờ ~12s. **Recipe rebuild step PHẢI kèm restart-FE note.** |

**Banned (đã có ở §3.1/§4):** `localhost:3000` thuần + `?tenant=` làm G1/G2★ evidence cho flow tenant-resolution-dependent.

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Flip G1 PASS cho FE flow chỉ với curl `:9000` + header gắn tay | Browser-walk qua FE `:3000` để FE tự inject header/token/route + curl bổ trợ per-AC |
| "Curl PASS hết AC rồi → G1 done" cho flow có dashboard/form | Curl verify endpoint là cần; browser-walk là đủ — cả hai cho FE flow |
| Gắn `X-Tenant-Id` tay vào curl rồi coi tenant resolution PASS | Để browser FE tự derive tenant header → bắt strip/mismatch thật (GAP-1068 class) |
| Chỉ test endpoint đã biết tồn tại bằng curl | Browser dashboard gọi mọi endpoint FE thực sự gọi → bắt 404 list-endpoint missing (GAP-1069 class) |
| Bỏ qua browser-walk "vì stack vừa up, chắc :3000 chạy" | Verify FE `:3000` thật render (bắt stale docker-proxy / ERR_EMPTY_RESPONSE GAP-1067 class) |
| Browser-walk happy path rồi flip ngay | Walk thêm ≥1 sad path qua browser (error message hiển thị, không silent) |
| Ghi browser-walk trong chat | Evidence vào wave plan / campaign row artifact per §3 |
| Curl từ WSL coi như tương đương browser (cũng có quirk riêng) | Real browser path là canonical cho FE flow G1 |
| Dùng `?tenant=`/query-override làm bằng chứng G1/G2 cho host/subdomain flow | Dùng subdomain Host thật (nip.io / etc-hosts) — query-override bypass resolution path (§3.1) |
| Mở `localhost:<port>` thuần rồi coi tenant resolution PASS | Host-based flow cần subdomain trong Host header; localhost thuần → pass-through fallback, không test resolution |

---

## 5. Override mechanism

Genuine exception (FE chưa build nhưng BE flow cần flip G1 cho phần API, browser env down, headless browser không khả dụng trong session):

```
git commit -m "...
G1_BROWSER_WALK_DEFER: <flow-id> — <reason — e.g. 'KH-4 API-only manual VietQR, no FE surface; curl-walk đủ' OR 'browser env down, headless deferred'>
G1_BROWSER_WALK_FOLLOWUP: <gap link OR wave plan link scheduling browser-walk trước G2 handoff>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review (likely §2 trigger scope mis-defined OR FE flows bị flip sớm bằng curl-only).

---

## 6. Worked self-test — Flow Verification Campaign KC-1 G2 (2026-06-08, originating incident)

**Scenario:** Các G1/G3 walk của KC-1 (và sibling flows) dùng `curl` qua gateway `:9000` gắn header tay (`X-Instance-Subdomain: sky-education`) → đều PASS → flip G1 `🔄 walk-pass-pending-human`. Khi human mở browser thật (G2), lộ 3 lỗi mà curl-walk che mất.

### 6.1 Apply rule retroactively tại G1 flip moment

Rule §1 mandate: trước flip KC-1 G1 PASS, chạy browser-walk qua FE `:3000`. Persona = Owner đăng nhập → mở dashboard/settings page.

### 6.2 Counterfactual — 3 G2 bugs vs browser-walk evidence §3

| Bug | Mô tả | §3 evidence row WOULD HAVE bắt | Verdict |
|---|---|---|---|
| **GAP-1067** | Stale docker-proxy port-forward `:3000` sau compose-up → ERR_EMPTY_RESPONSE; curl WSL quirk không lộ rõ | (a) FE entry point thật + (b) Console clean → browser mở `:3000` → ERR_EMPTY_RESPONSE ngay | ✅ Browser-walk bắt; curl-walk miss |
| **GAP-1068** | Tenant resolution: browser FE gửi `X-Tenant-Id` (gateway strip; cần `X-Instance-Subdomain` HOẶC JWT `tenantId` claim) → 400 mọi call; curl gắn `X-Instance-Subdomain` tay nên PASS | (c) Network 2xx + (d) FE-injected header observed → browser cho thấy FE gắn `X-Tenant-Id` → gateway 400 | ✅ Browser-walk bắt; curl manual-header che |
| **GAP-1069** | FE↔BE contract drift: dashboard gọi `GET /api/v1/classes` + `/api/v1/invoices` list → 404 (BE không expose list endpoint đó); curl chỉ test endpoint đã biết tồn tại | (c) Network status + (e) FE route resolves → browser dashboard gọi list endpoint thật → 404 | ✅ Browser-walk bắt; curl test-known-endpoint miss |

### 6.3 Verdict

3/3 bugs (100%) sẽ lộ NGAY ở G1 nếu browser-walk chạy trước flip, thay vì đợi tới G2 human session. Root insight: G1 agent-curl-walk với header thủ công KHÔNG tương đương real-browser path (FE tự inject header/credential/route).

**Cost-save:**
- Không có rule (thực tế): curl-walk PASS → flip G1 → G2 human session lòi 3 lỗi → fix loop + re-walk + user round-trip + lại G2
- Có rule: browser-walk ~5-10 phút trước flip G1 bắt 3 lỗi → fix PRE-flip → G2 human gặp flow sạch
- **Net savings: ~1 vòng G2 round-trip + restored trust trong G1 gate × mỗi FE flow của 22-flow campaign**

### 6.4 Self-test PASS ✅

Rule fires correctly trên originating KC-1 G2 incident. Prospective application tới Flow Verification Campaign §4 các FE flow còn lại eliminate "curl PASS nhưng G2 lòi" class permanently.

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho wave plan closure PR / campaign update flipping §4 row → `🔄 walk-pass-pending-human` cho flow có FE:

- [ ] Flow có FE surface (dashboard / form / portal / page)? Nếu API-only → override trailer §5 + lý do?
- [ ] Wave plan / campaign row có section `## G1 browser-walk evidence` per §3?
- [ ] Evidence cover (a) FE `:3000` thật + (b) console clean + (c) Network 2xx + (d) FE-injected header observed + (e) route resolves + (f) ≥1 sad path?
- [ ] Evidence là browser path (Chrome DevTools / Playwright headless), KHÔNG phải curl gắn header tay?
- [ ] **(host/subdomain flow) Evidence dùng subdomain Host thật (nip.io / etc-hosts) per §3.1, KHÔNG phải `?tenant=`/query-override (bypass resolution path)?**
- [ ] FE port khớp flow per `kitehub-kiteclass-boundary.md` §2 (KH-* = `:3001`, KC-* = `:3000`; platform-side KC exception §4.1)?
- [ ] Nếu override trailer present, reason + follow-up valid per §5?

### 7.2 Campaign §1 gate definition cross-reference

`flow-verification-campaign.md` §1 G1 row updated: G1 PASS cho flow có FE PHẢI gồm browser-walk qua FE `:3000` per rule này (không chỉ agent curl-walk). Cross-link added.

### 7.3 Memory auto-load (paired same-PR)

Memory entry `feedback_g1_browser_walk_before_flip.md` loads at session start, reminds checklist trước khi flip G1 PASS cho FE flow.

### 7.4 Override mechanism

Per §5 trailer `G1_BROWSER_WALK_DEFER:` — logged quarterly retro. Pattern frequency >10%/quarter → meta-review.

### 7.5 Detector — access-mode slice SHIPPED (2026-06-16, recurrence #2); G1-flip-completeness slice deferred

**SHIPPED — `scripts/check-walk-recipe-access-mode.sh` (§3.1/§3.2 access-mode slice):** statically-detectable per `cross-flow-bug-class-sweep.md` §4.1. Greps walk-artifact MD (`*g2-recipe*.md` + `flow-verification-campaign.md` + `*flow*.md` wave plans) cho KC tenant-flow URL dùng bare `http://localhost:3000`/`127.0.0.1:3000` OR `?tenant=` (vs production-accurate nip.io subdomain). FP-guarded: chỉ match URL thật (`http://` scheme), KHÔNG flag prose documenting the ban. Wired `quality-docs.yml` job `walk-recipe-access-mode` (WARN mode — ~34 pre-existing recipe grandfathered → GAP-1456 remediate; flip `--strict` sau). Self-test trong CI job (bad fixture caught + nip.io clean). Triggered by recurrence #2: GAP-811 `?tenant=` slip (2026-06-08, →§3.1) + Phase-3 consolidated recipe `localhost:3000` slip (2026-06-16) — recurrence ≥2 → ship per `incident-to-rule-pipeline.md` §3.1.

**DEFERRED — G1-flip-completeness slice:** scan wave plan / campaign PR body cho G1-flip signal + verify `## G1 browser-walk evidence` section + classify flow FE-vs-API-only — requires campaign-state parser + FE-surface classification, NOT trivial bash; FP risk moderate (API-only flows legitimately skip browser-walk). Reviewer-checklist §7.1 + memory §7.3 + campaign cross-ref §7.2 sufficient; revisit khi recurrence ≥2 cho slice này.

---

## 8. Relationship to other rules

- **`g2-handoff-md-mandate.md`** v1.0.0 §1 — sister rule covers G2 handoff recipe AFTER G1 PASS; rule này covers điều kiện browser-real cho chính G1 PASS (trước handoff). Compose: browser-walk → G1 PASS → ship G2 recipe.
- **`pre-walk-persona-simulation-mandate.md`** v1.0.0 §1 — sister covers PRE-walk persona simulation (failure modes trước walk); rule này covers browser-real verify là phần của walk-to-G1-flip. Different moment.
- **`feature-ship-runtime-walk-mandate.md`** v1.1.0 §3.4 — sister covers catalog-then-batch DURING walk; rule này adds "walk PHẢI qua browser thật cho FE flow" trước khi coi walk = G1 PASS.
- **`pre-handoff-self-test-completeness.md`** v1.2.0 §1 + §3 — "API returns 201 ≠ user can do this" + POST-FIX re-walk; rule này = concrete G1-gate instance cùng triết lý (verify FLOW qua FE thật, không chỉ endpoint).
- **`flow-verification-campaign.md`** §1 G1 gate + §2 loop protocol — rule này sharpens G1 PASS criteria cho FE flow (browser-walk mandatory).
- **`e2e-rst-test-layer-boundary.md`** §3 — RST→E2E promotion; mỗi browser-walk finding (GAP-1067/1068/1069 class) là candidate cho E2E spec promotion upstream.
- **`local-fix-production-parity-check.md`** — G3 production-parity; rule này (G1 browser-walk) là upstream gate trước G3.
- **`agent-model-opus-default.md`** v1.0.0 + `agent-background-spawn-default.md` v1.0.1 — nếu browser-walk delegate cho headless Playwright agent, Opus + background apply.
- **`incident-to-rule-pipeline.md`** v1.1 — rule này = direct output 2026-06-08 KC-1 G2 incident (curl-walk PASS che 3 bugs) qua 5-stage pipeline.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + memory + worked self-test §6 + rules-index.csv row + output-review-mandate.md §3 row + campaign §1 cross-ref all paired same PR.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn browser-walk → mọi FE flow G1 flip subsequent auto-comply prospectively).
- **`output-review-mandate.md`** §3 — paired same-PR row "User-facing flow G1 browser-walk readiness".
- **`feedback_g1_browser_walk_before_flip.md`** (memory, paired same-PR per Enforcement Parity).

---

## 9. Log

- **2026-06-16 (v1.3.0):** MINOR — added §3.3 "Canonical KC tenant access recipe — env local (WSL2 + Windows browser)". Triggered by user direction 2026-06-16 "log đúng host này vào meta cho env local" sau khi Phase-3 G2 walk gặp 3 lỗi access liên tiếp: (1) nip.io timeout, (2) `sky-education.kitehub.local` NXDOMAIN, (3) slug `g2-test-center-5` TENANT_NOT_FOUND. Root cause phân tích: nip.io timeout THẬT do post-rebuild stale port-forward `:3000` (GAP-1067) — KHÔNG phải nip.io hỏng (landing-100 2026-06-12 dùng nip.io PASS); slug sai do lấy `instances.slug` thay vì tenant `subdomain`; domain sai `.kitehub.local` thay vì quy ước `.kiteclass.local`. §3.3 codify: slug-source (verify by-subdomain resolve), Host ≥3-phần (middleware constraint), nip.io default + `.kiteclass.local` hosts fallback, + POST-REBUILD restart-FE gotcha (GAP-1067). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user 3 access failures + "log vào meta") → Classify ✓ (§3.1/§3.2 mandate access-mode-parity nhưng KHÔNG codify HOW reproduce trong env WSL2+Windows cụ thể + slug-source caveat + post-rebuild gotcha → mỗi recipe author re-derive, sai lặp) → Rule+Enforce ✓ (§3.3 concrete recipe table + paired recipe fix `2026-06-16-g2-recipe-phase3-consolidated.md` slug→sky-education + restart note) → Self-Test ✓ (recipe author áp §3.3: verify sky-education by-subdomain resolve ✅, 3-phần host ✅, restart fix verified :3000→200) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds reproduction recipe for existing §3.1/§3.2 mandate; no constraint loosening). META P1 force-multiplier per `meta-gap-priority.md` §3 — mọi KC G2 recipe subsequent dùng đúng host pattern, eliminate slug-source + nip.io-timeout-misdiagnose + domain-typo class.
- **2026-06-16 (v1.2.1):** PATCH — §7.5 detector access-mode slice flipped DEFERRED → **SHIPPED** (`scripts/check-walk-recipe-access-mode.sh` + `quality-docs.yml` job `walk-recipe-access-mode`, WARN). Triggered by recurrence #2 cùng "access-mode slip trong walk artifact" class: GAP-811 `?tenant=` (2026-06-08, →§3.1) + Phase-3 consolidated G2 recipe `localhost:3000` cho KC tenant flow (2026-06-16, user-flagged "sao lại localhost:3000? sai kiến trúc? meta chưa đủ mạnh nhỉ?"). Recurrence ≥2 + statically-detectable (per `cross-flow-bug-class-sweep.md` §4.1) → ship detector per `incident-to-rule-pipeline.md` §3.1 (không còn HONEST-defer hợp lệ). Detector FP-guarded (chỉ match `http://` URL thật, không flag prose ban-mention) — self-test PASS (bad fixture caught strict exit 1, nip.io + prose clean). Surface ~34 pre-existing recipe violation → grandfather WARN + GAP-1456 remediate (flip `--strict` sau). Phase-3 recipe của session này đã fix sang nip.io (`g2-test-center-5.127.0.0.1.nip.io:3000`). Per 5-stage: Detect ✓ (user-flagged recurrence) → Classify ✓ (rule §3.1/§3.2 đã có nhưng detector deferred → recipe vẫn slip; class statically-detectable) → Rule+Enforce ✓ (detector script + CI job WARN + self-test + GAP-1456 + this log per `rule-change-process.md` §6.5) → Self-Test ✓ (detector fires on both originating slips; counterfactual: detector active từ §3.1 → recipe `localhost:3000` caught at PR time) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — additive detector for existing constraint, no constraint change; pre-existing recipes grandfathered WARN).
- **2026-06-09 (v1.1.1):** PATCH — added §7.1 reviewer-checklist row "FE port khớp flow per `kitehub-kiteclass-boundary.md` §2 (KH-* `:3001`, KC-* `:3000`)". Cross-ref paired same-PR với new rule `kitehub-kiteclass-boundary.md` v1.0.0 (per its §6.2 Enforcement Parity) sau 2026-06-09 KH-3/KC-8 recipe FE port drift. Additive checklist row, no constraint loosening. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5).
- **2026-06-11 (v1.2.0):** MINOR — added §3.2 "Nguyên tắc tổng quát — production access-mode PHẢI local-reproduce-được" generalizing §3.1 subdomain-specific case to ANY production access/entry mechanism (Host→tenant subdomain / custom-domain / JWT-claim routing / header-derived scope / OAuth redirect URI) + phân tầng functional-parity (local-reproduce-được → G1/G2★ mandatory) vs infra-parity (TLS/LB/wildcard-cert/real-DNS → G3-infra AWS-gated). Triggered by user direction 2026-06-11 "đã có meta quy định phải test được local 100% như production chưa (vd production test bằng subdomain → local cũng phải test được)" + "xem xét gộp G2,G3" sau landing-100 GAP-811 state-check (code middleware shipped nhưng gap PARTIAL/OPEN chờ nip.io subdomain walk). State-check kết luận: §3.1 đã cover subdomain case mạnh; thiếu = phát biểu nguyên tắc TỔNG QUÁT cross-mechanism → §3.2 generalizes. Paired same-PR với `flow-verification-campaign.md` §1 G2★-absorbs-G3-functional gate redefinition (user chốt AskUserQuestion 2026-06-11 "G2★ absorbs G3-functional"). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged meta-coverage question) → Classify ✓ (§3.1 covers subdomain instance only; general "production access-mode → local-reproduce" principle previously implicit; `local-fix-production-parity-check.md` covers config-shape không access-topology; G3 gate covered stack-parity không access-mode explicitly) → Rule+Enforce ✓ (this §3.2 + campaign §1 gate redefinition + §7.1 reviewer-checklist still applies + worked self-test landing-100 GAP-811 `?tenant=` slip retroactive) → Self-Test ✓ (landing-100: G1 dùng `?tenant=` probe = bằng chứng giả per §3.2; nip.io subdomain walk = đúng access-mode → flip GAP-811/1077 chỉ sau nip.io walk) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn access-mode-parity → mọi flow (host-based / OAuth / JWT-routing) G1/G2★ subsequent verify đúng production access path. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — generalizes previously-subdomain-specific principle; no constraint loosening; existing host-flow walks grandfathered re-verify per campaign loop; applies prospectively từ this PR forward 2026-06-11). Atomic-unique-bar §5.1: ✅ atomic (single concept: production access-mode local-reproducible) + ✅ unique (generalizes own §3.1, no sister overlap) + ✅ widely applicable (mọi access-mode-determined flow) + ✅ body §3.2 ≤2 conjunction.
- **2026-06-08 (v1.1.0):** MINOR — added §3.1 "Production-accurate domain simulation cho host/subdomain-based flow" + §4 2 banned rows (`?tenant=`/query-override + localhost-thuần as G1/G2 evidence) + §7.1 reviewer-checklist row. Triggered by 2026-06-08 user-flagged miss: recipe GAP-811 (host→tenant landing) ban đầu tôi chọn `?tenant=sky-education` làm primary test path vì tiện/no-sudo — nhưng `extractSlug()` ưu tiên `?tenant=` TRƯỚC Host (middleware line 99) → BYPASS `extractSlugFromHost()` = bypass chính resolution path cần verify. User push 2 lần ("dev phải tự làm à?" → "discuss lại mô phỏng đúng production") mới ra đúng = nip.io wildcard (`sky-education.127.0.0.1.nip.io:3001` — Host header thật, no sudo, verified resolve 127.0.0.1 + middleware parse parts[0]). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged "session trước miss à") → Classify ✓ (rule v1.0.0 §3 evidence (d) "FE-injected header observed" không phân biệt subdomain-Host-thật vs query-override; gap: HOW to make browser send production-accurate Host locally chưa codify → mỗi recipe author re-derive ad-hoc + dễ rơi convenient-wrong path `?tenant=`) → Rule+Enforce ✓ (this §3.1 + §4 rows + §7.1 row + worked self-test trên GAP-811 `?tenant=` slip + memory `feedback_production_accurate_local_domain_sim.md` paired same-PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (rule fires đúng trên chính incident: `?tenant=` = banned evidence per §3.1, nip.io = production-accurate; counterfactual rule v1.1.0 active từ đầu → recipe chọn nip.io ngay, 0 user push-back) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 22-flow campaign có nhiều host/subdomain flow (tenant-by-domain landing, custom domain); 1 chuẩn nip.io-vs-`?tenant=` → mọi host-based flow G1/G2 subsequent auto-comply, eliminate "test bypass resolution path" class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — tightens browser-walk fidelity cho host-based flows; no constraint loosening; existing host-flow walks grandfathered re-verify per campaign loop; applies prospectively từ this PR forward 2026-06-08). Atomic-unique-bar §5.1: ✅ atomic (single concept: production-accurate Host simulation cho host-based flow) + ✅ unique (extends own §3 evidence với domain-fidelity dimension, no overlap sister rules) + ✅ widely applicable (mọi host/subdomain flow × campaign) + ✅ body §3.1 ≤2 conjunction. Detector deferred per §3.1 conditions (reviewer-checklist + worked self-test + memory sufficient).
- **2026-06-08 (v1.0.0):** Rule created in response to Flow Verification Campaign KC-1 G2 session 2026-06-08: G1/G3 walk dùng `curl` qua gateway `:9000` gắn header tay (`X-Instance-Subdomain: sky-education`) → đều PASS → flip G1; nhưng human browser test (G2) lộ 3 lỗi mà curl-walk che mất — GAP-1067 (stale docker-proxy `:3000` ERR_EMPTY_RESPONSE) / GAP-1068 (tenant resolution browser gửi `X-Tenant-Id` gateway strip → 400) / GAP-1069 (FE↔BE contract drift dashboard `GET /api/v1/classes` + `/api/v1/invoices` 404). User chốt (AskUserQuestion 2026-06-08) "Thêm meta-rule browser-walk trước khi flip G1". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged + 3 G2 bugs) → Classify ✓ (no existing rule mandates browser-real walk là điều kiện cần của G1 PASS cho FE flow; `feature-ship-runtime-walk-mandate.md` §3.4 covers catalog DURING walk không phân biệt curl-vs-browser; `pre-walk-persona-simulation-mandate.md` covers PRE-walk simulation; `g2-handoff-md-mandate.md` covers AFTER G1 PASS; `pre-handoff-self-test-completeness.md` §1 covers triết lý "endpoint ≠ flow" nhưng không bind vào G1-gate-cho-FE-flow boundary) → Rule+Enforce ✓ (this file + reviewer-checklist §7.1 + memory `feedback_g1_browser_walk_before_flip.md` paired same-PR + campaign §1 cross-ref + rules-index.csv row + output-review-mandate.md §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên KC-1 G2 originating incident — rule fires correctly, 3/3 bugs surface ở G1 với browser-walk + counterfactual ~1 G2 round-trip saved per FE flow) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn browser-walk-before-G1-flip → Flow Verification Campaign §4 các FE flow subsequent auto-comply prospectively → eliminate "curl PASS nhưng G2 lòi" class permanently. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-implicit "G1 walk cho FE flow phải qua FE :3000 thật"; no constraint loosening; existing flows flipped trước rule grandfathered (re-verify khi G2 lòi bug per campaign loop); rule applies prospectively từ next FE flow G1 flip forward 2026-06-08). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: browser-real walk là điều kiện cần của G1 PASS cho FE flow) + ✅ unique (sister rules cover PRE-walk sim / DURING-walk catalog / POST-fix re-walk / G2-handoff — khác boundary) + ✅ widely applicable (mọi FE flow G1 flip × 22-flow campaign) + ✅ body discipline §1 ≤2 conjunction "và". Path-scoped per `context-budget-mandate.md` §3.2 (`wave-*-flow-*.md` + `flow-verification-campaign.md` + `*-g2-recipe-*.md`) — không tăng always-load band (README rule-count ceiling: 13 always-load OK <18; rule này thêm 1 path-scoped → 78/100 OK). Detector (§7.5) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + memory auto-load + campaign cross-ref + worked self-test §6 sufficient cho v1.0.0.
