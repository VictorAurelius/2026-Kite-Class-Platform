#!/usr/bin/env python3
"""Live local host for the gap board — always-fresh, reads gap-status.csv per request.

Usage:
    python3 scripts/serve-gap-board.py            # http://127.0.0.1:8787
    python3 scripts/serve-gap-board.py -p 9090    # custom port

Unlike the static `render-gap-board.py` (one-shot HTML file), this serves a LIVE
host: every page load re-reads the canonical `gap-status.csv`, so the board is
always current — no manual regenerate after a gap status flip. Stdlib only, zero
deps, binds 127.0.0.1 (local only). Ctrl-C to stop.

Routes:
  GET /                       → fresh kanban board (re-rendered from CSV)
  GET /<relative-gap-path>.md → raw gap markdown (so card links open)
"""
import argparse
import html as _html
import importlib.util
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parent.parent
GAPS_DIR = ROOT / "documents/04-quality/gaps"

GAP_PAGE = """<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>/*TITLE*/</title>
<style>
 body{max-width:900px;margin:0 auto;padding:24px;font:14px/1.6 -apple-system,Segoe UI,Roboto,sans-serif;color:#1f2933}
 a.back{display:inline-block;margin-bottom:16px;color:#1565c0;text-decoration:none}
 h1{font-size:22px;border-bottom:2px solid #eceff1;padding-bottom:8px} h2{font-size:17px;margin-top:24px}
 h3{font-size:15px} code{background:#eef1f4;padding:1px 5px;border-radius:4px;font-size:.9em}
 pre{background:#1f2933;color:#e6edf3;padding:12px;border-radius:8px;overflow:auto} pre code{background:none;color:inherit}
 table{border-collapse:collapse;width:100%;margin:12px 0;font-size:13px} th,td{border:1px solid #dfe3e8;padding:6px 9px;text-align:left;vertical-align:top}
 th{background:#eceff1} hr{border:none;border-top:1px solid #dfe3e8;margin:18px 0} li{margin:2px 0}
</style></head><body><a class="back" href="/">← Gap Board</a>/*BODY*/</body></html>"""


def _md_link(m: "re.Match") -> str:
    text, href = m.group(1), m.group(2)
    gm = re.search(r"(GAP-\d+)", href)
    if href.endswith(".md") and gm:          # gap-file link → in-app /gap/<id>
        href = "/gap/" + gm.group(1)
    return f'<a href="{href}">{text}</a>'


def _inline(s: str) -> str:
    s = _html.escape(s)
    s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s)
    s = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", s)
    s = re.sub(r"\[([^\]]+)\]\(([^)\s]+)\)", _md_link, s)
    # Bare GAP-NNN references → clickable. Lookbehind skips ids already inside an
    # href (/gap/…), link text (>…<) or <code> so we never nest/double-link.
    s = re.sub(r"(?<![/>\w-])(GAP-\d+)\b", r'<a href="/gap/\1">\1</a>', s)
    return s


def render_markdown(md: str) -> str:
    """Minimal, safe markdown→HTML for gap files (headings/bold/code/lists/tables/hr/links)."""
    out, table = [], []
    in_code = in_ul = False

    def flush_table():
        nonlocal table
        if not table:
            return
        rows = [r for r in table if not re.match(r"^\s*\|?[\s:|-]+\|?\s*$", r)]
        html_rows = []
        for idx, r in enumerate(rows):
            cells = [c.strip() for c in r.strip().strip("|").split("|")]
            tag = "th" if idx == 0 else "td"
            html_rows.append("<tr>" + "".join(f"<{tag}>{_inline(c)}</{tag}>" for c in cells) + "</tr>")
        out.append("<table>" + "".join(html_rows) + "</table>")
        table = []

    def close_ul():
        nonlocal in_ul
        if in_ul:
            out.append("</ul>")
            in_ul = False

    for ln in md.split("\n"):
        if ln.strip().startswith("```"):
            if in_code:
                out.append("</code></pre>")
                in_code = False
            else:
                close_ul()
                flush_table()
                out.append("<pre><code>")
                in_code = True
            continue
        if in_code:
            out.append(_html.escape(ln))
            continue
        if ln.strip().startswith("|"):
            close_ul()
            table.append(ln)
            continue
        flush_table()
        m = re.match(r"^(#{1,4})\s+(.*)", ln)
        if m:
            close_ul()
            lvl = len(m.group(1))
            out.append(f"<h{lvl}>{_inline(m.group(2))}</h{lvl}>")
            continue
        if re.match(r"^\s*[-*]\s+", ln):
            if not in_ul:
                out.append("<ul>")
                in_ul = True
            out.append("<li>" + _inline(re.sub(r"^\s*[-*]\s+", "", ln)) + "</li>")
            continue
        close_ul()
        if re.match(r"^\s*---+\s*$", ln):
            out.append("<hr>")
            continue
        out.append("<p>" + _inline(ln) + "</p>" if ln.strip() else "")
    if in_ul:
        out.append("</ul>")
    if in_code:
        out.append("</code></pre>")
    flush_table()
    return "\n".join(out)

# render-gap-board.py has a hyphen → not import-able by name; load via importlib.
_spec = importlib.util.spec_from_file_location(
    "render_gap_board", Path(__file__).resolve().parent / "render-gap-board.py")
rgb = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rgb)


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, body, ctype="text/html; charset=utf-8"):
        data = body.encode("utf-8") if isinstance(body, str) else body
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = unquote(self.path.split("?", 1)[0].lstrip("/"))
        if path in ("", "index.html", "board"):
            try:
                rows = rgb.load_rows(rgb.CSV_PATH)
                self._send(200, rgb.build_html(rows))
            except Exception as exc:  # noqa: BLE001 — surface render errors to browser
                self._send(500, f"<pre>render error: {exc}</pre>")
            return
        # Card link → resolve gap by ID (robust to subfolder/moves), render to HTML.
        if path.startswith("gap/"):
            gid = path[4:].strip("/")
            if re.match(r"^GAP-\d+$", gid):
                matches = sorted(GAPS_DIR.rglob(f"{gid}-*.md"))
                if matches:
                    body = render_markdown(matches[0].read_text(encoding="utf-8"))
                    page = GAP_PAGE.replace("/*TITLE*/", _html.escape(gid)).replace("/*BODY*/", body)
                    self._send(200, page)
                    return
            self._send(404, f"<a href='/'>← Gap Board</a><h1>404 — {_html.escape(gid)} not found</h1>")
            return
        # Back-compat: raw .md path (confined to gaps dir).
        if path.endswith(".md"):
            target = (GAPS_DIR / path).resolve()
            if str(target).startswith(str(GAPS_DIR.resolve())) and target.is_file():
                self._send(200, render_markdown(target.read_text(encoding="utf-8")))
                return
        self._send(404, "<a href='/'>← Gap Board</a><h1>404</h1>")

    def log_message(self, *_args):  # quiet — no per-request stderr spam
        pass


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-p", "--port", type=int, default=8787)
    ap.add_argument("--host", default="127.0.0.1")
    args = ap.parse_args()
    srv = ThreadingHTTPServer((args.host, args.port), Handler)
    n = len(rgb.load_rows(rgb.CSV_PATH))
    print(f"Gap board live host → http://{args.host}:{args.port}  ({n} gaps, re-read per request)")
    print("Ctrl-C to stop.")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print("\nstopped.")
        srv.server_close()


if __name__ == "__main__":
    main()
