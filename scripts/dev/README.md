# scripts/dev/ — Dev Self-Test Enablement Scripts

**Last Updated:** 2026-05-17
**Wave:** 87 (Dev Self-Test Enablement)
**Tham chiếu:** `.claude/rules/docs-folder-structure.md` §3

---

## 1. Purpose

Folder này chứa scripts hỗ trợ dev (solo) tự walk-through 94 USER-VERIFY rows
+ 27 INSUFFICIENT_SPEC rows trong
[`documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`](../../documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv)
trong 1-2 buổi mà KHÔNG bị stuck creds/seed/state.

Scripts trong folder này CHỈ chạy ở **DEV environment local**. KHÔNG được chạy
trên staging/production (mỗi script có safety check `NODE_ENV != production`).

---

## 2. Directory map

```
scripts/dev/
├── README.md                    # File này (Wave 87 Bucket B)
├── self-test-preflight.sh       # 6-gate check trước khi walk-through (Bucket B)
├── self-test-reset.sh           # Reset DB + Redis giữa các round (Bucket B)
├── seed-personas.sh             # (Bucket A — xem PR riêng) seed 7 personas
└── smoke-creds.sh               # (Bucket A — xem PR riêng) lấy credentials
```

**Bucket A scripts** (`seed-personas.sh` + `smoke-creds.sh`) ship trong PR
riêng (Wave 87 Bucket A). Section §5 dưới đây là placeholder; sau khi Bucket A
merge, README sẽ được cập nhật trong cùng PR đó (xem GAP-540 nếu cần track).

---

## 3. File placement rules

| File pattern | Đặt ở đâu | Lý do |
|---|---|---|
| `self-test-*.sh` | `scripts/dev/` (folder này) | Scripts hỗ trợ dev self-test cycle |
| `seed-*.sh` | `scripts/dev/` (folder này) | Seed data scripts cho local DB |
| `smoke-*.sh` | `scripts/dev/` (folder này) | Smoke test + creds retrieval helpers |
| Production scripts | `scripts/` (root) hoặc `infrastructure/` | Tách biệt khỏi dev tooling |
| CI scripts | `scripts/` (root) hoặc `.github/workflows/` | Không phụ thuộc local DB |
| Service-specific scripts | `kitehub/{service}/scripts/` | Scoped tới service đó |

**Naming convention:** `kebab-case.sh` với prefix mô tả mục đích
(`self-test-*`, `seed-*`, `smoke-*`).

---

## 4. Archive policy

Khi 1 script trong folder này lỗi thời (Phase 2+ replace, hoặc test artifact
chuyển sang Playwright):

1. Move file → `scripts/dev/_archived/YYYY-MM-DD-<original-name>.sh`
2. Update README §2 directory map
3. Cite archive reason trong commit message
4. KHÔNG xóa hẳn trừ khi >90 ngày không reference

Tham chiếu: `.claude/rules/docs-folder-structure.md` §5 (archive convention).

---

## 5. Self-test cycle usage

### 5.1 Cycle diagram

```
┌──────────────────────────────────────────────────────────────────┐
│  DEV SELF-TEST CYCLE (Wave 87)                                   │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐  │
│  │ Step 1      │→ │ Step 2       │→ │ Step 3                  │  │
│  │ PREFLIGHT   │  │ SEED         │  │ WALKTHROUGH             │  │
│  │ (6 gates)   │  │ (7 personas) │  │ (94 USER-VERIFY rows)   │  │
│  └─────────────┘  └──────────────┘  └────────────┬────────────┘  │
│                                                  │               │
│                                                  ▼               │
│                                       ┌─────────────────────┐    │
│                                       │ Step 4 RESET        │    │
│                                       │ (TRUNCATE + FLUSH)  │    │
│                                       └──────────┬──────────┘    │
│                                                  │               │
│                                                  ▼               │
│                                  (round mới — quay lại Step 2)   │
└──────────────────────────────────────────────────────────────────┘
```

### 5.2 Cycle commands

```bash
# Step 1: Preflight — kiểm tra 6 gates (Docker, Flyway, role, ALB, DNS, Resend)
bash scripts/dev/self-test-preflight.sh

# Step 2: Seed personas (Bucket A — sau khi merge)
bash scripts/dev/seed-personas.sh

# Step 3: Lấy creds (Bucket A — sau khi merge) + walk-through CSV
bash scripts/dev/smoke-creds.sh
#   → mở CSV: documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv
#   → tick từng row PASS/FAIL theo verify_via

# Step 4: Reset giữa các round (DRY-RUN trước để xem plan)
bash scripts/dev/self-test-reset.sh --dry-run
bash scripts/dev/self-test-reset.sh
```

### 5.3 Help mọi script

Mỗi script support `--help` (hoặc `-h`):

```bash
bash scripts/dev/self-test-preflight.sh --help
bash scripts/dev/self-test-reset.sh --help
```

---

## 6. Troubleshooting

### Gate 1 fail (Docker stack)
- Run `bash kitehub/scripts/up.sh` để start stack
- Check `docker ps` xem container nào missing
- Restart Docker Desktop nếu container start nhưng không healthy

### Gate 2 fail (Flyway)
- Postgres container start chưa xong → wait 30s rồi chạy lại
- Migration fail → check log `docker logs kitehub-subscription | grep -i flyway`

### Gate 3 fail (Admin role mismatch)
- Tham chiếu `GAP-518` (admin role canonical sync)
- Fix BE seed hoặc FE role-guard tới khi cùng dùng `PLATFORM_ADMIN`

### Gate 4 fail (ALB unreachable)
- Check AWS console → Load Balancer status
- Verify security group cho port 443 (per Wave 86 deploy)

### Gate 5 fail (DNS)
- `dig +short api.kitehub.me` empty → check Cloudflare DNS record
- Có thể Cloudflare cutover chưa hoàn tất (Wave 86 PR #1466)

### Gate 6 fail (Resend)
- Tạo `.env.test` từ `.env.test.example` (Bucket A) + set `RESEND_API_KEY`
- Set `RESEND_VERIFIED_RECIPIENTS=admin@kitehub.me,owner@sky-education.test,...`
- API key invalid → rotate trong Resend dashboard

### Reset script fail (seed-personas.sh chưa exist)
- Bucket A chưa merge → script in WARN nhưng vẫn TRUNCATE + FLUSH
- Sau khi Bucket A merge, re-run `scripts/dev/seed-personas.sh` thủ công

---

## 7. Tham chiếu

- **Wave 87 plan:** [`documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md`](../../documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md)
- **CSV walkthrough:** [`documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`](../../documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv)
- **Rule docs structure:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)
- **Rule pre-handoff verify:** [`.claude/rules/pre-handoff-self-test-completeness.md`](../../.claude/rules/pre-handoff-self-test-completeness.md)
- **Gaps:** GAP-518 (admin role), GAP-519 (admin nav), GAP-523 (CORS) — Bucket D
