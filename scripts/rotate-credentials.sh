#!/usr/bin/env bash
# rotate-credentials.sh — General-purpose credential rotation wrapper
# =============================================================================
#
# Purpose: Wrapper guide-tool cho user-execution rotation of production
# credentials (admin password, vendor API keys, signing tokens). Original
# scope = 3 credentials leaked trong session 2026-05-13 (per GAP-525); giờ
# serve cho routine rotation theo cadence trong
# `documents/05-guides/operations/secrets-rotation-runbook.md`.
#
# Backward-compat: scripts/rotate-leaked-credentials.sh symlink trỏ về đây
# (Wave 82 Bucket F1 rename per Task #59); old invocations continue to work.
#
# Wrapper KHÔNG tự rotate — chỉ verify reachability + emit step-by-step
# Vietnamese instructions + audit log skeleton.
#
# Per `.claude/rules/agent-aws-access.md` §4.3: secretsmanager put-secret-value
# + vendor portal revoke = Tier 3 mutations — agent KHÔNG được tự chạy.
# Per `.claude/rules/agent-action-bias.md` §3 row 5: destructive shared-state
# action without prior authorization = user-confirm + user-execute.
#
# Wrapper provides:
#   - Reachability pre-flight checks (Tier 1 read-only)
#   - Per-credential step-by-step Vietnamese guidance
#   - Audit log skeleton dưới documents/04-quality/audits/aws-verification/
#   - Idempotent — re-run safe
#
# Usage:
#   bash scripts/rotate-credentials.sh --dry-run            # Default; reachability check only
#   bash scripts/rotate-credentials.sh --cred=admin-password
#   bash scripts/rotate-credentials.sh --cred=cloudflare-token
#   bash scripts/rotate-credentials.sh --cred=resend-api-key
#   bash scripts/rotate-credentials.sh --cred=all
#
# Legacy alias (deprecated, still works via symlink):
#   bash scripts/rotate-leaked-credentials.sh ...
#
# Related runbooks:
#   - documents/05-guides/operations/credential-rotation-runbook.md (general)
#   - documents/05-guides/operations/credential-rotation-2026-05-13.md (this incident)
#   - documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
#
# Per GAP-525 PARTIAL closure path:
#   1. Wave 72a Bucket D shipped runbook + incident artifact (Status PARTIAL 50%)
#   2. Wave 77 Bucket C (this PR) ships automation wrapper (Status PARTIAL 85%)
#   3. User executes rotation OUTSIDE Claude (Status DONE 100%)
#      Closure commit trailer: GAP-525_USER_ROTATED: admin-pwd YYYY-MM-DD / \
#        cloudflare YYYY-MM-DD / resend YYYY-MM-DD
# =============================================================================

set -euo pipefail

# ----- Defaults -----
DRY_RUN=true
CRED="all"
AWS_PROFILE_DEFAULT="${AWS_PROFILE:-dev-admin}"
AWS_REGION_DEFAULT="${AWS_REGION:-ap-southeast-1}"
INCIDENT_DATE="2026-05-13"
TODAY=$(date -u +%Y-%m-%d)

# Audit artifact root
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AUDIT_DIR="$REPO_ROOT/documents/04-quality/audits/credential-rotation"

# ----- Helpers -----
log()  { printf '[rotate-leaked] %s\n' "$*"; }
warn() { printf '[rotate-leaked] WARN: %s\n' "$*" >&2; }
err()  { printf '[rotate-leaked] ERROR: %s\n' "$*" >&2; }

usage() {
  cat <<EOF
Usage: $0 [--dry-run] [--cred=<name>]

Options:
  --dry-run               (default) Reachability pre-flight check only. No mutation.
  --cred=admin-password   Hướng dẫn rotate admin@kitehub.me password.
  --cred=cloudflare-token Hướng dẫn rotate Cloudflare API token (read-only).
  --cred=resend-api-key   Hướng dẫn rotate Resend API key (sending).
  --cred=all              Hướng dẫn tất cả 3 credentials (default if --dry-run absent).
  --help                  Show this help.

Examples:
  $0                                     # Dry-run reachability check
  $0 --cred=admin-password               # Show admin password rotation steps
  $0 --cred=all                          # Show all 3 credential rotation steps

References:
  - GAP-525 (documents/04-quality/gaps/GAP-525-*.md)
  - Runbook: documents/05-guides/operations/credential-rotation-2026-05-13.md
  - Incident: documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
EOF
}

# ----- Parse args -----
for arg in "$@"; do
  case "$arg" in
    --dry-run)        DRY_RUN=true ;;
    --cred=*)         CRED="${arg#--cred=}"; DRY_RUN=false ;;
    --help|-h)        usage; exit 0 ;;
    *)                err "Unknown arg: $arg"; usage; exit 64 ;;
  esac
done

# ----- Reachability pre-flight (Tier 1 read-only per agent-aws-access.md §2.1) -----
reachability_check() {
  log "Pre-flight reachability checks (Tier 1 read-only)"
  local ok=0 fail=0

  # AWS Secrets Manager — describe-secret on each target (no value reveal)
  if command -v aws >/dev/null 2>&1; then
    log "  AWS CLI: $(aws --version 2>&1 | head -1)"
    for sid in \
      "kitehub/production/admin-seed-password" \
      "kitehub/production/resend-api-key" \
      "kitehub/production/cloudflare-api-token"; do
      if aws secretsmanager describe-secret \
           --secret-id "$sid" \
           --profile "$AWS_PROFILE_DEFAULT" \
           --region "$AWS_REGION_DEFAULT" \
           --query 'ARN' --output text >/dev/null 2>&1; then
        log "  ✅ AWS Secret reachable: $sid"
        ok=$((ok+1))
      else
        warn "  ⚠️  AWS Secret unreachable (or absent — OK for first-time create): $sid"
        fail=$((fail+1))
      fi
    done
  else
    warn "  AWS CLI not installed — install via 'pip install awscli' or use AWS web console"
    fail=$((fail+1))
  fi

  # Cloudflare API reachability (any HTTP response = reachable; auth not required here)
  if command -v curl >/dev/null 2>&1; then
    if curl -sS --max-time 10 -o /dev/null -w '%{http_code}' https://api.cloudflare.com/ 2>/dev/null | grep -qE '^[0-9]{3}$'; then
      log "  ✅ Cloudflare API reachable (api.cloudflare.com)"
      ok=$((ok+1))
    else
      warn "  ⚠️  Cloudflare API unreachable — check network"
      fail=$((fail+1))
    fi
  fi

  # Resend API reachability
  if command -v curl >/dev/null 2>&1; then
    if curl -sS --max-time 10 -o /dev/null -w '%{http_code}' https://api.resend.com/ 2>/dev/null | grep -qE '^[0-9]{3}$'; then
      log "  ✅ Resend API reachable (api.resend.com)"
      ok=$((ok+1))
    else
      warn "  ⚠️  Resend API unreachable — check network"
      fail=$((fail+1))
    fi
  fi

  log "Reachability summary: $ok pass / $fail warn"
  log ""
  log "Phase 1 BETA AWS profile: $AWS_PROFILE_DEFAULT (region $AWS_REGION_DEFAULT)"
  log "Audit log target dir:    $AUDIT_DIR"
  log ""

  if [[ $ok -eq 0 ]]; then
    err "Zero successful reachability checks. Verify AWS CLI + curl installed + network OK."
    return 1
  fi
  return 0
}

# ----- Audit log skeleton -----
write_audit_skeleton() {
  local cred_slug="$1"
  local audit_file="$AUDIT_DIR/${TODAY}-credential-rotation-${cred_slug}.md"

  if [[ -f "$audit_file" ]]; then
    log "  Audit skeleton already exists (idempotent): $audit_file"
    return 0
  fi

  mkdir -p "$AUDIT_DIR"
  cat > "$audit_file" <<EOF
---
title: Credential Rotation — ${cred_slug} (Wave 77 Bucket C automation)
status: pending
created: ${TODAY}
trigger: leak
related-gaps: [GAP-525]
parent-incident: documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
---

# Credential Rotation Audit — ${cred_slug}

## Scope

Rotation cho credential class \`${cred_slug}\` leak trong session ${INCIDENT_DATE}. Wave 77 Bucket C wrapper script
\`scripts/rotate-leaked-credentials.sh\` đã guide user-execution. User điền outcome vào template này
sau khi hoàn tất rotation.

## Pre-rotation state (verified via Tier 1 read-only)

\`\`\`
[FILL] aws secretsmanager describe-secret output (ARN + LastChangedDate trước rotation)
\`\`\`

## Rotation steps performed

| Step | Description | Status | Timestamp (UTC) |
|---|---|---|---|
| 1 | Generate new credential | pending | — |
| 2 | Store in AWS Secrets Manager (put-secret-value) | pending | — |
| 3 | Redeploy / re-seed consumer | pending | — |
| 4 | Smoke test new credential | pending | — |
| 5 | Revoke old credential at vendor portal | pending | — |
| 6 | Verify old credential rejected | pending | — |

## Verification

\`\`\`
[FILL] verify command outputs (per credential-rotation-runbook.md §4)
\`\`\`

## Notes

- Wrapper script: \`scripts/rotate-leaked-credentials.sh --cred=${cred_slug}\`
- Runbook: \`documents/05-guides/operations/credential-rotation-2026-05-13.md\`
- General runbook: \`documents/05-guides/operations/credential-rotation-runbook.md\`

## Closure

When all 6 rotation steps verified, set \`status: complete\` in frontmatter + update parent incident
artifact \`2026-05-14-wave-72a-3-credentials.md\` rotation-status table row to \`verified\`.

Flip GAP-525 \`PARTIAL\` → \`DONE\` only after all 3 credentials reach \`verified\` per
\`.claude/rules/gap-done-discipline.md\` §2.
EOF
  log "  ✅ Audit skeleton created: $audit_file"
}

# ----- Per-credential instructions (Vietnamese narrative + English identifiers) -----
guide_admin_password() {
  cat <<'EOF'

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔑 ROTATE #1 — Admin password (admin@kitehub.me)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Sensitivity: 🔴 CRITICAL — platform admin access
Estimated time: ~10 phút
Downtime: zero (re-seed atomic)

📋 BƯỚC 1 — Snapshot trạng thái secret hiện tại

  aws secretsmanager describe-secret \
    --secret-id kitehub/production/admin-seed-password \
    --profile dev-admin --region ap-southeast-1 \
    --query 'VersionIdsToStages'

📋 BƯỚC 2 — Tạo password mới (Option A preferred — Terraform re-roll)

  cd infrastructure/terraform-aws
  terraform apply \
    -target=random_password.seed_admin_password \
    -replace=random_password.seed_admin_password
  terraform apply -target=aws_secretsmanager_secret_version.admin_seed_password

  HOẶC Option B — Manual put-secret-value (emergency, không quản lý qua Terraform):

  NEW_PASSWORD=$(openssl rand -base64 24)
  aws secretsmanager put-secret-value \
    --secret-id kitehub/production/admin-seed-password \
    --secret-string "$NEW_PASSWORD" \
    --profile dev-admin --region ap-southeast-1
  unset NEW_PASSWORD       # quan trọng — KHÔNG để password trong shell history

📋 BƯỚC 3 — Re-seed admin account (đọc Secrets Manager + update users table)

  bash scripts/seed-direct-sql.sh --target-env=production --user=admin@kitehub.me

📋 BƯỚC 4 — Verify login UI

  • Mở browser → https://kitehub.me/login
  • Đăng nhập: admin@kitehub.me + password MỚI
  • Kỳ vọng: HTTP 200 + redirect /admin dashboard + role-guard accept

📋 BƯỚC 5 — Verify old password bị reject (nếu vẫn nhớ literal)

  • Đăng nhập với password CŨ → kỳ vọng HTTP 401

📋 BƯỚC 6 — Update audit artifact

  Edit: documents/04-quality/audits/credential-rotation/YYYY-MM-DD-credential-rotation-admin-password.md
  Fill in: rotation-status table rows 1-6 + verification outputs + status: complete

⚠️  RECOVERY (nếu rotation fail):
  AWS Secrets Manager giữ previous version 30 ngày. Revert:
  aws secretsmanager update-secret-version-stage \
    --secret-id kitehub/production/admin-seed-password \
    --version-stage AWSCURRENT \
    --move-to-version-id <previous-version-id>

EOF
  write_audit_skeleton "admin-password"
}

guide_cloudflare_token() {
  cat <<'EOF'

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔑 ROTATE #2 — Cloudflare API token (read-only scope)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Sensitivity: 🟠 MODERATE — read-only, can list DNS + zones (KHÔNG modify)
Estimated time: ~5 phút
Downtime: zero (read-only, no service consumer restart needed)

📋 BƯỚC 1 — Tạo token mới ở Cloudflare dashboard

  • Mở browser → https://dash.cloudflare.com/profile/api-tokens
  • Click "Create Token"
  • Template: "Read all resources" (cùng scope với token cũ)
  • Permissions:
      - Account → Account Settings → Read
      - Zone → Zone → Read
      - Zone → DNS → Read
  • Token TTL: chọn 90 days (per quarterly rotation cadence)
  • Click "Create Token" → COPY token (chỉ hiển thị 1 lần)

📋 BƯỚC 2 — Verify token mới hoạt động

  curl -sH "Authorization: Bearer $NEW_TOKEN" \
    https://api.cloudflare.com/client/v4/user/tokens/verify | jq '.success'
  # Kỳ vọng: true

📋 BƯỚC 3 — Lưu vào AWS Secrets Manager (optional — nếu có service consumer)

  aws secretsmanager put-secret-value \
    --secret-id kitehub/production/cloudflare-api-token \
    --secret-string "$NEW_TOKEN" \
    --profile dev-admin --region ap-southeast-1
  unset NEW_TOKEN

  Nếu chưa có secret, tạo mới:
  aws secretsmanager create-secret \
    --name kitehub/production/cloudflare-api-token \
    --description "Cloudflare API token — Read all resources scope" \
    --secret-string "$NEW_TOKEN" \
    --profile dev-admin --region ap-southeast-1

📋 BƯỚC 4 — Update MCP server config / local DNS audit scripts

  • Nếu có Claude Code Cloudflare MCP server config → cập nhật token
  • Nếu scripts/check-dns-propagation.sh đọc env var → cập nhật .env.production

📋 BƯỚC 5 — Revoke token CŨ ở Cloudflare dashboard

  • Mở browser → https://dash.cloudflare.com/profile/api-tokens
  • Tìm token cũ (prefix `cfut_B5d8tYY...`)
  • Click "..." → "Delete"
  • ⚠️ Bước này IRREVERSIBLE — chỉ thực hiện sau khi BƯỚC 2 verify token mới OK

📋 BƯỚC 6 — Verify token cũ bị reject

  curl -sH "Authorization: Bearer $OLD_TOKEN" \
    https://api.cloudflare.com/client/v4/user/tokens/verify | jq '.success'
  # Kỳ vọng: false (hoặc HTTP 401)

📋 BƯỚC 7 — Verify ở Cloudflare audit log

  • Browser: https://dash.cloudflare.com/?to=/account/audit-log
  • Kỳ vọng: thấy event "API Token Deleted" với timestamp hôm nay

📋 BƯỚC 8 — Update audit artifact

  Edit: documents/04-quality/audits/credential-rotation/YYYY-MM-DD-credential-rotation-cloudflare-token.md

EOF
  write_audit_skeleton "cloudflare-token"
}

guide_resend_api_key() {
  cat <<'EOF'

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔑 ROTATE #3 — Resend API key (sending scope)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Sensitivity: 🟠 MODERATE — sending access only, có thể gửi email as kitehub.me
Estimated time: ~10 phút
Downtime: ~30s (kitehub-email container restart pickup new key)

📋 BƯỚC 1 — Tạo API key mới ở Resend dashboard

  • Mở browser → https://resend.com/api-keys
  • Click "Create API Key"
  • Name: kitehub-production (hoặc kitehub-production-v2 nếu trùng tên)
  • Permission: "Sending access" — RESTRICT to domain kitehub.me
  • Click "Create" → COPY key (chỉ hiển thị 1 lần)

📋 BƯỚC 2 — Verify key mới hoạt động (gửi test email)

  curl -X POST https://api.resend.com/emails \
    -H "Authorization: Bearer $NEW_KEY" \
    -H "Content-Type: application/json" \
    -d '{"from":"noreply@kitehub.me","to":"dev@kitehub.me","subject":"rotation test","html":"ok"}'
  # Kỳ vọng: HTTP 200 + email_id trong response

  Verify Resend dashboard: https://resend.com/emails → email vừa gửi có status "delivered"

📋 BƯỚC 3 — Lưu vào AWS Secrets Manager

  aws secretsmanager put-secret-value \
    --secret-id kitehub/production/resend-api-key \
    --secret-string "$NEW_KEY" \
    --profile dev-admin --region ap-southeast-1
  unset NEW_KEY

📋 BƯỚC 4 — Redeploy kitehub-email service (pickup new key)

  Options:
  (a) Via deploy-production.yml workflow:
      gh workflow run deploy-production.yml -f confirm=APPLY

  (b) Via SSH/SSM nếu có direct access:
      docker compose restart kitehub-email
      docker compose logs kitehub-email --tail=20 | grep -iE "resend|email|init"
      # Kỳ vọng: zero "401" / "unauthorized" entries

📋 BƯỚC 5 — Smoke test qua kitehub-email endpoint nội bộ

  • Trigger transactional email (vd: signup confirmation, password reset)
  • Verify email arrives + Resend dashboard "delivered"

📋 BƯỚC 6 — Revoke key CŨ ở Resend dashboard

  • Mở browser → https://resend.com/api-keys
  • Tìm key cũ (prefix `re_hoMkdPyz...`)
  • Click "..." → "Revoke"
  • ⚠️ Bước này IRREVERSIBLE — chỉ thực hiện sau khi BƯỚC 5 verify smoke test OK

📋 BƯỚC 7 — Verify key cũ bị reject

  curl -sX POST https://api.resend.com/emails \
    -H "Authorization: Bearer $OLD_KEY" \
    -H "Content-Type: application/json" \
    -d '{"from":"noreply@kitehub.me","to":"dev@kitehub.me","subject":"old key test","html":"ok"}'
  # Kỳ vọng: HTTP 401

📋 BƯỚC 8 — Update audit artifact

  Edit: documents/04-quality/audits/credential-rotation/YYYY-MM-DD-credential-rotation-resend-api-key.md

EOF
  write_audit_skeleton "resend-api-key"
}

closure_reminder() {
  cat <<EOF

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ GAP-525 CLOSURE CHECKLIST (sau khi hoàn tất rotation)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. [ ] Update 3 audit skeleton files dưới $AUDIT_DIR/
       với rotation-status table rows 1-6 + status: complete

2. [ ] Update parent incident artifact:
       documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
       rotation-status table — 3 rows từ pending → verified

3. [ ] Flip GAP-525 Status: 🟡 PARTIAL → 🟢 DONE trong file:
       documents/04-quality/gaps/GAP-525-rotate-credentials-leaked-session-2026-05-13.md
       AC checkboxes 1-4 → [x]

4. [ ] Update gap-status.csv row:
       Sửa: GAP-525,...,PARTIAL,P0,...,50,...
       Thành: GAP-525,...,DONE,P0,...,100,2026-05-13,YYYY-MM-DD,Rotation complete

5. [ ] Optional: delete or redact local session transcript file:
       ~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/7517e076-d175-4fb2-bbdf-23bb355763d9.jsonl

6. [ ] File memory entry (per incident-to-rule-pipeline.md Stage 5):
       feedback_credential_leak_session_2026_05_13.md
       Describe "Option B paste in chat" pattern + cross-link runbook

7. [ ] Commit trailer khi closure PR ship:

   GAP-525_USER_ROTATED: admin-pwd YYYY-MM-DD / cloudflare YYYY-MM-DD / resend YYYY-MM-DD

   Trailer format: 3 credential class + ISO date completion mỗi loại.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

EOF
}

# ----- Main -----
log "Wave 77 Bucket C — Credential rotation automation wrapper"
log "Incident date: $INCIDENT_DATE — GAP-525 PARTIAL→85% (this wrapper) → DONE (user rotation)"
log ""

if ! reachability_check; then
  warn "Reachability check incomplete. Bạn vẫn có thể chạy --cred=<name> để xem hướng dẫn."
fi

if [[ "$DRY_RUN" == true ]]; then
  log ""
  log "Dry-run mode (default). Để xem hướng dẫn rotation, chạy:"
  log "  $0 --cred=admin-password"
  log "  $0 --cred=cloudflare-token"
  log "  $0 --cred=resend-api-key"
  log "  $0 --cred=all"
  log ""
  log "Reference docs:"
  log "  - documents/05-guides/operations/credential-rotation-2026-05-13.md"
  log "  - documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md"
  exit 0
fi

case "$CRED" in
  admin-password)    guide_admin_password ;;
  cloudflare-token)  guide_cloudflare_token ;;
  resend-api-key)    guide_resend_api_key ;;
  all)
    guide_admin_password
    guide_cloudflare_token
    guide_resend_api_key
    ;;
  *)
    err "Unknown --cred value: $CRED"
    usage
    exit 64
    ;;
esac

closure_reminder
