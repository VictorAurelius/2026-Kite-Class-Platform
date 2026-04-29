# Preview Server Runbook

How to view UI kits in browser. Multiple options — pick what works for your iteration speed.

## Option 1 — Python static (fastest, no install)

```bash
# From repo root
python3 -m http.server 9999 --bind 127.0.0.1 &

# Open browser
http://127.0.0.1:9999/documents/02-architecture/design-system/ui_kits/

# Stop
pkill -f "http.server 9999"
```

**Pros:** No install. Works on any system with Python 3.
**Cons:** No hot reload — manual refresh after each edit.

## Option 2 — Vite dev server (hot reload)

```bash
cd documents/02-architecture/design-system/ui_kits
npx vite --port 9999 --host 127.0.0.1
```

**Pros:** Hot reload on file save.
**Cons:** Requires npm install (Vite ~50MB on first run).

## Option 3 — npx serve (lightweight middle-ground)

```bash
npx serve -l 9999 documents/02-architecture/design-system/ui_kits
```

**Pros:** Simple, npx-cached after first run.
**Cons:** No hot reload.

## URL conventions

After server starts, the URL pattern is:

| Path | URL |
|------|-----|
| Landing | `http://127.0.0.1:9999/documents/02-architecture/design-system/ui_kits/` |
| kiteclass-pro v2 index | `http://127.0.0.1:9999/documents/02-architecture/design-system/ui_kits/kiteclass-pro-v2/` |
| kiteclass-pro screen | `http://127.0.0.1:9999/.../kiteclass-pro-v2/screens/dashboard-default.html` |
| kiteclass-parent | `http://127.0.0.1:9999/.../kiteclass-parent/` |
| Component demo | `http://127.0.0.1:9999/.../components/G5-payment-method-selector/index.html` |

(If using Option 2 Vite, drop the `documents/...` prefix — Vite's working dir is already `ui_kits/`.)

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Port 9999 in use | `lsof -i :9999` to find process; pick another port (e.g. 9998) |
| `colors_and_type.css` 404 | Verify path: `_shared/colors_and_type.css` relative from kit folder |
| SVG assets 404 | Verify path: `../_shared/assets/kite-mark.svg` from kit/screens |
| CORS errors | Use `--bind 127.0.0.1` (already in Option 1 default) |
| Vietnamese fonts not rendering | Check `<link>` to Google Fonts loads with `subsets=vietnamese` |

## Auto-capture (deferred to Phase 2)

`scripts/capture-screenshots.mjs` will use Playwright to capture each screen at 320 / 768 / 1440 viewports × light + dark. Output to `_shared/captures/{kit}/{screen}-{viewport}-{theme}.png`.

Tracked in **GAP-264** (Phase 2 of GAP-263).

Until Phase 2 lands, manual screenshot workflow:
1. Open URL in browser
2. Resize window to target viewport (use browser DevTools device emulation)
3. Screenshot with OS shortcut (e.g. `Ctrl+Shift+S` snipping tool)
4. Paste into chat for Code reviewer (or save under `_shared/captures-manual/{kit}/`)
