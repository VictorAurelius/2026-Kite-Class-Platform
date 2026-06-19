# Contributing to Kite Platform

Thanks for your interest! Kite Platform is a solo-developed graduation project, but the
codebase is public and contributions, issues, and discussions are welcome.

> **Note on language.** Code comments and commit messages are in **English**. All
> *user-facing* product copy is in **Vietnamese** (Vietnamese-first product). Docs and
> governance files are mixed — English for the visitor-facing layer, Vietnamese for
> internal narrative.

## Ground rules

This project follows a strict, self-imposed governance model (see [`CLAUDE.md`](CLAUDE.md)).
Even as a solo project, every change goes through the same pipeline:

1. **Branch, never commit to `main`.** Use `feature/<desc>` or `wave/<tag>` branches.
2. **Every change ships through a Pull Request** — including docs-only changes.
3. **Conventional Commits** — `type(scope): description` (`feat`, `fix`, `docs`,
   `refactor`, `test`, `chore`, `ci`). Written in English, present tense, "what" not "how".
4. **Tests first** for code changes (TDD: Red → Green → Refactor).
5. **Docker via scripts only** — never raw `docker-compose`; always `./scripts/up.sh` etc.
6. **Business logic = docs in the same PR.** Each domain keeps `rules.md` + `use-cases.md`
   + `api-contract.md` in sync with the code (3-layer business docs).

## Local setup

```bash
# KiteHub full stack (gateway + 6 services + infra + frontend)
cd kitehub/ && ./scripts/up.sh

# KiteClass dev stack (core + gateway + frontend + infra)
cd kiteclass/ && ./scripts/dev-docker.sh up

# E2E smoke test
cd kitehub/ && ./scripts/test-api-e2e.sh
```

Run `./scripts/help.sh` inside either folder for the full command list.

## Opening an issue

- **Bug** — steps to reproduce, expected vs actual, stack/log snippet.
- **Feature / idea** — the problem first, then the proposed solution.
- **Security** — please **do not** open a public issue; see [`SECURITY.md`](SECURITY.md).

## Pull request checklist

- [ ] Branched off `main`, PR targets `main`
- [ ] Conventional Commit title
- [ ] Tests added/updated and passing locally
- [ ] Business / API docs updated if behavior changed
- [ ] No secrets, credentials, or generated artifacts committed

## Code of conduct

By participating you agree to uphold our [Code of Conduct](CODE_OF_CONDUCT.md).
