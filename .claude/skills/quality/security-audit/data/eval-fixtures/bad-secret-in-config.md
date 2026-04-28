# Eval Fixture — bad-secret-in-config.md

# Expected: FAIL — application.yml has a hardcoded API key + bcrypt-skip dev tweak

**Skill:** `quality/security-audit`
**Scenario:** Synthetic codebase where developer committed a hardcoded API
key to `application.yml` (typical "I'll fix it before deploy" mistake).
**Which check fires:** Category 2 — Secrets & Credentials (-12+); Category 5
— Infrastructure Security may also flag if the same key appears in Docker
build args.
**Filed:** 2026-04-28 (GAP-253 pilot)

---

## Setup (synthetic)

### `kitehub/kitehub-branding/src/main/resources/application.yml`

```yaml
ai:
  provider: openai
  api-key: sk-proj-AbCdEf123456789aBcDeF123456789aBcDeF123456   # ← HARDCODED
  base-url: https://api.openai.com/v1

# Quick dev override — please remove before merge!
spring:
  security:
    user:
      password: admin123                                          # ← BLATANT
```

### `Dockerfile`

```dockerfile
ARG OPENAI_KEY=sk-proj-AbCdEf123456789aBcDeF123456789aBcDeF123456    # ← LEAKS in `docker history`
ENV OPENAI_API_KEY=$OPENAI_KEY
```

### Grep evidence (audit reproduction)

```bash
$ grep -rn "sk-proj-\|password\s*=\|api[_-]key\s*=" \
    --include="*.yml" --include="*.dockerfile" --include="Dockerfile" \
    kitehub/*/src/main/ kitehub/*/Dockerfile* | grep -v test
kitehub/kitehub-branding/src/main/resources/application.yml:3:  api-key: sk-proj-AbCdEf...
kitehub/kitehub-branding/src/main/resources/application.yml:9:      password: admin123
kitehub/kitehub-branding/Dockerfile:5:ARG OPENAI_KEY=sk-proj-AbCdEf...
```

---

## Expected audit-report excerpt

```
## Cat 2 — Secrets & Credentials       8/20  (-12)

### Hardcoded secrets detected:
1. `application.yml:3` — `ai.api-key` contains `sk-proj-` literal token
2. `application.yml:9` — `spring.security.user.password: admin123`
3. `Dockerfile:5` — `ARG OPENAI_KEY=` defaults to literal token

### Severity: 🛑 BLOCKER (per `core/two-stage-code-review.md` §Severity Rubric)

### Recommended actions:
1. Rotate the leaked OpenAI key IMMEDIATELY (treat as compromised)
2. Replace literals with `${OPENAI_API_KEY}` env-var refs
3. Add `application*.yml` regex patterns to pre-commit secret scanner
4. Add Dockerfile lint rule banning `ARG <KEY>=<literal>`
5. File gap: GAP-XXX-secret-leak-yml (P0, security)
```

---

## Why this matters

Real incident pattern (from memory `feedback_dependabot_pnpm_transitive.md`'s
sibling secret-leak class) — secrets in YAML are the #1 root cause of
GitHub-public credential leaks. Static audit must catch this before
`git push`.

The audit grep should be **broad** (all `*.yml` + Dockerfile) and only
filter out `*.example` / `*.test`. Don't skip `application-dev.yml` —
dev secrets often get reused in staging.

---

## How to use this fixture

Regression test: any change to security-audit Cat 2 logic must still flag
all 3 violations. If a refactor reduces sensitivity, reviewer must justify
explicitly per `output-review-mandate.md` §3.

This fixture pairs with `edge-transitive-cve.md` — together they exercise
the two distinct security-audit detection styles (pattern grep vs SBOM
walk).
