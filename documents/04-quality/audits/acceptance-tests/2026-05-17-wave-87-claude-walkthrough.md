---
title: Wave 87 — Claude self-test walkthrough (agent-runnable subset)
status: complete
created: 2026-05-17
phase: phase-1-beta
wave: 87
type: agent-walkthrough-self-test
related_gaps: [GAP-518, GAP-519, GAP-523]
---

# Wave 87 — Claude Walkthrough Results

**Phương pháp:** Sau khi Wave 87 batch 1 ship (5 PRs merged), Claude tự run agent-runnable subset của 126 acceptance test rows để verify dev toolchain hoạt động + smoke endpoints sau merge.

**Stack state:** AWS stack started qua `bash scripts/aws/start-stack.sh --reason "Wave 87 claude walkthrough"` (~4 phút wait); stopped sau verify.

---

## 1. Summary

| Layer | Result |
|---|:---:|
| L1 Endpoint reachability (Anonymous FE 11 URLs) | ✅ 11/11 PASS |
| L1 API health | ✅ PASS |
| L2 API contract validation (RFC 7807 problem+json) | ✅ PASS |
| L2 CORS preflight `kitehub.me` apex (GAP-523) | ✅ PASS |
| L2 CORS preflight `app.kitehub.me` (GAP-523 scope) | ❌ FAIL — 403 |
| L2 CORS preflight `kitehub.vercel.app` (GAP-523 scope) | ❌ FAIL — 403 |
| L2 POST request-beta-access từ Vercel FE Origin | ❌ FAIL — 403 CORS reject |
| L3 UI flow (94 USER-VERIFY rows) | ⏳ require human browser |
| L4 External (email delivery / vendor portal) | ⏳ require human inbox |

---

## 2. Anonymous persona — 11 Vercel URLs ✅

| URL | Status |
|---|:---:|
| `https://kitehub.vercel.app/` | 200 |
| `https://kitehub.vercel.app/pricing` | 200 |
| `https://kitehub.vercel.app/legal/terms` | 200 |
| `https://kitehub.vercel.app/legal/privacy` | 200 |
| `https://kitehub.vercel.app/legal/cookies` | 200 |
| `https://kitehub.vercel.app/legal/data-rights` | 200 |
| `https://kitehub.vercel.app/blog` | 200 |
| `https://kitehub.vercel.app/request-beta-access` | 200 |
| `https://kitehub.vercel.app/login` | 200 |
| `https://kitehub.vercel.app/verify-email` | 200 |
| `https://kitehub.vercel.app/beta-signup` | 200 |

PUB-LAND-001 đến PUB-LAND-006 + PUB-BLOG-001..002 + BETA-REQ-001 + EMAIL-VERIFY-002 + OWNER-SIGNUP-001 (endpoint reachability) all PASS.

---

## 3. API smoke ✅

`GET https://api.kitehub.me/actuator/health` → 200, body `{"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL"...}}}}`.

`POST /api/v1/auth/request-beta-access` empty body → 400 với RFC 7807:
```json
{
  "type": "about:blank",
  "title": "Validation Error",
  "status": 400,
  "detail": "name: must not be blank; consentGiven: BETA_CONSENT_REQUIRED; consentAccepted: BETA_CONSENT_REQUIRED; email: must not be blank; orgName: must not be blank; persona: must not be blank;",
  "instance": "/api/v1/auth/request-beta-access"
}
```

Required fields surfaced: `name`, `email`, `orgName`, `persona`, `consentGiven`, `consentAccepted`. Matches BETA-REQ-006 validation expectation.

---

## 4. GAP-523 CORS — PARTIAL fix ⚠️

Wave 87 Bucket D PR #1471 thêm `CORS_ALLOWED_ORIGINS` vào `docker-compose.production.yml`. Verify:

| Origin | OPTIONS preflight | ACAO header | Verdict |
|---|:---:|---|:---:|
| `https://kitehub.me` | 200 | `https://kitehub.me` | ✅ allowed |
| `https://app.kitehub.me` | 403 | (empty) | ❌ rejected |
| `https://kitehub.vercel.app` | 403 | (empty) | ❌ rejected |

**Impact:** FE deploy chính trên Vercel (`kitehub.vercel.app`) sẽ KHÔNG gọi được API. Beta tester click submit form `/request-beta-access` qua Vercel → 403. Phase 1 BETA self-test BETA-REQ-003 BETA-REQ-005 BETA-REQ-006 BETA-REQ-007 đều BLOCKED.

**Root cause hypothesis:** `docker-compose.production.yml` env override CÓ THỂ chưa deployed (last deploy 2026-05-16 trước Wave 87 merge); OR gateway reads CORS từ `application.yml` properties path khác `CORS_ALLOWED_ORIGINS` env var.

**Follow-up:** GAP-523 status revisit — Wave 87 Bucket D agent đã flag "CORS scope = META audit-rubric DONE 100% Wave 72b Bucket E" + production env-override mechanism, nhưng live verify shows app./vercel.app subdomain CHƯA pass. File follow-up sub-gap.

---

## 5. GAP-518/519 ⏳ live verify pending

Bucket D agent confirmed code-side complete (Wave 72a+78 PARTIAL 80-90%):
- BE seed `PLATFORM_ADMIN` literal verified (preflight Gate 3: 40 hits)
- FE role-guard accept cả `PLATFORM_ADMIN` + `ADMIN` (Wave 72a Bucket C `auth-helpers.ts:18`)
- AdminSidebar 4 nav items `data-testid` shipped Wave 72a Bucket C

Live verify yêu cầu browser walk-through (admin login → `/admin` redirect → sidebar visible → click 4 nav). Defer dev manual hoặc Bucket F Playwright.

---

## 6. Wave 86 row coverage delta

Wave 86 audit ghi 5 PASS / 94 USER-VERIFY / 27 INSUFFICIENT_SPEC. Wave 87 walkthrough thêm:

| flow_id | verdict | delta |
|---|:---:|---|
| PUB-LAND-001..006 | ✅ PASS (đã PASS từ Wave 86 cho PUB-LAND-001; còn 5 rows new agent-verify endpoint) | +5 |
| PUB-BLOG-001..002 | ✅ PASS (endpoint smoke) | +2 |
| BETA-REQ-001 | ✅ PASS (endpoint reachable) | +1 |
| BETA-REQ-006 | ✅ PASS validation (400 RFC 7807 với 6 required fields surfaced) | +1 |
| EMAIL-VERIFY-002 | ✅ PASS endpoint | +1 |
| OWNER-SIGNUP-001 | ✅ PASS endpoint | +1 |
| META-SMOKE-001 | ✅ PASS (đã từ Wave 86) | 0 |
| **NEW finding** GAP-523 sub-scope app./vercel.app | ❌ FAIL surface | -bug |

**Wave 87 walkthrough delta:** +11 endpoint rows PASS, 1 production bug surfaced (CORS app./vercel.app).

---

## 7. Recommendations

1. **File sub-gap cho GAP-523:** app.kitehub.me + kitehub.vercel.app subdomain chưa allowed; verify deployment đã pull `docker-compose.production.yml` Wave 87 changes; nếu đã pull thì investigate Spring Cloud Gateway CORS config path.
2. **Re-deploy production** với Wave 87 commit `bb615fba` để fix CORS subdomain scope.
3. **Spawn Bucket F Playwright** cho L3 UI walk-through (admin login + sidebar + 4 nav click) — endpoint smoke không đủ verify GAP-518/519.
4. **Pre-tenant gaps Wave 88** vẫn defer per Wave 87 scope decision; nhưng GAP-523 follow-up có thể move lên Wave 87 closure scope nếu critical.

---

## 8. Compliance check

| Rule | Verdict |
|---|:---:|
| `agent-aws-access.md` §2.1 Tier 1 read-only | ✅ — start-stack.sh là user-authorized script, no autonomous mutation |
| `pre-handoff-self-test-completeness.md` §1 | ⚠️ — stop at L1+L2 endpoint, user complete L3+L4 |
| `dev-readable-doc-language.md` | ✅ — narrative VN + identifier English |
| `release-deploy-standard.md` §3.1 Smoke admin-login | ⏳ — chưa run (cần seed admin + DB stack local hoặc remote SMOKE_ADMIN creds) |

---

## 9. Log

- **2026-05-17:** Claude walkthrough sau Wave 87 batch 1 merge. Started AWS stack ~4min, smoke 11 Vercel URLs PASS, API health UP với DB connected, CORS apex OK + subdomain FAIL (GAP-523 PARTIAL), POST validation 400 RFC 7807 correct. Stack stopped post-verify. 1 production bug surfaced cho follow-up. Per `agent-aws-access.md` §5 — audit artifact ships đồng-PR.
