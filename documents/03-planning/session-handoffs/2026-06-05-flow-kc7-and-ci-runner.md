---
title: Session Handoff — KC-7 invoice→payment G1 + CI self-hosted runner restore
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign KC-7 + CI runner infra
---

# Session Handoff 2026-06-05 — KC-7 G1 + CI runner restore

## Shipped (both merged to main)

| PR | Merged | Scope |
|---|---|---|
| **#2180** `ebf5fa16` | KC-5 attendance + KC-6 grade G1 PASS (from prior session, "merge when green") |
| **#2181** `a02e6109` | KC-7 G1 — P0 gateway role→authority bridge (GAP-1003) + 2 follow-up gaps |

## KC-7 invoice → payment → reconcile — G1 PASS

Pre-walk persona-sim (Opus, 9 FMs) → production-equivalent walk (kiteclass_shared DB, Flyway V88). **Schema-drift hypothesis NEGATIVE** (V79/V86/V88 already resolved — different from KC-5/KC-6).

**P0 GAP-1003 (DONE):** kiteclass-core had **no filter bridging gateway `X-User-Roles` header → Spring `GrantedAuthority`** → every `hasRole`/`hasAnyRole` `@PreAuthorize` (24 endpoints / 10 controllers) was dead-deny; `record-payment` returned 403 for all roles. IT masked it (`TestSecurityConfig` + `@WithMockUser` injects authorities). Fixed `GatewayHeaderAuthenticationFilter` (mirrors `kitehub-subscription` `XUserRolesHeaderFilter` GAP-706/783) + 7-case unit test + `SecurityConfig` wire. Live re-walk: record-payment OWNER **201** (was 403), SENT→PARTIAL→PAID, GET invoice 200, cross-tenant 404.

**Follow-ups OPEN:** GAP-1004 P1 (over-payment no-clamp + idempotency not enforced DB-side), GAP-1005 P1 (InvoiceController missing `@PreAuthorize`). recordedBy hardcode 1L → existing GAP-526.

**Walk fixtures consumed (sky tenant):** invoice 28 PAID, 15 over-paid, 14 has 2 payments. G2 uses invoice 9–13 (SENT, unpaid). G2 recipe: `documents/05-guides/operations/2026-06-05-g2-recipe-kc7-invoice-payment.md`.

## CI self-hosted runner restored (important infra change)

The 2 registered runners (`kite-dev-wsl-runner`, `NguyenVanKiet-runner-2`) were **offline ghosts** from the prior dev box (`/home/nguyenvankiet`). This machine (`kitedev`) had no runner → all `[self-hosted, Linux, X64]` CI jobs (Test Core Service, ShellCheck, DB schema/migration gates) sat queued forever → **#2180 was never actually green**.

**Fixed:** installed fresh `actions-runner v2.334.0` at `~/actions-runner` (user kitedev), registered with `--replace` (took over `kite-dev-wsl-runner`), started via `./run.sh` background daemon (no sudo/systemd). Then closed its env gaps:
- `shellcheck` binary → `~/.local/bin` (on runner PATH; workflow assumed pre-installed, fell back to no-NOPASSWD `sudo apt`).
- Warmed `~/.m2` via `cd kitehub && ./mvnw install -DskipTests` (stale `kitehub-platform` JAR lacked `Instance.slug` → schema-drift gate's `spring-boot:run` compile failed `cannot find symbol setSlug`).

⚠️ **The runner is a background `./run.sh` daemon on this machine** — keeps future CI working. Stop it when the box is idle (`kill` the run.sh process) or leave up. Re-register if machine wiped (runbook: `documents/05-guides/operations/self-hosted-github-actions-runner-runbook.md`).

## Local-only (uncommitted)

`.claude/statusline-kite.sh` has an added `PR open:N` segment (cached 60s, bg-refresh) — user-requested, not committed. Commit separately or leave.

## Next steps

1. **G2 human tests** for KC-5/KC-6/KC-7 (recipes shipped under `05-guides/operations/`).
2. **KC-8 Parent portal** + **KC-9 Student portal** (⬜ next flows; both depend on KC-4..KC-7).
3. Campaign §4: KC-1..KC-7 all 🔄 walk-pass-pending-human; KC-8/9 ⬜.
4. Follow-up gaps GAP-1004 + GAP-1005 (KC-7 hardening) when picked.
