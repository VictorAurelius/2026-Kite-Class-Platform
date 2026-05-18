# GAP-064: SCORM / xAPI Compliance (Corporate Training)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / LMS Standards
**Persona blocked:** P7 Corporate Training
**Detected:** 2026-04-14

## Problem

Corporate training departments yêu cầu SCORM (Sharable Content Object Reference Model) hoặc xAPI compliance để:
- Import content từ existing LMS
- Export to enterprise LMS (SAP SuccessFactors, Workday Learning)
- Compliance tracking (audit trail)

Platform hiện tại proprietary → không thể serve P7.

## Proposed Fix

### SCORM Support

- SCORM 1.2 + 2004 content playback
- Upload `.zip` SCORM package → extract → play
- Track: completion, score, time, bookmarks
- Report back to tenant LMS if required

### xAPI (Tin Can)

- More flexible than SCORM
- Statements: "I did X in Y context"
- LRS (Learning Record Store) endpoint

### Certificates

- Generate completion certificates (PDF via GAP-047)
- Unique verification codes
- Link với HR systems

## Acceptance Criteria

- [ ] SCORM 1.2 playback
- [ ] SCORM 2004 playback
- [ ] xAPI Statement API
- [ ] LRS integration
- [ ] Certificate generation + verification

## Dependencies

- GAP-047 (document generation)
- Likely defer to post-GA (P7 là Tier 2)

## Log
- 2026-04-14 — Persona P7 review
