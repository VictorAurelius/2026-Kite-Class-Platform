# Branching Strategy & Git Rules

> Pointer: read this when creating a new branch, unsure of base branch, or hitting git workflow restrictions. Parent skill: `../SKILL.md`.

<!-- TODO: verify against current state — project uses wave/* branches per CLAUDE.md, not the develop-based git-flow described below. Reconcile or annotate per actual practice. -->

## 🌳 Branching Strategy (Git Flow)

```
main ────●────────────────●────────────────●────────────────●─────► (Production)
         │                ▲                ▲                ▲
         │                │                │                │
develop ─┼──●──●──●──●────┼────●──●──●─────┼────●──●────────┼─────► (Integration)
         │  │             │    │           │    │           │
         │  │  feature/   │    │           │    │           │
         │  └──KC-123 ────┘    │           │    │           │
         │                     │           │    │           │
         │        feature/     │           │    │           │
         │        KC-456 ──────┘           │    │           │
         │                                 │    │           │
         │                    hotfix/      │    │           │
         │                    KC-789 ──────┴────┘           │
         │                                                  │
         │                                      release/    │
         │                                      v1.0 ───────┘
```

### Branch Types

| Branch | Pattern | From | Merge To | Purpose |
|--------|---------|------|----------|---------|
| `main` | `main` | - | - | Production code |
| `develop` | `develop` | `main` | `main` | Integration branch |
| `feature` | `feature/KC-{id}-{desc}` | `develop` | `develop` | New features |
| `bugfix` | `bugfix/KC-{id}-{desc}` | `develop` | `develop` | Bug fixes |
| `hotfix` | `hotfix/KC-{id}-{desc}` | `main` | `main`, `develop` | Urgent fixes |
| `release` | `release/v{version}` | `develop` | `main`, `develop` | Release prep |

### Branch Naming Rules

**Format:**
```
{type}/KC-{ticket-id}-{short-description}
```

**Examples:**
```bash
# Features
feature/KC-123-student-enrollment
feature/KC-456-qr-payment-integration

# Bug fixes
bugfix/KC-321-fix-attendance-calculation

# Hotfixes
hotfix/KC-999-security-vulnerability

# Releases
release/v1.0.0
```

**Rules:**
- Chỉ dùng lowercase
- Dùng dấu `-` thay space
- Giữ ngắn gọn (< 50 chars)
- Luôn có ticket ID

---

## ⚠️ Git Rules & Restrictions

### CRITICAL: Git Operations with GitHub CLI

**✅ WITH GITHUB CLI (`gh`) - AI CAN:**
- Create branches: `git checkout -b feature/new-branch`
- Commit changes: `git commit -m "message"`
- Push to remote: `git push origin <branch>` (after user confirmation)
- Create pull requests: `gh pr create --title "..." --body "..."`
- Check status: `git status`, `git log`, `gh pr status`

**❌ FORBIDDEN OPERATIONS:**
- Force push: `git push --force` (NEVER without explicit user request)
- Push to main directly: `git push origin main` (use PR workflow)
- Destructive operations: `git reset --hard`, `git clean -f` (see MEMORY.md)

**⏳ WORKFLOW:**
1. AI: Create feature branch: `git checkout -b feature/PR-X.X-name`
2. AI: Implement feature → commit locally
3. **AI: RUN LOCAL TESTS TRƯỚC KHI PUSH** (xem bước 3 chi tiết bên dưới)
4. AI: Ask user: "Tests pass, sẵn sàng push?"
5. **User**: Confirm "yes" hoặc request changes
6. AI: Push to remote: `git push -u origin feature/branch`
7. AI: Create PR: `gh pr create --title "..." --body "..."`
8. AI: Monitor CI via `scripts/check-ci.sh` (background)
9. AI: Return PR URL to user

**IMPORTANT:** Always ask before pushing to remote, even with GitHub CLI

### Bước 3: Self-Test Trước Khi Push (BẮT BUỘC)

**KHÔNG BAO GIỜ push code chưa test local.** CI là safety net, không phải nơi phát hiện lỗi lần đầu.

**LUÔN dùng script `scripts/test-local.sh`** — KHÔNG chạy lệnh test tự do.

```bash
# Auto-detect changed files → chạy tests phù hợp
./scripts/test-local.sh

# Quick mode (compile + checkstyle only, không full test)
./scripts/test-local.sh --quick

# Test cụ thể
./scripts/test-local.sh kiteclass core
./scripts/test-local.sh kiteclass frontend
./scripts/test-local.sh kiteclass all
```

Script tự động:
- Detect files changed (git diff) → chỉ test modules bị ảnh hưởng
- Skip nếu chỉ thay đổi docs
- Backend: compile + checkstyle + unit tests
- Frontend: vitest hoặc eslint (quick mode)
- Summary: passed/failed/skipped
- Exit code 1 nếu có failure → block push

**Nếu test fail → fix TRƯỚC khi push.** Không push code broken lên CI.
