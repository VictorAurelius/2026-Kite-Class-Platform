# Third-Party Platform Automation Discovery

**Priority:** 🟠 MANDATORY — discovery rule applied at first encounter
**Version:** 1.0.0
**Created:** 2026-05-10
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every Claude turn that introduces a NEW third-party platform / SaaS / cloud service / framework with potential for CLI/SDK/MCP automation. Examples: Vercel, Cloudflare, GitHub, Google Workspace (Calendar/Drive/Gmail), Stripe, Slack, Linear, AWS, Twilio, Sentry, etc.

---

## 1. The Rule

> **At first encounter with any third-party platform/service, Claude MUST evaluate whether automated setup (CLI, SDK, MCP server, official skill bundle) exists and is worth installing — BEFORE defaulting to manual UI walkthrough.** Document the decision (automate vs manual) inline with rationale.

This rule sharpens `agent-action-bias.md` §1 Part B ("command over UI") for cross-platform integrations: not just "prefer command this turn" but "evaluate one-time tooling investment that pays off across many future turns".

---

## 2. Discovery checklist

When user mentions a new platform/service or session needs first integration with one, run these checks BEFORE any manual UI guidance:

### 2.1 Tier 1 — Official tooling exists?

- [ ] Official CLI? (vd `vercel`, `wrangler`, `gh`, `gcloud`, `aws`, `stripe`, `gh extension`)
- [ ] Official SDK + good docs? (Node/Python/Go bindings)
- [ ] Official MCP server? (https://github.com/modelcontextprotocol/servers + official partner list)
- [ ] Official Claude Code skill bundle? (vd Vercel ships 27 skills auto on `npm i -g vercel`)

### 2.2 Tier 2 — Community tooling acceptable?

- [ ] Maintained community CLI (last release < 12 months)?
- [ ] Maintained community MCP server (active GitHub issues, recent releases)?
- [ ] Wrapper around platform's REST API mature enough?

### 2.3 Tier 3 — Manual API only

- [ ] If no CLI/SDK exists → consider direct REST API via `curl` for repeated tasks
- [ ] OR write thin wrapper script trong `scripts/` cho project-specific tasks

---

## 3. Decision matrix

After discovery, decide automate vs manual based on:

| Frequency | Setup effort | Recommendation |
|---|---|---|
| **Once-off, never again** (vd 1-time domain DNS A record click) | < 5 min | ❌ **Manual UI** — opportunity cost too high to install CLI |
| **2-3 times this session, never after** | < 5 min | ⚠️ Borderline — judgement call; default manual unless CLI extra-fast |
| **5+ times this session OR ≥1/week ongoing** | < 30 min | ✅ **Setup** — pays off within session |
| **Long-term automation use** (cron jobs, CI integration) | < 60 min | ✅ **Setup** — even if heavy, durable investment |
| **Trivial one-line check** (vd verify domain exists) | any | ❌ Manual `curl`/`dig` — don't install full SDK |

### Setup time budget guideline

- **0-5 min:** Free decision — install if any future use case anticipated
- **5-15 min:** Decide based on §3 matrix; document rationale
- **15-45 min:** Document decision explicitly in chat + gap file; require ≥3 anticipated use cases OR strategic value (vd skill bundle, ecosystem hooks)
- **>45 min:** File a gap for the setup itself; defer to dedicated PR; require explicit user approval

---

## 4. Documentation requirement

For every accept/reject decision, MUST document inline (in chat OR commit msg OR runbook):

```markdown
## Tooling decision — <PLATFORM>

**Discovery:** <CLI name + version OR "no official CLI; community X exists">
**Setup effort:** <minutes>
**Anticipated use cases:** <bullet list, ≥1 must be future-session>
**Decision:** ✅ Setup / ❌ Manual / ⚠️ Defer to gap
**Rationale:** <1-2 sentences>
```

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Skip directly to "click here in UI" without checking CLI/SDK existence | Run §2 checklist first; document why manual chosen if applicable |
| Install full CLI for 1-time read-only check | Use direct API `curl` or web-fetch — match tool to task |
| Setup CLI silently without telling user effort | Surface §3 matrix decision + estimated setup time |
| Default to manual UI because "user is on Termux/mobile" | Mobile is fine — OAuth flows work; UI clicks SAME effort either way; CLI saves time on iteration |
| Refuse to setup CLI because "scope creep" of current task | Document as parallel session investment; still ship if matrix says ✅ |
| Setup CLI then forget to document for future re-use | Always create or update runbook in `documents/05-guides/dev/` after setup |

---

## 6. Worked self-test (this session 2026-05-09 / 2026-05-10)

Apply rule retroactively to 3 platform encounters this session:

### 6.1 Google Calendar — encountered when user asked "calendar reminder Auto-renew" 2026-05-09

| §2 Check | Result |
|---|---|
| Official CLI | None |
| Official SDK | google-api-python-client / Google Calendar API REST |
| Official MCP server | None official |
| Community MCP server | `@cocal/google-calendar-mcp` v2.6.1 (active, last 2026-03) ✅ |
| Setup effort | ~30-45 phút first-time (Google Cloud OAuth + MCP install + config) |
| Anticipated use cases | Schedule reminders, log Phase 1 BETA milestones, weekly retro events, query upcoming meetings (≥4 future cases) |
| **Decision** | ✅ **Setup** — matrix row 4 (long-term automation) |
| **Rationale** | 3+ anticipated calendar automations in next 6 months Phase 1 BETA / Phase 1.5 PAID; MCP install one-time + amortized across session-spanning reminders |
| **Verified outcome** | 2 events created within minutes of setup (auto-renew your-product-a.me + AWS Activate D+14 + Email Routing D+1) — automation paid off within same session |

→ Rule fires correctly. Decision matched matrix. Worked example ✅

### 6.2 Vercel — encountered when user asked "set Vercel env var for me" 2026-05-09

| §2 Check | Result |
|---|---|
| Official CLI | `vercel` v53.3.1 ✅ |
| Official SDK | `@vercel/sdk` |
| Official MCP server | None standalone |
| Official Claude Code skill bundle | ✅ **27 skills auto-load** on `npm i -g vercel` (vercel:env, vercel:status, vercel:deploy, etc.) |
| Setup effort | ~5 min (npm global install + OAuth login + project link) |
| Anticipated use cases | env var management, Phase 1 BETA deploy, redeploy, log/metrics query, custom domain bind, future Workers — 6+ ongoing |
| **Decision** | ✅ **Setup** — matrix row 3 (5+ uses ongoing) + Tier 1 official skill bundle bonus |
| **Rationale** | 1-time UI env var add was 30s vs CLI 3min initial — UI faster THIS task. BUT 27 free skills + future deploy/redeploy/env management amortize across all Phase 1 BETA + Phase 1.5 launch work |
| **Verified outcome** | Linked + verified env var via CLI; skills now available for future Vercel ops |

→ Rule fires correctly even when current task is faster manual — long-term value tips matrix to ✅. Worked example ✅

### 6.3 Cloudflare — encountered when user asked "cloudflare có setup được tương tự không" 2026-05-10

| §2 Check | Result |
|---|---|
| Official CLI | `wrangler` v4.90.0 ✅ |
| Official SDK | `cloudflare` (Node), `cloudflare-python`, REST API |
| Official MCP server | Cloudflare Worker hosting + Workers AI MCPs exist (community + official builds) |
| Setup effort | ~5 min (similar to Vercel) |
| Anticipated use cases | DNS records management (post-launch tweaks), Email Routing rules, future Workers (Edge Functions for `*.your-product-a.me` tenant routing), zone settings — 4+ |
| **Decision** | ✅ **Setup** — matrix row 3 |
| **Rationale** | Wrangler scope is broader than <your-product-a> current need (Workers/Pages/D1/AI heavy) but DNS + Email Routing alone justify; ecosystem hooks for future Workers when scaling Phase 2 |
| **Verified outcome** | (in progress at rule-write time) — install done, OAuth pending user; expect verification post-OAuth |

→ Rule fires correctly. Decision matched matrix. Worked example ✅

### 6.4 Counter-example — Stripe / Plaid / Twilio (NOT setup this session)

When <your-product-a> roadmap mentions payment processor (GAP-228 Phase 1.5 BLOCKING), question arose mentally "should we install Stripe CLI now?".

| §2 Check | Result |
|---|---|
| Stripe CLI | Exists ✅ |
| Setup effort | ~10 min (Stripe API keys + CLI auth) |
| Anticipated use cases | NONE in current session — payment processor work is Phase 1.5 (~Tuần 13-18) |
| **Decision** | ❌ **Defer** — matrix row 1/5 (no use case until Phase 1.5) |
| **Rationale** | Premature setup; Stripe API keys live + OAuth state would be stale by Phase 1.5; better setup just-in-time when payment work begins |

→ Rule fires correctly — also rejects setup when no near-term use case. Worked example ✅

---

## 7. Enforcement

### 7.1 Reviewer-checklist (manual)

When reviewing PR that introduces NEW platform integration or first-time use of an external service, reviewer asks:
- [ ] Did Claude run §2 discovery checklist?
- [ ] Setup decision documented per §4?
- [ ] If Setup chosen, is runbook updated trong `documents/05-guides/dev/` cho future re-setup?
- [ ] If Manual chosen, is rationale (per §3 matrix row) documented inline?

### 7.2 Memory auto-load (per-session)

Memory entry `feedback_third_party_platform_automation_discovery.md` (paired same-PR per `rule-change-process.md` §6.5) auto-loads each session. 4-bullet checklist before any platform first-encounter:
1. Run §2 discovery checklist (Tier 1/2/3)
2. Apply §3 decision matrix
3. Document decision per §4 template
4. If ✅ Setup, update runbook in `documents/05-guides/dev/`

### 7.3 Self-test (in-turn)

Before defaulting to "Click X in UI..." instructions, mentally run §2 checklist + §3 matrix. If decision ❌ Manual is correct → proceed with confidence. If matrix says ✅ Setup → propose CLI route to user with effort estimate.

### 7.4 Override mechanism

For genuine cases where rule applied but exception warranted:

```
THIRD_PARTY_AUTOMATION_OVERRIDE: <platform> — <reason — vd "user explicitly prefers UI for this 1-time task">
```

Or in chat: state explicitly "Per §3 row N (one-off task), defaulting to manual UI; CLI install không justified."

Pattern frequency >5% per quarter triggers meta-review of §3 matrix thresholds.

---

## 8. Relationship to other rules

- **`mcp-first-with-fallback.md`** — covers MCP-vs-CLI tool selection AT USE TIME; THIS rule covers CLI/MCP-vs-MANUAL evaluation AT FIRST ENCOUNTER. Both apply: discover automation option (this rule) → prefer MCP if both MCP + CLI exist (mcp-first rule)
- **`agent-action-bias.md`** §1 Part B — "command over UI" for THIS turn; THIS rule extends to "evaluate cross-session tooling investment"
- **`incident-to-rule-pipeline.md`** — this rule is direct output of session 2026-05-09/2026-05-10 user-flagged miss "every time approaching new tech, consider setup option"; per 5-stage pipeline applied
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory + 3 worked self-tests + reviewer-checklist all ship same PR
- **`output-review-mandate.md`** §3 — every output type has review standard; this rule adds "first-encounter platform integration decision" as reviewable output

---

## 9. Open Items / Follow-ups

- [ ] Future enhancement: `audit-gate.py` AUDIT_RULES detector — scan PR commit messages for "Click X in Vercel UI / Cloudflare UI / etc." patterns without paired runbook entry. WARN if no `documents/05-guides/dev/<platform>-setup.md` referenced. Defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard.
- [ ] Quarterly retro: review last 90 days of new-platform encounters; verify §3 matrix decisions match outcomes (was setup actually amortized? was manual chosen wisely?)

---

## 10. Log

- **2026-05-10 (v1.0.0):** Rule created in response to user explicit request "thêm rules, mỗi khi tiếp cận 1 công nghệ mới, hoặc 1 nền tảng thứ 3, cần xem xét có setup được không". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged after observing 3 platform setups this session — Google Calendar / Vercel / Cloudflare) → Classify ✓ (no existing rule covers first-encounter platform automation discovery; `mcp-first-with-fallback.md` is narrower; `agent-action-bias.md` is per-turn not cross-session) → Rule+Enforce ✓ (this rule + memory `feedback_third_party_platform_automation_discovery.md` paired same-PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§6 worked examples on Google Calendar MCP / Vercel CLI / Wrangler CLI + Stripe counter-example) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new rule with built-in enforcement, no constraint loosening; existing platform integrations grandfathered, rule applies prospectively). Detector wiring (§9) deferred to 2nd recurrence per premature-rule guard.
