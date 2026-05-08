# Preflight checks

Each `*.sh` here is one independent check. Convention:

- Self-contained, runnable as `bash checks/<name>.sh` from repo root
- Emit lines `[LEVEL][cat-N][check-name] message` (LEVEL ∈ INFO/PASS/WARN/FAIL)
- Exit 0 on PASS/WARN, 1 on FAIL
- Tier 1 read-only only — never mutate AWS / GitHub / Docker registry state

| Cat | Check | Standard | Fallback |
|----:|-------|----------|----------|
| 7 | `github-vars-secrets.sh` | GitHub Actions Hardening + OpenSSF Scorecard | WARN+skip if `gh auth status` not green |
| 8 | `dockerfile-from-reachability.sh` | CIS Docker Benchmark §4.1/§4.2 + Chainguard Distroless | curl Docker Hub manifest API for `library/*` if `docker buildx` unavailable |

Categories 1–6 are owned by sibling preflight-simulator scaffolding work
(multiarch base image, IAM ARN naming drift, OIDC trust scope, secret
naming drift, region drift, runbook step coherence). The runner
`scripts/preflight.sh` auto-discovers any `*.sh` under this directory,
so checks integrate without coordination once shipped.
