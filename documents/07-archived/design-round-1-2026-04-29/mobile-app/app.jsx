// Mobile App v2 — Duolingo/Cake vibe
const { useState, useEffect } = React;
const Icon = ({ name, className = 'h-4 w-4', style }) => <i data-lucide={name} className={className} style={style}></i>;

// ─────── Kite illustration (SVG) ───────
function KiteIllustration() {
  return (
    <svg className="kite-svg" viewBox="0 0 200 200" fill="none">
      {/* string with curve */}
      <path d="M 95 80 Q 60 130, 30 195" stroke="#1A1438" strokeWidth="2" strokeLinecap="round" strokeDasharray="3 4" fill="none"/>
      {/* kite body — diamond */}
      <g transform="translate(100, 70) rotate(-15)">
        {/* shadow */}
        <path d="M 0 -50 L 36 0 L 0 50 L -36 0 Z" fill="#1A1438" transform="translate(4, 4)"/>
        {/* main */}
        <path d="M 0 -50 L 36 0 L 0 50 L -36 0 Z" fill="#FFD166" stroke="#1A1438" strokeWidth="2.5"/>
        {/* cross lines */}
        <line x1="0" y1="-50" x2="0" y2="50" stroke="#1A1438" strokeWidth="1.5" opacity=".5"/>
        <line x1="-36" y1="0" x2="36" y2="0" stroke="#1A1438" strokeWidth="1.5" opacity=".5"/>
        {/* color quadrants */}
        <path d="M 0 -50 L 36 0 L 0 0 Z" fill="#FB7185" stroke="#1A1438" strokeWidth="2"/>
        <path d="M 0 0 L -36 0 L 0 50 Z" fill="#A855F7" stroke="#1A1438" strokeWidth="2"/>
        {/* center dot */}
        <circle cx="0" cy="0" r="4" fill="#1A1438"/>
        {/* tail */}
        <g transform="translate(0, 50)">
          <path d="M 0 0 Q 8 12, -2 24 T 4 48 T -4 70" stroke="#1A1438" strokeWidth="2" fill="none" strokeLinecap="round"/>
          <ellipse cx="-2" cy="24" rx="6" ry="3" fill="#FB7185" stroke="#1A1438" strokeWidth="1.5" transform="rotate(20 -2 24)"/>
          <ellipse cx="4" cy="48" rx="6" ry="3" fill="#FFD166" stroke="#1A1438" strokeWidth="1.5" transform="rotate(-15 4 48)"/>
          <ellipse cx="-4" cy="70" rx="6" ry="3" fill="#A855F7" stroke="#1A1438" strokeWidth="1.5" transform="rotate(25 -4 70)"/>
        </g>
      </g>
      {/* sun */}
      <g transform="translate(35, 35)">
        <circle r="14" fill="#FFD166" stroke="#1A1438" strokeWidth="2.5"/>
        <g stroke="#1A1438" strokeWidth="2" strokeLinecap="round">
          <line x1="0" y1="-22" x2="0" y2="-18"/>
          <line x1="0" y1="18" x2="0" y2="22"/>
          <line x1="-22" y1="0" x2="-18" y2="0"/>
          <line x1="18" y1="0" x2="22" y2="0"/>
          <line x1="-15" y1="-15" x2="-13" y2="-13"/>
          <line x1="13" y1="13" x2="15" y2="15"/>
          <line x1="-15" y1="15" x2="-13" y2="13"/>
          <line x1="13" y1="-13" x2="15" y2="-15"/>
        </g>
      </g>
    </svg>
  );
}

// ─────── HOME ───────
function HomeScreen() {
  return (
    <div className="app">
      <div className="home-top">
        <div className="home-streak">🔥 12 ngày</div>
        <div className="home-coin">⭐ 1,240</div>
        <div className="home-bell"><Icon name="bell" className="h-4 w-4" style={{color:'#1A1438'}}/><span className="ndot"/></div>
      </div>

      <div className="kite-stage">
        <div className="kite-cloud c1"/>
        <div className="kite-cloud c2"/>
        <div className="kite-cloud c3"/>
        <div className="kite-greeting">
          <div className="lbl">Chào buổi sáng,</div>
          <div className="nm">Mẹ của<br/><em>Minh Anh!</em></div>
        </div>
        <KiteIllustration/>
      </div>

      <div className="home-section">
        <div className="section-title">
          <h3>Đang học <span className="em">ngay bây giờ</span></h3>
        </div>
        <div className="live-card">
          <div className="top-row">
            <span className="live-pill-2">Đang học</span>
          </div>
          <div className="ttl">TA Trẻ Em K21</div>
          <div className="meta">👩‍🏫 Cô Lan · còn 28 phút</div>
          <div className="row-2">
            <span className="room">📍 P.102</span>
            <span className="room">⏰ 14:00</span>
            <button className="live-cta">Xem<Icon name="chevron-right" className="h-3 w-3"/></button>
          </div>
        </div>
      </div>

      <div className="home-section">
        <div className="section-title">
          <h3>Lịch sắp tới</h3>
          <a>Tuần →</a>
        </div>
        <div className="upcoming-card">
          <div className="upcoming-time"><b>16</b><span>HÔM NAY</span></div>
          <div className="upcoming-body">
            <div className="t">Toán Tư Duy</div>
            <div className="m">👨‍🏫 Thầy Khang · P.305</div>
          </div>
          <Icon name="chevron-right" className="h-4 w-4" style={{color:'#94A3B8'}}/>
        </div>
        <div className="upcoming-card">
          <div className="upcoming-time violet"><b>09</b><span>NGÀY MAI</span></div>
          <div className="upcoming-body">
            <div className="t">Speaking Club</div>
            <div className="m">👩‍🏫 Cô Hannah · Online</div>
          </div>
          <Icon name="chevron-right" className="h-4 w-4" style={{color:'#94A3B8'}}/>
        </div>
        <div className="upcoming-card">
          <div className="upcoming-time green"><b>14</b><span>T5</span></div>
          <div className="upcoming-body">
            <div className="t">Mid-term Test 📝</div>
            <div className="m">⏱ 90 phút · P.201</div>
          </div>
          <Icon name="chevron-right" className="h-4 w-4" style={{color:'#94A3B8'}}/>
        </div>
      </div>

      <div className="home-section">
        <div className="section-title">
          <h3>Truy cập nhanh</h3>
        </div>
        <div className="qa2-grid">
          <div className="qa2">
            <div className="ic" style={{background:'#FFD166'}}>📷</div>
            <div className="ttl">Điểm danh QR</div>
            <div className="desc">Quét nhanh khi đến lớp</div>
          </div>
          <div className="qa2">
            <div className="ic" style={{background:'#BAE6FD'}}>📊</div>
            <div className="ttl">Bảng điểm</div>
            <div className="desc">8.4 quý này ↑</div>
          </div>
          <div className="qa2">
            <div className="ic" style={{background:'#FECACA'}}>💳</div>
            <div className="ttl">Học phí</div>
            <div className="desc" style={{color:'#DC2626',fontWeight:800}}>2.4M sắp đến hạn</div>
          </div>
          <div className="qa2">
            <div className="ic" style={{background:'#DDD6FE'}}>💬</div>
            <div className="ttl">Tin nhắn</div>
            <div className="desc">3 từ Cô Lan</div>
          </div>
        </div>
      </div>

      <div className="home-section">
        <div className="section-title">
          <h3>Chuyên cần tuần này</h3>
        </div>
        <div className="att2-row">
          {[
            {l:'T2',n:21,s:'present'},
            {l:'T3',n:22,s:'today'},
            {l:'T4',n:23,s:''},
            {l:'T5',n:24,s:''},
            {l:'T6',n:25,s:''},
            {l:'T7',n:26,s:''},
            {l:'CN',n:27,s:''},
          ].map((d,i)=>(
            <div key={i} className={'att2-day ' + d.s}>
              <span className="dl">{d.l}</span>
              <span className="dn">{d.n}</span>
              <span className="ds"/>
            </div>
          ))}
        </div>
      </div>

      <div className="home-section">
        <div className="section-title">
          <h3>Học viên của tuần</h3>
        </div>
        <div className="kid-2">
          <span className="star1">⭐</span>
          <span className="star2">✨</span>
          <div className="lbl">Bé là sao tuần này!</div>
          <div className="nm">Minh Anh ✨</div>
          <div className="why">Chuyên cần 100% và hoàn thành xuất sắc bài tập tuần qua. Ba mẹ tự hào lắm!</div>
        </div>
      </div>

      <Tabbar active="home"/>
    </div>
  );
}

// ─────── QR ───────
function QrCheckinScreen() {
  const [done, setDone] = useState(false);
  return (
    <div className="qr2">
      <div className="qr2-top">
        <div className="qr2-back"><Icon name="arrow-left" className="h-4 w-4"/></div>
        <h2>Điểm danh QR</h2>
      </div>
      <div className="qr2-class">
        <span style={{fontSize:14}}>📍</span>
        <span><b>TA Trẻ Em K21</b> · 14:00 · P.102</span>
      </div>

      {!done ? (
        <>
          <div className="qr2-card">
            <div className="qr2-frame">
              <div className="qr2-pat">
                <div className="qr2-pat-inner"><span className="eye"/></div>
              </div>
              <div className="qr2-scan-line"/>
            </div>
            <div className="hint">Đưa mã QR vào khung · <b>tự động nhận</b></div>
          </div>
          <button className="qr2-cta" onClick={()=>setDone(true)}>
            <Icon name="check" className="h-5 w-5"/> Tôi đã đến lớp
          </button>
        </>
      ) : (
        <>
          <div className="qr2-success">
            <div className="check-big"><Icon name="check" className="h-10 w-10" style={{color:'#14532D',strokeWidth:4}}/></div>
            <h3>Đã điểm danh! 🎉</h3>
            <div className="time-tag">⏰ 14:02 · trễ 2 phút</div>
            <div className="qr2-coin-row">
              <span className="qr2-coin-pill">⭐ +50</span>
              <span className="qr2-coin-pill">🔥 streak +1</span>
            </div>
            <div className="parent-note">
              <b>📲 Đã thông báo phụ huynh</b>
              "Bé Minh Anh đã đến lớp lúc 14:02"
            </div>
          </div>
          <button className="qr2-cta" onClick={()=>setDone(false)}>
            <Icon name="home" className="h-5 w-5"/> Về trang chủ
          </button>
        </>
      )}

      <Tabbar active="qr" dark/>
    </div>
  );
}

// ─────── GRADES ───────
function GradesScreen() {
  return (
    <div className="app">
      <div className="gr2-hero">
        <div className="gr2-hero-top">
          <div className="gr2-back"><Icon name="arrow-left" className="h-4 w-4"/></div>
          <div className="t">
            <b>Bảng điểm bé Minh Anh</b>
            <span>TA Trẻ Em K21 · Quý 1/2026</span>
          </div>
        </div>
        <div className="gr2-bigscore">
          <div className="lbl">Điểm trung bình</div>
          <div className="v">8.4<small>/10</small></div>
          <div className="delta">↑ 0.6 so với quý trước · Top 12% lớp</div>
        </div>
      </div>

      <div className="gr2-section">
        <div className="section-title"><h3>Theo kỹ năng</h3><a>Chi tiết →</a></div>
        <div className="gr2-skill-card">
          {[
            {l:'Listening', e:'👂', n:9.0, p:90, c:'#16A34A'},
            {l:'Speaking',  e:'🗣️', n:8.5, p:85, c:'#0EA5E9'},
            {l:'Reading',   e:'📖', n:8.2, p:82, c:'#0EA5E9'},
            {l:'Writing',   e:'✏️', n:7.8, p:78, c:'#F59E0B'},
            {l:'Vocabulary',e:'📚', n:7.5, p:75, c:'#F59E0B'},
          ].map((s,i)=>(
            <div key={i} className="gr2-skill-row">
              <div className="gr2-skill-emoji" style={{background: i%2?'#FEF3C7':'#DBEAFE'}}>{s.e}</div>
              <div className="gr2-skill-mid">
                <div className="nm">{s.l}</div>
                <div className="bar"><i style={{width:s.p+'%', background:s.c}}/></div>
              </div>
              <div className="gr2-skill-num" style={{color:s.c}}>{s.n.toFixed(1)}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="gr2-section">
        <div className="section-title"><h3>Kiểm tra gần đây</h3></div>
        {[
          {t:'Mid-term Test',     d:'24/04', n:'8.5', bg:'#BBF7D0', c:'#14532D', m:'Hoàn thành tốt 👏'},
          {t:'Bài 12 — Listening', d:'20/04', n:'9.0', bg:'#FEF3C7', c:'#92400E', m:'Xuất sắc! 🎉'},
          {t:'Bài 11 — Vocab',    d:'15/04', n:'7.0', bg:'#FED7AA', c:'#9A3412', m:'Cần ôn từ vựng'},
        ].map((x,i)=>(
          <div key={i} className="gr2-test">
            <div className="gr2-test-score" style={{background:x.bg, color:x.c}}>{x.n}</div>
            <div className="gr2-test-body">
              <div className="t">{x.t}</div>
              <div className="m">{x.m}</div>
            </div>
            <div className="gr2-test-date">{x.d}</div>
          </div>
        ))}
      </div>

      <div className="gr2-section">
        <div className="section-title"><h3>Nhận xét cô Lan</h3></div>
        <div className="gr2-teacher">
          <div className="gr2-teacher-row">
            <div className="gr2-teacher-av">L</div>
            <div className="gr2-teacher-msg">
              <b>Cô Lan · 25/04</b>
              Bé Minh Anh tiến bộ rõ rệt phần Listening tuần này. Cần luyện thêm vocabulary — phụ huynh có thể cho bé học 10 từ/ngày qua app nhé!
            </div>
          </div>
        </div>
      </div>

      <Tabbar active="grades"/>
    </div>
  );
}

// ─────── PAYMENT ───────
function PaymentScreen() {
  const [method, setMethod] = useState('momo');
  return (
    <div className="pay2">
      <div className="pay2-top">
        <div className="qr2-back" style={{color:'#1A1438'}}><Icon name="arrow-left" className="h-4 w-4"/></div>
        <h2>Thanh toán học phí</h2>
      </div>

      <div className="pay2-amount-card">
        <div className="lbl">Cần thanh toán</div>
        <div className="v">2.400.000<small>₫</small></div>
        <div className="due">⏰ Đến hạn 30/04 · còn 2 ngày</div>
      </div>

      <div className="pay2-detail">
        <div className="lblrow">Chi tiết</div>
        <div className="row"><span className="l">Học viên</span><span className="v">Bé Minh Anh</span></div>
        <div className="row"><span className="l">Lớp</span><span className="v">TA Trẻ Em K21</span></div>
        <div className="row"><span className="l">Kỳ</span><span className="v">Tháng 5/2026</span></div>
        <div className="row"><span className="l">Số buổi</span><span className="v">12 × 200K</span></div>
        <div className="row"><span className="l">Mã hoá đơn</span><span className="v" style={{fontFamily:'JetBrains Mono,monospace',fontSize:11.5}}>HD-2026-04-1248</span></div>
      </div>

      <div className="pay2-methods">
        <div className="pay2-methods-lbl">Phương thức</div>

        <div className={'pay2-method' + (method==='momo'?' is-active':'')} onClick={()=>setMethod('momo')}>
          <div className="lg" style={{background:'#A50064'}}>M°</div>
          <div className="b">
            <div className="t">Ví MoMo</div>
            <div className="m">Thanh toán nhanh · liên kết sẵn ✓</div>
          </div>
          <div className="ck">{method==='momo'&&<Icon name="check" className="h-3 w-3" style={{strokeWidth:4}}/>}</div>
        </div>

        <div className={'pay2-method' + (method==='vnpay'?' is-active':'')} onClick={()=>setMethod('vnpay')}>
          <div className="lg" style={{background:'#005BAA'}}>VN</div>
          <div className="b">
            <div className="t">VNPay QR</div>
            <div className="m">Quét QR bằng app ngân hàng</div>
          </div>
          <div className="ck">{method==='vnpay'&&<Icon name="check" className="h-3 w-3" style={{strokeWidth:4}}/>}</div>
        </div>

        <div className={'pay2-method' + (method==='zalo'?' is-active':'')} onClick={()=>setMethod('zalo')}>
          <div className="lg" style={{background:'#0068FF'}}>ZP</div>
          <div className="b">
            <div className="t">ZaloPay</div>
            <div className="m">Hoàn 0.5% qua Zalo Wallet 🎁</div>
          </div>
          <div className="ck">{method==='zalo'&&<Icon name="check" className="h-3 w-3" style={{strokeWidth:4}}/>}</div>
        </div>

        <div className={'pay2-method' + (method==='bank'?' is-active':'')} onClick={()=>setMethod('bank')}>
          <div className="lg" style={{background:'#1A1438'}}>🏦</div>
          <div className="b">
            <div className="t">Chuyển khoản</div>
            <div className="m">Vietcombank · 0123456789</div>
          </div>
          <div className="ck">{method==='bank'&&<Icon name="check" className="h-3 w-3" style={{strokeWidth:4}}/>}</div>
        </div>
      </div>

      <button className="pay2-cta">
        <Icon name="lock" className="h-4 w-4"/> Thanh toán 2.4M₫
      </button>

      <Tabbar active="pay"/>
    </div>
  );
}

// ─────── NOTIFICATIONS ───────
function NotificationsScreen() {
  return (
    <div className="app" style={{paddingTop:0}}>
      <div className="notif2-hero">
        <div className="notif2-hero-top">
          <h2>Thông báo</h2>
          <div className="mark"><Icon name="check-check" className="h-4 w-4" style={{color:'#1A1438'}}/></div>
        </div>
        <div className="notif2-summary"><b>3</b>thông báo mới · 12 tổng</div>
      </div>

      <div className="notif2-section">
        <div className="notif2-day">Hôm nay</div>

        <div className="notif2-item unread">
          <div className="notif2-ic" style={{background:'#BBF7D0'}}>✅</div>
          <div className="notif2-body">
            <div className="t">Bé Minh Anh đã đến lớp</div>
            <div className="m">Lớp TA Trẻ Em K21 · 14:02 · trễ 2 phút</div>
            <div className="meta-row"><span className="time">5 phút trước</span><span className="new-pill">Mới</span></div>
          </div>
        </div>

        <div className="notif2-item unread">
          <div className="notif2-ic" style={{background:'#FEF3C7'}}>🌟</div>
          <div className="notif2-body">
            <div className="t">Bé là Học viên của tuần! 🎉</div>
            <div className="m">Chuyên cần 100% và hoàn thành xuất sắc bài tập tuần này. Bé được tặng 200⭐!</div>
            <div className="meta-row"><span className="time">2 giờ trước</span><span className="new-pill">Mới</span></div>
          </div>
        </div>

        <div className="notif2-item unread">
          <div className="notif2-ic" style={{background:'#FECACA'}}>💳</div>
          <div className="notif2-body">
            <div className="t">Học phí sắp đến hạn</div>
            <div className="m">2.400.000₫ cho lớp TA Trẻ Em K21 · còn 2 ngày</div>
            <div className="meta-row"><span className="time">3 giờ trước</span><span className="new-pill">Mới</span></div>
          </div>
        </div>

        <div className="notif2-day">Hôm qua</div>

        <div className="notif2-item">
          <div className="notif2-ic" style={{background:'#DDD6FE'}}>💬</div>
          <div className="notif2-body">
            <div className="t">Cô Lan gửi nhận xét</div>
            <div className="m">"Bé Minh Anh tiến bộ rõ rệt phần Listening..."</div>
            <div className="meta-row"><span className="time">Hôm qua · 16:30</span></div>
          </div>
        </div>

        <div className="notif2-item">
          <div className="notif2-ic" style={{background:'#FED7AA'}}>📅</div>
          <div className="notif2-body">
            <div className="t">Lịch học tuần sau đã có</div>
            <div className="m">Tuần 06/05 · 3 buổi mới đã được cập nhật</div>
            <div className="meta-row"><span className="time">Hôm qua · 09:15</span></div>
          </div>
        </div>

        <div className="notif2-item">
          <div className="notif2-ic" style={{background:'#BBF7D0'}}>💰</div>
          <div className="notif2-body">
            <div className="t">Thanh toán thành công</div>
            <div className="m">+2.400.000₫ · Học phí TA Trẻ Em K21 tháng 4</div>
            <div className="meta-row"><span className="time">Hôm qua · 17:24</span></div>
          </div>
        </div>
      </div>

      <Tabbar active="notif"/>
    </div>
  );
}

// ─────── TABBAR ───────
function Tabbar({ active, dark }) {
  const tabs = [
    { id:'home',   ic:'home',            l:'Nhà' },
    { id:'grades', ic:'graduation-cap',  l:'Điểm' },
    { id:'qr',     ic:'qr-code',         l:'',    fab:true },
    { id:'pay',    ic:'wallet',          l:'Học phí' },
    { id:'notif',  ic:'bell',            l:'Thông báo' },
  ];
  return (
    <div className="tabbar2" style={dark?{background:'rgba(26,20,56,.92)', borderColor:'#FFD166'}:{}}>
      {tabs.map(t => (
        <div key={t.id} className={'tab2' + (active===t.id?' active':'')} style={dark?{color:active===t.id?'#FFD166':'rgba(255,255,255,.7)'}:{}}>
          {t.fab ? (
            <div className="tab2-fab"><Icon name={t.ic} className="h-5 w-5" style={{strokeWidth:2.5}}/></div>
          ) : (
            <>
              <Icon name={t.ic} className="h-5 w-5" style={{strokeWidth: active===t.id?2.5:2}}/>
              <span>{t.l}</span>
            </>
          )}
        </div>
      ))}
    </div>
  );
}

// ─────── STAGE ───────
const SCREENS = [
  { id: 'home',   label: 'Trang chủ',     icon: 'home',            comp: <HomeScreen/> },
  { id: 'qr',     label: 'Điểm danh QR',  icon: 'qr-code',         comp: <QrCheckinScreen/> },
  { id: 'grades', label: 'Bảng điểm',     icon: 'graduation-cap',  comp: <GradesScreen/> },
  { id: 'pay',    label: 'Thanh toán',    icon: 'wallet',          comp: <PaymentScreen/> },
  { id: 'notif',  label: 'Thông báo',     icon: 'bell',            comp: <NotificationsScreen/> },
];

function Stage() {
  const [view, setView] = useState('all');
  useEffect(() => { const id = setInterval(() => window.lucide && lucide.createIcons(), 350); return () => clearInterval(id); }, []);

  const list = view === 'all' ? SCREENS : SCREENS.filter(s => s.id === view);

  return (
    <div className="pa-stage" data-screen-label="Mobile parent app v2">
      <div className="pa-head">
        <div className="pa-eyebrow"><span className="dot"/> KiteClass · App phụ huynh & học sinh</div>
        <h1>Học mỗi ngày,<br/>theo dõi <em>vui như chơi.</em></h1>
        <p>Lịch học, điểm danh QR, bảng điểm theo kỹ năng, thanh toán MoMo/VNPay, thông báo realtime — tất cả gói trong một app.</p>
        <div className="pa-tabs">
          <button className={view==='all'?'is-active':''} onClick={()=>setView('all')}>
            <Icon name="layout-grid" className="h-3.5 w-3.5"/>Tất cả
          </button>
          {SCREENS.map(s => (
            <button key={s.id} className={view===s.id?'is-active':''} onClick={()=>setView(s.id)}>
              <Icon name={s.icon} className="h-3.5 w-3.5"/>{s.label}
            </button>
          ))}
        </div>
      </div>

      <div className="pa-flex">
        {list.map((s, i) => (
          <div className="pa-phone-wrap" key={s.id}>
            <IOSDevice width={320} height={680}>
              {s.comp}
            </IOSDevice>
            <div className="pa-phone-label">
              <span className="num">{i+1}</span>
              {s.label}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<Stage />);
