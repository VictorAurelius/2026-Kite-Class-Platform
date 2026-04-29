// kiteclass-pro v2 — extends _v1-baseline/app.jsx
//
// Persona: P2 Center Owner (medium density desktop primary)
// Direction B (HIGHEST priority Wave UI Kits Round 2)
//
// This file is a **future port target** — when this kit graduates from prototype
// to production (Track 2 GAP-263..267), the React app here drops into
// `kiteclass-frontend/src/app/(owner)/dashboard/` after Next.js conversion.
//
// For human review of v2 we ship 10 STATIC HTML screens under `screens/`.
// They use the same CSS tokens (`./styles.css`) so visual parity is enforced
// at the token layer rather than at the component layer.
//
// What changed vs _v1-baseline/app.jsx (when this is ported to live React):
//   1. Stat cards expanded 4 → 6 (added attendance-rate + late-fees card)
//   2. Sparklines now have visible end-dot pulse on milestone touch
//   3. Command palette: `Recent` + `Pinned` sections at top (was: action-only)
//   4. Command palette: 20+ commands grouped into 6 sections (Recent, Pinned, Search, Action, Navigation, Prefs)
//   5. Skeleton loader: matched-shape shimmer (was: gray block placeholders)
//   6. Drag-drop: drop-target ghost preview where item lands (was: just opacity drop)
//   7. Drag-drop: edit-mode banner with Reset/Save buttons + state shape spec
//   8. Dark-mode toggle: 4-frame sun→moon morph 300ms ease-out (was: instant swap)
//   9. Success milestone view: dedicated celebration banner + achievement timeline
//  10. Confetti: declarative 8-piece SVG burst with auto-dismiss toast (was: imperative DOM-injection)
//
// State shape (localStorage `pro-widgets-layout` v1):
//   { user_id, layout: [{widget_id,x,y,w,h}], hidden: [widget_id], saved_at }

const { useState, useEffect, useRef, useCallback } = React;

const Icon = ({ name, className = 'h-4 w-4', style }) => (
  <i data-lucide={name} className={className} style={style}></i>
);

// Sparkline SVG — preserves baseline geometry, adds optional milestone-touch pulse
function Spark({ data, color, filled = true, milestonePulse = false }) {
  const w = 80, h = 32;
  const max = Math.max(...data), min = Math.min(...data);
  const pts = data.map((v, i) => {
    const x = (i / (data.length - 1)) * w;
    const y = h - ((v - min) / (max - min || 1)) * (h - 4) - 2;
    return [x, y];
  });
  const d = pts.map((p, i) => (i === 0 ? 'M' : 'L') + p[0].toFixed(1) + ',' + p[1].toFixed(1)).join(' ');
  const a = d + ` L${w},${h} L0,${h} Z`;
  const last = pts[pts.length - 1];
  return (
    <svg className="pro-spark" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none">
      {filled && <path d={a} fill={color} opacity=".15" />}
      <path d={d} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
      <circle cx={last[0]} cy={last[1]} r="2.2" fill={color} />
      {milestonePulse && <circle cx={last[0]} cy={last[1]} r="6" fill={color} opacity=".25"><animate attributeName="r" from="2.2" to="8" dur="1.4s" repeatCount="indefinite"/><animate attributeName="opacity" from=".25" to="0" dur="1.4s" repeatCount="indefinite"/></circle>}
    </svg>
  );
}

// CommandPalette — extended to v2: Recent + Pinned at top, 20+ commands across 6 sections
function CommandPalette({ onClose, onAction }) {
  const [q, setQ] = useState('');
  const [sel, setSel] = useState(0);

  const sections = [
    { sec: '📌 Gần đây', key: 'recent', rows: [
      { ic: 'calendar-check', t: 'Điểm danh nhanh — IELTS Foundation 12A2', k: ['↵'], a: 'recent-attendance' },
      { ic: 'user',           t: 'Mở hồ sơ học viên: Nguyễn Văn An',         a: 'recent-student' },
      { ic: 'file-text',      t: 'Xuất báo cáo doanh thu T3/2026',            a: 'recent-report' },
    ]},
    { sec: '⭐ Ghim', key: 'pinned', rows: [
      { ic: 'qr-code',   t: 'Hiển thị QR điểm danh phòng học hiện tại', k: ['Q'], a: 'pin-qr' },
      { ic: 'megaphone', t: 'Gửi tin nhắn hàng loạt cho phụ huynh',     k: ['M'], a: 'pin-broadcast' },
    ]},
    { sec: '🔍 Tìm nhanh', key: 'search', rows: [
      { ic: 'users',     t: 'Tìm học viên theo tên / SĐT / mã HV', k: ['/','hv'],  a: 'search-student' },
      { ic: 'book-open', t: 'Tìm lớp học theo tên / phòng / GV',    k: ['/','lop'], a: 'search-class' },
      { ic: 'receipt',   t: 'Tìm hóa đơn theo mã / khoản / ngày',   k: ['/','hd'],  a: 'search-invoice' },
      { ic: 'user-cog',  t: 'Tìm giáo viên theo tên / chuyên môn',  k: ['/','gv'],  a: 'search-teacher' },
    ]},
    { sec: '⚡ Hành động', key: 'action', rows: [
      { ic: 'user-plus',     t: 'Thêm học viên mới',                          k: ['S'], a: 'add-student' },
      { ic: 'calendar-plus', t: 'Tạo lớp học mới',                            k: ['C'], a: 'add-class' },
      { ic: 'check-square',  t: 'Đánh dấu điểm danh tất cả lớp hôm nay',       k: ['A'], a: 'attendance-all' },
      { ic: 'award',         t: 'Chốt điểm cuối kỳ — lớp đã chọn',            k: ['G'], a: 'finalize-grades' },
      { ic: 'send',          t: 'Gửi hóa đơn tự động cho HV chưa đóng',        k: ['I'], a: 'send-invoices' },
      { ic: 'file-text',     t: 'Xuất báo cáo tháng (PDF + Excel)',           k: ['R'], a: 'export-report' },
    ]},
    { sec: '🧭 Điều hướng', key: 'nav', rows: [
      { ic: 'layout-dashboard', t: 'Đi tới · Trang chủ',              k: ['G','D'], a: 'go-dashboard' },
      { ic: 'users',            t: 'Đi tới · Danh sách học viên',     k: ['G','S'], a: 'go-students' },
      { ic: 'book-open',        t: 'Đi tới · Lớp học',                k: ['G','C'], a: 'go-classes' },
      { ic: 'credit-card',      t: 'Đi tới · Học phí + Hóa đơn',      k: ['G','P'], a: 'go-payments' },
      { ic: 'trending-up',      t: 'Đi tới · Báo cáo + Phân tích',    k: ['G','R'], a: 'go-reports' },
      { ic: 'settings',         t: 'Đi tới · Cài đặt trung tâm',      k: ['G','X'], a: 'go-settings' },
    ]},
    { sec: '🎨 Tùy chọn', key: 'prefs', rows: [
      { ic: 'moon',    t: 'Đổi giao diện Sáng / Tối',            k: ['⇧','L'], a: 'toggle-theme' },
      { ic: 'palette', t: 'Mở AI Branding · cá nhân hóa thương hiệu', a: 'open-branding' },
    ]},
  ];

  const flat = sections.flatMap(s => s.rows.map(r => ({ ...r, sec: s.sec })));
  const filtered = q ? flat.filter(r => r.t.toLowerCase().includes(q.toLowerCase())) : flat;

  useEffect(() => { setSel(0); }, [q]);

  useEffect(() => {
    const h = (e) => {
      if (e.key === 'Escape') onClose();
      else if (e.key === 'ArrowDown') { e.preventDefault(); setSel(s => Math.min(s + 1, filtered.length - 1)); }
      else if (e.key === 'ArrowUp')   { e.preventDefault(); setSel(s => Math.max(s - 1, 0)); }
      else if (e.key === 'Enter')     { filtered[sel] && onAction(filtered[sel].a); }
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [filtered, sel, onClose, onAction]);

  return (
    <div className="pro-overlay" role="dialog" aria-modal="true" aria-label="Bảng lệnh" onClick={onClose}>
      <div className="pro-palette" onClick={e => e.stopPropagation()}>
        <input
          className="pro-palette-input"
          autoFocus
          placeholder="Tìm học viên, lớp, lệnh… (gõ /lop /hv để lọc)"
          value={q}
          onChange={e => setQ(e.target.value)}
          aria-label="Tìm lệnh"
        />
        <div className="pro-palette-list" role="listbox">
          {q
            ? filtered.map((r, i) => (
                <div
                  key={i}
                  className={'pro-palette-item' + (i === sel ? ' is-active' : '')}
                  role="option"
                  aria-selected={i === sel}
                  onMouseEnter={() => setSel(i)}
                  onClick={() => onAction(r.a)}
                >
                  <Icon name={r.ic} />
                  <span>{r.t}</span>
                  {r.k && <span className="kbd-list">{r.k.map((k, j) => <kbd key={j}>{k}</kbd>)}</span>}
                </div>
              ))
            : sections.map(s => (
                <React.Fragment key={s.key}>
                  <div className="pro-palette-sec">{s.sec}</div>
                  {s.rows.map((r, i) => {
                    const gi = flat.indexOf(r);
                    return (
                      <div
                        key={i}
                        className={'pro-palette-item' + (gi === sel ? ' is-active' : '')}
                        role="option"
                        aria-selected={gi === sel}
                        onMouseEnter={() => setSel(gi)}
                        onClick={() => onAction(r.a)}
                      >
                        <Icon name={r.ic} />
                        <span>{r.t}</span>
                        {r.k && <span className="kbd-list">{r.k.map((k, j) => <kbd key={j}>{k}</kbd>)}</span>}
                      </div>
                    );
                  })}
                </React.Fragment>
              ))}
        </div>
        <div className="pro-palette-hint">
          <span><kbd>↑</kbd><kbd>↓</kbd> Di chuyển</span>
          <span><kbd>↵</kbd> Chọn</span>
          <span><kbd>esc</kbd> Đóng</span>
          <span style={{marginLeft:'auto'}}><b>20+</b> lệnh</span>
        </div>
      </div>
    </div>
  );
}

// Confetti (declarative) — replaces _v1-baseline imperative DOM-injection
function ConfettiBurst({ active }) {
  if (!active) return null;
  return (
    <div aria-hidden="true">
      {[1,2,3,4,5,6,7,8].map(i => (
        <div key={i} className={`confetti-piece cf-${i}`}></div>
      ))}
      {/* duplicates for density */}
      <div className="confetti-piece cf-1" style={{left:'42%',animationDelay:'.4s'}}></div>
      <div className="confetti-piece cf-3" style={{left:'65%',animationDelay:'.5s'}}></div>
      <div className="confetti-piece cf-5" style={{left:'18%',animationDelay:'.45s'}}></div>
      <div className="confetti-piece cf-7" style={{left:'80%',animationDelay:'.55s'}}></div>
    </div>
  );
}

// Toast stack — extended with progress bar (auto-dismiss visualisation)
function Toasts({ toasts }) {
  return (
    <div className="pro-toast-stack" role="status" aria-live="polite">
      {toasts.map(t => (
        <div key={t.id} className={'pro-toast ' + (t.kind || '')}>
          <div className="pro-toast-ic">
            <Icon name={t.kind === 'err' ? 'x' : t.kind === 'warn' ? 'alert-triangle' : 'check'} />
          </div>
          <div style={{flex:1}}>
            <b>{t.title}</b>
            <p>{t.body}</p>
          </div>
          <div className="toast-progress-bar"></div>
        </div>
      ))}
    </div>
  );
}

// (Main ProApp component — port target; for prototype we ship static HTML screens.)
//
// In production:
//   - widgets state persisted via localStorage key `pro-widgets-layout` (v1 schema above)
//   - keyboard shortcut `⌘K` opens palette · `⇧L` toggles theme
//   - `setLoading` simulates 700ms delay → renders skeleton state from styles.css `.sk`
//   - `prefers-reduced-motion: reduce` disables sun→moon morph (theme swap is instant)
//
// Components exported: ProApp, CommandPalette, ConfettiBurst, Toasts, Spark, Icon

Object.assign(window, { CommandPalette, ConfettiBurst, Toasts, Spark, Icon });
