#!/usr/bin/env bash
# =========================================================================
# backup-production.sh — Pre-deploy AWS RDS snapshot (GAP-389-A, Wave 36)
# =========================================================================
# Wraps `aws rds create-db-snapshot` for production database, idempotent +
# timestamp-tagged so it is safe to re-run from CI gate or manual coordinator
# invocation. Emits Prometheus-text metric lines (counter + gauge) so an
# external scrape (e.g. pushgateway) can record successful pre-deploy backups
# AND drive the `BackupJobFailure` alert.
#
# Usage:
#   ./scripts/backup-production.sh [--dry-run]
#
# Environment variables (override defaults):
#   AWS_REGION            — default: ap-southeast-1
#   RDS_INSTANCE_ID       — default: kite-rds-prod
#   SNAPSHOT_PREFIX       — default: kite-prod-pre-deploy
#   PUSHGATEWAY_URL       — optional; if set, metrics pushed via curl
#   BACKUP_WAIT_TIMEOUT   — default: 1200 (20 min)
#
# Exit codes:
#   0 = snapshot created (or dry-run OK)
#   1 = AWS CLI failure / timeout
#   2 = pre-flight failed (missing aws CLI, missing creds, missing instance)
#
# Standards:
#   - AWS Well-Architected Reliability pillar (REL-09 backup automation)
#   - release-deploy-standard.md §3.1 "Database backup taken pre-deploy"
#   - release-deploy-standard.md §3.4 (MAJOR / first PRODUCTION)
#   - logs-format-standard.md (structured key=value lines for SRE parsing)
#
# References:
#   - GAP-389 Wave 33 Ops P1 cluster (initial alert + script)
#   - GAP-430 Wave 41 Bucket A (this fix — alert/metric name alignment)
#   - .github/workflows/deploy-production.yml — pre-deploy CI gate
#   - documents/05-guides/operations/runbooks/backup-job-failure.md — runbook
# =========================================================================

set -euo pipefail

# ─── Config ────────────────────────────────────────────────────────────

AWS_REGION="${AWS_REGION:-ap-southeast-1}"
RDS_INSTANCE_ID="${RDS_INSTANCE_ID:-kite-rds-prod}"
SNAPSHOT_PREFIX="${SNAPSHOT_PREFIX:-kite-prod-pre-deploy}"
PUSHGATEWAY_URL="${PUSHGATEWAY_URL:-}"
BACKUP_WAIT_TIMEOUT="${BACKUP_WAIT_TIMEOUT:-1200}"

DRY_RUN="false"
if [ "${1:-}" = "--dry-run" ]; then
    DRY_RUN="true"
fi

# Colors (disabled if not a terminal)
if [ -t 1 ]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    YELLOW='\033[1;33m'
    NC='\033[0m'
    BOLD='\033[1m'
else
    GREEN='' RED='' YELLOW='' NC='' BOLD=''
fi

log_info()  { printf '%sINFO%s  %s\n' "$GREEN" "$NC" "$*" >&2; }
log_warn()  { printf '%sWARN%s  %s\n' "$YELLOW" "$NC" "$*" >&2; }
log_error() { printf '%sERROR%s %s\n' "$RED" "$NC" "$*" >&2; }

# ─── Pre-flight ────────────────────────────────────────────────────────

preflight() {
    if [ "$DRY_RUN" = "true" ]; then
        log_info "Pre-flight (dry-run mode — skipping AWS reachability checks)"
        return 0
    fi

    if ! command -v aws >/dev/null 2>&1; then
        log_error "aws CLI not installed. Install per https://docs.aws.amazon.com/cli/"
        exit 2
    fi

    if ! aws sts get-caller-identity --region "$AWS_REGION" >/dev/null 2>&1; then
        log_error "AWS credentials not configured or invalid for region $AWS_REGION"
        exit 2
    fi

    if ! aws rds describe-db-instances \
            --db-instance-identifier "$RDS_INSTANCE_ID" \
            --region "$AWS_REGION" >/dev/null 2>&1; then
        log_error "RDS instance '$RDS_INSTANCE_ID' not found in region $AWS_REGION"
        exit 2
    fi

    log_info "Pre-flight OK — region=$AWS_REGION instance=$RDS_INSTANCE_ID"
}

# ─── Snapshot ──────────────────────────────────────────────────────────

create_snapshot() {
    local timestamp
    timestamp=$(date -u +%Y%m%d-%H%M%S)
    local snapshot_id="${SNAPSHOT_PREFIX}-${timestamp}"

    log_info "Snapshot ID: $snapshot_id"

    if [ "$DRY_RUN" = "true" ]; then
        log_info "DRY-RUN — would invoke: aws rds create-db-snapshot \\"
        log_info "    --db-instance-identifier $RDS_INSTANCE_ID \\"
        log_info "    --db-snapshot-identifier $snapshot_id \\"
        log_info "    --region $AWS_REGION"
        log_info "DRY-RUN — would wait until snapshot available (timeout ${BACKUP_WAIT_TIMEOUT}s)"
        log_info "DRY-RUN — would emit counter kite_backup_snapshots_total{type=\"pre_deploy\"} +1"
        log_info "DRY-RUN — would emit gauge kite_backup_last_success_timestamp_seconds{type=\"pre_deploy\"} = \$(date +%s)"
        echo "$snapshot_id"
        return 0
    fi

    if ! aws rds create-db-snapshot \
            --db-instance-identifier "$RDS_INSTANCE_ID" \
            --db-snapshot-identifier "$snapshot_id" \
            --tags Key=Source,Value=pre-deploy-ci Key=DeployTimestamp,Value="$timestamp" \
            --region "$AWS_REGION" >/dev/null; then
        log_error "create-db-snapshot failed for $snapshot_id"
        exit 1
    fi

    log_info "Snapshot creation initiated; waiting (timeout ${BACKUP_WAIT_TIMEOUT}s)"

    if ! timeout "$BACKUP_WAIT_TIMEOUT" aws rds wait db-snapshot-available \
            --db-snapshot-identifier "$snapshot_id" \
            --region "$AWS_REGION"; then
        log_error "Snapshot $snapshot_id did not reach 'available' within ${BACKUP_WAIT_TIMEOUT}s"
        exit 1
    fi

    log_info "Snapshot $snapshot_id is available"
    echo "$snapshot_id"
}

# ─── Metric emission ──────────────────────────────────────────────────
#
# Emits TWO metrics so observability is coherent (GAP-430 Wave 41 Bucket A):
#   1. `kite_backup_snapshots_total` (counter) — historical record of how many
#      successful pre-deploy snapshots have been taken.
#   2. `kite_backup_last_success_timestamp_seconds` (gauge) — wall-clock time of
#      most recent success. THIS is what the `BackupJobFailure` alert watches
#      (PromQL: `time() - kite_backup_last_success_timestamp_seconds > 90000`),
#      so emitting it from this script is what makes the alert actually fire.
#
# Both are pushed in the same Pushgateway request so alert + counter stay in
# lockstep. Without the gauge, the alert was silent for any failure (Wave 40
# audit Bucket E P0 finding).

emit_metrics() {
    local snapshot_id="$1"

    # Counter — one increment per successful pre-deploy snapshot.
    local counter_metric="kite_backup_snapshots_total{type=\"pre_deploy\",region=\"${AWS_REGION}\",instance=\"${RDS_INSTANCE_ID}\"} 1"

    # Gauge — wall-clock seconds at success. Watched by `BackupJobFailure` alert.
    local now_seconds
    now_seconds=$(date +%s)
    local gauge_metric="kite_backup_last_success_timestamp_seconds{type=\"pre_deploy\",region=\"${AWS_REGION}\",instance=\"${RDS_INSTANCE_ID}\"} ${now_seconds}"

    log_info "Metric (counter): $counter_metric (snapshot=$snapshot_id)"
    log_info "Metric (gauge):   $gauge_metric"

    if [ -n "$PUSHGATEWAY_URL" ] && [ "$DRY_RUN" != "true" ]; then
        if command -v curl >/dev/null 2>&1; then
            local job_url="${PUSHGATEWAY_URL%/}/metrics/job/backup-production/instance/${RDS_INSTANCE_ID}"
            if printf "%s\n" \
                    "# TYPE kite_backup_snapshots_total counter" \
                    "$counter_metric" \
                    "# TYPE kite_backup_last_success_timestamp_seconds gauge" \
                    "$gauge_metric" \
                    | curl --max-time 10 --silent --show-error \
                        --data-binary @- "$job_url" >/dev/null; then
                log_info "Pushed counter + gauge to gateway: $job_url"
            else
                log_warn "Push to $job_url failed (non-fatal) — alert may stay stale"
            fi
        else
            log_warn "curl unavailable — metrics logged only (no push)"
        fi
    elif [ -z "$PUSHGATEWAY_URL" ]; then
        log_info "PUSHGATEWAY_URL unset — metrics logged only (scraper may tail logs)"
    fi
}

# ─── Main ──────────────────────────────────────────────────────────────

main() {
    printf '%sbackup-production.sh — pre-deploy RDS snapshot%s\n' "$BOLD" "$NC" >&2
    printf '  region=%s instance=%s prefix=%s dry_run=%s\n' \
        "$AWS_REGION" "$RDS_INSTANCE_ID" "$SNAPSHOT_PREFIX" "$DRY_RUN" >&2
    echo "═══════════════════════════════════════════════════════════════" >&2

    preflight
    local snapshot_id
    snapshot_id=$(create_snapshot)
    emit_metrics "$snapshot_id" >&2

    echo "═══════════════════════════════════════════════════════════════" >&2
    log_info "DONE — snapshot=$snapshot_id"
    # stdout = snapshot id (machine-readable for CI gate consumers)
    echo "$snapshot_id"
    exit 0
}

main "$@"
