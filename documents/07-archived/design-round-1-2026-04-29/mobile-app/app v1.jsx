// Parent / Student mobile app — multi-screen showcase
const { useState, useEffect } = React;
const Icon = ({ name, className = 'h-4 w-4', style }) => <i data-lucide={name} className={className} style={style}></i>;

// ───────── Screens ─────────
function HomeScreen() {
  return (
    <div className="app">
      <div className="app-hero deep">
        <div className="app-hero-row">
          <div className="app-avatar-md">A</div>
          <div style={{flex:1}}>
            <div className="app-greet">Chào buổi sáng</div>
            <div className="app-name">Phụ huynh Minh Anh</div>
          </div>
          <div className="app-bell"><Icon name="bell" className="h-4 w-4" style={{color:'#fff'}}/><span className="dot"/></div>
        </div>
        <div className="app-hero-kpi">
          <div className="k"><b>96%</b><span>Chuyên cần</span></div>
          <div className="k"><b>8.4</b><span>Điểm TB</span></div>
          <div className="k"><b>2</b><span>Lớp đang học</span></div>
        </div>
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Hôm nay · Thứ 3</h3><a>Tuần →</a></div>
        <div className="sched-chip live">
          <div className="sched-time live"><b>14</b><span>00 chiều</span></div>
          <div className="sched-bar" style={{background:'#F97316'}}/>
          <div className="sched-body">
            <div className="sched-title">TA Trẻ Em K21</div>
            <div className="sched-meta"><span>👩‍🏫 Cô Lan</span><span>📍 P.102</span></div>
          </div>
          <span className="sched-live-pill">Đang học</span>
        </div>
        <div className="sched-chip">
          <div className="sched-time"><b>16</b><span>00 chiều</span></div>
          <div className="sched-bar" style={{background:'#6366F1'}}/>
          <div className="sched-body">
            <div className="sched-title">Toán Tư Duy</div>
            <div className="sched-meta"><span>👨‍🏫 Thầy Khang</span><span>📍 P.305</span></div>
          </div>
        </div>
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Truy cập nhanh</h3></div>
        <div className="qa-grid">
          {[
            {ic:'qr-code', c:'#6366F1', l:'Điểm danh'},
            {ic:'graduation-cap', c:'#0EA5E9', l:'Bảng điểm'},
            {ic:'wallet', c:'#DB2777', l:'Học phí'},
            {ic:'message-square', c:'#16A34A', l:'Gv nhắn'},
          ].map((q,i)=>(
            <div key={i} className="qa">
              <div className="qa-ic" style={{background:q.c}}><Icon name={q.ic} className="h-4 w-4"/></div>
              <span>{q.l}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Học viên xuất sắc tuần</h3></div>
        <div className="kid-card">
          <h4>Học viên của tuần</h4>
          <div className="name">Bé Minh Anh ✨</div>
          <div className="why">Chuyên cần 100%, hoàn thành xuất sắc bài tập về nhà tuần này.</div>
        </div>
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Chuyên cần tuần này</h3><a>Xem tháng →</a></div>
        <div className="app-card">
          <div className="att-week">
            {[
              {l:'T2',n:21,s:'present'},
              {l:'T3',n:22,s:'today'},
              {l:'T4',n:23,s:''},
              {l:'T5',n:24,s:''},
              {l:'T6',n:25,s:''},
              {l:'T7',n:26,s:''},
              {l:'CN',n:27,s:''},
            ].map((d,i)=>(
              <div key={i} className={'att-day '+d.s}>
                <span className="lbl">{d.l}</span>
                <span className="num">{d.n}</span>
                <span className="stat"/>
              </div>
            ))}
          </div>
          <div style={{display:'flex',gap:14,marginTop:10,fontSize:10.5,color:'#64748B',justifyContent:'center'}}>
            <span><b style={{color:'#16A34A'}}>●</b> Có mặt</span>
            <span><b style={{color:'#F59E0B'}}>●</b> Trễ</span>
            <span><b style={{color:'#DC2626'}}>●</b> Vắng</span>
          </div>
        </div>
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Học phí sắp đến hạn</h3></div>
        <div className="app-card" style={{background:'linear-gradient(135deg,#FEF3C7,#fff)',border:'1px solid #FCD34D'}}>
          <div style={{display:'flex',alignItems:'center',gap:10}}>
            <div style={{width:40,height:40,borderRadius:12,background:'#FBBF24',color:'#fff',display:'grid',placeItems:'center',flexShrink:0}}>
              <Icon name="alert-circle" className="h-5 w-5"/>
            </div>
            <div style={{flex:1,minWidth:0}}>
              <div style={{fontSize:13,fontWeight:700}}>Học phí TA Trẻ Em K21</div>
              <div style={{fontSize:11,color:'#92400E',marginTop:2}}>Đến hạn 30/04 · còn 2 ngày</div>
            </div>
            <div style={{textAlign:'right'}}>
              <div style={{fontSize:15,fontWeight:800,color:'#92400E',fontFamily:'var(--font-mono)'}}>2.4M₫</div>
              <button style={{marginTop:4,padding:'4px 10px',background:'#92400E',color:'#fff',border:0,borderRadius:8,fontSize:10.5,fontWeight:700}}>Đóng ngay</button>
            </div>
          </div>
        </div>
      </div>

      <Tabbar active="home"/>
    </div>
  );
}

function QrCheckinScreen() {
  const [done, setDone] = useState(false);
  return (
    <div className="qr-screen">
      <div className="top">
        <div className="qr-back"><Icon name="arrow-left" className="h-4 w-4" style={{color:'#fff'}}/></div>
        <h2>Điểm danh QR</h2>
      </div>
      <div className="qr-class">📍 <b>TA Trẻ Em K21</b> · 14:00 · P.102</div>

      {!done ? (
        <>
          <div className="qr-frame">
            <span className="corner tl"/><span className="corner tr"/>
            <span className="corner bl"/><span className="corner br"/>
            <div className="qr">
              <div className="qr-pattern"><div className="qr-eye"/></div>
            </div>
            <div className="qr-scan"/>
          </div>
          <div className="qr-status">Đưa mã QR vào khung · <b>tự động nhận</b> trong 1 giây</div>
          <button className="qr-cta" onClick={() => setDone(true)}>
            <Icon name="check" className="h-4 w-4" style={{display:'inline-block',marginRight:6,verticalAlign:'-3px'}}/>
            Tôi đã đến lớp
          </button>
        </>
      ) : (
        <>
          <div style={{flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', textAlign:'center', padding:'32px 0'}}>
            <div className="qr-success"><Icon name="check" className="h-7 w-7" style={{color:'#4ADE80'}}/></div>
            <div style={{fontSize:20,fontWeight:800,marginBottom:6}}>Đã điểm danh!</div>
            <div style={{fontSize:13,opacity:.85,marginBottom:14}}>14:02 · trễ 2 phút</div>
            <div style={{padding:'10px 16px',background:'rgba(255,255,255,.12)',borderRadius:14,fontSize:11.5,backdropFilter:'blur(10px)',border:'1px solid rgba(255,255,255,.15)'}}>
              📲 Đã thông báo cho phụ huynh<br/>
              <span style={{opacity:.7}}>"Bé Minh Anh đã đến lớp lúc 14:02"</span>
            </div>
          </div>
          <button className="qr-cta" onClick={() => setDone(false)}>Về trang chủ</button>
        </>
      )}

      <Tabbar active="qr" light/>
    </div>
  );
}

function GradesScreen() {
  return (
    <div className="app">
      <div className="app-hero brand-sky">
        <div className="app-hero-row">
          <div className="qr-back" style={{background:'rgba(255,255,255,.18)'}}><Icon name="arrow-left" className="h-4 w-4" style={{color:'#fff'}}/></div>
          <div style={{flex:1, textAlign:'center'}}>
            <div className="app-name">Bảng điểm bé Minh Anh</div>
            <div className="app-greet">TA Trẻ Em K21 · Quý 1/2026</div>
          </div>
          <div style={{width:36}}/>
        </div>
        <div style={{textAlign:'center', marginTop:18}}>
          <div style={{fontSize:11, opacity:.85, textTransform:'uppercase', letterSpacing:'.08em'}}>Điểm trung bình</div>
          <div style={{fontSize:48, fontWeight:800, letterSpacing:'-.02em', lineHeight:1, marginTop:4}}>8.4<span style={{fontSize:24,opacity:.7}}>/10</span></div>
          <div style={{fontSize:12, opacity:.85, marginTop:4}}>↑ 0.6 so với quý trước · Top 12% lớp</div>
        </div>
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Theo kỹ năng</h3><a>Chi tiết →</a></div>
        <div className="grade-card">
          {[
            ['Listening', 9.0, 90, 'a'],
            ['Speaking', 8.5, 85, 'a'],
            ['Reading', 8.2, 82, 'b'],
            ['Writing', 7.8, 78, 'b'],
            ['Vocabulary', 7.5, 75, 'c'],
          ].map(([s,n,p,c],i)=>(
            <div key={i} className="grade-row">
              <span className="grade-skill">{s}</span>
              <div className="grade-bar"><i style={{width: p+'%', background: c==='a'?'#16A34A':c==='b'?'#0EA5E9':'#F59E0B'}}/></div>
              <span className={'grade-num '+c}>{n.toFixed(1)}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Bài kiểm tra gần đây</h3></div>
        {[
          ['Mid-term Test', '24/04', 8.5, 'a', 'Hoàn thành tốt'],
          ['Bài 12 — Listening', '20/04', 9.0, 'a', 'Xuất sắc!'],
          ['Bài 11 — Vocabulary', '15/04', 7.0, 'c', 'Cần ôn từ vựng'],
        ].map(([t,d,n,c,note],i)=>(
          <div key={i} className="app-card" style={{display:'flex',alignItems:'center',gap:10}}>
            <div style={{width:42,height:42,borderRadius:12,background:c==='a'?'#DCFCE7':c==='b'?'#DBEAFE':'#FEF3C7',color:c==='a'?'#16A34A':c==='b'?'#0EA5E9':'#F59E0B',display:'grid',placeItems:'center',fontWeight:800,fontFamily:'var(--font-mono)',fontSize:14}}>{n}</div>
            <div style={{flex:1,minWidth:0}}>
              <div style={{fontSize:13,fontWeight:700}}>{t}</div>
              <div style={{fontSize:11,color:'#64748B',marginTop:2}}>{note}</div>
            </div>
            <div style={{fontSize:11,color:'#94A3B8',fontFamily:'var(--font-mono)'}}>{d}</div>
          </div>
        ))}
      </div>

      <div className="app-section">
        <div className="app-sect-head"><h3>Nhận xét của giáo viên</h3></div>
        <div className="app-card" style={{background:'linear-gradient(135deg,#FEF3C7,#fff)'}}>
          <div style={{display:'flex',gap:10,alignItems:'flex-start'}}>
            <div style={{width:36,height:36,borderRadius:9999,background:'#F97316',color:'#fff',display:'grid',placeItems:'center',fontWeight:700,flexShrink:0}}>L</div>
            <div>
              <div style={{fontSize:12,fontWeight:700}}>Cô Lan · 25/04</div>
              <p style={{margin:'4px 0 0',fontSize:12.5,color:'#475569',lineHeight:1.5,fontStyle:'italic'}}>"Bé Minh Anh tiến bộ rõ rệt phần Listening tuần này. Cần luyện thêm vocabulary — phụ huynh có thể cho bé học 10 từ/ngày qua app."</p>
            </div>
          </div>
        </div>
      </div>

      <Tabbar active="grades"/>
    </div>
  );
}

function PaymentScreen() {
  const [method, setMethod] = useState('momo');
  return (
    <div className="pay-screen" style={{position:'relative',minHeight:'100%'}}>
      <div className="pay-head">
        <div className="app-hero-row">
          <div className="qr-back" style={{background:'rgba(255,255,255,.18)'}}><Icon name="arrow-left" className="h-4 w-4" style={{color:'#fff'}}/></div>
          <div style={{flex:1, textAlign:'center', fontSize:15, fontWeight:700}}>Thanh toán học phí</div>
          <div style={{width:36}}/>
        </div>
        <div className="pay-amount">
          <div className="lbl">Số tiền cần thanh toán</div>
          <div className="val">2.400.000<span style={{fontSize:18,marginLeft:2}}>₫</span></div>
          <div className="due">⏰ Đến hạn 30/04/2026</div>
        </div>
      </div>

      <div className="pay-detail">
        <div style={{fontSize:12,fontWeight:700,color:'#831843',textTransform:'uppercase',letterSpacing:'.08em',marginBottom:4}}>Chi tiết</div>
        <div className="row"><span className="l">Học viên</span><span className="v">Bé Minh Anh</span></div>
        <div className="row"><span className="l">Lớp</span><span className="v">TA Trẻ Em K21</span></div>
        <div className="row"><span className="l">Kỳ</span><span className="v">Tháng 5/2026</span></div>
        <div className="row"><span className="l">Số buổi</span><span className="v">12 buổi × 200K</span></div>
        <div className="row"><span className="l">Mã hóa đơn</span><span className="v" style={{fontFamily:'var(--font-mono)',fontSize:12}}>HD-2026-04-1248</span></div>
      </div>

      <div className="pay-methods">
        <div style={{fontSize:12,fontWeight:700,color:'#831843',textTransform:'uppercase',letterSpacing:'.08em',marginBottom:4}}>Phương thức</div>

        <div className={'pay-method' + (method==='momo'?' is-active':'')} onClick={()=>setMethod('momo')}>
          <div className="logo-box" style={{background:'#A50064'}}>M°</div>
          <div className="body">
            <div className="ttl">Ví MoMo</div>
            <div className="meta">Thanh toán nhanh · liên kết sẵn</div>
          </div>
          <div className="check">{method==='momo'&&<Icon name="check" className="h-3 w-3"/>}</div>
        </div>

        <div className={'pay-method' + (method==='vnpay'?' is-active':'')} onClick={()=>setMethod('vnpay')}>
          <div className="logo-box" style={{background:'#005BAA'}}>VN</div>
          <div className="body">
            <div className="ttl">VNPay QR</div>
            <div className="meta">Quét QR bằng app ngân hàng</div>
          </div>
          <div className="check">{method==='vnpay'&&<Icon name="check" className="h-3 w-3"/>}</div>
        </div>

        <div className={'pay-method' + (method==='zalopay'?' is-active':'')} onClick={()=>setMethod('zalopay')}>
          <div className="logo-box" style={{background:'#0068FF'}}>ZP</div>
          <div className="body">
            <div className="ttl">ZaloPay</div>
            <div className="meta">Hoàn 0.5% qua Zalo Wallet</div>
          </div>
          <div className="check">{method==='zalopay'&&<Icon name="check" className="h-3 w-3"/>}</div>
        </div>

        <div className={'pay-method' + (method==='bank'?' is-active':'')} onClick={()=>setMethod('bank')}>
          <div className="logo-box" style={{background:'#0F172A'}}>🏦</div>
          <div className="body">
            <div className="ttl">Chuyển khoản ngân hàng</div>
            <div className="meta">Vietcombank · 0123456789</div>
          </div>
          <div className="check">{method==='bank'&&<Icon name="check" className="h-3 w-3"/>}</div>
        </div>
      </div>

      <button className="pay-cta">
        <Icon name="lock" className="h-4 w-4"/> Thanh toán 2.400.000₫
      </button>
    </div>
  );
}

function NotificationsScreen() {
  return (
    <div className="app" style={{paddingTop:0}}>
      <div className="app-hero" style={{background:'linear-gradient(135deg,#6366F1,#A855F7)', borderRadius:0}}>
        <div className="app-hero-row">
          <div style={{flex:1}}>
            <div className="app-name" style={{fontSize:19}}>Thông báo</div>
            <div className="app-greet">3 thông báo mới · 12 tổng</div>
          </div>
          <div className="app-bell"><Icon name="check-check" className="h-4 w-4" style={{color:'#fff'}}/></div>
        </div>
      </div>

      <div className="app-section">
        <div style={{fontSize:11,fontWeight:700,color:'#94A3B8',textTransform:'uppercase',letterSpacing:'.08em',padding:'4px 4px 8px'}}>Hôm nay</div>

        <div className="notif-item unread">
          <div className="notif-ic" style={{background:'#DCFCE7',color:'#16A34A'}}><Icon name="check-circle-2" className="h-4 w-4"/></div>
          <div className="notif-body">
            <div className="notif-title">Bé Minh Anh đã đến lớp</div>
            <div className="notif-msg">Lớp TA Trẻ Em K21 · 14:02 · trễ 2 phút</div>
            <div className="notif-time">5 phút trước</div>
          </div>
        </div>

        <div className="notif-item unread">
          <div className="notif-ic" style={{background:'#DBEAFE',color:'#2563EB'}}><Icon name="award" className="h-4 w-4"/></div>
          <div className="notif-body">
            <div className="notif-title">🌟 Bé là Học viên của tuần!</div>
            <div className="notif-msg">Chuyên cần 100% và hoàn thành xuất sắc bài tập tuần này.</div>
            <div className="notif-time">2 giờ trước</div>
          </div>
        </div>

        <div className="notif-item unread">
          <div className="notif-ic" style={{background:'#FEF3C7',color:'#D97706'}}><Icon name="wallet" className="h-4 w-4"/></div>
          <div className="notif-body">
            <div className="notif-title">Học phí sắp đến hạn</div>
            <div className="notif-msg">2.400.000₫ cho lớp TA Trẻ Em K21 · còn 2 ngày</div>
            <div className="notif-time">3 giờ trước</div>
          </div>
        </div>

        <div style={{fontSize:11,fontWeight:700,color:'#94A3B8',textTransform:'uppercase',letterSpacing:'.08em',padding:'14px 4px 8px'}}>Hôm qua</div>

        <div className="notif-item">
          <div className="notif-ic" style={{background:'#F3E8FF',color:'#9333EA'}}><Icon name="message-square" className="h-4 w-4"/></div>
          <div className="notif-body">
            <div className="notif-title">Cô Lan gửi nhận xét</div>
            <div className="notif-msg">"Bé Minh Anh tiến bộ rõ rệt phần Listening..."</div>
            <div className="notif-time">Hôm qua · 16:30</div>
          </div>
        </div>

        <div className="notif-item">
          <div className="notif-ic" style={{background:'#FFE4E6',color:'#E11D48'}}><Icon name="calendar-check" className="h-4 w-4"/></div>
          <div className="notif-body">
            <div className="notif-title">Lịch học mới tuần sau</div>
            <div className="notif-msg">Đã cập nhật lịch tuần 06/05 · 3 buổi mới</div>
            <div className="notif-time">Hôm qua · 09:15</div>
          </div>
        </div>

        <div className="notif-item">
          <div className="notif-ic" style={{background:'#DCFCE7',color:'#16A34A'}}><Icon name="banknote" className="h-4 w-4"/></div>
          <div className="notif-body">
            <div className="notif-title">Thanh toán thành công</div>
            <div className="notif-msg">+2.400.000₫ · Học phí TA Trẻ Em K21 tháng 4</div>
            <div className="notif-time">Hôm qua · 17:24</div>
          </div>
        </div>
      </div>

      <Tabbar active="notif"/>
    </div>
  );
}

function Tabbar({ active, light }) {
  const tabs = [
    { id: 'home', ic: 'home', l: 'Trang chủ' },
    { id: 'grades', ic: 'graduation-cap', l: 'Bảng điểm' },
    { id: 'qr', ic: 'qr-code', l: '', fab: true },
    { id: 'pay', ic: 'wallet', l: 'Học phí' },
    { id: 'notif', ic: 'bell', l: 'Thông báo' },
  ];
  return (
    <div className="app-tabbar" style={light?{background:'rgba(15,20,40,.7)'}:{}}>
      {tabs.map(t => (
        <div key={t.id} className={'app-tab' + (active===t.id?' active':'')}>
          {t.fab ? (
            <div className="app-tab-fab"><Icon name={t.ic} className="h-5 w-5"/></div>
          ) : (
            <>
              <Icon name={t.ic} className="h-5 w-5" style={{color: light?'#fff':undefined}}/>
              <span style={light?{color:'rgba(255,255,255,.7)'}:{}}>{t.l}</span>
            </>
          )}
        </div>
      ))}
    </div>
  );
}

// ───────── Stage ─────────
const SCREENS = [
  { id: 'home', label: 'Trang chủ', icon: 'home', comp: <HomeScreen/> },
  { id: 'qr', label: 'Điểm danh QR', icon: 'qr-code', comp: <QrCheckinScreen/> },
  { id: 'grades', label: 'Bảng điểm', icon: 'graduation-cap', comp: <GradesScreen/> },
  { id: 'pay', label: 'Thanh toán', icon: 'wallet', comp: <PaymentScreen/> },
  { id: 'notif', label: 'Thông báo', icon: 'bell', comp: <NotificationsScreen/> },
];

const VIEWS = ['all', 'home', 'qr', 'grades', 'pay', 'notif'];

function Stage() {
  const [view, setView] = useState('all');
  useEffect(() => { const id = setInterval(() => window.lucide && lucide.createIcons(), 350); return () => clearInterval(id); }, []);

  const screensToShow = view === 'all' ? SCREENS : SCREENS.filter(s => s.id === view);

  return (
    <div className="pa-stage" data-screen-label="Mobile parent app">
      <div className="pa-stage-head">
        <h1>App phụ huynh & học sinh<br/><span className="grad">Trên đầu ngón tay.</span></h1>
        <p>Lịch học, điểm danh QR, bảng điểm theo kỹ năng, thanh toán MoMo/VNPay, thông báo realtime. Tất cả gói trong một app, đồng bộ tức thời với KiteClass.</p>
        <div className="pa-stage-tabs">
          <button className={view==='all'?'is-active':''} onClick={()=>setView('all')}><Icon name="layout-grid" className="h-3.5 w-3.5"/>Tất cả</button>
          {SCREENS.map(s => (
            <button key={s.id} className={view===s.id?'is-active':''} onClick={()=>setView(s.id)}>
              <Icon name={s.icon} className="h-3.5 w-3.5"/>{s.label}
            </button>
          ))}
        </div>
      </div>

      <div className="pa-flex">
        {screensToShow.map((s, i) => (
          <div className="pa-phone-wrap" key={s.id}>
            <IOSDevice width={320} height={680}>
              {s.comp}
            </IOSDevice>
            <div className="pa-phone-label">0{i+1} · <b>{s.label}</b></div>
          </div>
        ))}
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<Stage />);
