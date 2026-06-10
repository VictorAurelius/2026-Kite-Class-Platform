# kitehub-banner-renderer

**Last Updated:** 2026-06-10

HTML → WebP banner rasterise sidecar for `kitehub-branding` (GAP-1135).

## Why this exists

The TEMPLATE-mode banner pipeline (ADR-037 Amendment) composes a deterministic
3-layer banner **HTML** (`BannerHtmlComposer`) and hands it to the `BannerRenderer`
seam to rasterise into an image. Rasterising HTML needs a headless browser
(Chromium/Playwright). Bundling Node + Chromium into the JVM `kitehub-branding`
image would bloat it ~400MB, so the browser lives **here**, in a dedicated Node
sidecar. `kitehub-branding`'s `PlaywrightBannerRenderer` POSTs the composed HTML
and gets back a WebP, which it stores (MinIO/S3) and returns the URL for.

When `BANNER_RENDERER_URL` is unset on `kitehub-branding` (production default
until this sidecar is deployed there), `StubBannerRenderer` falls back to the
uploaded logo / template placeholder — the seam degrades gracefully.

## API

| Method | Path | Body | Response |
|--------|------|------|----------|
| `POST` | `/render` | `{ "html": string, "width"?: number, "height"?: number }` | `image/webp` bytes |
| `GET` | `/health` | — | `200 ok` |

Defaults: `width=1200`, `height=630` (OG-ready), `deviceScaleFactor=2`,
WebP quality 88 — matches `kiteclass-frontend/scripts/compose-sky-demo-banner.mjs`.

## Stack

- Base image `mcr.microsoft.com/playwright:v1.49.1-noble` (Node 20 + Chromium + OS deps)
- `playwright` (Chromium `setContent` + `screenshot`) + `sharp` (PNG → WebP)
- Single long-lived browser, one page per request, `--no-sandbox` for container

## Local run

Started automatically with the branding stack (profiles `branding-only`,
`branding-only-no-ai`, `full`):

```bash
bash kitehub/scripts/rebuild.sh banner-renderer   # build + (re)start
```

`kitehub-branding` reaches it by service name: `http://kitehub-banner-renderer:3000/render`.

## Env

| Var | Default | Purpose |
|-----|---------|---------|
| `PORT` | `3000` | Listen port |
| `RENDER_TIMEOUT_MS` | `15000` | Per-render Chromium timeout |
