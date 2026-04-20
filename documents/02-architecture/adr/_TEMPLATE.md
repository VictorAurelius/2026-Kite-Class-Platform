# ADR-NNN: [Title in Title Case]

<!--
Copy this file to `ADR-NNN-short-kebab-title.md` where NNN is the next free 3-digit ID
from the index in README.md. Delete HTML comments (like this one) before committing.
Process + reviewer checklist: see adr/README.md §ADR Process.
-->

**Status:** PROPOSED <!-- PROPOSED | ACCEPTED | DEPRECATED | SUPERSEDED by ADR-MMM -->
**Date:** YYYY-MM-DD
**Deciders:** @name1, @name2 <!-- Required: Tech Lead + ≥1 senior engineer -->
**Reviewers:** @name3 <!-- Optional consults: security/DBA/SRE/product as needed -->
**Related Gap(s):** GAP-XXX <!-- Omit line if no related gap -->
**Supersedes:** ADR-MMM <!-- Omit line if not superseding anything -->

## Context

<!--
What is the issue we're facing? What forces are at play (technical, political, social,
project)? Describe the state of the world that makes this decision necessary. Include:
- Current pain point or gap
- Constraints (deadlines, compat, compliance, budget)
- Stakeholders affected
-->

## Decision

<!--
What we decided. Be explicit — "We will do X" (active voice, no hedging).
State the decision in 1-2 sentences, then expand with detail as needed.
-->

## Consequences

### Positive
<!-- List concrete benefits this decision unlocks -->
- Benefit 1
- Benefit 2

### Negative
<!-- Honest costs, debts, or constraints this decision introduces -->
- Cost 1
- Cost 2

### Neutral
<!-- Changes in workflow or dependencies that are neither purely pro nor con -->
- Change in workflow
- New dependency

## Alternatives Considered

<!-- ≥2 alternatives required. Show you actually weighed options. -->

### Alternative A: [Name]
Pros: ...
Cons: ...
Rejected because: ...

### Alternative B: [Name]
Pros: ...
Cons: ...
Rejected because: ...

## Implementation Notes

<!-- Optional but useful: migration strategy, rollback plan, feature flags, phasing. -->

- Migration strategy
- Rollback plan
- Feature flags if applicable
- Monitoring / success criteria

## References

<!-- Cross-links. Use relative paths. -->

- Design pattern used: `.claude/rules/design-patterns.md` §X.Y
- Related ADRs: ADR-NNN (see `adr/README.md` index)
- Related rules: `.claude/rules/...`
- Related gap: `documents/04-quality/gaps/GAP-XXX-*.md`
- External links: RFCs, blog posts, spec references

## Log

<!--
Append-only decision history. Never rewrite past entries.
Entries newest-first; add one line per status transition or material update.
-->

- YYYY-MM-DD — Initial proposal
- YYYY-MM-DD — Accepted after review by @name1 + @name2
