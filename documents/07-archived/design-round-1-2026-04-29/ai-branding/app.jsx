// AI Branding Playground
const { useState, useEffect, useMemo } = React;
const Icon = ({ name, className = 'h-4 w-4', style }) => <i data-lucide={name} className={className} style={style}></i>;

// ---- color helpers ----
const hex2rgb = (hex) => {
  const h = hex.replace('#','');
  const v = h.length === 3 ? h.split('').map(c => c+c).join('') : h;
  return { r: parseInt(v.slice(0,2),16), g: parseInt(v.slice(2,4),16), b: parseInt(v.slice(4,6),16) };
};
const lum = ({r,g,b}) => {
  const f = (c) => { c/=255; return c<=.03928 ? c/12.92 : Math.pow((c+.055)/1.055, 2.4); };
  return .2126*f(r) + .7152*f(g) + .0722*f(b);
};
const contrast = (a,b) => {
  const la = lum(hex2rgb(a)), lb = lum(hex2rgb(b));
  const [hi,lo] = la>lb ? [la,lb] : [lb,la];
  return (hi+.05) / (lo+.05);
};
const tint = (hex, amt) => {
  const {r,g,b} = hex2rgb(hex);
  const m = (c) => Math.round(c + (255 - c) * amt);
  return '#' + [m(r),m(g),m(b)].map(c => c.toString(16).padStart(2,'0')).join('');
};
const shade = (hex, amt) => {
  const {r,g,b} = hex2rgb(hex);
  const m = (c) => Math.round(c * (1 - amt));
  return '#' + [m(r),m(g),m(b)].map(c => c.toString(16).padStart(2,'0')).join('');
};
const softAlpha = (hex, a=0.12) => {
  const {r,g,b} = hex2rgb(hex);
  return `rgba(${r},${g},${b},${a})`;
};

// ---- preset palettes ----
const PALETTES = [
  { id: 'sky', name: 'Sky Blue', desc: 'Tin cậy · ổn định', primary: '#0EA5E9', accent: '#F97316', bg: '#FFFFFF', fg: '#0F172A', display: 'system-ui' },
  { id: 'forest', name: 'Forest', desc: 'Tự nhiên · ấm', primary: '#16A34A', accent: '#F59E0B', bg: '#FAFDF7', fg: '#14532D', display: 'system-ui' },
  { id: 'rose', name: 'Rose Garden', desc: 'Trẻ em · vui', primary: '#EC4899', accent: '#8B5CF6', bg: '#FFF7FB', fg: '#831843', display: '"Comic Sans MS", system-ui' },
  { id: 'graphite', name: 'Graphite', desc: 'Cao cấp · tối giản', primary: '#0F172A', accent: '#F59E0B', bg: '#FFFFFF', fg: '#0F172A', display: 'Georgia, serif' },
  { id: 'mango', name: 'Mango', desc: 'Năng động · tươi', primary: '#F97316', accent: '#0EA5E9', bg: '#FFF7ED', fg: '#7C2D12', display: 'system-ui' },
  { id: 'royal', name: 'Royal', desc: 'Học thuật · sang', primary: '#6366F1', accent: '#F59E0B', bg: '#FAFAFF', fg: '#1E1B4B', display: 'Georgia, serif' },
];

const FONTS = [
  { id: 'sys', label: 'Hiện đại', family: 'system-ui, "Segoe UI", sans-serif' },
  { id: 'serif', label: 'Học thuật', family: 'Georgia, "Times New Roman", serif' },
  { id: 'rounded', name: 'Thân thiện', label: 'Thân thiện', family: '"Quicksand", system-ui, sans-serif' },
];

function App() {
  const [bizName, setBizName] = useState('SkyLight English');
  const [tagline, setTagline] = useState('Học tiếng Anh, bay cao hơn');
  const [domain, setDomain] = useState('skylight');
  const [logoText, setLogoText] = useState('S');

  const [primary, setPrimary] = useState('#0EA5E9');
  const [accent, setAccent] = useState('#F97316');
  const [bg, setBg] = useState('#FFFFFF');
  const [fg, setFg] = useState('#0F172A');
  const [activePreset, setActivePreset] = useState('sky');

  const [radius, setRadius] = useState(12);
  const [density, setDensity] = useState('medium');
  const [fontId, setFontId] = useState('sys');
  const [tab, setTab] = useState('app');
  const [toast, setToast] = useState(null);
  const [aiThinking, setAiThinking] = useState(false);

  useEffect(() => { window.lucide && lucide.createIcons(); });

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(null), 2400); };

  const applyPreset = (p) => {
    setPrimary(p.primary); setAccent(p.accent); setBg(p.bg); setFg(p.fg); setActivePreset(p.id);
    showToast(`✨ Áp dụng bảng "${p.name}"`);
  };

  // AI palette suggestion (deterministic, fakes it via name hash)
  const suggestFromLogo = () => {
    setAiThinking(true);
    setTimeout(() => {
      const h = (logoText + bizName).split('').reduce((a,c) => a + c.charCodeAt(0), 0);
      const pick = PALETTES[h % PALETTES.length];
      applyPreset(pick);
      setAiThinking(false);
      showToast(`🤖 AI gợi ý "${pick.name}" từ logo của bạn`);
    }, 900);
  };

  // contrast checks
  const cFgBg = useMemo(() => contrast(fg, bg), [fg, bg]);
  const cWhitePri = useMemo(() => contrast('#FFFFFF', primary), [primary]);
  const cWhiteAcc = useMemo(() => contrast('#FFFFFF', accent), [accent]);

  const verdict = (r) => r >= 7 ? 'pass' : r >= 4.5 ? 'warn' : 'fail';
  const verdictLabel = (r) => r >= 7 ? 'AAA' : r >= 4.5 ? 'AA' : 'Cảnh báo';

  const padding = density === 'compact' ? 10 : density === 'spacious' ? 18 : 14;
  const statSize = density === 'compact' ? 18 : density === 'spacious' ? 26 : 22;
  const font = FONTS.find(f => f.id === fontId).family;

  const cssVars = {
    '--t-primary': primary,
    '--t-primary-soft': softAlpha(primary, 0.12),
    '--t-accent': accent,
    '--t-bg': bg,
    '--t-fg': fg,
    '--t-radius': radius + 'px',
    '--t-card-pad': padding + 'px',
    '--t-stat-size': statSize + 'px',
    '--t-display': font,
    '--t-logo-bg': '#fff',
    '--t-logo-fg': primary,
    fontFamily: font,
  };

  const exportCss = `:root {
  --primary: ${primary};
  --primary-soft: ${softAlpha(primary,.12)};
  --accent: ${accent};
  --bg: ${bg};
  --fg: ${fg};
  --radius: ${radius}px;
  --pad: ${padding}px;
  --font-display: ${font};
}`;

  return (
    <div className="br-app">
      {/* ---- LEFT PANEL ---- */}
      <aside className="br-panel">
        <div className="br-head">
          <div className="mark"><Icon name="palette" className="h-4 w-4" style={{color:'#fff'}}/></div>
          <div>
            <h1>AI Branding Studio</h1>
            <p>Tùy biến KiteClass theo thương hiệu bạn — realtime.</p>
          </div>
        </div>

        <div className="br-section">
          <h3>Thông tin trung tâm</h3>
          <div className="br-tweak">
            <input className="br-text-input" placeholder="Tên trung tâm" value={bizName} onChange={e=>setBizName(e.target.value)}/>
            <input className="br-text-input" placeholder="Slogan" value={tagline} onChange={e=>setTagline(e.target.value)}/>
            <div style={{display:'flex',gap:6,alignItems:'center'}}>
              <input className="br-text-input" style={{flex:1}} placeholder="subdomain" value={domain} onChange={e=>setDomain(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g,''))}/>
              <span style={{fontSize:11,color:'rgba(248,250,252,.5)',fontFamily:'var(--font-mono)'}}>.kiteclass.com</span>
            </div>
          </div>
        </div>

        <div className="br-section">
          <h3>Logo {!aiThinking ? <button onClick={suggestFromLogo}><Icon name="sparkles" className="h-3 w-3"/> AI gợi ý</button> : <span className="br-ai-pop"><span className="dot"/>Đang phân tích...</span>}</h3>
          <div className="br-logo-up has">
            <div className="br-logo-preview" style={{background: primary, color:'#fff'}}>{logoText}</div>
            <input className="br-text-input" style={{textAlign:'center',marginBottom:6}} maxLength={2} value={logoText} onChange={e=>setLogoText(e.target.value.toUpperCase())}/>
            <small>Hoặc upload SVG / PNG (tối đa 2MB)</small>
            <div className="br-logo-suggestions">
              {['🪁','🎓','📚','🌟','🚀','🌈'].map(em => (
                <button key={em} onClick={() => setLogoText(em)}><span style={{fontSize:14}}>{em}</span></button>
              ))}
            </div>
          </div>
        </div>

        <div className="br-section">
          <h3>Bảng màu tổ chức {!aiThinking && <button onClick={suggestFromLogo}><Icon name="wand-2" className="h-3 w-3"/> Tạo từ logo</button>}</h3>
          <div className="br-palette-suggest">
            {PALETTES.map(p => (
              <div key={p.id} className={'br-palette-card' + (activePreset===p.id?' is-active':'')} onClick={() => applyPreset(p)}>
                <div className="br-palette-strip">
                  <span style={{background:p.primary}}/>
                  <span style={{background:p.accent}}/>
                  <span style={{background:tint(p.primary,.5)}}/>
                  <span style={{background:shade(p.primary,.3)}}/>
                </div>
                <div className="name">{p.name}</div>
                <div className="desc">{p.desc}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="br-section">
          <h3>Màu chính</h3>
          <div className="br-color-row">
            <div className="br-color-cell">
              <div className="br-swatch" style={{background:primary}}><input type="color" value={primary} onChange={e=>{setPrimary(e.target.value);setActivePreset(null);}}/></div>
              <div className="info"><div className="label">Primary</div><div className="hex">{primary.toUpperCase()}</div></div>
            </div>
            <div className="br-color-cell">
              <div className="br-swatch" style={{background:accent}}><input type="color" value={accent} onChange={e=>{setAccent(e.target.value);setActivePreset(null);}}/></div>
              <div className="info"><div className="label">Accent</div><div className="hex">{accent.toUpperCase()}</div></div>
            </div>
            <div className="br-color-cell">
              <div className="br-swatch" style={{background:bg, boxShadow:'inset 0 0 0 1px rgba(255,255,255,.2)'}}><input type="color" value={bg} onChange={e=>{setBg(e.target.value);setActivePreset(null);}}/></div>
              <div className="info"><div className="label">Nền</div><div className="hex">{bg.toUpperCase()}</div></div>
            </div>
            <div className="br-color-cell">
              <div className="br-swatch" style={{background:fg}}><input type="color" value={fg} onChange={e=>{setFg(e.target.value);setActivePreset(null);}}/></div>
              <div className="info"><div className="label">Chữ</div><div className="hex">{fg.toUpperCase()}</div></div>
            </div>
          </div>
        </div>

        <div className="br-section">
          <h3>Phong cách</h3>
          <div className="br-tweak">
            <div className="br-tweak-row">
              <label>Bo góc · {radius}px</label>
              <input type="range" min="0" max="24" value={radius} onChange={e=>setRadius(+e.target.value)} style={{flex:'0 0 100px'}}/>
            </div>
            <div className="br-tweak-row">
              <label>Mật độ</label>
              <div className="br-segment">
                {['compact','medium','spacious'].map(d => (
                  <button key={d} className={density===d?'is-active':''} onClick={()=>setDensity(d)}>{d==='compact'?'Gọn':d==='medium'?'Vừa':'Rộng'}</button>
                ))}
              </div>
            </div>
            <div className="br-tweak-row">
              <label>Font hiển thị</label>
              <div className="br-segment">
                {FONTS.map(f => (
                  <button key={f.id} className={fontId===f.id?'is-active':''} onClick={()=>setFontId(f.id)}>{f.label}</button>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="br-section">
          <button className="br-export" onClick={()=>showToast('💾 Đã lưu thương hiệu! KiteClass sẽ tự cập nhật trong 30s.')}>
            <Icon name="check" className="h-4 w-4"/> Lưu & xuất bản
          </button>
          <button className="br-export" style={{marginTop:8, background:'transparent', border:'1px solid rgba(255,255,255,.15)', color:'rgba(248,250,252,.85)'}} onClick={()=>showToast('📋 Đã copy CSS tokens vào clipboard.')}>
            <Icon name="copy" className="h-4 w-4"/> Copy CSS tokens
          </button>
        </div>
      </aside>

      {/* ---- RIGHT CANVAS ---- */}
      <main className="br-canvas">
        <div className="br-canvas-head">
          <div>
            <div className="title">Xem trước · {bizName || 'Trung tâm chưa đặt tên'}</div>
            <div className="sub">Mọi thay đổi áp dụng tức thời lên app, email, mobile.</div>
          </div>
          <div className="br-tabs">
            <button className={tab==='app'?'is-active':''} onClick={()=>setTab('app')}><Icon name="monitor" className="h-3.5 w-3.5"/>App</button>
            <button className={tab==='email'?'is-active':''} onClick={()=>setTab('email')}><Icon name="mail" className="h-3.5 w-3.5"/>Email</button>
            <button className={tab==='mobile'?'is-active':''} onClick={()=>setTab('mobile')}><Icon name="smartphone" className="h-3.5 w-3.5"/>Mobile</button>
            <button className={tab==='tokens'?'is-active':''} onClick={()=>setTab('tokens')}><Icon name="code" className="h-3.5 w-3.5"/>Tokens</button>
          </div>
        </div>

        <div className="br-grid">
          <div>
            {tab === 'app' && (
              <div className="br-frame" style={cssVars}>
                <div className="t-bar">
                  <div className="logo">{logoText}</div>
                  <div>
                    <div className="name">{bizName || 'Tên trung tâm'}</div>
                    <div className="dom">{domain || 'subdomain'}.kiteclass.com</div>
                  </div>
                  <div className="right">
                    <div className="pill">★ Pro</div>
                    <div style={{width:28,height:28,borderRadius:9999,background:'rgba(255,255,255,.2)'}}/>
                  </div>
                </div>
                <div className="t-body">
                  <div className="t-side">
                    <a className="active"><span className="ic"/>Tổng quan</a>
                    <a><span className="ic"/>Lớp học</a>
                    <a><span className="ic"/>Học viên</a>
                    <a><span className="ic"/>Điểm danh</a>
                    <a><span className="ic"/>Học phí</a>
                    <a><span className="ic"/>Báo cáo</a>
                  </div>
                  <div className="t-main">
                    <h2>Chào buổi sáng 👋</h2>
                    <p className="lead">{tagline || 'Slogan của trung tâm'}</p>
                    <div className="t-stats">
                      <div className="t-stat"><div className="l">Học viên</div><div className="v">428</div><div className="d">+12 tuần này</div></div>
                      <div className="t-stat"><div className="l">Lớp đang mở</div><div className="v">18</div><div className="d">3 đầy slot</div></div>
                      <div className="t-stat"><div className="l">Doanh thu T4</div><div className="v">82M</div><div className="d">+8.2%</div></div>
                    </div>
                    <div className="t-card">
                      <div className="t-card-head"><h3>Học viên gần đây</h3><span className="more">Xem tất cả →</span></div>
                      {[
                        ['Nguyễn Minh Anh', primary, 'IELTS · K12', 'suc', 'Mới'],
                        ['Trần Quốc Bảo', accent, 'TOEIC · K05', 'suc', 'Đang học'],
                        ['Phạm Đức Anh', shade(primary,.2), 'TA Trẻ Em · K21', 'warn', 'Sắp đến hạn'],
                      ].map((r,i)=>(
                        <div key={i} className="t-row">
                          <div className="av" style={{background:r[1]}}>{r[0].split(' ').slice(-1)[0][0]}</div>
                          <div className="nm">{r[0]}<div style={{fontSize:10.5,color:'rgba(15,23,42,.5)',fontWeight:400}}>{r[2]}</div></div>
                          <span className={'pl ' + r[3]}>{r[4]}</span>
                        </div>
                      ))}
                    </div>
                    <div className="t-btn-row">
                      <button className="t-btn"><Icon name="plus" className="h-3.5 w-3.5"/> Thêm học viên</button>
                      <button className="t-btn ghost"><Icon name="download" className="h-3.5 w-3.5"/> Xuất báo cáo</button>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {tab === 'email' && (
              <div className="br-email" style={cssVars}>
                <div className="br-email-head">
                  <div style={{width:32,height:32,borderRadius:9,background:'#fff',color:primary,display:'grid',placeItems:'center',fontWeight:800}}>{logoText}</div>
                  <div>
                    <div style={{fontWeight:700,fontSize:14}}>{bizName}</div>
                    <div style={{fontSize:10.5,opacity:.85,fontFamily:'var(--font-mono)'}}>noreply@{domain}.kiteclass.com</div>
                  </div>
                </div>
                <div className="br-email-body">
                  <h4>Chào mừng bạn đến với {bizName} 🎉</h4>
                  <p>Cảm ơn quý phụ huynh đã đăng ký lớp học cho bé. Chúng tôi sẽ liên hệ lại trong 24 giờ để xếp lịch học thử miễn phí.</p>
                  <p style={{fontStyle:'italic',color:primary}}>"{tagline}"</p>
                  <button className="t-btn" style={{background:primary,borderRadius:radius+'px'}}>Xem lịch học →</button>
                </div>
                <div className="br-email-foot">© 2026 {bizName} · Bạn nhận email này vì đã đăng ký tại {domain}.kiteclass.com</div>
              </div>
            )}

            {tab === 'mobile' && (
              <div style={{padding:'40px 0',display:'flex',gap:32,justifyContent:'center'}}>
                <div className="br-mobile" style={cssVars}>
                  <div className="br-mobile-screen">
                    <div className="top">
                      <div style={{display:'flex',alignItems:'center',gap:6,marginBottom:6}}>
                        <div style={{width:24,height:24,borderRadius:6,background:'#fff',color:primary,display:'grid',placeItems:'center',fontWeight:800,fontSize:12}}>{logoText}</div>
                        <div style={{fontSize:11,fontWeight:600}}>{bizName}</div>
                      </div>
                      <h5>Tổng quan tháng</h5>
                      <span>{tagline}</span>
                    </div>
                    <div className="mstat">
                      <div className="x"><b>428</b><span>Học viên</span></div>
                      <div className="x"><b>82M₫</b><span>Doanh thu</span></div>
                    </div>
                    {['Minh Anh','Quốc Bảo','Đức Anh'].map((n,i)=>(
                      <div key={i} className="mrow">
                        <div className="a" style={{background:[primary,accent,shade(primary,.2)][i]}}>{n.split(' ').slice(-1)[0][0]}</div>
                        <div style={{flex:1,fontSize:10.5,fontWeight:500}}>{n}</div>
                        <span style={{fontSize:9,color:primary,fontWeight:700}}>● Active</span>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="br-mobile" style={cssVars}>
                  <div className="br-mobile-screen">
                    <div className="top" style={{textAlign:'center',padding:'24px 14px'}}>
                      <div style={{width:40,height:40,borderRadius:10,background:'#fff',color:primary,display:'grid',placeItems:'center',fontWeight:800,fontSize:18,margin:'0 auto 8px'}}>{logoText}</div>
                      <h5>{bizName}</h5>
                      <span style={{fontSize:9}}>{tagline}</span>
                    </div>
                    <div style={{padding:'12px 4px',fontSize:11,color:'rgba(15,23,42,.6)',textAlign:'center'}}>Quét QR để điểm danh</div>
                    <div style={{margin:'0 auto',width:120,height:120,background:`repeating-conic-gradient(${fg} 0% 25%, ${bg} 0% 50%) 0 0/16px 16px`,borderRadius:radius+'px',border:`2px solid ${primary}`}}/>
                    <button className="t-btn" style={{margin:'14px auto 0',display:'flex',width:'80%',justifyContent:'center',fontSize:11}}>Điểm danh ngay</button>
                  </div>
                </div>
              </div>
            )}

            {tab === 'tokens' && (
              <pre className="br-tokens"><span className="c">/* design tokens · auto-generated */</span>{'\n'}<span className="k">:root</span> {'{'}{'\n'}  <span className="k">--primary:</span> <span className="v">{primary}</span>;{'\n'}  <span className="k">--primary-soft:</span> <span className="v">{softAlpha(primary,.12)}</span>;{'\n'}  <span className="k">--accent:</span> <span className="v">{accent}</span>;{'\n'}  <span className="k">--bg:</span> <span className="v">{bg}</span>;{'\n'}  <span className="k">--fg:</span> <span className="v">{fg}</span>;{'\n'}  <span className="k">--radius:</span> <span className="v">{radius}px</span>;{'\n'}  <span className="k">--pad:</span> <span className="v">{padding}px</span>;{'\n'}  <span className="k">--font-display:</span> <span className="v">{font}</span>;{'\n'}{'}'}{'\n\n'}<span className="c">/* Áp dụng vào: kiteclass-frontend/src/app/globals.css */}</span></pre>
            )}
          </div>

          <div style={{display:'flex',flexDirection:'column',gap:14}}>
            <div className="br-a11y">
              <h4><Icon name="shield-check" className="h-3.5 w-3.5" style={{color:'#4ADE80'}}/> Kiểm tra tương phản WCAG</h4>
              <div className="br-a11y-row">
                <div style={{display:'flex',gap:2}}>
                  <div className="swatch" style={{background:fg}}/>
                  <div className="swatch" style={{background:bg}}/>
                </div>
                <span className="label">Chữ trên nền</span>
                <span className="ratio">{cFgBg.toFixed(2)}</span>
                <span className={'badge '+verdict(cFgBg)}>{verdictLabel(cFgBg)}</span>
              </div>
              <div className="br-a11y-row">
                <div style={{display:'flex',gap:2}}>
                  <div className="swatch" style={{background:'#fff'}}/>
                  <div className="swatch" style={{background:primary}}/>
                </div>
                <span className="label">Trắng trên primary</span>
                <span className="ratio">{cWhitePri.toFixed(2)}</span>
                <span className={'badge '+verdict(cWhitePri)}>{verdictLabel(cWhitePri)}</span>
              </div>
              <div className="br-a11y-row">
                <div style={{display:'flex',gap:2}}>
                  <div className="swatch" style={{background:'#fff'}}/>
                  <div className="swatch" style={{background:accent}}/>
                </div>
                <span className="label">Trắng trên accent</span>
                <span className="ratio">{cWhiteAcc.toFixed(2)}</span>
                <span className={'badge '+verdict(cWhiteAcc)}>{verdictLabel(cWhiteAcc)}</span>
              </div>
              <div style={{fontSize:10.5,color:'rgba(248,250,252,.45)',marginTop:8,lineHeight:1.5}}>AA ≥ 4.5 · AAA ≥ 7. Phụ huynh thường xem trên màn nhỏ — cố gắng đạt AA tối thiểu.</div>
            </div>

            <div className="br-a11y">
              <h4><Icon name="eye" className="h-3.5 w-3.5" style={{color:'#22D3EE'}}/> Áp dụng tới</h4>
              {[
                ['Web app · KiteClass', 'monitor'],
                ['Email transaksi · phụ huynh', 'mail'],
                ['App phụ huynh (iOS/Android)', 'smartphone'],
                ['Báo cáo PDF', 'file-text'],
                ['QR điểm danh', 'qr-code'],
              ].map(([l,i],k)=>(
                <div key={k} style={{display:'flex',alignItems:'center',gap:10,padding:'7px 0',fontSize:12,color:'rgba(248,250,252,.75)',borderBottom:'1px solid rgba(255,255,255,.05)'}}>
                  <Icon name={i} className="h-3.5 w-3.5" style={{color:'rgba(248,250,252,.4)'}}/>
                  <span style={{flex:1}}>{l}</span>
                  <Icon name="check" className="h-3.5 w-3.5" style={{color:'#4ADE80'}}/>
                </div>
              ))}
            </div>

            <div className="br-a11y">
              <h4><Icon name="layers" className="h-3.5 w-3.5" style={{color:'#A78BFA'}}/> Tone palette tự sinh</h4>
              <div style={{display:'flex',gap:3,marginTop:6,height:36,borderRadius:8,overflow:'hidden'}}>
                {[0.85,0.7,0.5,0.3,0.15,0,0.15,0.3,0.5].map((t,i)=>(
                  <div key={i} style={{flex:1,background: i<5 ? tint(primary,t) : shade(primary,t)}}/>
                ))}
              </div>
              <div style={{fontSize:10.5,color:'rgba(248,250,252,.45)',marginTop:6,fontFamily:'var(--font-mono)'}}>50 · 100 · 200 · 300 · 400 · <b style={{color:'#fff'}}>500</b> · 600 · 700 · 800</div>
            </div>
          </div>
        </div>
      </main>

      {toast && <div className="br-toast">{toast}</div>}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
