# Audit Chain Break — SRE Runbook

**Domain:** KiteClass Core / Compliance / Child Protection
**Trigger:** `child_protection.audit.chain.break{instance, entityType}` Micrometer counter > 0
**Severity:** P1 — non-repudiation guarantee compromised
**Owner:** SRE on-call + Safeguarding lead (joint)
**Reference rules:** `BR-CHILD-PROTECT-007` (Phase 1C v1), `BR-CHILD-PROTECT-009` (Phase 1C v1.5)
**Reference code:** `kiteclass-core/.../module/childprotection/service/AuditChainVerificationCron.java` + `ChildProtectionAuditServiceImpl.verifyChain(...)`

---

## 1. What just happened

The daily `AuditChainVerificationCron` (02:30 UTC) iterated every distinct `(instance_id, entity_type)` chain in `child_protection_audit_log` and re-computed `SHA-256(prev_hash || payload_json)` for each entry. **At least one chain failed:** either a `prev_hash` did not match the previous entry's `content_hash`, or a row's `content_hash` did not match the recomputed hash.

This means one of:
1. **Tamper** — somebody mutated a row directly in the database, bypassing the V54 `REVOKE DELETE` GRANT (rogue DBA / direct SQL / superuser).
2. **DB corruption** — disk-level corruption or replication-lag artifact (rare; Postgres usually surfaces this elsewhere first).
3. **Application bug** — a code path appended a row with a wrong `prev_hash` or `content_hash`. Investigate recent deploys + the payload of the broken entry.

**Do NOT delete or "fix" the chain row before completing §3 investigation.** The chain itself is forensic evidence.

---

## 2. Immediate response (≤30 min)

1. **Acknowledge alert** in PagerDuty / on-call channel.
2. **Capture context**:
   ```
   instance_id  = <from alert tags>
   entity_type  = <from alert tags>
   detected_at  = <alert timestamp>
   ```
3. **Page Safeguarding lead.** This is a compliance event, not pure ops. The lead may need to notify legal counsel (24h Đ.51 reporting clock if the chain covers an active mandatory-report case).
4. **Freeze writes if active case** — if the affected chain covers an Incident in `INVESTIGATING` / `ESCALATED` status, escalate to Safeguarding lead before any further append. The lead decides whether to stop new appends to that entity until §4 remediation.
5. **Preserve evidence.** Before any DB action:
   ```sql
   -- Capture a forensic snapshot of the broken chain
   COPY (
     SELECT * FROM child_protection_audit_log
     WHERE instance_id = '<instance>' AND entity_type = '<entity>'
     ORDER BY id
   ) TO '/tmp/audit-chain-break-<instance>-<entity>-<timestamp>.csv' WITH CSV HEADER;
   ```

---

## 3. Investigation

### 3.1 Locate the break

```sql
-- Find the offending row(s) — re-compute hash inline
WITH chain AS (
  SELECT id, prev_hash, content_hash, payload_json,
         LAG(content_hash) OVER (ORDER BY id) AS expected_prev
  FROM child_protection_audit_log
  WHERE instance_id = '<instance>' AND entity_type = '<entity>'
)
SELECT id, prev_hash, expected_prev, content_hash
FROM chain
WHERE prev_hash != COALESCE(expected_prev, REPEAT('0', 64));
```

### 3.2 Classify the break

| Symptom | Likely cause |
|---------|--------------|
| `prev_hash` of row N does not match `content_hash` of row N-1 | A row between them was deleted OR the prev_hash was overwritten |
| `content_hash` does not equal `SHA-256(prev_hash \|\| payload_json)` for row N | The payload_json or content_hash was overwritten |
| Multiple consecutive rows broken | Bulk write mutation — likely intentional tamper |
| Single row, last in chain, broken | Possible application bug in a recent append; check deploy history |

### 3.3 Audit the audit

```sql
-- Who has DELETE / UPDATE on the table right now?
SELECT grantee, privilege_type
FROM information_schema.role_table_grants
WHERE table_name = 'child_protection_audit_log'
  AND privilege_type IN ('DELETE', 'UPDATE', 'TRUNCATE');

-- Who is the table owner?
SELECT tableowner FROM pg_tables WHERE tablename = 'child_protection_audit_log';
```

Cross-reference grant-holders + recent superuser activity (Postgres logs / cloud audit trail). If a non-app role gained DELETE / UPDATE between the last successful verification and now, escalate to security lead — this is a credentials compromise, not a chain bug.

### 3.4 Check for application bugs

```bash
git log --since="<last verified date>" --until="<break detected date>" \
    -- kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/childprotection/
```

If an audit-related code change shipped between verifications, run the existing `ChildProtectionAuditServiceImplTest` against the new build and confirm the unit tests still pass. Bug fixes go through the normal PR pipeline — do NOT hot-fix the chain.

---

## 4. Remediation

**Do NOT delete or update broken rows in production.** A broken chain is non-repudiation evidence; the broken state itself proves tamper occurred. If a replacement chain is needed for ongoing operations:

### 4.1 Sealed-archive pattern (recommended)

1. Rename the broken table for forensic preservation:
   ```sql
   ALTER TABLE child_protection_audit_log
       RENAME TO child_protection_audit_log_archived_<YYYYMMDD>;
   CREATE TABLE child_protection_audit_log (LIKE child_protection_audit_log_archived_<YYYYMMDD> INCLUDING ALL);
   ```
2. Re-apply the V54 grants on the new table.
3. Append an `INCIDENT_AUDIT_CHAIN_RESET` action to the new table with payload referencing the archived table name + reason. This entry becomes the new genesis for the affected chain — future verifications start fresh from this reset point but the archived table remains queryable for forensics.
4. Notify Safeguarding lead + legal counsel of the reset.

### 4.2 Status-quo continuation (small breaks)

For an isolated single-row break with no compliance-relevant entries downstream, Safeguarding lead may choose to leave the chain broken + log the incident in the audit retro. The cron will continue to fire the Micrometer counter daily until the broken row is sealed-archived.

---

## 5. Post-incident

1. **File a follow-up gap** under `documents/04-quality/gaps/` documenting:
   - Detection time vs break-introduction time (latency)
   - Cause classification (§3.2)
   - Remediation chosen (§4)
   - Process or code changes to prevent recurrence
2. **Review V54 grants** — does the typical app role still have only INSERT + SELECT? If a temporary GRANT was issued (e.g., for a migration), confirm it was REVOKEd.
3. **Update compliance log** — if the affected chain covered a mandatory-report Incident, note in the Incident's audit trail (new chain) that integrity was lost between specific dates.
4. **Schedule a tabletop exercise** if root cause was operational (rogue DBA, credentials misuse) — refresh DB-access discipline + V54-style invariants on adjacent audit tables.

---

## 6. Escalation contacts

| Role | When |
|------|------|
| SRE on-call | First responder |
| Safeguarding lead | All chain-break events |
| Legal counsel | If the affected chain covers an active mandatory-report case (Đ.51 24h clock implications) |
| Security lead | If §3.3 surfaces credentials compromise / unexpected GRANT changes |
| DPO | If § the affected entity contains children's PII (PDPL Decree 13/2023 Art 16 reportable event) |

---

## 7. Related

- **Rules:** `documents/01-business/kiteclass/child-protection/rules.md` BR-CHILD-PROTECT-007 + BR-CHILD-PROTECT-009
- **Code:** `kiteclass-core/.../childprotection/service/AuditChainVerificationCron.java` + `ChildProtectionAuditServiceImpl#verifyChain`
- **Migration:** `V54__add_incident_visibility_scope_and_audit_log.sql` (REVOKE DELETE invariant)
- **Gaps:** GAP-359 sub-task 359.5 (this cron); GAP-322c (parent Phase 1C scope)
- **Compliance:** Luật Trẻ em 2016 Đ.51 + PDPL Decree 13/2023/NĐ-CP Art 16 + BLHS Đ.147
