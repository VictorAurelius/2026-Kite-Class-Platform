# GAP-417: setup.sh JWT_SECRET unquoted breaks .env parse

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟡 P2 (dev-friction; first-time-setup blocker until manual fix)
**Domain:** DevOps / Local dev tooling
**Found:** 2026-05-07 (Option B' real-backend E2E session)
**Affects:** First-time `bash kitehub/scripts/setup.sh` run on any new dev machine

## Problem

`scripts/setup.sh` generates `.env` with secrets via `openssl rand`. JWT_SECRET line:

```bash
JWT_SECRET=$(openssl rand -base64 64)
```

`openssl rand -base64 64` outputs **76+ chars including `+/=` and a trailing newline**. The newline gets embedded into `.env` as a line break, so the resulting file looks like:

```
JWT_SECRET=Kq6NqG5ftSRnyKVlL2PClB0nO4dhEBT0jsl1LIyPxzQSXSUP70oNGAdniqKxIlE/
FqXo/qnAZaqwQAuSaj33Gw==
INTERNAL_API_SECRET=...
```

`docker-compose` rejects this with:
```
failed to read .../kitehub/.env: line 22: unexpected character "/" in variable name "FqXo/qnAZaqwQAuSaj33Gw=="
```

→ `bash scripts/up.sh` fails immediately on first-time setup.

## Root Cause

- `openssl rand -base64 N` always wraps output at 64-char boundary with `\n`
- Heredoc captures the literal newline
- `.env` parser treats post-newline content as a new var name

## Reproduction

```bash
rm kitehub/.env
bash kitehub/scripts/setup.sh
bash kitehub/scripts/up.sh --profile infra-only
# → ENV parse error
```

## Proposed Fix

Strip newlines + base64 chars that conflict with shell:

```bash
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n=/+')
```

Or quote in heredoc + escape:

```bash
cat > "$ENV_FILE" <<EOF
JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
...
EOF
```

Apply same defensive `tr -d '\n'` to ENCRYPTION_MASTER_KEY (also `openssl rand -base64`).

Test on fresh machine: `rm .env && setup.sh && up.sh --profile infra-only` → must succeed first try.

## Acceptance Criteria

- [x] `kitehub/scripts/setup.sh` JWT_SECRET + ENCRYPTION_MASTER_KEY commands strip `\n`
- [x] `.env` file parses cleanly (no multi-line values)
- [x] Self-test: `rm kitehub/.env && bash kitehub/scripts/setup.sh && grep -c '^[A-Z_]*=' kitehub/.env` returns expected var count (no orphan continuation lines)
- [x] Add comment in setup.sh referencing this gap

## Related

- Surfaced 2026-05-07 Option B' session (PR #951 dev-stack fixes)
- Workaround applied manually session 2026-05-07: `JWT_NEW=$(openssl rand -base64 64 | tr -d '\n=/+'); sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_NEW}|" .env`

## Log

- **2026-05-07** DONE — fix shipped in dev-stack cluster PR. `setup.sh:36-39` adds `tr -d '\n=/+'` to ENCRYPTION_KEY + JWT_SECRET; gap-referencing comment added explaining the openssl base64 wrap behavior. Self-test verified inline: synthesized `.env` parses cleanly with `grep -c '^[A-Z_]*='` returning expected var count (no orphan continuation lines from embedded newlines).
