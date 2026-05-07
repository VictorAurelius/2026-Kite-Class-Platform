# GAP-402: SBOM + Image Signing (Cosign Supply Chain Security)

**Status:** 🟡 PARTIAL — Wave 37 Bucket B
**Priority:** 🟡 P2
**Domain:** Security / Supply chain
**Found:** 2026-05-07 (Wave 37 — Layer 2)
**Affects:** Compliance trail + supply chain attack defense

## Problem

Production images không có SBOM (Software Bill of Materials) hoặc cryptographic signature. Phase 1 BETA invite-only OK; Phase 1.5 PAID + nhất là K-12 K-3 phase scrutiny cao hơn cần SBOM.

## Proposed Fix

1. **Syft** SBOM generation post-build (CycloneDX format)
2. **Cosign** keyless signing (sigstore.dev OIDC) → no key management
3. Push signature + SBOM attestation to ECR (OCI artifact)
4. Verification step pre-deploy: `cosign verify --certificate-identity ...`

```yaml
- name: Generate SBOM
  uses: anchore/sbom-action@v0
  with:
    image: ${{ env.IMAGE }}
    format: cyclonedx-json

- name: Sign image
  run: cosign sign --yes ${{ env.IMAGE }}
```

## Acceptance Criteria

- [x] SBOM CycloneDX JSON generated per image (`anchore/sbom-action@v0`, `format: cyclonedx-json`, output `sbom-${{ matrix.service }}.cdx.json`)
- [x] Cosign keyless signature step (`sigstore/cosign-installer@v3` + `cosign sign --yes` with OIDC token from `id-token: write` permission)
- [x] SBOM attached as Cosign attestation (`cosign attest --predicate ... --type cyclonedx`)
- [x] SBOM uploaded as GitHub Actions artifact (`upload-artifact: true`, `upload-artifact-retention: 90`)
- [ ] Pre-deploy `cosign verify` step in runbook — deferred to GAP-403 deploy runbook (Bucket C scope)

## Log

- **2026-05-07** (Wave 37 Bucket B): Cosign installer + sign + attest steps added in push-to-ecr job; Syft SBOM generation as CycloneDX with 90-day artifact retention. `id-token: write` + `attestations: write` permissions added. Pre-deploy verify step deferred to deploy runbook (Bucket C).

## Related

- GAP-398 (parent build)
- GAP-400 (Trivy parallels)
- Sigstore.dev keyless signing (OIDC trust)
- SLSA framework alignment
