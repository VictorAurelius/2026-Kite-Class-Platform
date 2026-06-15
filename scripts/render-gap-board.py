#!/usr/bin/env python3
"""Render gap-status.csv as a self-contained Redmine-like kanban board (HTML).

Usage:
    python3 scripts/render-gap-board.py            # writes documents/04-quality/gaps/gap-board.html
    python3 scripts/render-gap-board.py -o /tmp/board.html

Read-only VIEW layer over the canonical CSV (per gap-architecture-v2.md +
meta-csv-index-pattern.md). Does NOT mutate gap-status.csv. Regenerate on demand.

- Default view: kanban grouped by status (OPEN / IN_PROGRESS / PARTIAL / PLANNED
  visible; DONE / WONTFIX hidden behind a toggle).
- Client-side filters (priority / domain / phase / text search) + table toggle.
- Each card links to its gap .md file (relative path).
- Zero external deps, zero server — open the HTML in a browser.
"""
import csv
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "documents/04-quality/gaps/gap-status.csv"
DEFAULT_OUT = ROOT / "documents/04-quality/gaps/gap-board.html"

STATUS_ORDER = ["OPEN", "IN_PROGRESS", "PARTIAL", "PLANNED", "DONE", "WONTFIX"]
ACTIVE_STATUSES = ["OPEN", "IN_PROGRESS", "PARTIAL", "PLANNED"]


def load_rows(csv_path: Path):
    rows = []
    with csv_path.open(encoding="utf-8") as f:
        # Skip leading comment lines (#...) and blank lines before the header.
        lines = [ln for ln in f if not ln.lstrip().startswith("#")]
    reader = csv.DictReader([ln for ln in lines if ln.strip()])
    for r in reader:
        if not r.get("id"):
            continue
        try:
            pct = int(r.get("completion_pct") or 0)
        except ValueError:
            pct = 0
        rows.append({
            "id": r["id"].strip(),
            "file": (r.get("filename") or "").strip(),
            "title": (r.get("title_short") or "").strip(),
            "status": (r.get("status") or "").strip() or "OPEN",
            "priority": (r.get("priority") or "").strip() or "P3",
            "domain": (r.get("domain") or "").strip() or "—",
            "phase": (r.get("phase") or "").strip() or "n/a",
            "pct": pct,
            "found": (r.get("found_date") or "").strip(),
            "verified": (r.get("last_verified") or "").strip(),
            "notes": (r.get("notes") or "").strip(),
        })
    return rows


HTML = r"""<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>KiteHub Gap Board</title>
<style>
  :root { --p0:#e74c3c; --p1:#e67e22; --p2:#f1c40f; --p3:#27ae60; --bg:#f4f6f8; --card:#fff; --line:#dfe3e8; }
  * { box-sizing: border-box; }
  body { margin:0; font:13px/1.45 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif; background:var(--bg); color:#1f2933; }
  header { position:sticky; top:0; z-index:5; background:#1f2933; color:#fff; padding:10px 16px; display:flex; flex-wrap:wrap; gap:10px; align-items:center; }
  header h1 { font-size:15px; margin:0 12px 0 0; font-weight:600; }
  header .counts { font-size:12px; opacity:.85; }
  .controls { display:flex; flex-wrap:wrap; gap:8px; align-items:center; padding:10px 16px; background:#fff; border-bottom:1px solid var(--line); position:sticky; top:40px; z-index:4; }
  .controls select, .controls input { font:12px inherit; padding:5px 8px; border:1px solid var(--line); border-radius:6px; }
  .controls input[type=text] { min-width:220px; }
  .controls label { font-size:12px; color:#52606d; display:flex; align-items:center; gap:4px; }
  .board { display:flex; gap:12px; padding:14px 16px; overflow-x:auto; align-items:flex-start; }
  .col { flex:1 0 280px; min-width:280px; background:#eceff1; border-radius:10px; padding:8px; }
  .col h2 { font-size:12px; text-transform:uppercase; letter-spacing:.04em; margin:4px 6px 8px; display:flex; justify-content:space-between; color:#3e4c59; }
  .col h2 .n { background:#cfd8dc; border-radius:10px; padding:1px 8px; font-size:11px; }
  .card { background:var(--card); border:1px solid var(--line); border-left:4px solid #bbb; border-radius:8px; padding:8px 10px; margin-bottom:8px; box-shadow:0 1px 2px rgba(0,0,0,.05); }
  .card.P0 { border-left-color:var(--p0); } .card.P1 { border-left-color:var(--p1); }
  .card.P2 { border-left-color:var(--p2); } .card.P3 { border-left-color:var(--p3); }
  .card .top { display:flex; justify-content:space-between; align-items:center; gap:6px; margin-bottom:3px; }
  .card a.id { font-weight:700; color:#1565c0; text-decoration:none; font-size:12px; }
  .card a.id:hover { text-decoration:underline; }
  .badge { font-size:10px; font-weight:700; color:#fff; border-radius:4px; padding:1px 6px; }
  .badge.P0 { background:var(--p0); } .badge.P1 { background:var(--p1); }
  .badge.P2 { background:var(--p2); color:#5c4a00; } .badge.P3 { background:var(--p3); }
  .card .title { font-size:12px; color:#1f2933; margin:2px 0 5px; }
  .card .meta { display:flex; flex-wrap:wrap; gap:6px; font-size:10.5px; color:#616e7c; }
  .card .meta span { background:#eef1f4; border-radius:4px; padding:1px 6px; }
  .bar { height:4px; background:#e3e8ec; border-radius:3px; margin-top:6px; overflow:hidden; }
  .bar > i { display:block; height:100%; background:#42a5f5; }
  /* table view */
  table { width:calc(100% - 32px); margin:14px 16px; border-collapse:collapse; background:#fff; font-size:12px; }
  th, td { border:1px solid var(--line); padding:5px 8px; text-align:left; vertical-align:top; }
  th { background:#eceff1; cursor:pointer; position:sticky; top:88px; }
  tr.P0 td:first-child { border-left:4px solid var(--p0); } tr.P1 td:first-child { border-left:4px solid var(--p1); }
  tr.P2 td:first-child { border-left:4px solid var(--p2); } tr.P3 td:first-child { border-left:4px solid var(--p3); }
  .hidden { display:none !important; }
  .empty { color:#9aa5b1; font-size:11px; padding:8px 6px; }
  footer { padding:10px 16px; color:#7b8794; font-size:11px; }
</style>
</head>
<body>
<header>
  <h1>🪁 KiteHub Gap Board</h1>
  <span class="counts" id="counts"></span>
</header>
<div class="controls">
  <input type="text" id="q" placeholder="Tìm id / title / notes…">
  <select id="fpri"><option value="">Priority: tất cả</option><option>P0</option><option>P1</option><option>P2</option><option>P3</option></select>
  <select id="fdom"><option value="">Domain: tất cả</option></select>
  <select id="fpha"><option value="">Phase: tất cả</option></select>
  <label><input type="checkbox" id="showDone"> Hiện DONE/WONTFIX</label>
  <label><input type="checkbox" id="tableView"> Table view</label>
  <span style="margin-left:auto;color:#52606d" id="shown"></span>
</div>
<div class="board" id="board"></div>
<table id="table" class="hidden"><thead><tr>
  <th data-k="id">ID</th><th data-k="priority">Pri</th><th data-k="status">Status</th>
  <th data-k="domain">Domain</th><th data-k="phase">Phase</th><th data-k="pct">%</th>
  <th data-k="title">Title</th><th data-k="verified">Verified</th>
</tr></thead><tbody id="tbody"></tbody></table>
<footer>View-only render of <code>gap-status.csv</code> (canonical per gap-architecture-v2.md). Regenerate: <code>python3 scripts/render-gap-board.py</code></footer>
<script>
const DATA = /*DATA*/;
const STATUS_ORDER = /*STATUS*/;
const ACTIVE = /*ACTIVE*/;
const $ = s => document.querySelector(s);
const el = (t,c) => { const e=document.createElement(t); if(c)e.className=c; return e; };

function uniq(k){ return [...new Set(DATA.map(d=>d[k]).filter(Boolean))].sort(); }
for(const d of uniq('domain')){ const o=el('option'); o.textContent=o.value=d; $('#fdom').appendChild(o); }
for(const p of uniq('phase')){ const o=el('option'); o.textContent=o.value=p; $('#fpha').appendChild(o); }

function counts(){
  const c={}; for(const d of DATA) c[d.status]=(c[d.status]||0)+1;
  $('#counts').textContent = STATUS_ORDER.filter(s=>c[s]).map(s=>`${s} ${c[s]}`).join('  ·  ') + `  ·  Σ ${DATA.length}`;
}

function match(d){
  const q=$('#q').value.toLowerCase().trim();
  if($('#fpri').value && d.priority!==$('#fpri').value) return false;
  if($('#fdom').value && d.domain!==$('#fdom').value) return false;
  if($('#fpha').value && d.phase!==$('#fpha').value) return false;
  if(!$('#showDone').checked && (d.status==='DONE'||d.status==='WONTFIX')) return false;
  if(q && !(d.id+' '+d.title+' '+d.notes).toLowerCase().includes(q)) return false;
  return true;
}

function card(d){
  const c=el('div','card '+d.priority);
  const top=el('div','top');
  const a=el('a','id'); a.href=d.file; a.textContent=d.id; a.title=d.file;
  const b=el('span','badge '+d.priority); b.textContent=d.priority;
  top.append(a,b); c.appendChild(top);
  const t=el('div','title'); t.textContent=d.title; c.appendChild(t);
  const m=el('div','meta');
  for(const v of [d.domain, d.phase, d.verified&&('✓ '+d.verified)].filter(Boolean)){ const s=el('span'); s.textContent=v; m.appendChild(s); }
  c.appendChild(m);
  if(d.pct>0&&d.pct<100){ const bar=el('div','bar'); const i=el('i'); i.style.width=d.pct+'%'; bar.appendChild(i); c.appendChild(bar); }
  return c;
}

function render(){
  const rows=DATA.filter(match);
  $('#shown').textContent = rows.length+' / '+DATA.length+' gaps';
  const table=$('#tableView').checked;
  $('#board').classList.toggle('hidden',table);
  $('#table').classList.toggle('hidden',!table);
  if(table){ renderTable(rows); return; }
  const board=$('#board'); board.innerHTML='';
  const cols = STATUS_ORDER.filter(s => $('#showDone').checked || ACTIVE.includes(s));
  for(const st of cols){
    const list=rows.filter(d=>d.status===st);
    const col=el('div','col');
    const h=el('h2'); h.innerHTML=`<span>${st}</span><span class="n">${list.length}</span>`; col.appendChild(h);
    if(!list.length){ const e=el('div','empty'); e.textContent='—'; col.appendChild(e); }
    list.sort((a,b)=>a.priority.localeCompare(b.priority) || b.pct-a.pct);
    for(const d of list) col.appendChild(card(d));
    board.appendChild(col);
  }
}

let sortK='id', sortAsc=true;
function renderTable(rows){
  rows=[...rows].sort((a,b)=>{ let x=a[sortK],y=b[sortK]; if(sortK==='pct'){x=+x;y=+y;} return (x>y?1:x<y?-1:0)*(sortAsc?1:-1); });
  const tb=$('#tbody'); tb.innerHTML='';
  for(const d of rows){
    const tr=el('tr',d.priority);
    const cells=[`<a href="${d.file}" style="color:#1565c0;text-decoration:none">${d.id}</a>`,d.priority,d.status,d.domain,d.phase,d.pct+'%',null,d.verified];
    cells.forEach((v,i)=>{ const td=el('td'); if(i===0||i===6){ if(i===0) td.innerHTML=v; else td.textContent=d.title; } else td.textContent=v; tr.appendChild(td); });
    tb.appendChild(tr);
  }
}
document.querySelectorAll('th[data-k]').forEach(th=>th.onclick=()=>{ const k=th.dataset.k; if(sortK===k)sortAsc=!sortAsc; else {sortK=k;sortAsc=true;} render(); });

['q','fpri','fdom','fpha','showDone','tableView'].forEach(id=>{ const e=$('#'+id); e.addEventListener(e.type==='checkbox'?'change':'input',render); });
counts(); render();
</script>
</body>
</html>
"""


def build_html(rows) -> str:
    """Render the full self-contained board HTML from parsed gap rows.

    Shared by the static renderer (main) and the live server (serve-gap-board.py).
    """
    # Escape HTML-sensitive chars so JSON values containing literal "</script>"
    # (e.g. XSS/JsonLd gap notes) cannot break out of the <script> block.
    # \uXXXX are valid JSON escapes — JS parses them back to the originals.
    data_json = (json.dumps(rows, ensure_ascii=False)
                 .replace("<", "\\u003c")
                 .replace(">", "\\u003e")
                 .replace("&", "\\u0026"))
    return (HTML
            .replace("/*DATA*/", data_json)
            .replace("/*STATUS*/", json.dumps(STATUS_ORDER))
            .replace("/*ACTIVE*/", json.dumps(ACTIVE_STATUSES)))


def main():
    out = Path(sys.argv[sys.argv.index("-o") + 1]) if "-o" in sys.argv else DEFAULT_OUT
    rows = load_rows(CSV_PATH)
    html = build_html(rows)
    out.write_text(html, encoding="utf-8")
    by_status = {}
    for r in rows:
        by_status[r["status"]] = by_status.get(r["status"], 0) + 1
    print(f"Rendered {len(rows)} gaps → {out}")
    print("  " + "  ".join(f"{s}={by_status[s]}" for s in STATUS_ORDER if s in by_status))


if __name__ == "__main__":
    main()
