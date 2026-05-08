# Standards References — Authoritative sources cited

This skill grounds in published standards from reputable organizations. NOT free-form heuristics.

## Primary anchors

### AWS Well-Architected Framework
- **URL:** https://aws.amazon.com/architecture/well-architected/
- **Pillar mapped:** Operational Excellence (OPS-04 — Implement observability; OPS-05 — Reduce defects, ease remediation, automate operations)
- **Why cited:** Pre-deploy preflight = "design for failure + automate operations" pillar 1 prescription. Manual review alone insufficient at scale.

### OpenSSF Scorecard
- **URL:** https://github.com/ossf/scorecard
- **Specs:** https://github.com/ossf/scorecard/blob/main/docs/checks.md
- **Why cited:** Open Source Security Foundation maintains reproducible supply-chain checks. Skill applies subset of Scorecard checks (Pinned-Dependencies, Token-Permissions, Dangerous-Workflow) at preflight stage.

### GitHub Actions Security Hardening Guide
- **URL:** https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
- **Subsections:** "Hardening for GitHub-hosted runners", "Using OpenID Connect with reusable workflows"
- **Why cited:** Workflow `if:` conditions, OIDC trust scope, secrets enumeration — all directly from GitHub's official hardening guide.

### Sigstore Cosign Best Practices
- **URL:** https://docs.sigstore.dev/cosign/signing/overview/
- **Spec:** https://docs.sigstore.dev/cosign/key_management/overview/
- **Why cited:** Keyless signing requires `id-token: write` workflow permission + OIDC token TTL ~6min. Skill checks cosign step ordering ≤ 6min from token issue.

### CIS Docker Benchmark v1.6 (CIS Docker Community Edition Benchmark)
- **URL:** https://www.cisecurity.org/benchmark/docker
- **Subsections:** §4.1 (Image base hygiene), §4.7 (Verify signatures), §5.31 (Build cache)
- **Why cited:** Multi-arch base image manifest verify + base image freshness directly from CIS.

### HashiCorp Terraform Best Practices
- **URL:** https://developer.hashicorp.com/terraform/language/style
- **IAM specifics:** https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html (AWS official, referenced by Terraform community)
- **Why cited:** Resource ARN pattern `least-privilege` requires actual resource enumeration, not wildcard guesses. Skill cross-checks terraform Resource patterns against actual resource ARN format.

### CNCF Cloud Native Trail Map
- **URL:** https://landscape.cncf.io/guide
- **Section:** Step 3 (Container Runtime + Image), Step 7 (Distributed Tracing), Step 8 (Service Proxy/Discovery)
- **Why cited:** Cloud-native deploy preflight follows CNCF graduated-project ecosystem (containerd manifest spec, OCI image spec, Sigstore Cosign).

### OCI Image Specification
- **URL:** https://github.com/opencontainers/image-spec/blob/main/spec.md
- **Section:** §5.1 Image Manifest Property `manifests[].platform`
- **Why cited:** Multi-arch manifest format definition. Skill check #1 reads OCI manifest list per spec.

### Twelve-Factor App
- **URL:** https://12factor.net
- **Factors:** III (Config), V (Build/release/run), X (Dev/prod parity)
- **Why cited:** Region pin + secret naming consistency = Factor III strict separation; deploy artifact preflight = Factor V "strict separation between build and run stages".

### NIST SP 800-53 Rev 5 (security and privacy controls)
- **URL:** https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-53r5.pdf
- **Controls:** AC-3 (Access Enforcement), AC-6 (Least Privilege), CM-2 (Baseline Configuration)
- **Why cited:** IAM ARN pattern least-privilege = AC-6 control; terraform-as-baseline = CM-2.

## Secondary references (industry consensus, reputable but not formal standards)

### Snyk State of Cloud Native Application Security 2024
- **URL:** https://snyk.io/reports/state-of-cloud-native-application-security/
- **Why cited:** Industry data on top deploy-time misconfigurations (#1 = IAM over-permissioning, #3 = base image vulnerabilities) — informs which categories skill prioritizes.

### Datadog State of DevOps 2024 (DORA Adoption)
- **URL:** https://www.datadoghq.com/state-of-devops/
- **Why cited:** Deploy preflight = lead-time-for-changes optimization (DORA metric).

### Chainguard Distroless Best Practices
- **URL:** https://www.chainguard.dev/unchained/distroless-the-evolution
- **Why cited:** Multi-arch base image alternative recommendations (skill catalog includes Chainguard images as Tier-2 alternative when alpine variants lack arm64).

### AWS IAM Access Analyzer Best Practices
- **URL:** https://docs.aws.amazon.com/IAM/latest/UserGuide/access-analyzer.html
- **Why cited:** Resource pattern verification = subset of what Access Analyzer policy validation does at runtime; preflight catches before terraform apply.

## Anti-references (deliberately NOT cited)

- **No vendor whitepaper marketing** (e.g., AWS Solutions Library generic blog posts)
- **No Stack Overflow answers as primary source** (informs investigation, not authoritative)
- **No Medium / dev.to articles** (un-versioned, may drift)
- **No internal-only docs from other companies** (Spotify Backstage, Netflix tech blog) unless directly applicable + linked from CNCF graduated project

Skill catalog updates require new check to cite a primary or secondary reference from above list. PRs adding categories without standard reference → reject.

## Update protocol

When a primary standard publishes new revision (vd: Well-Architected updated 2025), this file MUST update version + URL within 30 days; checklist files in `reference/check-catalog.md` update if new control added relevant to preflight.
