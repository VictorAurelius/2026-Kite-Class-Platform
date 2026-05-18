# GAP-212: DefaultUrlAllowlistValidatorTest flaky due to DNS of `api.partner.com` → loopback

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (blocks every Core Service CI run until fixed; not a production defect)
**Domain:** Backend / Testing
**Detected:** 2026-04-24 (surfaced while opening PR #474 Sub-PR 5.0; confirmed pre-existing on main)
**Related PRs:** #474 (blocked by this), #471 (Core CI also failed on this test, merged anyway)
**Related Docs:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/security/impl/DefaultUrlAllowlistValidatorTest.java`

## Current State (verified 2026-04-24)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Validator impl | `kiteclass-core/src/main/java/com/kiteclass/core/common/security/impl/DefaultUrlAllowlistValidator.java` (L149-170 `resolvesInternally`) | ✅ shipped (Wave 4 Sub-PR 4.2, behaves correctly — loopback guard is intentional DNS-rebind protection) |
| Test file | `kiteclass-core/src/test/java/com/kiteclass/core/common/security/impl/DefaultUrlAllowlistValidatorTest.java` | 🟡 6 of 7 tests using hostname fixtures reference real-sounding public hostnames (`api.partner.com`, `api.shared.example`, `*.trusted.org`); 1 of those (`allowsTenantListedHost:70`) fails deterministically when the resolver returns `::1` or `127.0.0.1` |
| CI evidence | `gh run view 24877619799 --log-failed` | ❌ fails on PR #474; also failed on PR #471 (`ci/solo-dev-no-post-merge-ci`, merged despite failure) |

**Grep commands run:**
```bash
grep -rlE "api\.partner|DefaultUrlAllowlist" kiteclass/kiteclass-core/src
ls documents/04-quality/gaps/ | grep -iE "url|allowlist|ssrf|dns"
getent ahosts api.partner.com      # → ::1 (IPv6 loopback) on this WSL2 env
python3 -c "import socket; print(socket.getaddrinfo('api.partner.com', None))"  # → confirms ::1 + 127.0.0.1
```

## Problem

`DefaultUrlAllowlistValidatorTest.allowsTenantListedHost` allowlists `api.partner.com` and asserts `isAllowed("https://api.partner.com/webhook", "t1") == true`.

The validator's `resolvesInternally` calls `InetAddress.getAllByName("api.partner.com")` as a DNS-rebind guard. On envs where the resolver returns a loopback / link-local address for that name (WSL2 + systemd-resolved NXDOMAIN fallback, GitHub-hosted runners with containerd DNS quirks), the guard returns `true` → validator denies → test fails.

It is **not a validator bug** — the DNS-rebind guard is intended behaviour per ADR-011 §SSRF. The test is brittle because it picks a hostname whose resolution depends on the runner's resolver policy.

## Context

- Pre-existing; surfaced only because solo-dev CI policy (2026-04-24 PR #471) removed `push: main` on test workflows, so main-branch CI no longer runs. PR CI now exposes the flake on every non-trivial PR.
- Blocks every PR that triggers `Core Service CI/CD` until fixed.
- Test was authored in Wave 4 Sub-PR 4.2 (2026-04-14) — green at the time on the runner in use; environmental change broke it later.

## Evidence

```
[ERROR] Failures:
[ERROR]   DefaultUrlAllowlistValidatorTest.allowsTenantListedHost:70
Expecting value to be true but was false
[ERROR] Tests run: 1035, Failures: 1, Errors: 0, Skipped: 52
```

- Local reproduction on `main` branch (no Sub-PR 5.0 changes): SAME failure.
- `getent ahosts api.partner.com` → `::1 STREAM` + `127.0.0.1 STREAM`.
- `allowsDefaultListedHostForAnyTenant` (line 82) uses `api.shared.example` — NXDOMAIN, safe. This is why only `allowsTenantListedHost` fails.

## Proposed Fix

Swap the real-sounding hostname for an RFC-2606 reserved non-resolving TLD that is guaranteed never to map to loopback or public IP:

```java
// Before
env.setProperty("security.url.allowlist.t1", "api.partner.com");
assertThat(v.isAllowed("https://api.partner.com/webhook", "t1")).isTrue();

// After (RFC 2606: .invalid never resolves)
env.setProperty("security.url.allowlist.t1", "api.partner.invalid");
assertThat(v.isAllowed("https://api.partner.invalid/webhook", "t1")).isTrue();
```

When `getAllByName("api.partner.invalid")` throws `UnknownHostException`, `resolvesInternally` catches and returns `false` (per its own comment: "Unresolvable → false"), so the validator returns `true` — matching test intent.

Apply the same pattern to any sibling test using hostnames whose resolution might drift (`rejectsTenantUnlistedHost`, `allowsDefaultListedHostForAnyTenant`, `wildcardDomainMatchesSubdomain`).

Scope: test file only. Zero production code change. Zero behavior change to validator.

## Acceptance Criteria

- [ ] All tests in `DefaultUrlAllowlistValidatorTest` pass deterministically on WSL2, macOS, Linux, and GitHub-hosted runners
- [ ] No real-resolvable public hostname used as a fixture (replaced with `.invalid`, `.test`, `.example`, or IP literal)
- [ ] DNS-rebind guard paths still covered (one test remains to exercise the internal-IP rejection branch)
- [ ] Separate PR (small, docs + test-only) — does NOT piggyback on Wave 5 Sub-PRs
- [ ] PR #474 (Sub-PR 5.0) re-runs Core Service CI green after the fix PR merges

## Related

- ADR-011 Defense-in-Depth Security §SSRF (validator spec — unchanged)
- GAP-041 Security Hardening Injection (🟢 DONE, Wave 4 Sub-PR 4.2 — origin of this test)
- Rule: `.claude/rules/audit-to-gap-pipeline.md` §2.5 (state-check performed above)
- Rule: CLAUDE.md §CI Trigger Policy (why this went unnoticed on main)

## Log

- 2026-04-24 — Initial write-up. State-check confirmed validator behaviour is correct; defect is in test hostname fixture. Fix is scoped to test file only. Reserved P1 because it unblocks every Core CI run.
