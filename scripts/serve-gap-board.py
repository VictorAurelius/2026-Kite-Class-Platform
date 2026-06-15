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
import importlib.util
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parent.parent
GAPS_DIR = ROOT / "documents/04-quality/gaps"

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
        # Serve gap .md files (card links). Confine to the gaps dir (no path escape).
        if path.endswith(".md"):
            target = (GAPS_DIR / path).resolve()
            if str(target).startswith(str(GAPS_DIR.resolve())) and target.is_file():
                self._send(200, target.read_text(encoding="utf-8"),
                           "text/markdown; charset=utf-8")
                return
        self._send(404, "<h1>404</h1>")

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
