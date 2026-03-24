# Service Documentation Standard

## Required Files

Every service directory MUST have:

| File | Max Lines | Content |
|------|-----------|---------|
| `README.md` | 150 | Purpose, tech, ports, env vars, API overview, links to documents/ |
| `docs/QUICK-START.md` | 100 | Prerequisites, build, run, test commands |

## Optional Files in docs/

- Service-specific technical guides (e.g., THEME-SYSTEM.md for frontend)
- Testing guides specific to this service
- Docker build guides specific to this service

## NOT Allowed in service docs/

- PR summaries --> `documents/07-archived/`
- Business logic --> `documents/01-business/`
- Architecture decisions --> `documents/02-architecture/`
- Implementation plans --> `documents/03-planning/`

## README Template

```markdown
# Service Name

One-paragraph description of the service's purpose and responsibilities.

## Tech Stack

- **Language/Framework** - version info
- **Key Libraries** - purpose

## Ports

| Context | Port |
|---------|------|
| Standalone | `XXXX` |
| Docker (internal) | `8080` |
| Docker (host) | `XXXX` |

## Dependencies

- **Service** - host:port (purpose)

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VAR_NAME` | `default` | Description |

## API Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/path` | Description |

## Monitoring

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`

## Links

- Business logic: `documents/01-business/{project}/{domain}.md`
- Architecture: `documents/02-architecture/`
- Quick start: `docs/QUICK-START.md`
```

## QUICK-START Template

```markdown
# Quick Start - Service Name

## Prerequisites
- Required tools and versions

## Build
- Build commands

## Run Standalone
- Run commands with required env vars

## Run with Docker
- Docker commands (using scripts)

## Test
- Test commands

## Verify
- Health check and basic verification commands
```

## Links Required in README

- Business logic: `documents/01-business/{project}/{domain}.md`
- Architecture: `documents/02-architecture/`
- Full documentation: `documents/README.md`

## Line Count Check

Verify with: `wc -l service-name/README.md` (must be <= 150)
