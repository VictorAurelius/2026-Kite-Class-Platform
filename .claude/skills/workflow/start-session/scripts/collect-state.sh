#!/usr/bin/env bash
# collect-state.sh — gather session-start context for /start-session skill
# Output: human-readable summary to stdout; errors to stderr
# Usage: ./collect-state.sh [--quick|--json] [--no-aws|--refresh-aws] [--audit-snapshot]
#
# Accuracy fix 2026-04-24 per GAP-206:
#   - Wave + blockers parsed from ROADMAP.md (not filename mtime / alphabetical)
#   - Recent merges from git log
#   - /repo-status integration for full health
#   - Scratchpad awareness for documents/action-2.md
#
# AWS Phase 1 BETA snapshot 2026-05-09 (this PR):
#   - Tier 1 read-only commands per .claude/rules/agent-aws-access.md §2.1
#     (sts get-caller-identity / ec2 describe-instances / rds describe-db-instances /
#      elbv2 describe-load-balancers / cloudtrail describe-trails+get-trail-status /
#      cloudwatch describe-alarms --state-value ALARM)
#   - 30-minute cache at .claude/session-aws-cache/snapshot.json (gitignored)
#   - --no-aws skips the section; --refresh-aws bypasses cache; --audit-snapshot
#     writes a verification artifact under documents/04-quality/audits/aws-verification/

set -u

MODE="full"
AWS_ENABLED=true
AWS_REFRESH=false
AWS_AUDIT=false
for arg in "$@"; do
  case "$arg" in
    --quick)          MODE="quick" ;;
    --json)           MODE="json"  ;;
    --no-aws)         AWS_ENABLED=false ;;
    --refresh-aws)    AWS_REFRESH=true ;;
    --audit-snapshot) AWS_AUDIT=true ;;
  esac
done

TS="$(date -Iseconds)"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || exit 1

BRANCH="$(git branch --show-current 2>/dev/null || echo unknown)"

# Dirty check — with scratchpad awareness
DIRTY_FILES="$(git diff --name-only 2>/dev/null; git diff --cached --name-only 2>/dev/null)"
DIRTY_FILES="$(echo "$DIRTY_FILES" | sort -u | grep -v '^$' || true)"
DIRTY_COUNT="$(echo "$DIRTY_FILES" | grep -c '.' 2>/dev/null || echo 0)"

if [ "$DIRTY_COUNT" = "0" ]; then
  BRANCH_STATE="clean"
elif [ "$DIRTY_COUNT" = "1" ] && [ "$DIRTY_FILES" = "documents/action-2.md" ]; then
  BRANCH_STATE="clean (scratchpad only: documents/action-2.md)"
else
  BRANCH_STATE="dirty ($DIRTY_COUNT file(s))"
fi

# PRs (gh required — gracefully skip if not authed)
# Enhanced 2026-05-28: per-PR CI status + mergeStateStatus + GAP-NNN correlation
# with documents/04-quality/gaps/**/closed/. Catches "gap DONE but PR unmerged" class
# (vd PR #1929 GAP-786 — file ở closed/ nhưng PR vẫn OPEN missed bởi title parsing only).
OPEN_PRS="?"
TOP_PRS=""        # legacy ; separated (preserved for JSON mode backward compat)
TOP_PRS_LINES=""  # multi-line render for full output
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  PR_JSON="$(gh pr list --state open --limit 5 \
    --json number,title,mergeStateStatus,statusCheckRollup 2>/dev/null || echo '[]')"
  OPEN_PRS="$(echo "$PR_JSON" | jq 'length' 2>/dev/null || echo '?')"
  if command -v jq >/dev/null 2>&1 && [ "$OPEN_PRS" != "?" ] && [ "$OPEN_PRS" != "0" ]; then
    # Tab-separated stream: num \t title \t merge_state \t fail \t pend
    while IFS=$'\t' read -r num title merge_state fail_count pend_count; do
      [ -z "$num" ] && continue
      # CI icon
      if [ "${fail_count:-0}" -gt 0 ]; then
        ci_icon="${fail_count}❌"
      elif [ "${pend_count:-0}" -gt 0 ]; then
        ci_icon="${pend_count}⏳"
      else
        ci_icon="✓"
      fi
      # Parse first GAP-NNN ref from title
      gap_ref="$(echo "$title" | grep -oE 'GAP-[0-9]+' | head -1)"
      gap_flag=""
      if [ -n "$gap_ref" ]; then
        if find documents/04-quality/gaps -path '*/closed/*' \
             -name "${gap_ref}-*.md" -print -quit 2>/dev/null | grep -q .; then
          gap_flag="    ⚠️ ${gap_ref} DONE in repo — PR ready to merge (gap-DONE-unmerged)"
        else
          gap_flag="    · ${gap_ref} not yet in closed/"
        fi
      fi
      # Truncate title for display
      title_short="$(echo "$title" | head -c 60)"
      TOP_PRS_LINES+="
  · #${num} [${merge_state:-?} ${ci_icon}] ${title_short}"
      if [ -n "$gap_flag" ]; then
        TOP_PRS_LINES+="
${gap_flag}"
      fi
      TOP_PRS+="#${num} [${merge_state:-?} ${ci_icon}] ${title_short};"
    done < <(echo "$PR_JSON" | jq -r '.[] | [
      .number,
      .title,
      (.mergeStateStatus // "?"),
      ([.statusCheckRollup[]? | select((.conclusion // .state) == "FAILURE")] | length),
      ([.statusCheckRollup[]? | select((.conclusion // .state) == "PENDING" or (.conclusion // .state) == "IN_PROGRESS" or (.conclusion // .state) == "QUEUED")] | length)
    ] | @tsv' 2>/dev/null)
  fi
fi

# Repo-status — full multi-factor health (replaces minimal CI check)
RS_LEVEL="unknown"
RS_CI="?"
RS_CVE_HIGH="?"
RS_CVE_CRIT="?"
RS_STALE_BRANCHES="?"
RS_AUDIT_P0="?"
if [ -f scripts/repo-status.sh ]; then
  RS_JSON="$(bash scripts/repo-status.sh --json 2>/dev/null || echo '{}')"
  if command -v jq >/dev/null 2>&1; then
    RS_LEVEL="$(echo "$RS_JSON" | jq -r '.level // "unknown"' 2>/dev/null || echo unknown)"
    RS_CI="$(echo "$RS_JSON" | jq -r '.ci.status // "unknown"' 2>/dev/null || echo unknown)"
    RS_CVE_HIGH="$(echo "$RS_JSON" | jq -r '(.security.high // 0) + (.security.code_scan_errors // 0)' 2>/dev/null || echo 0)"
    RS_CVE_CRIT="$(echo "$RS_JSON" | jq -r '.security.critical // 0' 2>/dev/null || echo 0)"
    RS_STALE_BRANCHES="$(echo "$RS_JSON" | jq -r '.branches.stale_branches // 0' 2>/dev/null || echo 0)"
    RS_AUDIT_P0="$(echo "$RS_JSON" | jq -r '.audit.p0 // 0' 2>/dev/null || echo 0)"
  fi
fi

# Current wave — primary: wave-history.jsonl tail (canonical per wave-pack-planner Rule 15).
# Fix 2026-05-21 context bloat: skip ROADMAP read (was triggering 10+ path-scoped rules per
# session start, ~150k bytes auto-loaded — see feedback_meta_context_optimization.md).
CURRENT_WAVE=""
WAVE_HISTORY=".claude/skills/quality/wave-pack-planner/data/wave-history.jsonl"
if [ -f "$WAVE_HISTORY" ] && command -v jq >/dev/null 2>&1; then
  CURRENT_WAVE="$(tail -1 "$WAVE_HISTORY" 2>/dev/null \
    | jq -r '"Wave \(.wave) — \(.outcome[0:80])"' 2>/dev/null \
    | head -c 140 || echo '')"
fi
# Fallback: mtime newest filename (legacy behavior, labeled explicitly)
if [ -z "$CURRENT_WAVE" ] && [ -d documents/03-planning/waves ]; then
  CURRENT_WAVE="(mtime fallback) $(ls -t documents/03-planning/waves/*.md 2>/dev/null | head -1 \
    | xargs -I{} basename {} .md 2>/dev/null || echo '')"
fi

# Session locks
LOCK_DIR=".claude/session-locks"
ACTIVE_LOCKS=0
LOCK_LIST=""
if [ -d "$LOCK_DIR" ]; then
  ACTIVE_LOCKS="$(find "$LOCK_DIR" -name 'session-*.lock' -mmin -240 2>/dev/null | wc -l | tr -d ' ')"
  LOCK_LIST="$(find "$LOCK_DIR" -name 'session-*.lock' -mmin -240 2>/dev/null \
    -exec basename {} \; | tr '\n' ';')"
  # Auto-purge stale locks (>4h = 240 min)
  find "$LOCK_DIR" -name 'session-*.lock' -mmin +240 -delete 2>/dev/null || true
fi

# Worktree husks under .claude/worktrees/ — agent-scratch ephemeral per
# `agent-background-spawn-default.md`; closure protocol mandates prune
# (post-wave-cleanup.md). Count for hint emission.
WT_HUSK_COUNT=0
if [ -d ".claude/worktrees" ]; then
  WT_HUSK_COUNT="$(git worktree list 2>/dev/null \
    | awk '$1 ~ /\/\.claude\/worktrees\// {print}' \
    | wc -l | tr -d ' ')"
fi

# Blocker gaps — primary path: gap-status.csv (canonical per gap-architecture-v2.md).
# Fix 2026-05-21 context bloat: switched from ROADMAP-primary to CSV-primary to eliminate
# the ROADMAP file read at session start (was triggering ~10 gap-* path-scoped rules).
# CSV is faster + more reliable + smaller trigger surface than awking ROADMAP table.
GAP_CSV="documents/04-quality/gaps/gap-status.csv"
BLOCKERS=""
if [ -f "$GAP_CSV" ]; then
  BLOCKERS="$(awk -F, '/^GAP-/ && $5=="P0" && ($4=="OPEN" || $4=="PARTIAL" || $4=="IN_PROGRESS") {print $1}' "$GAP_CSV" \
    | head -6 | tr '\n' ';' || echo '')"
fi

# Phase 1 BETA P0 count — surface for Phase 1 phương châm focus (per CLAUDE.md §CURRENT PHASE).
# Mechanical CSV count: P0 + phase-1-beta + (OPEN | PARTIAL | IN_PROGRESS).
PHASE1_BETA_P0_COUNT="?"
PHASE1_BETA_P0_PARTIAL_COUNT="?"
if [ -f "$GAP_CSV" ]; then
  PHASE1_BETA_P0_COUNT="$(awk -F, '/^GAP-/ && $5=="P0" && $7=="phase-1-beta" && ($4=="OPEN" || $4=="PARTIAL" || $4=="IN_PROGRESS")' "$GAP_CSV" | wc -l | tr -d ' ')"
  PHASE1_BETA_P0_PARTIAL_COUNT="$(awk -F, '/^GAP-/ && $5=="P0" && $7=="phase-1-beta" && $4=="PARTIAL"' "$GAP_CSV" | wc -l | tr -d ' ')"
fi

# Recent merges — last 5 squash-merges on main in past 3 days
RECENT_MERGES=""
if command -v git >/dev/null 2>&1; then
  RECENT_MERGES="$(git log main --since='3 days ago' --oneline 2>/dev/null \
    | grep -E '#[0-9]+\)?$' | head -5 | tr '\n' '§' || echo '')"
fi

# MCP servers — per .claude/rules/mcp-first-with-fallback.md prefer MCP if connected.
# Surface status here so session start sees it (avoid the 2026-04-26 anti-pattern of
# defaulting to gh CLI all session because MCP availability was never checked).
MCP_TOTAL=0
MCP_CONNECTED=0
MCP_FAILED=""
if command -v claude >/dev/null 2>&1; then
  MCP_RAW="$(claude mcp list 2>/dev/null || true)"
  # Lines look like: "name: docker run ... - ✓ Connected" or "✗ Failed to connect" or "✗ Needs authentication"
  MCP_TOTAL="$(echo "$MCP_RAW" | grep -cE '^[a-zA-Z0-9_-]+:.*-\s*[✓✗]' || echo 0)"
  MCP_CONNECTED="$(echo "$MCP_RAW" | grep -cE '✓\s*Connected' || echo 0)"
  MCP_FAILED="$(echo "$MCP_RAW" | grep -E '✗' \
    | sed -E 's/^([a-zA-Z0-9_-]+):.*$/\1/' | tr '\n' ',' | sed 's/,$//' || echo '')"
fi

# AWS Phase 1 BETA snapshot — Tier 1 read-only per .claude/rules/agent-aws-access.md.
# Cached 30 minutes in .claude/session-aws-cache/snapshot.json (gitignored).
# --no-aws skips entirely; --refresh-aws forces a re-fetch; --audit-snapshot writes
# a verification artifact per agent-aws-access.md §5.
AWS_CACHE_DIR=".claude/session-aws-cache"
AWS_CACHE_FILE="$AWS_CACHE_DIR/snapshot.json"
AWS_CACHE_TTL_SEC=1800   # 30 minutes
AWS_STATUS="skipped"     # skipped | no-cli | no-auth | cached | fresh | error
AWS_REGION_OUT="?"

aws_collect() {
  if [ "$AWS_ENABLED" != "true" ]; then
    AWS_STATUS="skipped"
    return
  fi
  if ! command -v aws >/dev/null 2>&1; then
    AWS_STATUS="no-cli"
    return
  fi
  if ! command -v jq >/dev/null 2>&1; then
    AWS_STATUS="error"
    return
  fi

  local now cache_ts cache_age
  now="$(date +%s)"
  cache_ts=0
  if [ -f "$AWS_CACHE_FILE" ]; then
    cache_ts="$(jq -r '.timestamp_epoch // 0' "$AWS_CACHE_FILE" 2>/dev/null || echo 0)"
  fi
  cache_age=$(( now - cache_ts ))

  # Fast path: cache fresh
  if [ "$AWS_REFRESH" != "true" ] && [ "$cache_age" -lt "$AWS_CACHE_TTL_SEC" ] && [ -s "$AWS_CACHE_FILE" ]; then
    AWS_STATUS="cached"
    AWS_REGION_OUT="$(jq -r '.region // "?"' "$AWS_CACHE_FILE" 2>/dev/null || echo '?')"
    return
  fi

  # Verify auth (single Tier 1 call; safe per agent-aws-access.md §2.1)
  # Use kite-readonly profile explicitly — read-only by design per §2.1.
  # User can override with AWS_PROFILE env var if they want a different read-only profile.
  local identity
  local profile_arg=""
  if [ -z "${AWS_PROFILE:-}" ]; then
    profile_arg="--profile kite-readonly"
  fi
  identity="$(timeout 5 aws sts get-caller-identity $profile_arg --output json 2>/dev/null || true)"
  if [ -z "$identity" ]; then
    AWS_STATUS="no-auth"
    return
  fi

  local region profile_for_config
  profile_for_config="${AWS_PROFILE:-kite-readonly}"
  region="$(aws configure get region --profile "$profile_for_config" 2>/dev/null || echo "${AWS_REGION:-?}")"
  AWS_REGION_OUT="$region"

  # Tier 1 fan-out — each call timeout-bounded, never throws.
  # profile_arg already set above (kite-readonly default unless AWS_PROFILE override).
  local ec2 rds alb trails alarms
  ec2="$(timeout 8 aws ec2 describe-instances $profile_arg \
    --query 'Reservations[].Instances[].{id:InstanceId,state:State.Name,name:Tags[?Key==`Name`]|[0].Value,type:InstanceType}' \
    --output json 2>/dev/null || echo '[]')"
  rds="$(timeout 8 aws rds describe-db-instances $profile_arg \
    --query 'DBInstances[].{id:DBInstanceIdentifier,state:DBInstanceStatus,class:DBInstanceClass}' \
    --output json 2>/dev/null || echo '[]')"
  alb="$(timeout 8 aws elbv2 describe-load-balancers $profile_arg \
    --query 'LoadBalancers[].{name:LoadBalancerName,state:State.Code,type:Type}' \
    --output json 2>/dev/null || echo '[]')"
  trails="$(timeout 8 aws cloudtrail describe-trails $profile_arg \
    --query 'trailList[].{name:Name,multi:IsMultiRegionTrail,home:HomeRegion}' \
    --output json 2>/dev/null || echo '[]')"

  # For each trail, check IsLogging (Tier 1 get-trail-status — returns boolean only,
  # no secret material; OK to run unconfirmed at session start)
  local trail_status="[]"
  if [ "$(echo "$trails" | jq 'length' 2>/dev/null || echo 0)" != "0" ]; then
    trail_status="$(echo "$trails" | jq -r '.[].name' | while IFS= read -r tname; do
      [ -z "$tname" ] && continue
      logging="$(timeout 5 aws cloudtrail get-trail-status --name "$tname" $profile_arg --query 'IsLogging' --output text 2>/dev/null || echo unknown)"
      printf '{"name":"%s","is_logging":"%s"}\n' "$tname" "$logging"
    done | jq -s '.' 2>/dev/null || echo '[]')"
  fi

  alarms="$(timeout 8 aws cloudwatch describe-alarms $profile_arg --state-value ALARM \
    --query 'MetricAlarms[].AlarmName' --output json 2>/dev/null || echo '[]')"

  mkdir -p "$AWS_CACHE_DIR"
  jq -n \
    --arg ts "$(date -Iseconds)" \
    --argjson tsep "$now" \
    --arg region "$region" \
    --argjson identity "$identity" \
    --argjson ec2 "$ec2" \
    --argjson rds "$rds" \
    --argjson alb "$alb" \
    --argjson trails "$trail_status" \
    --argjson alarms "$alarms" \
    '{timestamp:$ts, timestamp_epoch:$tsep, region:$region, identity:$identity,
      ec2:$ec2, rds:$rds, alb:$alb, trails:$trails, alarms_in_alarm:$alarms}' \
    > "$AWS_CACHE_FILE"
  AWS_STATUS="fresh"
}

aws_render_lines() {
  # Stdout = multi-line block; safe to interpolate into the full output. No-op
  # if cache absent or status indicates no data was collected.
  case "$AWS_STATUS" in
    skipped)  echo "  · (skipped — pass --no-aws to keep skipped, omit flag to enable)"; return ;;
    no-cli)   echo "  · (aws CLI not installed — skipping)"; return ;;
    no-auth)  echo "  · (aws not authenticated — run 'aws sts get-caller-identity' to verify)"; return ;;
    error)    echo "  · (jq missing or unexpected error)"; return ;;
  esac
  if [ ! -s "$AWS_CACHE_FILE" ]; then
    echo "  · (no cache data)"
    return
  fi

  local account ec2_total ec2_running ec2_stopped ec2_summary
  local rds_summary rds_count alb_summary alb_count
  local trail_summary alarm_count alarm_list cache_age_min cache_ts now
  account="$(jq -r '.identity.Account // "?"' "$AWS_CACHE_FILE")"
  ec2_total="$(jq '.ec2 | length' "$AWS_CACHE_FILE")"
  ec2_running="$(jq '[.ec2[] | select(.state=="running")] | length' "$AWS_CACHE_FILE")"
  ec2_stopped="$(jq '[.ec2[] | select(.state=="stopped")] | length' "$AWS_CACHE_FILE")"
  ec2_summary="$(jq -r '[.ec2[] | "\(.name // .id)=\(.state)"] | join(", ")' "$AWS_CACHE_FILE")"
  rds_count="$(jq '.rds | length' "$AWS_CACHE_FILE")"
  rds_summary="$(jq -r '[.rds[] | "\(.id)=\(.state)"] | join(", ")' "$AWS_CACHE_FILE")"
  alb_count="$(jq '.alb | length' "$AWS_CACHE_FILE")"
  alb_summary="$(jq -r '[.alb[] | "\(.name)=\(.state)"] | join(", ")' "$AWS_CACHE_FILE")"
  trail_summary="$(jq -r '[.trails[] | "\(.name)=IsLogging:\(.is_logging)"] | join(", ")' "$AWS_CACHE_FILE")"
  alarm_count="$(jq '.alarms_in_alarm | length' "$AWS_CACHE_FILE")"
  alarm_list="$(jq -r '.alarms_in_alarm | join(", ")' "$AWS_CACHE_FILE")"

  cache_ts="$(jq -r '.timestamp_epoch // 0' "$AWS_CACHE_FILE")"
  now="$(date +%s)"
  cache_age_min=$(( (now - cache_ts) / 60 ))

  cat <<EOS
  · Account/Region: $account / $AWS_REGION_OUT
  · EC2:           $ec2_running running, $ec2_stopped stopped, $ec2_total total${ec2_summary:+ — $ec2_summary}
  · RDS:           $rds_count instance(s)${rds_summary:+ — $rds_summary}
  · ALB:           $alb_count load balancer(s)${alb_summary:+ — $alb_summary}
  · CloudTrail:    ${trail_summary:-<none — audit baseline missing per aws-observability-first.md>}
  · Alarms ALARM:  $alarm_count$([ "$alarm_count" -gt 0 ] && echo " ⚠️  $alarm_list")
  · Cache:         ${AWS_STATUS} (age ${cache_age_min}m, TTL 30m)
EOS
}

aws_write_audit_artifact() {
  # Per agent-aws-access.md §5: multi-command verification → log to
  # documents/04-quality/audits/aws-verification/. Triggered only via
  # --audit-snapshot to avoid folder bloat (§5.3 ad-hoc exception).
  if [ "$AWS_AUDIT" != "true" ]; then return; fi
  if [ ! -s "$AWS_CACHE_FILE" ]; then return; fi
  case "$AWS_STATUS" in
    no-cli|no-auth|skipped|error) return ;;
  esac

  local audit_dir audit_file ymd
  audit_dir="documents/04-quality/audits/aws-verification"
  ymd="$(date +%Y-%m-%d)"
  audit_file="$audit_dir/${ymd}-session-start-snapshot.md"
  mkdir -p "$audit_dir"

  {
    cat <<EOM
---
title: AWS Verification — session-start snapshot
status: complete
created: ${ymd}
phase: post-deploy
---

# AWS Verification Report — session-start snapshot

## Scope

Periodic Phase 1 BETA stack health check captured at \`/start-session\` via \`collect-state.sh --audit-snapshot\`.
Tier 1 read-only commands per \`.claude/rules/agent-aws-access.md\` §2.1.

## Commands run

- \`aws sts get-caller-identity\`
- \`aws ec2 describe-instances\`
- \`aws rds describe-db-instances\`
- \`aws elbv2 describe-load-balancers\`
- \`aws cloudtrail describe-trails\` + \`get-trail-status\` (per trail)
- \`aws cloudwatch describe-alarms --state-value ALARM\`

## Results

\`\`\`
EOM
    aws_render_lines
    cat <<EOM
\`\`\`

Raw snapshot (gitignored): \`${AWS_CACHE_FILE}\`

## Findings

EOM
    local alarm_count
    alarm_count="$(jq '.alarms_in_alarm | length' "$AWS_CACHE_FILE" 2>/dev/null || echo 0)"
    if [ "$alarm_count" -gt 0 ]; then
      echo "- ⚠️  ${alarm_count} CloudWatch alarm(s) in ALARM state — triage required"
    else
      echo "- No active alarms; baseline healthy at capture time"
    fi
    local trails_logging_off
    trails_logging_off="$(jq -r '[.trails[] | select(.is_logging != "True")] | length' "$AWS_CACHE_FILE" 2>/dev/null || echo 0)"
    if [ "$trails_logging_off" -gt 0 ]; then
      echo "- ⚠️  ${trails_logging_off} CloudTrail trail(s) with IsLogging != True — audit blind spot"
    fi
    cat <<'EOM'

## Next steps

- If alarms in ALARM: triage via `documents/05-guides/operations/runbooks/`
- If RDS/EC2 state unexpected: `terraform plan` from `infrastructure/terraform-aws/` to check drift
- If CloudTrail not logging: `aws cloudtrail start-logging --name <trail>` per `aws-observability-first.md`
EOM
  } > "$audit_file"

  echo "  · Audit artifact: $audit_file" >&2
}

aws_collect
aws_write_audit_artifact

if [ "$MODE" = "json" ]; then
  cat <<EOF
{
  "timestamp": "$TS",
  "branch": "$BRANCH",
  "branch_state": "$BRANCH_STATE",
  "open_prs": "$OPEN_PRS",
  "top_prs": "$TOP_PRS",
  "repo_status_level": "$RS_LEVEL",
  "ci_main": "$RS_CI",
  "cve_critical": "$RS_CVE_CRIT",
  "cve_high": "$RS_CVE_HIGH",
  "stale_branches": "$RS_STALE_BRANCHES",
  "audit_p0": "$RS_AUDIT_P0",
  "current_wave": "$CURRENT_WAVE",
  "active_locks": $ACTIVE_LOCKS,
  "lock_files": "$LOCK_LIST",
  "blocker_gaps": "$BLOCKERS",
  "phase_1_beta_p0_active": "$PHASE1_BETA_P0_COUNT",
  "phase_1_beta_p0_partial": "$PHASE1_BETA_P0_PARTIAL_COUNT",
  "recent_merges": "$RECENT_MERGES",
  "mcp_total": $MCP_TOTAL,
  "mcp_connected": $MCP_CONNECTED,
  "mcp_failed": "$MCP_FAILED",
  "aws_status": "$AWS_STATUS",
  "aws_region": "$AWS_REGION_OUT",
  "aws_cache_file": "$AWS_CACHE_FILE"
}
EOF
  exit 0
fi

if [ "$MODE" = "quick" ]; then
  AWS_QUICK=""
  case "$AWS_STATUS" in
    cached|fresh)
      if [ -s "$AWS_CACHE_FILE" ] && command -v jq >/dev/null 2>&1; then
        ec2r="$(jq '[.ec2[] | select(.state=="running")] | length' "$AWS_CACHE_FILE" 2>/dev/null || echo ?)"
        alarms="$(jq '.alarms_in_alarm | length' "$AWS_CACHE_FILE" 2>/dev/null || echo ?)"
        AWS_QUICK=" · AWS: ${ec2r} EC2 running / ${alarms} alarms ($AWS_STATUS)"
      fi
      ;;
    skipped) AWS_QUICK="" ;;
    *)       AWS_QUICK=" · AWS: $AWS_STATUS" ;;
  esac
  echo "Mức: $RS_LEVEL · Nhánh: $BRANCH ($BRANCH_STATE) · PRs: $OPEN_PRS · CVE H/C: $RS_CVE_HIGH/$RS_CVE_CRIT · MCP: $MCP_CONNECTED/$MCP_TOTAL${AWS_QUICK} · Wave: ${CURRENT_WAVE:-chưa rõ}"
  exit 0
fi

# Full output — tiếng Việt per CLAUDE.md §CRITICAL Communication Language (GAP-207)
cat <<EOF
# Trạng thái session @ $TS

Nhánh:             $BRANCH ($BRANCH_STATE)
Mức repo:          $RS_LEVEL
  · CI main:       $RS_CI
  · CVE:           $RS_CVE_CRIT critical, $RS_CVE_HIGH high
  · Branches cũ:   $RS_STALE_BRANCHES
  · Audit P0:      $RS_AUDIT_P0
PRs đang mở:       $OPEN_PRS${TOP_PRS_LINES}
MCP servers:       $MCP_CONNECTED/$MCP_TOTAL connected${MCP_FAILED:+ (FAILED: $MCP_FAILED — see hint below)}
Wave hiện tại:     ${CURRENT_WAVE:-<chưa rõ — check ROADMAP.md thủ công>}
Gaps blocker:      ${BLOCKERS:-<none>}
Phase 1 BETA P0:   ${PHASE1_BETA_P0_COUNT} active (${PHASE1_BETA_P0_PARTIAL_COUNT} PARTIAL) — query: bash scripts/query-gaps.sh P0 "" phase-1-beta
Session locks:     $ACTIVE_LOCKS  [$LOCK_LIST]
Worktree husks:    $WT_HUSK_COUNT (.claude/worktrees/agent-*)$([ "$WT_HUSK_COUNT" -ge 3 ] && echo "  ⚠️  ≥3 → run: bash scripts/prune-merged-worktrees.sh --dry-run")

AWS Phase 1 BETA (Tier 1 read-only per .claude/rules/agent-aws-access.md):
$(aws_render_lines)

Merges gần đây (3 ngày):
$(echo "${RECENT_MERGES:-<none>}" | tr '§' '\n' | sed 's/^/  · /')

Ghi chú:
  · Wave primary: wave-pack-planner/data/wave-history.jsonl (Rule 15 canonical); blockers primary: gap-status.csv (Phase 4 per gap-architecture-v2.md)
  · Phase 1 BETA P0 count: query gap-status.csv (canonical); per CLAUDE.md §CURRENT PHASE focus
  · Context budget fix 2026-05-21: ROADMAP.md NO longer read at session start (saves ~150k bytes auto-loaded rules)
  · Mức repo qua scripts/repo-status.sh --json (4 yếu tố)
  · Lock dir: $LOCK_DIR (auto-purge sau 4h stale)
  · Các field cần gh — đảm bảo 'gh auth status' OK
  · MCP failed → 'docker ps' + 'docker pull ghcr.io/github/github-mcp-server' + 'claude mcp list' để reconnect.
    Per .claude/rules/mcp-first-with-fallback.md §3: nếu MCP unavailable thì fallback CLI; nhưng phải biết để swap khi fix xong.
  · AWS snapshot dùng cache 30m tại $AWS_CACHE_DIR/ (gitignored). Cờ:
    --no-aws (skip), --refresh-aws (force re-fetch), --audit-snapshot (ghi documents/04-quality/audits/aws-verification/<date>-session-start-snapshot.md).
    Per .claude/rules/agent-aws-access.md §2.1 chỉ Tier 1 read-only; §5.3 ad-hoc no-log default; §5 audit artifact when --audit-snapshot.
  · ⚠️ Wave-eligibility: trước khi /continue, Claude PHẢI check action sắp tới có ≥3 sub-tasks disjoint không.
    Nếu YES → tạo wave plan + spawn 4-5 parallel agents thay vì serial PRs.
    Refs: feedback_wave_plan_before_serial_prs.md + feedback_parallel_agent_strategy.md
    Anti-pattern 2026-04-26: GAP-229 chạy 3 phases serial (~90min) thay vì parallel (~30min).
EOF
