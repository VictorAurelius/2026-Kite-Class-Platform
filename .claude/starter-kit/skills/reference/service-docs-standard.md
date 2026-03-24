# Portable Skill — adapt {project} placeholders

# Service Documentation Standard

## Required Files

Every service directory MUST have:

| File | Max Lines | Content |
|------|-----------|---------|
| `README.md` | 150 | Purpose, tech, ports, env vars, API overview |
| `docs/QUICK-START.md` | 100 | Prerequisites, build, run, test commands |

## NOT Allowed in service docs/

- PR summaries → `documents/07-archived/`
- Business logic → `documents/01-business/`
- Architecture decisions → `documents/02-architecture/`
- Implementation plans → `documents/03-planning/`

## README Template

```markdown
# Service Name

One-paragraph description.

## Tech Stack
- **Framework** - version
- **Key Libraries** - purpose

## Ports
| Context | Port |
|---------|------|
| Standalone | `XXXX` |
| Docker | `XXXX` |

## Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|

## API Overview
| Method | Endpoint | Description |
|--------|----------|-------------|

## Links
- Business logic: `documents/01-business/{project}/{domain}/`
- Architecture: `documents/02-architecture/`
- Quick start: `docs/QUICK-START.md`
```

## QUICK-START Template

```markdown
# Quick Start - Service Name

## Prerequisites
## Build
## Run
## Test
## Verify
```

## Line Count Check

Verify with: `wc -l {service}/README.md` (must be <= 150)
