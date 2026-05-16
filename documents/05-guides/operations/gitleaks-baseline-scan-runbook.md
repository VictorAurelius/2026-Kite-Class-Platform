# Gitleaks Baseline Scan Runbook

**Status:** active
**Last Updated:** 2026-05-16
**Wave origin:** 86 Bucket E Fix 2
**Owner:** Security ops / on-call

Step-by-step runbook to (a) reproduce the Wave 86 baseline scan, (b) triage new findings, (c) update `.gitleaks.toml` allowlist when justified.

---

## 1. Pre-requisites

- `gitleaks` v8.x (CLI). Install locally:

```bash
cd /tmp
curl -sLo gitleaks.tar.gz https://github.com/gitleaks/gitleaks/releases/download/v8.21.2/gitleaks_8.21.2_linux_x64.tar.gz
tar xzf gitleaks.tar.gz
./gitleaks version  # expect 8.21.2 or later
```

Or via package manager:

```bash
# macOS
brew install gitleaks

# Ubuntu (via go)
go install github.com/gitleaks/gitleaks/v8@latest
```

- Repository cloned with full history (`git clone` not `--depth 1`).

---

## 2. Reproduce baseline scan

From repo root:

```bash
# Full scan with project config — should exit 0 (zero leaks)
gitleaks detect --source . --config .gitleaks.toml --verbose --redact
echo "exit=$?"
```

Expected output:
```
INF 2173+ commits scanned.
INF scan completed in ~6s
INF no leaks found
exit=0
```

If non-zero exit → new finding. Triage per §4.

---

## 3. CI integration

Workflow: `.github/workflows/gitleaks-scan.yml` — uses `gitleaks/gitleaks-action@v2`.

Trigger:
- PR to main touching `**/*.{java,ts,tsx,js,yml,yaml,json,env*,sh,py}`, `Dockerfile*`, `.gitleaks.toml`
- Push to main

Failure → PR comment + CI fail. Author fixes by either removing secret OR updating `.gitleaks.toml` per §5.

---

## 4. Triage new finding

When CI flags a finding:

1. **Read the finding** — open PR comment from gitleaks-action OR re-run locally with `--verbose` (no `--redact`) to see actual matched string.

2. **Classify** as one of:
   - **TRUE positive** — real secret accidentally committed. STOP. Rotate immediately per `credential-rotation-runbook.md`, then commit removal in same PR.
   - **FALSE positive — test fixture** — explicitly placeholder secret in `*Test*.java` / `*test*.yml` / `__tests__/*.tsx`. Add path to `.gitleaks.toml` `[allowlist].paths`.
   - **FALSE positive — documentation sample** — JWT/token shown in `api-contract.md` for FE consumers. Add path or regex pattern to allowlist.
   - **FALSE positive — placeholder string** — `REPLACE_WITH_BASE64` / `<YOUR_KEY_HERE>`. Already covered by `[allowlist].regexes`; verify.
   - **TRUE positive — rotated key documented** — old AWS Access Key ID visible in audit doc. AccessKeyId (AKIA…) is PUBLIC, not secret; secret = `aws_secret_access_key` 40-char string. If only the ID, allowlist. If the secret part visible, ROTATE + scrub doc.

3. **For TRUE positive** — rotate before merge. Follow `credential-rotation-runbook.md`. Do NOT just `git rm`; secret is in git history. Use `git filter-repo` only if rotation isn't sufficient (rarely needed).

---

## 5. Update allowlist (.gitleaks.toml)

For false positives:

```bash
# Edit .gitleaks.toml
# Add path under [allowlist].paths:
'''path/to/test/file\.java''',

# OR regex pattern under [allowlist].regexes:
'''DOCUMENTED_SAMPLE_TOKEN_PREFIX_.*''',

# Re-run to verify
gitleaks detect --source . --config .gitleaks.toml
```

Document the addition in `.gitleaks.toml` header comment block citing the wave/gap/PR reason.

---

## 6. Baseline result (Wave 86, 2026-05-16)

Initial scan: **108 findings → all 108 allowlisted as false positives**, zero TRUE leaks.

| Rule | Count | Triage |
|------|-------|--------|
| `generic-api-key` | 90 | Sample JWT/token in `documents/01-business/*/api-contract.md` + test fixtures + archived docs |
| `aws-access-token` | 13 | AccessKeyId (AKIA…) values in `aws-verification/*.md` audit docs documenting rotated readonly key (AKID is public, secret already rotated) |
| `kubernetes-secret-yaml` | 2 | `secrets.yaml` placeholder `REPLACE_WITH_BASE64` |
| `curl-auth-header` / `curl-auth-user` | 3 | Documentation samples in archived `deployment-guide.md` |

**Verdict:** Zero TRUE leaks. Pass Phase 1 BETA gate.

---

## 7. Cadence

- **Pre-merge:** Every PR matching path filter triggers gitleaks-action
- **Periodic full repo:** Quarterly — run `gitleaks detect` against `main` + audit allowlist drift; remove allowlist entries that no longer point to files (false positives that got fixed)
- **Post-incident:** If credential compromise suspected, run full scan + `git log -p -S "<known-prefix>"` to find unauthorized commits

---

## 8. References

- Gitleaks repo: https://github.com/gitleaks/gitleaks
- gitleaks-action: https://github.com/gitleaks/gitleaks-action
- Wave 86 baseline audit: `documents/04-quality/audits/security/2026-05-16-wave-86-gitleaks-baseline.md`
- Config: `.gitleaks.toml` (repo root)
- Workflow: `.github/workflows/gitleaks-scan.yml`
- Related: `credential-rotation-runbook.md`, `.claude/rules/pre-launch-secrets-hardening-checklist.md`
