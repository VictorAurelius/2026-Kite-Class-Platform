# Domain Management — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.domain.verification`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| DOM-01 | Tier restriction | PREMIUM và ENTERPRISE only | canUseCustomDomain() |
| DOM-02 | Token format | kitehub-verify={random-uuid} | DomainService.initiateCustomDomain() |
| DOM-03 | Verification timeout | 48 giờ | `kitehub.domain.verification.timeout-hours` |
| DOM-04 | Mock mode default | true (DNS không check) | `kitehub.domain.verification.mock-mode` |
| DOM-05 | Domain uniqueness | 1 custom domain per instance toàn platform | findByCustomDomainAndDeletedFalse() |
| DOM-06 | Re-initiation allowed | Cùng instance có thể re-initiate domain | ownership check |
| DOM-07 | Backup URL | https://{subdomain}.kiteclass.com | buildResponse() |
| DOM-08 | DNS record instruction | TXT: @ {token} hoặc _kitehub-verify.{domain} | buildResponse() |
| DOM-09 | Mock mode behavior | DNS not resolvable → PENDING (không FAILED) | verifyCustomDomain() |
| DOM-10 | Production mode | DNS not found → stays PENDING_VERIFY | verifyCustomDomain() |

## Domain Status States

```
NONE → PENDING_VERIFY → VERIFIED
              ↓
           FAILED (timeout - chưa implement trong scheduler)
```

## Config

```yaml
kitehub:
  domain:
    verification:
      timeout-hours: 48
      mock-mode: ${DOMAIN_VERIFICATION_MOCK:true}
```
