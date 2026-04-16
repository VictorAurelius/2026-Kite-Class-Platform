# UI Screenshots

Lưu screenshots UI audit của KiteClass và KiteHub frontends.

## Cấu trúc

```
documents/screenshots/
├── README.md                ← file này
├── kiteclass-latest/        ← KiteClass captures (regenerated mỗi lần)
│   ├── manifest.md          ← ✅ COMMITTED — index + quick reference
│   └── {page}/*.png         ← ❌ gitignored (local only)
├── kiteclass-{label}/       ← KiteClass labeled captures
│   ├── manifest.md          ← ✅ COMMITTED
│   └── {page}/*.png         ← ❌ gitignored
├── kitehub-latest/          ← KiteHub captures (regenerated mỗi lần)
│   ├── manifest.md          ← ✅ COMMITTED
│   └── {page}/*.png         ← ❌ gitignored
└── kitehub-{label}/         ← KiteHub labeled captures
    ├── manifest.md          ← ✅ COMMITTED
    └── ...
```

## Git Policy

| File | Status | Lý do |
|------|--------|-------|
| `*.png` | ❌ gitignored | Binary, lớn, local-only |
| `kiteclass-latest/**/*.png` | ❌ gitignored | Regenerated mỗi lần |
| `kitehub-latest/**/*.png` | ❌ gitignored | Regenerated mỗi lần |
| `*/manifest.md` | ✅ committed | Text, nhỏ, audit history |

## Chạy Capture

```bash
# Cả hai apps (KiteClass port 4700 + KiteHub port 4701)
./scripts/capture-ui-all.sh --label pr-XXX

# Chỉ KiteClass
cd kiteclass/kiteclass-frontend
npx tsx scripts/capture-screenshots.ts --label pr-XXX

# Chỉ KiteHub
cd kitehub/kitehub-frontend
npx tsx scripts/capture-screenshots.ts --label pr-XXX

# Latest (không label, không commit manifest)
./scripts/capture-ui-all.sh
```

## WSL2 + NTFS Note

Nếu dev server chưa chạy và bạn đang ở WSL2 mount NTFS (`/mnt/f/`):
1. Start dev server từ **Windows PowerShell** trước:
   ```powershell
   # KiteClass
   cd F:\nam4\doan\2026-Kite-Class-Platform\kiteclass\kiteclass-frontend
   npm run dev
   # KiteHub (cửa sổ mới)
   cd F:\nam4\doan\2026-Kite-Class-Platform\kitehub\kitehub-frontend
   npm run dev
   ```
2. Sau đó chạy capture từ WSL2 (kết nối tới localhost:4700/4701)

## manifest.md Format

Mỗi folder capture có 1 `manifest.md` auto-generated:

```markdown
# Screenshot Manifest — pr-XXX

**App:** KiteClass Frontend (port 4700)
**Generated:** 2026-04-03T10:30:00Z
**Screenshots:** 118 ok / 2 errors / 120 total

## Quick Index
| Page | Route | Auth | Screenshots | Notes |

## Pages
### `classes` → `/classes`
| File | Theme | Viewport | Size | Status |
| classes/light-desktop.png | light | desktop | 245KB | ✓ |
...
**Visual notes:** _(fill during audit)_
```

`manifest.md` là **quick reference** — không cần đọc lại ảnh để biết có gì trong folder.
Khi cần chi tiết visual, dùng skill `/ui-review` để Claude đọc và score screenshots.

## Skill

Xem `.claude/skills/quality/ui-review/SKILL.md` để biết cách chấm điểm /128 per screen.
