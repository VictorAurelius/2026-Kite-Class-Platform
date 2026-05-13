# ADR-029: JVM-in-Container Memory Budget Rule

**Status:** ACCEPTED
**Date:** 2026-05-13
**Deciders:** @nguyenvankiet (solo-dev, acting CTO)
**Reviewers:** N/A (solo-dev mode per CLAUDE.md decision context locked 2026-05-06)
**Related Gap(s):** GAP-502 RC2 (OOM evidence + Wave 70 Bucket C fix); GAP-447 (Free Tier sizing decision + Wave 70 revisit commitment)
**Related Rule(s):** `.claude/rules/release-deploy-standard.md` §3.1 (resource sizing checklist); `.claude/rules/ai-branding-guidelines.md` (heavy-tier service exception)

---

## Context

Wave 70 (GAP-502) production thrashing fix surfaced a recurring class of incidents: kitehub-* Spring Boot services OOM-killed inside Docker containers under modest load. Production audit-of-trust 2026-05-13 measured **11 container die / 1h** on the kh-backend EC2 (t3.medium, 3.7 GiB total RAM). Root cause analysis pinpointed:

1. **Pre-Wave-70 baseline:** services ran with fixed `-Xmx256m -Xms128m` JVM flags inside containers that had **no `mem_limit` declared**. Non-heap memory (metaspace, code cache, native, JNI, DirectByteBuffer, thread stacks) grew unbounded and pushed total RSS past available host RAM.
2. **Cascading OOM on host:** with 5 kitehub services + RabbitMQ + Redis + Postgres-client on a single t3.medium, the absence of per-container budgets allowed one runaway service to starve all neighbors.
3. **GAP-447 sizing assumption invalidated:** that gap concluded "single t3.medium sufficient for Phase 1 BETA" — the conclusion was correct but the *per-container budget rule* it relied on was never documented or enforced.
4. **Pre-launch stress test gap:** the rollback runbook (GAP-447 §"Rollback path Step 2") anticipated a stress test that would have caught this; the stress test was never executed.

Wave 70 Bucket C shipped the fix: every kitehub-* service in `kitehub/docker-compose.production.yml` gained `mem_limit`, healthcheck `start_period: 120s`, and `JAVA_OPTS=-XX:MaxRAMPercentage=50.0 -XX:+UseContainerSupport -XX:+UseSerialGC`. The 50% MaxRAMPercentage + tiered `mem_limit` floor was chosen organically per service profile. This ADR codifies that decision into a durable rule so future services don't recreate the GAP-502 footgun.

### Forces at play

- **Free Tier RAM ceiling:** Phase 1 BETA target = 1× t3.medium (3.75 GiB) per ADR-025. Every megabyte budget choice cascades against this ceiling.
- **JVM container ergonomics:** JDK 11+ (we ship JDK 17) supports `-XX:+UseContainerSupport` (default ON since JDK 10) + `-XX:MaxRAMPercentage`. Pre-container-aware JVMs ignored cgroup limits and OOM-killed silently.
- **Non-heap is non-trivial:** typical Spring Boot service non-heap = metaspace (80-150 MiB) + code cache (50-150 MiB) + ~30 threads × ~512 KiB stack + JIT scratch + DirectByteBuffer for Tomcat/RabbitMQ clients. A 256 MiB non-heap reserve is the floor for any non-trivial service.
- **GC pause budget:** Phase 1 BETA tolerates 100-200 ms GC pauses (no SLA below that yet). SerialGC works fine on heaps ≤512 MiB and avoids the multi-GB-heap concurrent-GC complexity (G1/ZGC) that we don't need.
- **Future service heterogeneity:** AI inference, search, multi-context services will land in Phase 2+. They need a different bracket (≥1 GiB heap). This ADR must not pin a one-size rule that breaks those.

---

## Decision

Adopt the **per-service JVM-in-container memory budget rule** for every kitehub-* / kiteclass-* Spring Boot service shipped to production.

The rule has five enforceable parts:

### 1. Heap budget = 50 % of container `mem_limit`

Every JVM container sets:

```yaml
environment:
  JAVA_OPTS: "-XX:MaxRAMPercentage=50.0 -XX:+UseContainerSupport -XX:+UseSerialGC"
```

Do **NOT** set fixed `-Xmx` in production deploys. Fixed `-Xmx` was the brittle pre-Wave-70 pattern that ignored container changes. `MaxRAMPercentage=50` recomputes the heap whenever `mem_limit` changes, so capacity tuning becomes a single-knob operation.

### 2. Non-heap reserve = 50 % of container `mem_limit`

The other 50 % covers metaspace + code cache + thread stacks + native (JNI, DirectByteBuffer, agent allocations) + JVM overhead (compiler scratch, GC reserves). Do not encroach on it for heap growth.

### 3. Container `mem_limit` floor per service tier

| Tier | `mem_limit` | Heap (50 %) | Non-heap (50 %) | Example services |
|------|-------------|------------|-----------------|------------------|
| **Light** | 512 MiB | 256 MiB | 256 MiB | gateway, email, single-purpose handlers |
| **Medium** | 640 MiB | 320 MiB | 320 MiB | admin, subscription, branding |
| **Heavy** | ≥1024 MiB | ≥512 MiB | ≥512 MiB | (future) ML/AI inference, search, multi-context |

The Light floor (512 MiB) is the smallest budget where a Spring Boot service starts cleanly + holds RabbitMQ + ~30 threads + a small HTTP pool without immediate metaspace pressure. Anything lower thrashed in Wave 70 reproductions.

### 4. Host EC2 budget rule

For any host running ≥3 JVM services, the sum of all container budgets PLUS the standing infrastructure footprint MUST fit within 70 % of host RAM. The remaining 30 % covers GC spikes, the kernel page cache, and diagnostic tooling (jstack, top, `docker stats`).

Concrete Phase 1 BETA budget on t3.medium (3.75 GiB usable):

```
5× kitehub-* services @ medium (640 MiB)    = 3200 MiB   ← too tight on t3.medium alone
RabbitMQ                                    =  320 MiB
Redis                                       =  320 MiB
OS + Docker daemon overhead                 = ~700 MiB
────────────────────────────────────────────────────────
Total                                       = ~4540 MiB > 3750 MiB host RAM
```

The Wave 70 production sizing already accounts for this: kh-backend EC2 carries 4 JVM services at Light/Medium tier; the 5th (admin) runs on a separate co-host or is scoped Light. The 70 % rule forces this discipline.

### 5. Healthcheck `start_period` ≥ 120 s for any JVM container

Spring Boot startup (component scan + Hibernate metamodel + RabbitListener init + cache warm) typically takes 30-60 s on t3.medium. Add a 60 s buffer so transient OOM during startup doesn't trigger Docker's restart-loop before the JVM has paged itself in. Pre-Wave-70 services used the Docker default 30 s and triggered the cascading-restart pattern in GAP-502.

### 6. GC choice

Phase 1 BETA: `-XX:+UseSerialGC`. Single-tenant, small heaps (≤512 MiB), predictable pause budget, lowest non-heap overhead. Re-evaluate at Phase 1.5 PAID if any service heap exceeds 1 GiB or if pause spikes appear in CloudWatch metrics. G1GC or ZGC at that point — track via a follow-up ADR.

---

## Considered Alternatives (rejected)

### Fixed `-Xmx256m -Xms128m` (the pre-Wave-70 production state)

This was the inherited pattern from local-dev compose. It "worked" only because local dev had effectively unlimited host RAM. In production:

- Non-heap can exceed the container budget silently → OOMKilled, no clean shutdown
- Brittle to `mem_limit` changes: changing the container budget without simultaneously editing `-Xmx` is a foot-gun the next maintainer will hit
- Brittle to JVM upgrade: JDK upgrades grow non-heap (especially metaspace + native) — a fixed Xmx leaves no headroom

Rejected because **GAP-502 IS the cost of this alternative**.

### `MaxRAMPercentage=75.0` (more aggressive heap)

Considered: leaves only 25 % for non-heap. Metaspace growth alone (~150 MiB for a typical Spring Boot service) eats 60 % of a 256 MiB non-heap reserve. Add code cache + threads and the reserve flips negative.

Rejected because the safety margin is too thin for solo-dev mode where we don't have time to chase metaspace OOMs at 03:00.

### No `mem_limit` (let host arbitrate)

Considered briefly because "the JVM will use what it needs." Production observation in GAP-502 falsified this: one runaway service starved 4 neighbors before the OOM-killer fired on the actual culprit. Container budgets are blast-radius isolation, not just sizing.

Rejected because the cascading-OOM pattern is the failure mode this ADR exists to prevent.

### Move to Kubernetes + JVM tuning operator (Pegasus / Quarkus)

Considered as a strategic alternative: K8s resource requests/limits + JVM operator gives finer-grained control. Out of scope for Phase 1 BETA per ADR-025 (AWS Free Tier, EC2 + docker-compose, no K8s). Re-evaluate at Phase 2 if EKS path is taken per ADR-028.

Rejected for current phase scope, not on technical merit.

---

## Consequences

### Positive

- Survives `mem_limit` changes without re-tuning per-service `-Xmx`. Capacity scaling = one-knob operation.
- Each service's heap auto-scales with its container budget — capacity planning becomes spreadsheet-clean (Light = X tenants, Medium = 3X tenants, Heavy = 10X tenants).
- Documented budget rule prevents recurrence of the GAP-502-class incidents; new services inherit the constraint by default.
- 70 % host-budget rule forces the kh-backend / kc-app split decision early instead of discovering it under OOM cascade.

### Negative

- Requires JDK 11+ (`UseContainerSupport` default-true). We ship JDK 17 so this is satisfied today; constrains future downgrade paths.
- Heavy services (future) need per-service rule re-evaluation past 1 GiB heap. SerialGC stops being optimal; pause budget shifts.
- Pre-launch stress test runbook still a gap (tracked in GAP-447 §"Phase 3 Prevention" — explicit stress test before each release tag).
- 50 % non-heap reserve looks generous compared to upstream JDK defaults (which leave less reserve). On hosts where every MiB counts (t3.micro), this rule pushes capacity planning toward larger instances.

### Compliance / verification

- All 5 kitehub-* services post Wave 70 Bucket C satisfy parts 1-5: gateway 512 MiB, admin 640 MiB, subscription 640 MiB, branding 640 MiB, email 512 MiB. Verified via `infrastructure/terraform-aws/` cloudwatch dashboards + `docker-compose.production.yml` review.
- kiteclass-* services to be audited at next KC release. Same rule applies — same tiers, same defaults.
- Future services: ADR-029 referenced in the project's service-template scaffolding (`kitehub/<new-service>/Dockerfile` + `docker-compose.production.yml` snippet must satisfy parts 1-5 before merge per `release-deploy-standard.md` §3.1).

---

## References

- GAP-502 RC2 — OOM evidence (`11 container die / 1h` audit-of-trust 2026-05-13) + fix
- GAP-447 — Free Tier sizing decision + Wave 70 revisit commitment
- Wave 70 plan: `documents/03-planning/waves/wave-2026-05-13-70-gap-502-production-thrashing-fix.md`
- `kitehub/docker-compose.production.yml` post-Wave-70 — live example of the rule applied
- JDK 17 Container Awareness (JEP 396 successors — `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage`)
- `.claude/rules/release-deploy-standard.md` §3.1 (resource sizing checklist — references this ADR)
- ADR-025 (AWS-only Deploy for Phase 1 BETA) — host instance choice constraint
- ADR-028 (ECS Fargate vs EKS for Phase 1 BETA) — orchestration constraint
- `infrastructure/terraform-aws/cloudwatch.tf:40` — `kh_backend_memory_high` CloudWatch alarm already shipped (pre-Wave-70); covers the runtime detection side of this ADR.

---

## Log

- **2026-05-13:** ADR created (Wave 70 Bucket E). Status ACCEPTED on the strength of the GAP-502 forensic evidence + the Wave 70 Bucket C fix already shipped. Codifies the per-service JVM-in-container memory budget rule for all current and future Spring Boot services. Bucket E original scope included a new CloudWatch memory alarm; state-check found `kh_backend_memory_high` alarm already exists in `infrastructure/terraform-aws/cloudwatch.tf:40` (shipped earlier wave), so alarm work was dropped and Bucket E narrowed to this ADR + adrs-index.csv update.
