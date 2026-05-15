---
id: GAP-568
title: BE CORS_ALLOWED_ORIGINS sweep + preflight verify pre-DNS-flip across 7 services (Wave 82 Bucket B prerequisite)
status: OPEN
priority: P0
domain: DevOps
phase: phase-1-beta
percent_complete: 0
created: 2026-05-15
updated: 2026-05-15
wave_target: 82
---

# GAP-568 — Wave 82 BE CORS allowlist sweep pre-DNS-flip

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — BLOCKING Bucket D DNS cutover
**Domain:** DevOps / Backend (cross-cutting)
**Found:** 2026-05-15 (Wave 82 Bucket A outside-in failure-mode matrix audit, finding F11)
**Affects:** Wave 82 FE self-host DNS cutover — toàn bộ 7 BE services (kitehub-{admin,branding,email,gateway,subscription} + kiteclass-{core,gateway})

---

## Problem

Wave 82 Bucket A failure-mode matrix audit (finding F11) flag rủi ro silent: hiện tại tất cả BE services đều hardcode `CORS_ALLOWED_ORIGINS` env var trỏ tới Vercel preview domain hiện hành (vd `https://kitehub.vercel.app` hoặc `https://kitehub-frontend-*.vercel.app`). Khi Bucket D flip DNS từ Vercel sang EC2 self-host (new origin `https://app.kitehub.me` hoặc tương đương), nếu BE chưa được update allowlist:

- Browser preflight `OPTIONS` request từ new FE origin → BE trả về 403 (origin not in allowlist)
- Hoặc tệ hơn: BE trả 200 nhưng KHÔNG echo `Access-Control-Allow-Origin` header → browser block silently
- User thấy "Network error" generic, FE devtools console show CORS error
- Không log line nào dễ filter trên BE → hours debug

Đây là **silent fail class** vì:
- Curl test BE health endpoint từ máy dev vẫn 200 OK (no preflight needed)
- BE log không print warning cho rejected origin (Spring Security CORS filter default)
- Discovery chỉ qua user-facing FE failure post-flip

`production-env-config-registry.md` §2 đã flag CORS_ALLOWED_ORIGINS thuộc suspect-default class — tức là dễ stale nếu không có sweep discipline.

---

## Root Cause

Mỗi BE service có riêng `CORS_ALLOWED_ORIGINS` env var trong `application-production.yml` hoặc qua AWS Secrets Manager. Hiện trạng 7 services không nhất quán:
- Một số dùng wildcard `*` (UNSAFE production, defer fix riêng)
- Một số explicit list Vercel domain
- Một số có thể missing config → default deny tất cả origin

Failure matrix F11 chỉ ra: TRƯỚC khi user trigger DNS edit flip, phải sweep 7 services, verify mỗi service có new EC2 origin trong allowlist, và verify qua preflight curl từ máy ngoài — chứ không trust grep code only.

---

## Proposed Fix

### Bước 1: Inventory hiện trạng CORS config 7 services

Coordinator chạy state-check:

```bash
# Grep current CORS config across all 7 services
for svc in kitehub-admin kitehub-branding kitehub-email kitehub-gateway kitehub-subscription kiteclass-core kiteclass-gateway; do
  echo "=== $svc ==="
  grep -rn "CORS_ALLOWED_ORIGINS\|allowed-origins\|allowedOrigins" \
    kitehub/$svc/src/main/resources/ \
    kiteclass/$svc/src/main/resources/ \
    2>/dev/null | head -10
done
```

Document findings trong AWS verification artifact `documents/04-quality/audits/aws-verification/2026-MM-DD-wave-82-cors-sweep-pre-flip.md` theo `pre-mutation-state-check.md` §3 template.

### Bước 2: Update allowlist trên cả 7 services

Cho mỗi service, edit `application-production.yml` (hoặc Secrets Manager entry tương ứng) để include CẢ HAI origins trong giai đoạn cutover:

```yaml
# kitehub/kitehub-gateway/src/main/resources/application-production.yml (ví dụ)
kite:
  cors:
    allowed-origins:
      - https://kitehub.vercel.app          # legacy — keep during cutover
      - https://app.kitehub.me              # NEW EC2 self-host
      - https://kiteclass.kitehub.me        # NEW KiteClass origin nếu có
```

Hoặc qua env var:

```bash
CORS_ALLOWED_ORIGINS=https://kitehub.vercel.app,https://app.kitehub.me,https://kiteclass.kitehub.me
```

Repeat cho 7 services. Sau khi cutover stable ≥7 ngày, có thể remove legacy Vercel entry (defer task, không scope gap này).

### Bước 3: Deploy updated BE config TRƯỚC DNS flip

Theo `concurrent-production-mutation-ops.md`, deploy BE update + DNS flip phải serialize:
1. Apply BE config change (workflow_dispatch deploy hoặc Secrets Manager rotate + restart 7 services)
2. Verify BE healthy với new config: `curl -fsS https://api.kitehub.me/actuator/health` returns 200
3. Verify preflight (Bước 4 dưới)
4. THEN user trigger DNS edit flip

### Bước 4: Preflight verify từng service từ ngoài

Test mỗi service public endpoint với new origin:

```bash
NEW_ORIGIN="https://app.kitehub.me"

# kitehub-gateway (proxy mọi BE)
for endpoint in \
  "https://api.kitehub.me/api/v1/auth/login" \
  "https://api.kitehub.me/api/v1/beta-status" \
  "https://api.kitehub.me/api/v1/admin/staff/invitations" \
  "https://api.kitehub.me/api/v1/billing/invoices" \
  "https://api.kitehub.me/api/v1/branding/jobs" \
  "https://api.kitehub.me/api/v1/email/send" \
  "https://api.kitehub.me/api/v1/subscriptions"; do
  echo "=== $endpoint ==="
  curl -sI -X OPTIONS \
    -H "Origin: $NEW_ORIGIN" \
    -H "Access-Control-Request-Method: POST" \
    -H "Access-Control-Request-Headers: Content-Type,Authorization" \
    "$endpoint" \
    | grep -iE "^(HTTP|access-control)"
done
```

Expected output mỗi endpoint:
- `HTTP/2 200` (hoặc 204 No Content)
- `access-control-allow-origin: https://app.kitehub.me` (echo lại NEW_ORIGIN, KHÔNG wildcard `*` nếu service dùng credentials)
- `access-control-allow-credentials: true` (nếu service dùng cookie/JWT)
- `access-control-allow-methods: ...` chứa POST/GET/PUT/DELETE phù hợp

Failure mode: nếu bất kỳ endpoint thiếu `access-control-allow-origin` matching → BLOCK DNS flip, fix config trước.

### Bước 5: AWS verification artifact

Ship audit doc với:
- Inventory 7 services CORS config trước update
- Diff config sau update
- Preflight curl output cho 7 endpoints (paste raw output)
- Verdict: safe to proceed với DNS flip Bucket D

---

## Acceptance Criteria

- [ ] State-check inventory: grep result cho `CORS_ALLOWED_ORIGINS` / `allowed-origins` từ 7 services documented trong audit artifact
- [ ] BE config updated cho 7 services include cả Vercel legacy + new EC2 origin trong allowlist (qua `application-production.yml` hoặc Secrets Manager)
- [ ] BE deploy successful: `kitehub-gateway` + 6 service health endpoints trả 200 sau config rotate
- [ ] Preflight curl test 7 endpoints với `Origin: https://app.kitehub.me` → mỗi endpoint trả `HTTP 200/204` + `access-control-allow-origin: https://app.kitehub.me` (NOT `*`, NOT missing)
- [ ] Preflight tương tự cho KiteClass origin nếu có: `Origin: https://kiteclass.kitehub.me` → kiteclass-{core,gateway} accept
- [ ] AWS verification artifact `documents/04-quality/audits/aws-verification/2026-MM-DD-wave-82-cors-sweep-pre-flip.md` ship trước user trigger DNS flip (per `pre-mutation-state-check.md` §3)
- [ ] DNS flip Bucket D BLOCKED qua reviewer-checklist cho đến khi gap này DONE
- [ ] Cross-link `production-env-config-registry.md` §2 suspect-default + §7.4 live verification pattern

## Related

- Wave plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §1 Brainstorm Q3 + §3 Bucket B (pre-flight) + Bucket D (DNS flip)
- Failure-matrix finding: **F11** Wave 82 Bucket A outside-in audit 2026-05-15
- Sister gaps: GAP-565 (F6 SG) · GAP-566 (F7 RAM) · GAP-567 (F10 cert)
- Rules: `.claude/rules/production-env-config-registry.md` §2 + §7.4 · `.claude/rules/pre-mutation-state-check.md` §3 · `.claude/rules/concurrent-production-mutation-ops.md` (BE deploy + DNS flip serialize)
- Pattern reference: Wave 81 Bucket F fail-fast env var sweep (4 secrets cross-service consistency)

## Log

- **2026-05-15:** Gap filed via Wave 82 Bucket A outside-in failure-mode matrix audit (finding F11). P0 BLOCKING DNS cutover Bucket D — phải address SAU khi Bucket B EC2 provision (new origin domain confirmed) NHƯNG TRƯỚC khi flip DNS. Silent fail class — curl health check không expose CORS issue; chỉ browser preflight test detect được. 7 services cần sweep đồng thời để tránh partial-update inconsistency.
