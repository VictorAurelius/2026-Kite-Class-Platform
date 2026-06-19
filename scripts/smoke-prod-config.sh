#!/usr/bin/env bash
# =============================================================================
# smoke-prod-config.sh — Production deploy config-parity gate (Layer 1 / G3-config)
# =============================================================================
#
# Mục đích: SAU khi deploy AWS, verify MỌI config-điểm phụ-thuộc-topology
# (cross-EC2 private IP / SG ingress / nginx /api proxy / S3 IAM-role /
# secret-fetch /etc/kite/.env / OTel) resolve ĐÚNG trên stack 3-EC2 thật.
# Biến "fix config từng cái khi deploy lòi" → "smoke → bảng PASS/FAIL → batch-fix".
#
# Mỗi check ánh xạ 1 hàng trong:
#   documents/05-guides/deploy/prod-deploy-config-registry.md §3
#
# KHI NÀO CHẠY:
#   Sau `terraform apply` (redev) + push image ECR + deploy qua SSM.
#   KHÔNG phải gate PR; đây là post-deploy gate.
#
# YÊU CẦU:
#   - AWS profile có quyền SSM SendCommand + read-only describe/get (Tier 1
#     per .claude/rules/agent-aws-access.md §2.1). KHÔNG mutation.
#   - Stack LIVE (3 EC2 running + RDS available). Khi stack stopped/teardown →
#     dùng --dry-run để self-test danh sách check (không gọi AWS).
#   - `aws` CLI + `jq` + `curl`.
#
# CẢNH BÁO: script này CHỈ read-only. SSM send-command chỉ chạy lệnh
# describe/get/curl/grep/test trên host — KHÔNG sửa state, KHÔNG sinh billable
# artifact (per .claude/rules/aws-cost-guard.md).
#
# USAGE:
#   bash scripts/smoke-prod-config.sh --dry-run                 # list 8 check, no AWS
#   bash scripts/smoke-prod-config.sh --eip 52.221.161.175 \
#       --tenant co-ha-toan                                     # live smoke
#   bash scripts/smoke-prod-config.sh --profile dev-admin \
#       --eip <ip> --tenant <slug>                              # explicit profile
#
# CATALOG-THEN-REPORT: chạy HẾT 8 check (không exit ngay lần FAIL đầu) → in
# bảng PASS/FAIL cuối → exit code = số check FAIL (để batch-fix).
#
# EXIT CODES:
#   0  = mọi check PASS (hoặc --dry-run hoàn tất)
#   N  = N check FAIL
#   2  = config invalid (thiếu --eip/--tenant cho live, hoặc thiếu aws/jq)
# =============================================================================

set -euo pipefail

# ─── Config / defaults ──────────────────────────────────────────────────────
MODE="live"
PROFILE="dev-admin"
REGION="ap-southeast-1"
EIP=""
TENANT=""
DOMAIN_ROOT="kitehub.me"

# Topology private IPs (registry §3 — hardcoded, cập nhật khi redev terraform apply).
# KH_BACKEND_IP/GATEWAY_PORT/BANNER_PORT giữ làm tài liệu topology (registry-mirror);
# checks hiện chỉ trỏ KC_APP_IP+CORE_PORT, nhưng giữ đủ bộ để Layer 2 mở rộng check.
# shellcheck disable=SC2034  # topology-doc constants, intentional
KH_BACKEND_IP="10.0.0.129"      # gateway :8080
KC_APP_IP="10.0.0.155"          # kiteclass-core :8081 + banner-renderer :3000
# shellcheck disable=SC2034
GATEWAY_PORT="8080"
CORE_PORT="8081"
# shellcheck disable=SC2034
BANNER_PORT="3000"

# Resources (registry §3)
S3_ASSETS="kitehub-assets-production"
S3_KC_FILES="kiteclass-files-production-906286017800"
ENV_FILE="/etc/kite/.env"
REQUIRED_ENV_KEYS="DB_HOST JWT_SECRET JWT_CHALLENGE_SECRET KITECLASS_CORE_URL OTEL_SDK_DISABLED"

# EC2 tag → instance lookup (dynamic, per CLAUDE.md AWS stack section + GAP-492)
TAG_KH_BACKEND="kitehub-kh-backend"
TAG_KC_APP="kitehub-kc-app"
TAG_KC_APP_FE="kitehub-kc-app-fe"

# Endpoint assumptions (FLAG: verify — see report at end)
COURSES_ENDPOINT="/api/v1/courses"            # tenant landing courses fetch (bug 2/6)
LOGIN_ENDPOINT="/api/v1/auth/login"           # smoke login (parity smoke-login-happy-path.sh)
LOGIN_USER="${SMOKE_USER:-}"
LOGIN_PASS="${SMOKE_PASS:-}"

# ─── Colors ─────────────────────────────────────────────────────────────────
if [ -t 1 ]; then
    GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; NC='\033[0m'
else
    GREEN=''; RED=''; YELLOW=''; BOLD=''; NC=''
fi

# ─── Result accumulator (catalog-then-report) ───────────────────────────────
declare -a RESULTS   # each entry: "STATUS|check-name|detail"

record() {  # $1=STATUS(PASS|FAIL|SKIP) $2=name $3=detail
    RESULTS+=("$1|$2|$3")
}

info() { echo -e "  ${YELLOW}[..]${NC} $*"; }

# ─── Arg parse ──────────────────────────────────────────────────────────────
while [ $# -gt 0 ]; do
    case "$1" in
        --dry-run) MODE="dry-run"; shift ;;
        --profile) PROFILE="$2"; shift 2 ;;
        --eip)     EIP="$2"; shift 2 ;;
        --tenant)  TENANT="$2"; shift 2 ;;
        --region)  REGION="$2"; shift 2 ;;
        -h|--help) sed -n '2,60p' "$0"; exit 0 ;;
        *) echo "Unknown arg: $1"; exit 2 ;;
    esac
done

AWS="aws --profile $PROFILE --region $REGION"

echo -e "${BOLD}=== smoke-prod-config.sh (mode=$MODE, profile=$PROFILE, region=$REGION) ===${NC}"

# ─── Dry-run: list checks, no AWS calls ─────────────────────────────────────
if [ "$MODE" = "dry-run" ]; then
    cat <<'DRYEOF'

DRY-RUN — danh sách 8 check sẽ chạy ở chế độ live (KHÔNG gọi AWS):

  1. Landing render       curl -sI https://<tenant>.kitehub.me/ → 200      [bug 1/6]
  2. /api/ proxy          GET https://<tenant>.kitehub.me/api/<courses> → JSON !503 [bug 2/6]
  3. Cross-EC2 core       SSM kh-backend: curl http://10.0.0.155:8081/actuator/health → UP [bug 1/3]
  4. Secret-fetch keys    SSM each host: test -f /etc/kite/.env + grep required keys [bug 7]
  5. S3 access            SSM: aws s3 ls <bucket> via instance-role        [bug 4]
  6. nginx /api/ blocks   SSM kc-app-fe: nginx -T | grep -c 'location /api/' >= 2 [bug 6]
  7. Login happy-path     POST <eip>/api/auth/login → 200 + JWT            [smoke tổng]
  8. OTel disabled        SSM each host: grep OTEL_SDK_DISABLED /etc/kite/.env = true [bug 8]

Live run: bash scripts/smoke-prod-config.sh --eip <ip> --tenant <slug> [--profile dev-admin]
DRYEOF
    echo ""
    echo -e "${GREEN}Dry-run complete — 8 checks listed, exit 0.${NC}"
    exit 0
fi

# ─── Live preflight: deps + required args ───────────────────────────────────
command -v aws >/dev/null 2>&1 || { echo -e "${RED}[ABORT]${NC} aws CLI not found"; exit 2; }
command -v jq  >/dev/null 2>&1 || { echo -e "${RED}[ABORT]${NC} jq not found"; exit 2; }
command -v curl >/dev/null 2>&1 || { echo -e "${RED}[ABORT]${NC} curl not found"; exit 2; }

if [ -z "$EIP" ] || [ -z "$TENANT" ]; then
    echo -e "${RED}[ABORT]${NC} live mode requires --eip <ip> and --tenant <slug>"
    echo "        (use --dry-run to self-test without a live stack)"
    exit 2
fi

# Cred check (Tier 1 — per pre-flight-aws-lifecycle-check.md §3.1)
if ! $AWS sts get-caller-identity >/dev/null 2>&1; then
    echo -e "${RED}[ABORT]${NC} aws sts get-caller-identity failed for profile '$PROFILE' — rotate creds"
    exit 2
fi

# ─── SSM helper: run a read-only shell command on a tagged instance ──────────
# Resolves instance ID by Name tag, sends command, polls invocation, echoes stdout.
# Returns 0 with stdout on Success; non-zero otherwise.
resolve_instance_id() {  # $1 = Name tag value
    $AWS ec2 describe-instances \
        --filters "Name=tag:Name,Values=$1" "Name=instance-state-name,Values=running" \
        --query 'Reservations[].Instances[].InstanceId' --output text 2>/dev/null | awk '{print $1}'
}

ssm_run() {  # $1 = Name tag, $2 = shell command → echoes stdout, returns 0 on Success
    local tag="$1" cmd="$2" iid cid status out
    iid=$(resolve_instance_id "$tag")
    if [ -z "$iid" ] || [ "$iid" = "None" ]; then
        echo "INSTANCE_NOT_FOUND"
        return 3
    fi
    cid=$($AWS ssm send-command \
        --instance-ids "$iid" \
        --document-name "AWS-RunShellScript" \
        --parameters "commands=[\"$cmd\"]" \
        --query 'Command.CommandId' --output text 2>/dev/null) || { echo "SSM_SEND_FAIL"; return 4; }

    # poll up to ~45s
    for _ in $(seq 1 15); do
        status=$($AWS ssm get-command-invocation \
            --command-id "$cid" --instance-id "$iid" \
            --query 'Status' --output text 2>/dev/null || echo "Pending")
        case "$status" in
            Success) break ;;
            Failed|Cancelled|TimedOut) break ;;
            *) sleep 3 ;;
        esac
    done
    out=$($AWS ssm get-command-invocation \
        --command-id "$cid" --instance-id "$iid" \
        --query 'StandardOutputContent' --output text 2>/dev/null || echo "")
    echo "$out"
    [ "$status" = "Success" ]
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 1 — Landing render (bug 1/6): public reachability of tenant landing
# ═════════════════════════════════════════════════════════════════════════════
check_landing_render() {
    info "Check 1: landing render https://$TENANT.$DOMAIN_ROOT/"
    local code
    code=$(curl -sS -m 15 -o /dev/null -w "%{http_code}" \
        --resolve "$TENANT.$DOMAIN_ROOT:443:$EIP" \
        "https://$TENANT.$DOMAIN_ROOT/" 2>/dev/null || echo "000")
    if [ "$code" = "200" ]; then
        record PASS "landing-render" "HTTP 200 from $TENANT.$DOMAIN_ROOT"
    else
        record FAIL "landing-render" "HTTP $code (expect 200) — check INTERNAL_API_URL / nginx / EIP DNS (registry #1/#2/#17/#20)"
    fi
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 2 — /api/ proxy (bug 2/6): courses fetch must not 503
# ═════════════════════════════════════════════════════════════════════════════
check_api_proxy() {
    info "Check 2: GET https://$TENANT.$DOMAIN_ROOT$COURSES_ENDPOINT"
    local code
    code=$(curl -sS -m 15 -o /dev/null -w "%{http_code}" \
        --resolve "$TENANT.$DOMAIN_ROOT:443:$EIP" \
        "https://$TENANT.$DOMAIN_ROOT$COURSES_ENDPOINT" 2>/dev/null || echo "000")
    # Accept 2xx (data) or 401/403 (auth-gated but routed); reject 503/502/504 (proxy/route broken) + 000
    case "$code" in
        2*|401|403) record PASS "api-proxy" "HTTP $code (routed, not 503) for $COURSES_ENDPOINT" ;;
        502|503|504) record FAIL "api-proxy" "HTTP $code — gateway→core route OR SG 8080 broken (registry #5/#9/#20)" ;;
        *) record FAIL "api-proxy" "HTTP $code (expect 2xx/401/403) — nginx /api/ proxy OR endpoint path (verify $COURSES_ENDPOINT)" ;;
    esac
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 3 — Cross-EC2 core reachable (bug 1/3): gateway→core private IP + SG 8081
# ═════════════════════════════════════════════════════════════════════════════
check_cross_ec2_core() {
    info "Check 3: SSM kh-backend → curl http://$KC_APP_IP:$CORE_PORT/actuator/health"
    local out
    out=$(ssm_run "$TAG_KH_BACKEND" \
        "curl -s -m 8 http://$KC_APP_IP:$CORE_PORT/actuator/health || echo CURL_FAIL") || true
    if echo "$out" | grep -q '"status":"UP"\|\"status\": \"UP\"\|UP'; then
        record PASS "cross-ec2-core" "kiteclass-core health UP via $KC_APP_IP:$CORE_PORT"
    elif [ "$out" = "INSTANCE_NOT_FOUND" ]; then
        record FAIL "cross-ec2-core" "kh-backend instance not found (tag $TAG_KH_BACKEND) — stack down?"
    else
        record FAIL "cross-ec2-core" "core unreachable from kh-backend — KITECLASS_CORE_URL IP OR SG self-ref 8081 (registry #1/#3); out='${out:0:80}'"
    fi
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 4 — Secret-fetch keys present (bug 7): /etc/kite/.env on each host
# ═════════════════════════════════════════════════════════════════════════════
check_secret_keys() {
    info "Check 4: /etc/kite/.env presence + required keys (kh-backend + kc-app)"
    local hosts=("$TAG_KH_BACKEND" "$TAG_KC_APP")
    local key_grep
    key_grep=$(echo "$REQUIRED_ENV_KEYS" | sed 's/ /\\|/g')
    local all_ok=1
    for h in "${hosts[@]}"; do
        local out
        out=$(ssm_run "$h" \
            "test -f $ENV_FILE && grep -cE '^($key_grep)=' $ENV_FILE || echo NOFILE") || true
        if [ "$out" = "NOFILE" ] || [ "$out" = "INSTANCE_NOT_FOUND" ]; then
            record FAIL "secret-keys[$h]" "$ENV_FILE missing OR host down (heredoc bug 7? registry #10/#19)"
            all_ok=0
        elif [ "$out" -ge 3 ] 2>/dev/null; then
            record PASS "secret-keys[$h]" "$ENV_FILE has $out/5 required keys"
        else
            record FAIL "secret-keys[$h]" "$ENV_FILE only $out required keys (heredoc stale? registry #10)"
            all_ok=0
        fi
    done
    [ "$all_ok" = "1" ] || true
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 5 — S3 access via instance-role (bug 4)
# ═════════════════════════════════════════════════════════════════════════════
check_s3_access() {
    info "Check 5: S3 ls via instance-role (kc-app→$S3_KC_FILES, kh-backend→$S3_ASSETS)"
    # kc-app → kiteclass-files bucket
    local out1 out2
    out1=$(ssm_run "$TAG_KC_APP" \
        "aws s3 ls s3://$S3_KC_FILES --region $REGION >/dev/null 2>&1 && echo OK || echo DENIED") || true
    if [ "$out1" = "OK" ]; then
        record PASS "s3-kc-files" "instance-role can ls $S3_KC_FILES"
    else
        record FAIL "s3-kc-files" "S3 ls failed ($out1) — IAM-role S3 grant missing (registry #6)"
    fi
    # kh-backend → assets bucket (branding)
    out2=$(ssm_run "$TAG_KH_BACKEND" \
        "aws s3 ls s3://$S3_ASSETS --region $REGION >/dev/null 2>&1 && echo OK || echo DENIED") || true
    if [ "$out2" = "OK" ]; then
        record PASS "s3-assets" "instance-role can ls $S3_ASSETS"
    else
        record FAIL "s3-assets" "S3 ls failed ($out2) — branding IAM-role S3 grant missing (registry #7)"
    fi
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 6 — nginx /api/ proxy in tenant + app blocks (bug 6)
# ═════════════════════════════════════════════════════════════════════════════
check_nginx_api_blocks() {
    info "Check 6: SSM kc-app-fe → nginx -T | grep -c 'location /api/' >= 2"
    local out
    out=$(ssm_run "$TAG_KC_APP_FE" \
        "nginx -T 2>/dev/null | grep -c 'location /api/' || echo 0") || true
    if [ "$out" = "INSTANCE_NOT_FOUND" ]; then
        record FAIL "nginx-api-blocks" "kc-app-fe instance not found (tag $TAG_KC_APP_FE)"
    elif [ "$out" -ge 2 ] 2>/dev/null; then
        record PASS "nginx-api-blocks" "nginx has $out 'location /api/' blocks (app + tenant wildcard)"
    else
        record FAIL "nginx-api-blocks" "only $out 'location /api/' blocks (expect >=2) — tenant OR app proxy missing (registry #9)"
    fi
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 7 — Login happy-path (smoke tổng): POST login → 200 + JWT
# ═════════════════════════════════════════════════════════════════════════════
check_login() {
    info "Check 7: POST https://$TENANT.$DOMAIN_ROOT$LOGIN_ENDPOINT"
    if [ -z "$LOGIN_USER" ] || [ -z "$LOGIN_PASS" ]; then
        record SKIP "login" "SMOKE_USER/SMOKE_PASS env not set — login check skipped (set to enable)"
        return
    fi
    local resp code body jwt
    resp=$(curl -sS -m 15 \
        --resolve "$TENANT.$DOMAIN_ROOT:443:$EIP" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$LOGIN_USER\",\"password\":\"$LOGIN_PASS\"}" \
        -w "\nHTTP_CODE:%{http_code}" \
        "https://$TENANT.$DOMAIN_ROOT$LOGIN_ENDPOINT" 2>/dev/null || echo "HTTP_CODE:000")
    code=$(echo "$resp" | grep -oE 'HTTP_CODE:[0-9]+' | cut -d: -f2)
    body=$(echo "$resp" | sed '/HTTP_CODE:/d')
    if [ "$code" != "200" ]; then
        record FAIL "login" "login HTTP $code (expect 200) — DB/secret/route (registry #13/#19)"
        return
    fi
    jwt=$(echo "$body" | grep -oE '"(access[Tt]oken|token|access_token)"[[:space:]]*:[[:space:]]*"[^"]+"' \
        | head -1 | sed -E 's/.*:[[:space:]]*"([^"]+)"/\1/')
    if [ -n "$jwt" ] && [ ${#jwt} -gt 20 ]; then
        record PASS "login" "200 + JWT (len=${#jwt})"
    else
        record FAIL "login" "200 but no JWT in body"
    fi
}

# ═════════════════════════════════════════════════════════════════════════════
# CHECK 8 — OTel disabled (bug 8): grep OTEL_SDK_DISABLED=true on each host
# ═════════════════════════════════════════════════════════════════════════════
check_otel_disabled() {
    info "Check 8: OTEL_SDK_DISABLED=true in /etc/kite/.env (kh-backend + kc-app)"
    local hosts=("$TAG_KH_BACKEND" "$TAG_KC_APP")
    for h in "${hosts[@]}"; do
        local out
        out=$(ssm_run "$h" \
            "grep -E '^OTEL_SDK_DISABLED=' $ENV_FILE 2>/dev/null | cut -d= -f2 || echo MISSING") || true
        out=$(echo "$out" | tr -d '[:space:]')
        if [ "$out" = "true" ]; then
            record PASS "otel-disabled[$h]" "OTEL_SDK_DISABLED=true"
        else
            record FAIL "otel-disabled[$h]" "OTEL_SDK_DISABLED='$out' (expect true) — log flood risk (registry #11)"
        fi
    done
}

# ─── Run all checks (catalog — never exit early) ────────────────────────────
echo ""
check_landing_render
check_api_proxy
check_cross_ec2_core
check_secret_keys
check_s3_access
check_nginx_api_blocks
check_login
check_otel_disabled

# ─── Report ─────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}=== smoke-prod-config report ===${NC}"
FAIL_N=0; PASS_N=0; SKIP_N=0
for r in "${RESULTS[@]}"; do
    st="${r%%|*}"; rest="${r#*|}"; name="${rest%%|*}"; detail="${rest#*|}"
    case "$st" in
        PASS) echo -e "  ${GREEN}[PASS]${NC} ${BOLD}$name${NC} — $detail"; PASS_N=$((PASS_N+1)) ;;
        FAIL) echo -e "  ${RED}[FAIL]${NC} ${BOLD}$name${NC} — $detail"; FAIL_N=$((FAIL_N+1)) ;;
        SKIP) echo -e "  ${YELLOW}[SKIP]${NC} ${BOLD}$name${NC} — $detail"; SKIP_N=$((SKIP_N+1)) ;;
    esac
done
echo ""
echo -e "${BOLD}Summary: $PASS_N PASS / $FAIL_N FAIL / $SKIP_N SKIP${NC}"
if [ "$FAIL_N" -gt 0 ]; then
    echo -e "${RED}→ Batch-fix các FAIL trên (cross-ref registry §3) rồi re-run smoke trước khi báo live.${NC}"
fi

exit "$FAIL_N"
