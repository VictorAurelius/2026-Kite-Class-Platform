// KiteClass Pro Dashboard
const { useState, useEffect, useRef, useCallback } = React;
const Icon = ({ name, className = 'h-4 w-4', style }) => <i data-lucide={name} className={className} style={style}></i>;

// Sparkline SVG
function Spark({ data, color, filled = true }) {
  const w = 80, h = 32;
  const max = Math.max(...data), min = Math.min(...data);
  const pts = data.map((v, i) => {
    const x = (i / (data.length - 1)) * w;
    const y = h - ((v - min) / (max - min || 1)) * (h - 4) - 2;
    return [x, y];
  });
  const d = pts.map((p, i) => (i === 0 ? 'M' : 'L') + p[0].toFixed(1) + ',' + p[1].toFixed(1)).join(' ');
  const a = d + ` L${w},${h} L0,${h} Z`;
  return (
    <svg className="pro-spark" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none">
      {filled && <path d={a} fill={color} opacity=".15" />}
      <path d={d} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
      <circle cx={pts[pts.length - 1][0]} cy={pts[pts.length - 1][1]} r="2.2" fill={color} />
    </svg>
  );
}

// Area chart (tuần doanh thu)
function AreaChart({ data, colorA, colorB }) {
  const w = 720, h = 200, pad = { l: 32, r: 16, t: 12, b: 28 };
  const iw = w - pad.l - pad.r, ih = h - pad.t - pad.b;
  const allVals = data.flatMap(d => [d.a, d.b]);
  const max = Math.max(...allVals) * 1.1;
  const xStep = iw / (data.length - 1);
  const toY = v => pad.t + ih - (v / max) * ih;
  const toX = i => pad.l + i * xStep;
  const line = key => data.map((d, i) => (i === 0 ? 'M' : 'L') + toX(i).toFixed(1) + ',' + toY(d[key]).toFixed(1)).join(' ');
  const area = key => line(key) + ` L${toX(data.length - 1)},${pad.t + ih} L${pad.l},${pad.t + ih} Z`;
  const gridY = [0, 0.25, 0.5, 0.75, 1].map(t => pad.t + ih * t);
  return (
    <svg className="pro-chart" viewBox={`0 0 ${w} ${h}`}>
      <defs>
        <linearGradient id="gA" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor={colorA} stopOpacity=".35"/><stop offset="100%" stopColor={colorA} stopOpacity="0"/></linearGradient>
        <linearGradient id="gB" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor={colorB} stopOpacity=".25"/><stop offset="100%" stopColor={colorB} stopOpacity="0"/></linearGradient>
      </defs>
      {gridY.map((y, i) => <line key={i} x1={pad.l} x2={w - pad.r} y1={y} y2={y} stroke="currentColor" strokeOpacity=".08" />)}
      <path d={area('b')} fill="url(#gB)" />
      <path d={area('a')} fill="url(#gA)" />
      <path d={line('b')} stroke={colorB} strokeWidth="2" fill="none" strokeDasharray="3 3" opacity=".7" />
      <path d={line('a')} stroke={colorA} strokeWidth="2.2" fill="none" />
      {data.map((d, i) => (
        <g key={i}>
          <text x={toX(i)} y={h - 8} fontSize="10" fill="currentColor" fillOpacity=".5" textAnchor="middle" fontFamily="var(--font-mono)">{d.l}</text>
        </g>
      ))}
      {[0, 0.5, 1].map((t, i) => <text key={i} x={pad.l - 6} y={pad.t + ih * (1 - t) + 3} fontSize="9.5" fill="currentColor" fillOpacity=".5" textAnchor="end" fontFamily="var(--font-mono)">{Math.round(max * t / 1e6)}M</text>)}
    </svg>
  );
}

// Command palette
function CommandPalette({ onClose, onAction }) {
  const [q, setQ] = useState('');
  const [sel, setSel] = useState(0);
  const items = [
    { sec: 'Hành động', rows: [
      { ic: 'user-plus', t: 'Thêm học viên mới', k: ['S'], a: 'add-student' },
      { ic: 'calendar-plus', t: 'Tạo lớp học', k: ['C'], a: 'add-class' },
      { ic: 'calendar-check', t: 'Điểm danh nhanh', k: ['A'], a: 'attendance' },
      { ic: 'file-text', t: 'Xuất báo cáo tháng', k: ['R'], a: 'report' },
    ]},
    { sec: 'Điều hướng', rows: [
      { ic: 'layout-dashboard', t: 'Tổng quan', k: ['G','D'], a: 'nav-dashboard' },
      { ic: 'book-open', t: 'Lớp học', k: ['G','C'], a: 'nav-classes' },
      { ic: 'users', t: 'Học viên', k: ['G','S'], a: 'nav-students' },
      { ic: 'credit-card', t: 'Thanh toán', k: ['G','P'], a: 'nav-pay' },
    ]},
    { sec: 'Cài đặt', rows: [
      { ic: 'moon', t: 'Đổi sáng/tối', k: ['⇧','L'], a: 'theme' },
      { ic: 'palette', t: 'AI Branding', a: 'brand' },
    ]},
  ];
  const flat = items.flatMap(s => s.rows.map(r => ({ ...r, sec: s.sec })));
  const filtered = q ? flat.filter(r => r.t.toLowerCase().includes(q.toLowerCase())) : flat;
  useEffect(() => { setSel(0); }, [q]);
  useEffect(() => {
    const h = (e) => {
      if (e.key === 'Escape') onClose();
      else if (e.key === 'ArrowDown') { e.preventDefault(); setSel(s => Math.min(s + 1, filtered.length - 1)); }
      else if (e.key === 'ArrowUp') { e.preventDefault(); setSel(s => Math.max(s - 1, 0)); }
      else if (e.key === 'Enter') { filtered[sel] && onAction(filtered[sel].a); }
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [filtered, sel, onClose, onAction]);
  return (
    <div className="pro-overlay" onClick={onClose}>
      <div className="pro-palette" onClick={e => e.stopPropagation()}>
        <input className="pro-palette-input" autoFocus placeholder="Gõ để tìm lệnh, học viên, lớp..." value={q} onChange={e => setQ(e.target.value)} />
        <div className="pro-palette-list">
          {q ? (
            filtered.map((r, i) => (
              <div key={i} className={'pro-palette-item' + (i === sel ? ' is-active' : '')} onMouseEnter={() => setSel(i)} onClick={() => onAction(r.a)}>
                <Icon name={r.ic} className="h-4 w-4" /><span>{r.t}</span>
                {r.k && <span className="kbd-list">{r.k.map((k, j) => <kbd key={j}>{k}</kbd>)}</span>}
              </div>
            ))
          ) : (
            items.map(s => (
              <React.Fragment key={s.sec}>
                <div className="pro-palette-sec">{s.sec}</div>
                {s.rows.map((r, i) => {
                  const gi = flat.indexOf(r);
                  return (
                    <div key={i} className={'pro-palette-item' + (gi === sel ? ' is-active' : '')} onMouseEnter={() => setSel(gi)} onClick={() => onAction(r.a)}>
                      <Icon name={r.ic} className="h-4 w-4" /><span>{r.t}</span>
                      {r.k && <span className="kbd-list">{r.k.map((k, j) => <kbd key={j}>{k}</kbd>)}</span>}
                    </div>
                  );
                })}
              </React.Fragment>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

// Toast stack
function Toasts({ toasts }) {
  return (
    <div className="pro-toast-stack">
      {toasts.map(t => (
        <div key={t.id} className={'pro-toast ' + (t.kind || '')}>
          <div className="pro-toast-ic"><Icon name={t.kind === 'err' ? 'x' : t.kind === 'warn' ? 'alert-triangle' : 'check'} className="h-4 w-4" /></div>
          <div style={{flex:1}}>
            <b>{t.title}</b>
            <p>{t.body}</p>
          </div>
        </div>
      ))}
    </div>
  );
}

function fireConfetti() {
  const colors = ['#6366F1','#22D3EE','#22C55E','#F59E0B','#EC4899','#A855F7'];
  for (let i = 0; i < 40; i++) {
    const c = document.createElement('div');
    c.className = 'confetti';
    c.style.left = (20 + Math.random() * 60) + '%';
    c.style.background = colors[i % colors.length];
    c.style.animationDelay = (Math.random() * 0.3) + 's';
    c.style.transform = `rotate(${Math.random() * 360}deg)`;
    document.body.appendChild(c);
    setTimeout(() => c.remove(), 2000);
  }
}

function ProApp() {
  const [dark, setDark] = useState(() => localStorage.getItem('pro-dark') !== 'false');
  const [cmdOpen, setCmdOpen] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [widgets, setWidgets] = useState(() => JSON.parse(localStorage.getItem('pro-widgets') || 'null') || ['stats','chart','schedule','activity','quick','roster']);
  const [loading, setLoading] = useState(true);
  const dragRef = useRef(null);

  useEffect(() => {
    document.body.className = dark ? '' : 'pro-light';
    localStorage.setItem('pro-dark', dark);
  }, [dark]);
  useEffect(() => { localStorage.setItem('pro-widgets', JSON.stringify(widgets)); }, [widgets]);
  useEffect(() => { setTimeout(() => setLoading(false), 700); }, []);
  useEffect(() => { window.lucide && lucide.createIcons(); });

  const addToast = useCallback((t) => {
    const id = Date.now() + Math.random();
    setToasts(s => [...s, { id, ...t }]);
    setTimeout(() => setToasts(s => s.filter(x => x.id !== id)), 3800);
  }, []);

  useEffect(() => {
    const h = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') { e.preventDefault(); setCmdOpen(true); }
      if (e.shiftKey && e.key.toLowerCase() === 'l') { setDark(d => !d); }
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, []);

  const handleAction = (a) => {
    setCmdOpen(false);
    if (a === 'theme') { setDark(d => !d); addToast({ title: 'Đã đổi giao diện', body: 'Giao diện được ghi nhớ cho lần sau.' }); }
    else if (a === 'attendance') { fireConfetti(); addToast({ title: '🎉 Điểm danh hoàn tất!', body: 'Lớp IELTS Foundation K12 · 24/24 có mặt.' }); }
    else if (a === 'add-student') addToast({ title: 'Thêm học viên', body: 'Mở form nhập thông tin học viên mới.' });
    else if (a === 'report') addToast({ title: 'Đang xuất báo cáo', body: 'Tháng 4/2026 · PDF + Excel.' });
    else addToast({ title: 'Đã chọn lệnh', body: a });
  };

  const onDragStart = (i) => (e) => { dragRef.current = i; e.currentTarget.classList.add('dragging'); };
  const onDragEnd = (e) => { e.currentTarget.classList.remove('dragging'); document.querySelectorAll('.pro-widget').forEach(x => x.classList.remove('drop-target')); };
  const onDragOver = (i) => (e) => { e.preventDefault(); e.currentTarget.classList.add('drop-target'); };
  const onDragLeave = (e) => e.currentTarget.classList.remove('drop-target');
  const onDrop = (i) => (e) => {
    e.preventDefault();
    const from = dragRef.current;
    if (from == null || from === i) return;
    const next = [...widgets];
    const [moved] = next.splice(from, 1);
    next.splice(i, 0, moved);
    setWidgets(next);
    addToast({ title: 'Đã sắp xếp lại', body: 'Bố cục được lưu tự động.' });
  };

  const Widget = ({ id, i, children, span }) => (
    <div className={'pro-widget span-' + span} draggable onDragStart={onDragStart(i)} onDragEnd={onDragEnd} onDragOver={onDragOver(i)} onDragLeave={onDragLeave} onDrop={onDrop(i)}>
      <div className="pro-widget-handle"><Icon name="grip-vertical" className="h-4 w-4" /></div>
      {children}
    </div>
  );

  const renderWidget = (id, i) => {
    if (id === 'stats') return (
      <div className="pro-widget span-12" key={id} style={{padding:0,border:0,background:'transparent'}}>
        <div className="pro-grid">
          {[
            { l: 'Học viên', v: '428', t: '+12', ic: 'users', c: '#6366F1', data: [380,390,395,402,410,418,428] },
            { l: 'Lớp đang mở', v: '18', t: '+2', ic: 'book-open', c: '#22D3EE', data: [14,15,15,16,17,17,18] },
            { l: 'Buổi hôm nay', v: '12', t: '3 đang live', ic: 'calendar', c: '#F59E0B', data: [8,10,9,11,12,11,12] },
            { l: 'Doanh thu T4', v: '82.4M', t: '+8.2%', ic: 'trending-up', c: '#22C55E', data: [62,68,71,74,76,79,82.4] },
          ].map((s, j) => (
            <div className="pro-widget span-3" key={s.l}>
              <div className="pro-stat-head">
                <span className="pro-stat-label">{s.l}</span>
                <div className="pro-stat-icon" style={{background: s.c + '20', color: s.c}}><Icon name={s.ic} className="h-4 w-4"/></div>
              </div>
              {loading ? <div className="sk" style={{height:30,width:80,marginTop:8}}/> : <div className="pro-stat-value">{s.v}</div>}
              <div className="pro-stat-foot">
                <span className="pro-stat-trend up"><Icon name="arrow-up-right" className="h-3 w-3"/>{s.t}</span>
                <Spark data={s.data} color={s.c} />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
    if (id === 'chart') return (
      <Widget id={id} i={i} span="8" key={id}>
        <div className="pro-chart-head">
          <div>
            <h3 style={{margin:0}}>Doanh thu tuần</h3>
            <div style={{fontSize:11.5,color:'var(--pro-muted)',marginTop:2}}>4 tuần gần nhất · so với kỳ trước</div>
          </div>
          <div className="pro-chart-tabs">
            <button>7D</button><button className="is-active">4W</button><button>3M</button><button>1Y</button>
          </div>
        </div>
        <AreaChart
          data={[
            {l:'T1',a:62e6,b:58e6},{l:'T2',a:71e6,b:61e6},{l:'T3',a:68e6,b:64e6},{l:'T4',a:76e6,b:66e6},
            {l:'T5',a:74e6,b:68e6},{l:'T6',a:82e6,b:70e6},{l:'T7',a:88e6,b:72e6},
          ]}
          colorA="#6366F1" colorB="#22D3EE"
        />
        <div className="pro-chart-legend">
          <span><i style={{background:'#6366F1'}}/>Tuần này · 82.4M</span>
          <span><i style={{background:'#22D3EE'}}/>Tuần trước · 76M</span>
        </div>
      </Widget>
    );
    if (id === 'schedule') return (
      <Widget id={id} i={i} span="4" key={id}>
        <h3>Lịch hôm nay <a>Xem lịch →</a></h3>
        {[
          { t: '08:00', title: 'IELTS Foundation K12', m: 'P.201 · Cô Hương', live: true, c: '#6366F1' },
          { t: '10:00', title: 'TOEIC 600+ K05', m: 'P.305 · Thầy Khang', c: '#22D3EE' },
          { t: '14:00', title: 'TA Trẻ Em 6-8 K21', m: 'P.102 · Cô Lan', c: '#F59E0B' },
          { t: '16:00', title: 'Giao tiếp B1 K08', m: 'P.203 · Thầy Minh', c: '#A855F7' },
        ].map((s, k) => (
          <div key={k} className="pro-sched-item">
            <div className="pro-sched-time"><b>{s.t}</b></div>
            <div className="pro-sched-bar" style={{background: s.c}}/>
            <div className="pro-sched-body">
              <div className="pro-sched-title">{s.title}</div>
              <div className="pro-sched-meta">{s.m}</div>
            </div>
            {s.live && <span className="pro-sched-live">Live</span>}
          </div>
        ))}
      </Widget>
    );
    if (id === 'activity') return (
      <Widget id={id} i={i} span="6" key={id}>
        <h3>Hoạt động gần đây <a>Tất cả →</a></h3>
        <div className="pro-activity">
          {[
            { ic: 'user-plus', c: '#22C55E', t: <><b>Trần Quốc Bảo</b> đã đăng ký lớp <b>TOEIC 600+</b></>, time: '5 phút trước' },
            { ic: 'credit-card', c: '#6366F1', t: <><b>Nguyễn Minh Anh</b> đã thanh toán <b>2.400.000đ</b></>, time: '18 phút trước' },
            { ic: 'check-circle', c: '#22D3EE', t: <>Điểm danh <b>IELTS Foundation K12</b> · 24/24 có mặt</>, time: '1 giờ trước' },
            { ic: 'alert-triangle', c: '#F59E0B', t: <><b>3 học viên</b> đến hạn đóng học phí tuần này</>, time: '2 giờ trước' },
            { ic: 'book-open', c: '#A855F7', t: <>Lớp mới <b>IELTS 6.5+ K03</b> đã mở đăng ký</>, time: '4 giờ trước' },
          ].map((a, k) => (
            <div key={k} className="pro-activity-item">
              <div className="pro-activity-icon" style={{background: a.c + '20', color: a.c}}><Icon name={a.ic} className="h-3.5 w-3.5"/></div>
              <div className="pro-activity-body">{a.t}<div className="pro-activity-time">{a.time}</div></div>
            </div>
          ))}
        </div>
      </Widget>
    );
    if (id === 'quick') return (
      <Widget id={id} i={i} span="3" key={id}>
        <h3>Lối tắt</h3>
        <div className="pro-quick">
          {[
            { ic: 'user-plus', c: '#6366F1', t: 'Thêm HV', s: 'Mới' },
            { ic: 'qr-code', c: '#22C55E', t: 'QR điểm danh', s: 'Nhanh' },
            { ic: 'file-text', c: '#F59E0B', t: 'Báo cáo', s: 'PDF/Excel' },
            { ic: 'send', c: '#A855F7', t: 'Gửi tin', s: 'Hàng loạt' },
          ].map((q, k) => (
            <button key={k} className="pro-quick-btn" onClick={() => addToast({title: q.t, body: 'Đã mở — '+q.s})}>
              <div className="ic" style={{background: q.c+'20', color: q.c}}><Icon name={q.ic} className="h-4 w-4"/></div>
              <b>{q.t}</b><span>{q.s}</span>
            </button>
          ))}
        </div>
      </Widget>
    );
    if (id === 'roster') return (
      <Widget id={id} i={i} span="3" key={id}>
        <h3>Sắp đến hạn <a>Nhắc →</a></h3>
        {[
          ['Nguyễn Minh Anh','2.4M','#6366F1','warn'],
          ['Trần Quốc Bảo','1.8M','#22D3EE','warn'],
          ['Phạm Đức Anh','2.4M','#A855F7','warn'],
        ].map((r, k) => (
          <div key={k} style={{display:'flex',alignItems:'center',gap:10,padding:'8px 0',borderBottom:'1px solid var(--pro-border)'}}>
            <div style={{width:28,height:28,borderRadius:9999,background:r[2],color:'#fff',display:'flex',alignItems:'center',justifyContent:'center',fontWeight:600,fontSize:11}}>{r[0].split(' ').slice(-1)[0][0]}</div>
            <div style={{flex:1,minWidth:0}}>
              <div style={{fontSize:12.5,fontWeight:500,overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap'}}>{r[0]}</div>
              <div style={{fontSize:11,color:'var(--pro-muted)'}}>Còn 3 ngày</div>
            </div>
            <span className="pro-pill pro-pill-warn">{r[1]}</span>
          </div>
        ))}
      </Widget>
    );
  };

  return (
    <div className="pro-app" data-screen-label="01 Dashboard Pro">
      <aside className="pro-side">
        <div className="pro-brand">
          <div className="pro-brand-mark"><Icon name="graduation-cap" className="h-4 w-4"/></div>
          <div>
            <div className="pro-brand-name">SkyLight</div>
            <div className="pro-brand-sub">skylight.kitehub.me</div>
          </div>
        </div>
        <div className="pro-cmd" onClick={() => setCmdOpen(true)}>
          <Icon name="search" className="h-3.5 w-3.5"/> Tìm kiếm…
          <kbd>⌘K</kbd>
        </div>
        <nav className="pro-nav">
          <div className="pro-nav-section">Chính</div>
          <a className="is-active"><Icon name="layout-dashboard" className="h-4 w-4"/>Tổng quan</a>
          <a><Icon name="book-open" className="h-4 w-4"/>Lớp học</a>
          <a><Icon name="users" className="h-4 w-4"/>Học viên</a>
          <a><Icon name="calendar-check" className="h-4 w-4"/>Điểm danh<span className="pro-badge">3</span></a>
          <a><Icon name="calendar" className="h-4 w-4"/>Lịch học</a>
          <div className="pro-nav-section">Tài chính</div>
          <a><Icon name="credit-card" className="h-4 w-4"/>Học phí</a>
          <a><Icon name="trending-up" className="h-4 w-4"/>Báo cáo</a>
          <div className="pro-nav-section">Khác</div>
          <a><Icon name="message-square" className="h-4 w-4"/>Tin nhắn</a>
          <a><Icon name="settings" className="h-4 w-4"/>Cài đặt</a>
        </nav>
        <div className="pro-user">
          <div className="pro-avatar">LH</div>
          <div style={{flex:1,minWidth:0}}>
            <div className="pro-user-name">Lê Hoàng</div>
            <div className="pro-user-role">Quản lý trung tâm</div>
          </div>
        </div>
      </aside>
      <div className="pro-main">
        <div className="pro-top">
          <div className="pro-crumb"><span>Trung tâm</span> <Icon name="chevron-right" className="h-3 w-3"/> <b>Tổng quan</b></div>
          <div className="pro-top-actions">
            <button className="pro-iconbtn" onClick={() => setDark(d => !d)} title="Đổi sáng/tối (⇧L)"><Icon name={dark ? 'sun' : 'moon'} className="h-4 w-4"/></button>
            <button className="pro-iconbtn"><Icon name="bell" className="h-4 w-4"/><span className="pulse"/></button>
            <button className="pro-btn pro-btn-ghost" onClick={() => setCmdOpen(true)}><Icon name="search" className="h-3.5 w-3.5"/> Tìm <kbd style={{fontFamily:'var(--font-mono)',fontSize:10,background:'rgba(255,255,255,.08)',padding:'1px 5px',borderRadius:4,marginLeft:4}}>⌘K</kbd></button>
            <button className="pro-btn pro-btn-primary" onClick={() => { fireConfetti(); addToast({title:'🎉 Đã tạo!',body:'Lớp mới sẵn sàng đón học viên.'}); }}><Icon name="plus" className="h-4 w-4"/>Tạo mới</button>
          </div>
        </div>
        <div className="pro-content">
          <div className="pro-hello">
            <div>
              <h1>Chào buổi sáng, Hoàng <span className="wave">👋</span></h1>
              <p>Hôm nay có <b style={{color:'var(--pro-text)'}}>12 buổi học</b> · 3 lớp đang chờ điểm danh · doanh thu tuần <b style={{color:'var(--pro-success)'}}>+8.2%</b></p>
            </div>
            <div className="meta">
              <span>📅 Thứ Ba, <b>23/04/2026</b></span>
              <span>🌤️ Hà Nội · 26°C</span>
            </div>
          </div>
          <div className="pro-grid">
            {widgets.map((id, i) => renderWidget(id, i))}
          </div>
          <div style={{textAlign:'center',color:'var(--pro-muted)',fontSize:11.5,marginTop:8}}>
            💡 Mẹo: kéo-thả widget để sắp xếp lại · nhấn <kbd style={{fontFamily:'var(--font-mono)',background:'var(--pro-surface-2)',padding:'1px 6px',borderRadius:4,border:'1px solid var(--pro-border)'}}>⌘K</kbd> để mở lệnh nhanh · <kbd style={{fontFamily:'var(--font-mono)',background:'var(--pro-surface-2)',padding:'1px 6px',borderRadius:4,border:'1px solid var(--pro-border)'}}>⇧L</kbd> đổi sáng/tối
          </div>
        </div>
      </div>
      {cmdOpen && <CommandPalette onClose={() => setCmdOpen(false)} onAction={handleAction} />}
      <Toasts toasts={toasts} />
    </div>
  );
}

Object.assign(window, { ProApp });
