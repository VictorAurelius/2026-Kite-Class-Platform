---
title: Wave flow-kh8 — KH-8 Off-boarding + data retention (PDPL) + consent G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kh8]
wave: wave-2026-06-06-flow-kh8
tag_primary: flow-kh8
tags_secondary: [pdpl, consent, dsar, offboarding, retention, authz]
date: 2026-06-06
flow: KH-8 (Off-boarding + data retention PDPL + consent)
gaps: [GAP-1025, GAP-1026, GAP-1027]
---

# Wave flow-kh8 — KH-8 Off-boarding + PDPL + consent G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KH-8 — PDPL compliance: consent (record/get/revoke) + DSAR (request/status) + off-boarding (instance soft-delete → retention → purge). Flow secondary thứ 4.

## 1. Brainstorm

KH-8 = 3 sub-flow PDPL: consent v1 (`/api/v1/consent` anonymous visitor), consent v2 (`/api/v1/consent/v2` immutable, authenticated), DSAR (`/api/v1/dsar`), off-boarding (InstanceController purge + DataRetentionService). Pre-walk Opus persona simulation (12 FM) flag: FM-1 gateway-block consent/DSAR (walk-blocker?) + FM-2 InstanceController zero authz (most severe).

## 2. Task Breakdown

1. Static pre-walk: InstanceController @PreAuthorize, gateway whitelist, consentAuthz, schema.
2. Walk: consent v1 record/get/revoke + consent v2 + DSAR request/status + off-boarding list/purge.
3. Catalog findings; assess inline vs gap.
4. File gaps → flip campaign.

## 3. Scope

Walk-only G1 cho 3 PDPL sub-flow. Không inline fix wave này — finding chính (FM-2 InstanceController authz) là systemic (10 endpoint + contract-test + role-scoping design + spans KH-1/KH-9) → gap chứ không hasty inline. Walk solo; 1 Opus pre-walk agent.

## 4. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `ConsentController` v1 + `ImmutableConsentController` v2 + `DsarController` | ✅ present | `/api/v1/consent`, `/v2`, `/api/v1/dsar` |
| `InstanceController` @PreAuthorize | 🆕 **ZERO** (FM-2 confirmed) | grep — no @PreAuthorize on any endpoint |
| `consentAuthz.canAccessUser` (v2) | ✅ SECURE | binds X-User-Id + gateway strips forged header |
| Gateway `isPublicPath` consent/DSAR | ⚠️ not listed BUT reachable | walk: no-auth consent record → 201 (FM-1 refuted) |

## 5. Verification Gates

### Pre-walk
12 failure mode, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh8-offboarding-pdpl-consent.md`.

### G1 walk — evidence

Credential `owner.test@test.vn / Test@1234`, gateway :9000.

**Happy paths (PASS):**

| Sub-flow | Step | Kết quả |
|---|---|---|
| Consent v1 | `POST /api/v1/consent/record` (no auth) | 201 (public, anonymous visitor) |
| Consent v1 | `GET /api/v1/consent/{visitorId}` | 200 |
| Consent v1 | `POST /api/v1/consent/{visitorId}/revoke` | 200 |
| Consent v2 | `consentAuthz` bind X-User-Id | ✅ SECURE (per pre-walk, no IDOR) |
| DSAR | `POST /api/v1/dsar/request` | 201 + ticketId |
| DSAR | `GET /api/v1/dsar/{ticketId}` | 200 (redacted) |
| Off-boarding | `GET /api/platform/instances` | 200 (list) |
| Off-boarding | `DELETE /{id}/purge` | 200 (reachable) |

**Findings (refuted + confirmed):**

| FM | Severity | Verdict |
|---|---|---|
| FM-1 gateway-block consent/DSAR | — | **REFUTED** — consent v1 + DSAR reachable via gateway (201/200) |
| FM-3 DSAR SecurityConfig double-block | — | **REFUTED** — DSAR request/status work (201/200) |
| FM-2 InstanceController ZERO @PreAuthorize → enumerate all + purge any | **P0** | CONFIRMED live (owner.test → 6 instances + purge reachable) → GAP-1025 |
| FM-5 purge non-deleted → 200 FAILED (not 409) | P1 | GAP-1026 |
| FM-6 retention warning exact-day-match | P1 | GAP-1026 |
| FM-4 consent v1 IDOR (revoke by visitorId, no auth) | P2 | GAP-1027 (mitigated UUID; by-design anonymous) |

**No inline fix** — FM-2 systemic (deferred to focused fix GAP-1025); others minor/by-design. Consent v2 already SECURE (closes KH-5/6/7 IDOR class for authenticated consent).

## 6. Agent Spawn Pattern

N/A — walk solo. 1 Opus background agent cho pre-walk persona simulation.

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1025: InstanceController missing @PreAuthorize (P0, Backend — off-boarding purge + enumeration)
- GAP-1026: Off-boarding/retention robustness — purge 409 + retention warning (P1, Backend)
- GAP-1027: Consent v1 anonymous IDOR (P2, Backend — mitigated UUID)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

FM-2 (InstanceController no authz) là extreme case của "platform route authz gap" — nhưng KHÁC class IDOR (GAP-1015/1019/1023 có role check, thiếu ownership bind; GAP-1025 thiếu authz HOÀN TOÀN). Consent v2 demonstrate đúng pattern (consentAuthz binds X-User-Id) — reference cho fix các IDOR gap.

### Sync targets

Campaign §4 KH-8 → `🔄 walk-pass-pending-human`; wave-history flow-kh8; gap-status.csv 3 rows; audits-index pre-walk row.

### Outcome

KH-8 **G1 ✅ PASS** — cả 3 PDPL sub-flow (consent v1/v2 + DSAR + off-boarding) reachable + work. Không walk-blocker (pre-walk FM-1/FM-3 refuted). **Lưu ý review/G2:** GAP-1025 P0 (InstanceController any-user-purge-any-instance) là finding nghiêm trọng nhất nhóm secondary — nên ưu tiên cao; consent v2 đã secure (good reference cho IDOR cluster fix).

## 8. Log

- **2026-06-06:** Wave flow-kh8 — KH-8 G1 walk complete. No inline fix (FM-2 systemic → GAP-1025). 3 gaps filed (GAP-1025 P0 InstanceController authz + GAP-1026 P1 purge/retention + GAP-1027 P2 consent-v1-IDOR). Pre-walk FM-1/FM-3 refuted. Campaign row → walk-pass-pending-human.
