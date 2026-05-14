# JWT Signing Secret Rotation Runbook

**Status:** active
**Scope:** `kitehub-subscription` JWT signing key (HS256)
**Related:** GAP-520 (Wave 72a), `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.6
**Cadence:** quarterly + on-incident (credential leak, employee departure with access to secret, regulator request)

## 1. What this rotates

The `JWT_SECRET` env var that `kitehub-subscription` uses to sign all KiteHub
access + refresh tokens. Rotation must avoid forcing every logged-in tenant to
re-login, so the service supports a **dual-key window**: the previous secret
keeps verifying old tokens while the current secret signs new ones.

Token TTLs (per `pre-launch-auth-hardening-checklist.md` §2.8):

- Access token: 24h (legacy; target ≤15min)
- Refresh token: 7d

The rotation window must therefore stay open for ≥ refresh-token TTL (7d) so
that no refresh-token issued before the cutover is forcibly invalidated.

## 2. AWS Secrets Manager layout

| Secret name | Purpose |
|---|---|
| `kitehub/production/jwt-signing-secret` | The CURRENT signing key. Stored as `AWSCURRENT` version label. |
| (versioned history) | Older versions retained automatically; pull the immediate predecessor as the PREVIOUS key during rotation. |

Each rotation creates a NEW version of the same secret. The version-history
itself is the audit trail.

## 3. Rotation steps

### 3.1 Generate the new secret

```bash
NEW_SECRET=$(openssl rand -base64 64 | tr -d '\n')
echo "New secret length: ${#NEW_SECRET} chars"
```

Confirm `${#NEW_SECRET} >= 32`. If shorter (shouldn't happen with `-base64 64`)
regenerate before continuing.

### 3.2 Capture the current secret (it becomes the PREVIOUS during the window)

```bash
PREV_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id kitehub/production/jwt-signing-secret \
  --query SecretString --output text)
```

Store `$PREV_SECRET` somewhere outside CloudShell history for the duration of
the rotation window. Treat with the same care as the new secret.

### 3.3 Stage the new secret as a new Secrets Manager version

```bash
aws secretsmanager put-secret-value \
  --secret-id kitehub/production/jwt-signing-secret \
  --secret-string "$NEW_SECRET"
```

This bumps `AWSCURRENT` to the new value and tags the prior value as `AWSPREVIOUS`.

### 3.4 Update the production env to run dual-key mode

For each EC2 host running `kitehub-subscription` (or via terraform/ECS task
definition update), set:

- `JWT_SECRET_CURRENT=$NEW_SECRET`
- `JWT_SECRET_PREVIOUS=$PREV_SECRET`

Restart the service. On boot, the logs should show:

```
JWT dual-key mode ACTIVE — current + previous (rotation window)
```

If they say `JWT single-key mode`, env vars did not propagate — re-check.

### 3.5 Verify

```bash
# A token issued BEFORE the rotation should still validate via /api/v1/auth/refresh.
# A token issued AFTER the rotation should validate normally.
# Both should yield a fresh access+refresh pair signed with the new key.

# Tail metrics — confirm fallback counter is incrementing for old refresh calls:
curl -s https://api.kitehub.me/actuator/prometheus | grep jwt_verify_fallback_total
```

The `jwt.verify.fallback` counter increments every time a token is verified
against the previous key. During the rotation window this number should rise
and then plateau as old tokens expire.

### 3.6 Close the rotation window (after refresh-token TTL has elapsed)

Set a calendar reminder for **7+ days** after step 3.4. Then:

1. Confirm `jwt.verify.fallback` counter has stopped incrementing (last
   increment timestamp > 6 hours ago).
2. Remove the `JWT_SECRET_PREVIOUS` env var (or set it to empty).
3. Restart the service.
4. Boot log should say `JWT single-key mode — no previous secret configured`.
5. Document the rotation in `documents/04-quality/audits/security-rotations.md`
   (date, who initiated, secret version-ids before/after).

## 4. Rollback

If the new secret breaks something (e.g., service refuses to boot due to
config typo):

1. Swap env vars: set `JWT_SECRET_CURRENT=$PREV_SECRET`, clear
   `JWT_SECRET_PREVIOUS`.
2. Restart the service.
3. Investigate the failure offline before re-attempting rotation.

The Secrets Manager `AWSPREVIOUS` label can be promoted back to `AWSCURRENT`
via `update-secret-version-stage` if you also need to roll back the
Secrets-Manager source-of-truth.

## 5. Open items (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

- **Automated rotation:** lambda-triggered Secrets Manager rotation that
  performs §3.1-3.4 unattended. Defer until 2nd manual rotation completes
  successfully (i.e., we have evidence the steps are stable). Tracked in
  GAP-520 follow-up.
- **RS256 migration:** move from shared-secret HS256 to public/private RS256
  so verification can happen in services that don't hold the signing key
  (e.g., gateway). Tracked in GAP-520 follow-up.

## 6. Acceptance criteria

A rotation is considered successful when:

- [ ] New secret written to AWS Secrets Manager (new version_id captured)
- [ ] Service boot log shows `JWT dual-key mode ACTIVE`
- [ ] Real refresh call with a pre-rotation token returns 200 + a new pair
- [ ] `jwt.verify.fallback` counter incremented at least once
- [ ] Window closed after ≥7 days; service back to single-key mode
- [ ] Rotation logged in `security-rotations.md`
