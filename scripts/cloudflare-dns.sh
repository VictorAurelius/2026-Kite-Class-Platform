#!/usr/bin/env bash
# cloudflare-dns.sh — wrapper around Cloudflare REST API cho zone kitehub.me operations.
#
# Usage:
#   bash scripts/cloudflare-dns.sh list                         # list all DNS records
#   bash scripts/cloudflare-dns.sh list-mx                      # list MX only
#   bash scripts/cloudflare-dns.sh add CNAME api kitehub-alb-... [--proxied]
#   bash scripts/cloudflare-dns.sh delete <record-id>
#   bash scripts/cloudflare-dns.sh toggle-proxy <record-name>   # DNS only <-> Proxied
#   bash scripts/cloudflare-dns.sh set-apex <ip> [--proxied|--dns-only] [--ttl <seconds>]
#                                                                # Replace apex (kitehub.me) with A record -> <ip>.
#                                                                # Deletes any existing apex A/CNAME first (idempotent).
#   bash scripts/cloudflare-dns.sh origin-cert <hostnames>      # Generate Origin Cert via API
#   bash scripts/cloudflare-dns.sh zone                          # zone metadata
#   bash scripts/cloudflare-dns.sh get-ssl-mode                  # current SSL mode
#   bash scripts/cloudflare-dns.sh set-ssl-mode <flexible|full|strict>
#   bash scripts/cloudflare-dns.sh get-always-https              # current Always Use HTTPS state
#   bash scripts/cloudflare-dns.sh set-always-https <on|off>
#
# Env requirements:
#   CLOUDFLARE_API_TOKEN          — token with Zone:DNS:Edit + Zone:Zone:Read scope
#                                   (+ Zone:SSL:Edit + Zone:Zone Settings:Edit cho ssl-mode/always-https)
#   CLOUDFLARE_ZONE_ID_KITEHUB_ME — zone ID (auto-loaded từ ~/.bashrc nếu setup per
#                                   documents/05-guides/dev/cloudflare-cli-setup.md)
#
# Per `.claude/rules/third-party-platform-automation-discovery.md` §6.3:
# Wrangler doesn't expose DNS commands → REST API via curl is the supported path.
# This script wraps common ops cho future automation (vd switch DNS only → Proxied
# khi ALB cert ready Tier 3).

set -u

API="https://api.cloudflare.com/client/v4"
TOKEN="${CLOUDFLARE_API_TOKEN:-}"
ZONE_ID="${CLOUDFLARE_ZONE_ID_KITEHUB_ME:-}"

if [ -z "$TOKEN" ] || [ -z "$ZONE_ID" ]; then
  echo "ERROR: missing env vars" >&2
  echo "Set CLOUDFLARE_API_TOKEN + CLOUDFLARE_ZONE_ID_KITEHUB_ME (per ~/.bashrc setup)" >&2
  exit 1
fi

cmd="${1:-help}"
shift || true

case "$cmd" in
  zone)
    curl -s -H "Authorization: Bearer $TOKEN" "$API/zones/$ZONE_ID" \
      | python3 -m json.tool
    ;;

  list)
    curl -s -H "Authorization: Bearer $TOKEN" "$API/zones/$ZONE_ID/dns_records?per_page=50" \
      | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(f'Zone records: {d[\"result_info\"][\"total_count\"]}')
print(f'{\"ID\":36} {\"TYPE\":6} {\"NAME\":30} {\"CONTENT\":55} PROXY')
print('-'*140)
for r in d['result']:
    proxy = '☁️ Proxied' if r['proxied'] else 'DNS only '
    print(f'{r[\"id\"]:36} {r[\"type\"]:6} {r[\"name\"][:30]:30} {str(r[\"content\"])[:55]:55} {proxy}')
"
    ;;

  list-mx|list-cname|list-txt|list-a)
    type=$(echo "$cmd" | sed 's/list-//' | tr 'a-z' 'A-Z')
    curl -s -H "Authorization: Bearer $TOKEN" "$API/zones/$ZONE_ID/dns_records?type=$type&per_page=50" \
      | python3 -m json.tool
    ;;

  add)
    type="${1:-}"; name="${2:-}"; content="${3:-}"; proxy="${4:-}"
    [ -z "$type" ] || [ -z "$name" ] || [ -z "$content" ] && { echo "Usage: $0 add <TYPE> <NAME> <CONTENT> [--proxied]"; exit 1; }
    proxied=$([ "$proxy" = "--proxied" ] && echo "true" || echo "false")
    curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      "$API/zones/$ZONE_ID/dns_records" \
      -d "{\"type\":\"$type\",\"name\":\"$name\",\"content\":\"$content\",\"proxied\":$proxied,\"ttl\":1}" \
      | python3 -m json.tool
    ;;

  delete)
    rid="${1:-}"
    [ -z "$rid" ] && { echo "Usage: $0 delete <record-id>"; exit 1; }
    curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
      "$API/zones/$ZONE_ID/dns_records/$rid" \
      | python3 -m json.tool
    ;;

  toggle-proxy)
    name="${1:-}"
    [ -z "$name" ] && { echo "Usage: $0 toggle-proxy <record-name>"; exit 1; }
    # Find record by name
    record=$(curl -s -H "Authorization: Bearer $TOKEN" "$API/zones/$ZONE_ID/dns_records?name=$name" \
      | python3 -c "import sys,json; d=json.load(sys.stdin); r=d['result'][0]; print(f'{r[\"id\"]}|{r[\"proxied\"]}|{r[\"type\"]}|{r[\"content\"]}|{r[\"name\"]}')")
    rid=$(echo "$record" | cut -d'|' -f1)
    cur_proxy=$(echo "$record" | cut -d'|' -f2)
    type=$(echo "$record" | cut -d'|' -f3)
    content=$(echo "$record" | cut -d'|' -f4)
    fname=$(echo "$record" | cut -d'|' -f5)
    new_proxy=$([ "$cur_proxy" = "True" ] && echo "false" || echo "true")
    echo "Toggle $fname ($type) from proxied=$cur_proxy → $new_proxy"
    curl -s -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      "$API/zones/$ZONE_ID/dns_records/$rid" \
      -d "{\"proxied\":$new_proxy}" \
      | python3 -c "import sys,json; d=json.load(sys.stdin); print('OK' if d.get('success') else 'ERROR:'+str(d.get('errors')))"
    ;;

  get-ssl-mode)
    curl -s -H "Authorization: Bearer $TOKEN" "$API/zones/$ZONE_ID/settings/ssl" \
      | python3 -c "import sys,json; r=json.load(sys.stdin);
print(r['result']['value']) if r.get('success') else (print('ERROR:', r.get('errors')), sys.exit(1))"
    ;;

  set-ssl-mode)
    mode="${1:-}"
    case "$mode" in
      off|flexible|full|strict) ;;
      *) echo "Usage: $0 set-ssl-mode <off|flexible|full|strict>" >&2; exit 1 ;;
    esac
    echo "Switching SSL mode → $mode"
    curl -s -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      "$API/zones/$ZONE_ID/settings/ssl" \
      -d "{\"value\":\"$mode\"}" \
      | python3 -c "import sys,json; r=json.load(sys.stdin);
print('OK — SSL mode now:', r['result']['value']) if r.get('success') else (print('ERROR:', r.get('errors')), sys.exit(1))"
    ;;

  get-always-https)
    curl -s -H "Authorization: Bearer $TOKEN" "$API/zones/$ZONE_ID/settings/always_use_https" \
      | python3 -c "import sys,json; r=json.load(sys.stdin);
print(r['result']['value']) if r.get('success') else (print('ERROR:', r.get('errors')), sys.exit(1))"
    ;;

  set-always-https)
    state="${1:-}"
    case "$state" in
      on|off) ;;
      *) echo "Usage: $0 set-always-https <on|off>" >&2; exit 1 ;;
    esac
    echo "Switching Always Use HTTPS → $state"
    curl -s -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      "$API/zones/$ZONE_ID/settings/always_use_https" \
      -d "{\"value\":\"$state\"}" \
      | python3 -c "import sys,json; r=json.load(sys.stdin);
print('OK — Always Use HTTPS now:', r['result']['value']) if r.get('success') else (print('ERROR:', r.get('errors')), sys.exit(1))"
    ;;

  set-apex)
    # Atomic apex record replacement: delete any existing apex A/CNAME, create
    # new A record pointing to <ip>. Idempotent: re-running with same IP is a
    # no-op aside from possible TTL/proxied change.
    #
    # Usage: set-apex <ip> [--proxied|--dns-only] [--ttl <seconds>]
    target_ip="${1:-}"
    [ -z "$target_ip" ] && { echo "Usage: $0 set-apex <ip> [--proxied|--dns-only] [--ttl <seconds>]" >&2; exit 1; }
    shift
    # Validate IPv4
    if ! echo "$target_ip" | grep -qE '^([0-9]{1,3}\.){3}[0-9]{1,3}$'; then
      echo "ERROR: '$target_ip' is not a valid IPv4 address" >&2
      exit 1
    fi
    proxied="false"
    ttl="60"
    while [ $# -gt 0 ]; do
      case "$1" in
        --proxied) proxied="true"; shift ;;
        --dns-only) proxied="false"; shift ;;
        --ttl) ttl="${2:-60}"; shift 2 ;;
        *) echo "Unknown flag: $1" >&2; exit 1 ;;
      esac
    done
    # Cloudflare requires ttl=1 when proxied=true (Auto). For dns-only, ttl >= 60.
    if [ "$proxied" = "true" ]; then
      ttl_to_send=1
    else
      ttl_to_send="$ttl"
    fi
    zone_name=$(curl -s -H "Authorization: Bearer $TOKEN" "$API/zones/$ZONE_ID" \
      | python3 -c "import sys,json; print(json.load(sys.stdin)['result']['name'])")
    echo "Target apex: $zone_name -> A $target_ip (proxied=$proxied, ttl=$ttl_to_send)"

    # Pre-flight: capture current apex state for audit trail
    echo "--- Current apex records (pre-flip) ---"
    curl -s -H "Authorization: Bearer $TOKEN" \
      "$API/zones/$ZONE_ID/dns_records?name=$zone_name&type=A" \
      | python3 -c "import sys,json; d=json.load(sys.stdin); [print(f\"  A    id={r['id']} content={r['content']} proxied={r['proxied']} ttl={r['ttl']}\") for r in d.get('result',[])]"
    curl -s -H "Authorization: Bearer $TOKEN" \
      "$API/zones/$ZONE_ID/dns_records?name=$zone_name&type=CNAME" \
      | python3 -c "import sys,json; d=json.load(sys.stdin); [print(f\"  CNAME id={r['id']} content={r['content']} proxied={r['proxied']} ttl={r['ttl']}\") for r in d.get('result',[])]"

    # Find existing apex A or CNAME records to delete (collect IDs)
    existing_ids=$(curl -s -H "Authorization: Bearer $TOKEN" \
      "$API/zones/$ZONE_ID/dns_records?name=$zone_name" \
      | python3 -c "
import sys,json
d=json.load(sys.stdin)
ids=[r['id'] for r in d.get('result',[]) if r.get('type') in ('A','CNAME','AAAA')]
print(' '.join(ids))
")

    # Delete each existing apex A/CNAME/AAAA record
    if [ -n "$existing_ids" ]; then
      for rid in $existing_ids; do
        echo "Deleting existing apex record id=$rid"
        curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
          "$API/zones/$ZONE_ID/dns_records/$rid" \
          | python3 -c "import sys,json; d=json.load(sys.stdin); print('  OK' if d.get('success') else '  ERROR:'+str(d.get('errors')))"
      done
    else
      echo "No existing apex A/CNAME/AAAA found"
    fi

    # Create new A record
    echo "Creating apex A record"
    create_result=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      "$API/zones/$ZONE_ID/dns_records" \
      -d "{\"type\":\"A\",\"name\":\"$zone_name\",\"content\":\"$target_ip\",\"proxied\":$proxied,\"ttl\":$ttl_to_send}")
    echo "$create_result" | python3 -c "
import sys,json
d=json.load(sys.stdin)
if d.get('success'):
  r=d['result']
  print(f\"  OK id={r['id']} content={r['content']} proxied={r['proxied']} ttl={r['ttl']}\")
else:
  print('  ERROR:'+str(d.get('errors')))
  sys.exit(1)
"
    echo "--- Post-flip verification ---"
    sleep 2
    curl -s -H "Authorization: Bearer $TOKEN" \
      "$API/zones/$ZONE_ID/dns_records?name=$zone_name&type=A" \
      | python3 -c "
import sys,json,os
d=json.load(sys.stdin)
target=os.environ.get('TARGET_IP','')
for r in d.get('result',[]):
  match='MATCH' if r['content']==target else 'MISMATCH'
  print(f\"  A id={r['id']} content={r['content']} proxied={r['proxied']} ttl={r['ttl']} [{match}]\")
" TARGET_IP="$target_ip"
    ;;

  origin-cert)
    # Origin CA endpoint requires SEPARATE token với permissions:
    # - Account: SSL and Certificates: Edit (account-level)
    # - Zone: Zone: Read (so Cloudflare can validate hostnames belong to your account)
    # Both scopes mandatory in SAME token. Single account-only OR zone-only token will fail.
    # See documents/05-guides/dev/cloudflare-cli-setup.md §2.5 for token creation.
    # Alternative: Cloudflare Dashboard → SSL/TLS → Origin Server → Create Certificate (1 click).
    ORIGIN_CA_TOKEN="${CLOUDFLARE_API_TOKEN_ORIGIN_CA:-}"
    if [ -z "$ORIGIN_CA_TOKEN" ]; then
      echo "ERROR: CLOUDFLARE_API_TOKEN_ORIGIN_CA env not set" >&2
      echo "" >&2
      echo "Recommended: use Dashboard manual instead — 1 click vs token setup" >&2
      echo "  https://dash.cloudflare.com/<account>/kitehub.me/ssl-tls/origin" >&2
      echo "" >&2
      echo "If you want CLI: create token with BOTH scopes:" >&2
      echo "  - Account: SSL and Certificates: Edit" >&2
      echo "  - Zone: Zone: Read (specific zone kitehub.me)" >&2
      exit 1
    fi
    hostnames="${1:-kitehub.me,*.kitehub.me}"
    echo "Generating Origin Cert for hostnames: $hostnames"
    keyfile=$(mktemp -t cf-origin-key.XXXXXX)
    csrfile=$(mktemp -t cf-origin-csr.XXXXXX)
    openssl req -new -newkey rsa:2048 -nodes -keyout "$keyfile" -out "$csrfile" \
      -subj "/CN=kitehub.me" 2>&1 | head -1
    csr_content=$(cat "$csrfile" | python3 -c "import sys,json; print(json.dumps(sys.stdin.read()))")
    hosts_json=$(echo "$hostnames" | python3 -c "import sys,json; print(json.dumps(sys.stdin.read().strip().split(',')))")
    response=$(curl -s -X POST -H "Authorization: Bearer $ORIGIN_CA_TOKEN" -H "Content-Type: application/json" \
      "$API/certificates" \
      -d "{\"hostnames\":$hosts_json,\"requested_validity\":5475,\"request_type\":\"origin-rsa\",\"csr\":$csr_content}")
    cert=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['result']['certificate'] if d.get('success') else 'ERROR:'+str(d.get('errors')))")
    if [[ "$cert" == ERROR* ]]; then
      echo "$cert"
      rm -f "$keyfile" "$csrfile"
      exit 1
    fi
    outdir="$HOME/.gcal-mcp/cloudflare-origin-cert"
    mkdir -p "$outdir"
    chmod 700 "$outdir"
    echo "$cert" > "$outdir/kitehub.me.pem"
    cp "$keyfile" "$outdir/kitehub.me.key"
    chmod 600 "$outdir"/*
    rm -f "$keyfile" "$csrfile"
    echo "✓ Origin Cert saved:"
    ls -la "$outdir/"
    echo ""
    echo "Validity: 15 years (5475 days)"
    echo "Hostnames: $hostnames"
    echo ""
    echo "Future use (when ALB ready):"
    echo "  aws acm import-certificate \\"
    echo "    --certificate fileb://$outdir/kitehub.me.pem \\"
    echo "    --private-key fileb://$outdir/kitehub.me.key \\"
    echo "    --region ap-southeast-1"
    ;;

  help|*)
    cat <<EOF
cloudflare-dns.sh — Cloudflare REST API wrapper for kitehub.me

Commands:
  zone                     Show zone metadata
  list                     List all DNS records
  list-mx | list-cname | list-txt | list-a   Filter by type
  add <TYPE> <NAME> <CONTENT> [--proxied]    Add new record
  delete <record-id>       Remove record
  toggle-proxy <name>      Switch DNS only <-> Proxied
  set-apex <ip> [--proxied|--dns-only] [--ttl <s>]
                           Atomic apex (kitehub.me) A-record swap. Deletes any
                           existing apex A/CNAME/AAAA, creates A -> <ip>.
                           Default proxied=false, ttl=60.
  origin-cert [hostnames]  Generate Cloudflare Origin Cert (default: kitehub.me,*.kitehub.me)
                           Saves .pem + .key to ~/.gcal-mcp/cloudflare-origin-cert/
  get-ssl-mode             Show current SSL mode (off/flexible/full/strict)
  set-ssl-mode <mode>      Set SSL mode — Tier 3 §6 needs 'strict'
  get-always-https         Show Always Use HTTPS state (on/off)
  set-always-https <on|off>  Toggle Always Use HTTPS — Tier 3 §7 needs 'on'

Examples:
  bash scripts/cloudflare-dns.sh list
  bash scripts/cloudflare-dns.sh add CNAME staging cname.vercel-dns.com
  bash scripts/cloudflare-dns.sh toggle-proxy api.kitehub.me
  bash scripts/cloudflare-dns.sh set-apex 13.228.25.147                # dns-only A apex
  bash scripts/cloudflare-dns.sh set-apex 13.228.25.147 --proxied      # proxied A apex (orange cloud)
  bash scripts/cloudflare-dns.sh origin-cert
  bash scripts/cloudflare-dns.sh set-ssl-mode strict     # Tier 3 §6
  bash scripts/cloudflare-dns.sh set-always-https on     # Tier 3 §7
EOF
    ;;
esac
