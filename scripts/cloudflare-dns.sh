#!/usr/bin/env bash
# cloudflare-dns.sh — wrapper around Cloudflare REST API cho zone kitehub.me operations.
#
# Usage:
#   bash scripts/cloudflare-dns.sh list                         # list all DNS records
#   bash scripts/cloudflare-dns.sh list-mx                      # list MX only
#   bash scripts/cloudflare-dns.sh add CNAME api kitehub-alb-... [--proxied]
#   bash scripts/cloudflare-dns.sh delete <record-id>
#   bash scripts/cloudflare-dns.sh toggle-proxy <record-name>   # DNS only ↔ Proxied
#   bash scripts/cloudflare-dns.sh origin-cert <hostnames>      # Generate Origin Cert via API
#   bash scripts/cloudflare-dns.sh zone                          # zone metadata
#
# Env requirements:
#   CLOUDFLARE_API_TOKEN          — token with Zone:DNS:Edit + Zone:Zone:Read scope
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

  origin-cert)
    hostnames="${1:-kitehub.me,*.kitehub.me}"
    echo "Generating Origin Cert for hostnames: $hostnames"
    # Generate CSR + private key locally first
    keyfile=$(mktemp -t cf-origin-key.XXXXXX)
    csrfile=$(mktemp -t cf-origin-csr.XXXXXX)
    openssl req -new -newkey rsa:2048 -nodes -keyout "$keyfile" -out "$csrfile" \
      -subj "/CN=kitehub.me" 2>&1 | head -1
    csr_content=$(cat "$csrfile" | python3 -c "import sys,json; print(json.dumps(sys.stdin.read()))")
    hosts_json=$(echo "$hostnames" | python3 -c "import sys,json; print(json.dumps(sys.stdin.read().strip().split(',')))")
    response=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
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
  toggle-proxy <name>      Switch DNS only ↔ Proxied
  origin-cert [hostnames]  Generate Cloudflare Origin Cert (default: kitehub.me,*.kitehub.me)
                           Saves .pem + .key to ~/.gcal-mcp/cloudflare-origin-cert/

Examples:
  bash scripts/cloudflare-dns.sh list
  bash scripts/cloudflare-dns.sh add CNAME staging cname.vercel-dns.com
  bash scripts/cloudflare-dns.sh toggle-proxy api.kitehub.me
  bash scripts/cloudflare-dns.sh origin-cert
EOF
    ;;
esac
