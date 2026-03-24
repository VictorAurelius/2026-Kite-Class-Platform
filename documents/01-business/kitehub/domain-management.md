# Domain Management

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| DOM-01 | Tier restriction | PREMIUM and ENTERPRISE only | canUseCustomDomain() |
| DOM-02 | Token format | kitehub-verify={random-uuid} | DomainService.initiateCustomDomain() |
| DOM-03 | Verification timeout | 48 hours (configurable) | `kitehub.domain.verification.timeout-hours` |
| DOM-04 | Mock mode default | true (DNS not checked) | `kitehub.domain.verification.mock-mode` |
| DOM-05 | Domain uniqueness | One custom domain per instance across platform | findByCustomDomainAndDeletedFalse() |
| DOM-06 | Re-initiation allowed | Same instance can re-initiate its own domain | (ownership check) |
| DOM-07 | Backup URL always available | https://{subdomain}.kiteclass.com | buildResponse() |
| DOM-08 | DNS record instruction | TXT record: @ {token} or _kitehub-verify.{domain} | buildResponse() |
| DOM-09 | Mock mode behavior | DNS not resolvable -> PENDING (not FAILED) | verifyCustomDomain() |
| DOM-10 | Production mode behavior | DNS not found -> stays PENDING_VERIFY | verifyCustomDomain() |

## Domain Status States

```
NONE -> PENDING_VERIFY -> VERIFIED
                  |
                  v
               FAILED (timeout - not yet implemented in scheduler)
```

| Status | Description |
|--------|-------------|
| NONE | No custom domain configured |
| PENDING_VERIFY | Domain set, waiting for DNS TXT record verification |
| VERIFIED | DNS TXT record confirmed, domain active |
| FAILED | Verification timed out (planned, not yet in scheduler) |

## Flow

### Domain Setup Flow
1. User requests custom domain setup (POST /api/instances/{id}/domain)
2. Validate instance tier is PREMIUM or ENTERPRISE
3. Check domain not used by another instance
4. Generate verification token: `kitehub-verify={uuid}`
5. Set domain status to PENDING_VERIFY
6. Return token + DNS instructions + backup URL

### Domain Verification Flow
1. User adds TXT record to their DNS
2. User triggers verification (POST /api/instances/{id}/domain/verify)
3. System attempts DNS TXT lookup for the domain
4. If TXT record matches token -> status = VERIFIED, set verifiedAt
5. If TXT record not found:
   - Mock mode: stay PENDING_VERIFY (graceful for dev)
   - Production: stay PENDING_VERIFY (timeout handled by future scheduler)

### Domain Removal Flow
1. User requests removal (DELETE /api/instances/{id}/domain)
2. Clear: customDomain, domainVerifyToken, domainVerifiedAt
3. Set status to NONE

### DNS Check Implementation
- Current: InetAddress.getAllByName() for basic resolution
- TXT record lookup: not fully implemented (returns false)
- Production: should use JNDI or dnsjava for actual TXT record check

## Emails

No domain-specific emails are sent. Domain status is communicated via API responses.

## Config

```yaml
kitehub:
  domain:
    verification:
      timeout-hours: 48                          # Hours before verification expires
      mock-mode: ${DOMAIN_VERIFICATION_MOCK:true} # true=dev, false=production
```
