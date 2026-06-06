---
title: Wave flow-kh7 — KH-7 Custom domain management G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kh7]
wave: wave-2026-06-06-flow-kh7
tag_primary: flow-kh7
tags_secondary: [domain, dns, idor, validation]
date: 2026-06-06
flow: KH-7 (Custom domain / domain management)
gaps: [GAP-1023, GAP-1024]
---

# Wave flow-kh7 — KH-7 Custom domain management G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KH-7 — Owner gắn custom domain: add → DNS TXT verify → status → delete. Flow secondary thứ 3.

## 1. Brainstorm

KH-7 = `/api/instances/{id}/domain` (kitehub-subscription). Pre-walk Opus persona simulation (12 FM) flag: (i) gateway route TỒN TẠI (không blocker), (ii) DNS VERIFIED unreachable local → walk ceiling = add→PENDING_VERIFY. Headline: FM-1 IDOR — DomainController ZERO @PreAuthorize.

## 2. Task Breakdown

1. Static pre-walk: @PreAuthorize (zero?), regex, tier gate, schema.
2. Walk: GET status → POST domain → verify → IDOR → sad paths → DELETE.
3. Batch-fix small/safe/high-value bug → rebuild → re-walk.
4. File gap còn lại → flip campaign.

## 3. Scope

Walk-only G1 cho add/verify/status/delete qua gateway. Trong scope: fix inline 3 (FM-1 @PreAuthorize add, FM-2 regex broaden, FM-5 reserved denylist). Ngoài scope (→ gap): cross-tenant ownership bind (gateway change), verification state machine (cert/timeout). Walk solo; 1 Opus pre-walk agent. Instance tier tạm set PREMIUM cho walk (restore FREE sau).

## 4. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `DomainController` 4 endpoints | ✅ present | `/api/instances/{id}/domain` (POST/POST verify/DELETE/GET) |
| `DomainController` @PreAuthorize | 🆕 **ZERO** (IDOR confirmed) | grep returned none → fixed this wave |
| `DomainService.initiateCustomDomain` tier gate | ✅ present | `instance.canUseCustomDomain()` PREMIUM/ENTERPRISE |
| Domain storage `instances.custom_domain/domain_verify_token/domain_status` | ✅ present | information_schema — **no drift** (per FM-12) |
| `DnsTxtLookupService` real JNDI lookup | ✅ present | DNS VERIFIED unreachable local (expected ceiling) |

## 5. Verification Gates

### Pre-walk
12 failure mode, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh7-domain-management.md`. Gateway route exists; DNS VERIFIED unreachable local.

### G1 walk — evidence

Credential `owner.test@test.vn / Test@1234` (instance `22003e3c…`, tạm tier PREMIUM cho walk), gateway :9000.

**Happy paths (PASS):**

| Step | Kết quả |
|---|---|
| `GET /domain` (baseline) | 200, status NONE + backupUrl |
| `POST /domain` `{customDomain:school.com}` | 200, PENDING_VERIFY + verifyToken `kitehub-verify={uuid}` + TXT record |
| `POST /domain/verify` | 200, stays PENDING_VERIFY (DNS unreachable — expected ceiling, not 500) |
| `DELETE /domain` | 204 |

**Sad paths (PASS):** non-existent instance → 404; (post-fix) 3-label domain → 200; reserved kitehub.me → 400.

**Bugs surfaced (5) — 3 fix inline, 2 file gap:**

| FM | Severity | Verdict |
|---|---|---|
| FM-1 IDOR: DomainController ZERO @PreAuthorize → bất kỳ user thao tác mọi instance domain (GET 200 + DELETE 204 cross-tenant) | **P0** | **PARTIAL inline** (@PreAuthorize added; ownership bind → GAP-1023) |
| FM-2 regex rejects 3-label (`school.example.com` + `*.edu.vn` VN schools) → 400 | P1 | **FIXED inline** |
| FM-5 no reserved-domain denylist (`kitehub.me` 200) | P1 | **FIXED inline** |
| FM-3/4 verification state machine incomplete (no cert/timeout/FAILED) | P1 | GAP-1024 |
| FM-1 cross-tenant ownership bind (owner A → owner B) | P0 | GAP-1023 (systemic) |

### Inline fixes (this wave)

1. **FM-1 partial** — `DomainController` +`@PreAuthorize(OWNER_AUTHZ)` cả 4 endpoint (defense-in-depth; chặn non-owner role). Cross-tenant bind → GAP-1023.
2. **FM-2** — `DomainSetupRequest` regex `^[a-zA-Z0-9]...\.[a-zA-Z]{2,}$` (2-label only) → `^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$` (multi-label, hỗ trợ `*.edu.vn`).
3. **FM-5** — `DomainService` reserved-domain denylist (kitehub.me/kiteclass.com + subdomains) → 400.

`./mvnw test` (DomainServiceTest + InstanceApiContractTest): 23/23 PASS (incl new reserved-domain test). Rebuild + re-walk: school.example.com 200 ✓, truong.edu.vn 200 ✓, kitehub.me 400 ✓, app.kiteclass.com 400 ✓.

## 6. Agent Spawn Pattern

N/A — walk solo. 1 Opus background agent cho pre-walk persona simulation.

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1023: Domain cross-tenant IDOR (P0 PARTIAL, Backend — @PreAuthorize done, ownership bind open)
- GAP-1024: Domain verification state machine incomplete (P1, Backend)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

FM-1 IDOR là recurrence #3 của "platform route thiếu tenant ownership bind" class — GAP-1015 (subscription) + GAP-1019 (branding) + GAP-1023 (domain). Tất cả cùng root: gateway không forward/enforce JWT tenantId cho `/api/platform/**` + `/api/instances/**`. **Highest-value cross-cutting fix** sau khi G1 sweep xong.

### Sync targets

Campaign §4 KH-7 → `🔄 walk-pass-pending-human`; wave-history flow-kh7; gap-status.csv 2 rows; audits-index pre-walk row. Instance 22003e3c restore FREE + domain cleared sau walk.

### Outcome

KH-7 **G1 ✅ PASS** — flow add/verify/status/delete walk được end-to-end (ceiling PENDING_VERIFY do local DNS, expected). 3 fix inline (FM-1 partial @PreAuthorize + FM-2 regex VN-domain + FM-5 reserved denylist). **Lưu ý review/G2:** GAP-1023 P0 IDOR là recurrence #3 cùng systemic gateway-tenant-bind (GAP-1015/1019) — đề xuất 1 fix wave chung cho cả 3 sau khi G1 sweep complete.

## 8. Log

- **2026-06-06:** Wave flow-kh7 — KH-7 G1 walk complete. 3 inline fixes (FM-1 @PreAuthorize 4 endpoints + FM-2 regex multi-label + FM-5 reserved denylist) + 2 gaps (GAP-1023 P0 PARTIAL IDOR + GAP-1024 P1 state machine). Campaign row → walk-pass-pending-human.
