# GAP-510: SERVER_PORT=8080 unify across 4 BE services

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (port-chain mismatch between gateway route URIs and service application.yml ports)
**Domain:** DevOps
**Found:** 2026-05-13 (Bucket E audit-service-ports.sh)
**Closed:** 2026-05-13 (PR #1270, Wave 71 Bucket B)
**Affects:** kitehub-subscription, kitehub-branding, kitehub-email, kitehub-admin

## Problem

Gateway route URIs use `:8080`; services bind 8081/8083/8084 per application.yml defaults → request never reaches container port.

## Fix

`docker-compose.production.yml`: add `SERVER_PORT: 8080` env + change healthcheck URL to `localhost:8080/actuator/health` for 4 BE services. Also rewrote `EMAIL_SERVICE_URL` subscription→email from `:8084` to `:8080` (coupled fix).

## Acceptance Criteria

- [x] 5 `SERVER_PORT: 8080` entries (4 BE + 1 gateway)
- [x] 5 healthchecks on `:8080`
- [x] Post-deploy: docker ps shows all services healthy on 8080
- [x] Plan 1 Bước 2 verify implicitly confirms port chain works

## Related

- PR: #1270, Wave 71 Bucket B
- Sibling: GAP-509 (gateway routes), GAP-511 (profile rename)

## Log

- **2026-05-13:** Filed retroactively at closure. Deploy v0.9.0-beta-staging.12 reached service via port 8080 successfully.
