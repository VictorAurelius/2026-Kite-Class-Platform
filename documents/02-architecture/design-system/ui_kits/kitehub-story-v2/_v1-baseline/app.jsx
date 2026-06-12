// KiteHub Story landing — kite character, scrolly storytelling, before/after slider
const { useState, useEffect, useRef, useCallback } = React;
const Icon = ({ name, className = 'h-4 w-4' }) => <i data-lucide={name} className={className}></i>;

// ---------- Inline SVG Kite character ----------
function KiteCharacter() {
  return (
    <svg viewBox="0 0 240 280" width="220" height="260" style={{ filter: 'drop-shadow(0 24px 48px rgba(14,165,233,.45))' }}>
      <defs>
        <linearGradient id="kg1" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#22D3EE"/><stop offset=".5" stopColor="#0EA5E9"/><stop offset="1" stopColor="#0284C7"/>
        </linearGradient>
        <linearGradient id="kg2" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#F97316"/><stop offset="1" stopColor="#EA580C"/>
        </linearGradient>
        <linearGradient id="kg3" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#A855F7"/><stop offset="1" stopColor="#9333EA"/>
        </linearGradient>
      </defs>
      {/* String to anchor (drawn separately by .kh2-kite-tail) */}
      {/* Diamond body */}
      <polygon points="120,20 220,100 120,180 20,100" fill="url(#kg1)" stroke="rgba(255,255,255,.3)" strokeWidth="2"/>
      {/* Cross struts */}
      <line x1="120" y1="20" x2="120" y2="180" stroke="rgba(255,255,255,.4)" strokeWidth="2"/>
      <line x1="20" y1="100" x2="220" y2="100" stroke="rgba(255,255,255,.4)" strokeWidth="2"/>
      {/* Inner panels */}
      <polygon points="120,30 210,100 120,170 30,100" fill="rgba(255,255,255,.05)"/>
      {/* Hub */}
      <circle cx="120" cy="100" r="22" fill="url(#kg2)" stroke="#fff" strokeWidth="3"/>
      <circle cx="120" cy="100" r="8" fill="#FED7AA"/>
      {/* Friendly eyes */}
      <circle cx="113" cy="98" r="2" fill="#0F172A"/>
      <circle cx="127" cy="98" r="2" fill="#0F172A"/>
      <path d="M115 106 Q120 110 125 106" stroke="#0F172A" strokeWidth="1.5" fill="none" strokeLinecap="round"/>
      {/* Bow ties on tail */}
      <ellipse cx="120" cy="200" rx="12" ry="6" fill="url(#kg2)"/>
      <ellipse cx="120" cy="220" rx="10" ry="5" fill="url(#kg3)"/>
      <ellipse cx="120" cy="238" rx="8" ry="4" fill="url(#kg2)"/>
    </svg>
  );
}

// ---------- HERO ----------
function Hero() {
  const stars = Array.from({ length: 40 }, (_, i) => ({
    top: Math.random() * 100, left: Math.random() * 100, delay: Math.random() * 3
  }));
  return (
    <section className="kh2-hero">
      <div className="kh2-grid" />
      <div className="kh2-stars">
        {stars.map((s, i) => <span key={i} className="kh2-star" style={{ top: s.top + '%', left: s.left + '%', animationDelay: s.delay + 's' }} />)}
      </div>
      <div className="kh2-container kh2-hero-inner">
        <div>
          <span className="kh2-eyebrow-pill"><span className="pulse" /> Đã có 2.400+ trung tâm cùng bay với KiteHub</span>
          <h1 className="kh2-h-mega">
            Cho trung tâm<br />của bạn <span className="grad">đôi cánh</span>{' '}
            <span className="underline">bay cao</span>.
          </h1>
          <p className="kh2-lede">
            KiteHub gói trọn việc quản lý học viên, lớp học, học phí và thương hiệu vào một nền tảng. Ít việc tay hơn, nhiều thời gian dạy hơn.
          </p>
          <div className="kh2-cta-row">
            <a className="kh2-btn kh2-btn-primary" href="#cta">
              Dùng thử miễn phí 14 ngày <Icon name="arrow-right" className="h-4 w-4" />
            </a>
            <a className="kh2-btn kh2-btn-glass" href="#day"><Icon name="play" className="h-4 w-4" /> Xem một ngày với KiteClass</a>
          </div>
          <div className="kh2-trust-row">
            <span><Icon name="check" className="h-3.5 w-3.5 check" /> Không cần thẻ tín dụng</span>
            <span className="kh2-trust-divider" />
            <span><Icon name="check" className="h-3.5 w-3.5 check" /> Hủy bất kỳ lúc nào</span>
            <span className="kh2-trust-divider" />
            <span><Icon name="check" className="h-3.5 w-3.5 check" /> Hỗ trợ 24/7 tiếng Việt</span>
          </div>
        </div>
        <div className="kh2-kite-stage">
          <div className="kh2-tile kh2-tile-1">
            <div className="kh2-tile-label">Học viên</div>
            <div className="kh2-tile-value">1.248</div>
            <div className="kh2-tile-trend"><Icon name="trending-up" className="h-3 w-3" /> +84 tuần này</div>
          </div>
          <div className="kh2-tile kh2-tile-2">
            <div className="kh2-tile-label">Doanh thu</div>
            <div className="kh2-tile-value">128M₫</div>
            <div className="kh2-tile-trend"><Icon name="trending-up" className="h-3 w-3" /> +12.4%</div>
          </div>
          <div className="kh2-tile kh2-tile-3">
            <div className="kh2-tile-label">Lớp đang mở</div>
            <div className="kh2-tile-value">42</div>
            <div className="kh2-tile-trend"><Icon name="check-circle-2" className="h-3 w-3" /> 38 đầy slot</div>
          </div>
          <div className="kh2-tile kh2-tile-4">
            <div className="kh2-tile-label">Giữ chân</div>
            <div className="kh2-tile-value">94%</div>
            <div className="kh2-tile-trend"><Icon name="heart" className="h-3 w-3" /> Học viên hài lòng</div>
          </div>
          <div className="kh2-kite"><KiteCharacter /></div>
          <div className="kh2-kite-tail" />
        </div>
      </div>
      <div className="kh2-scroll-cue"><span>Cuộn xuống</span><div className="line" /></div>
    </section>
  );
}

// ---------- A DAY IN THE LIFE ----------
const DAY_STEPS = [
  { time: '07:30', icon: 'sunrise', t: 'Mở trung tâm', d: 'Bạn vừa dậy, mở app — KiteClass đã sẵn lịch hôm nay. 12 buổi học, 3 lớp cần điểm danh, 2 phụ huynh nhắn tin. Tất cả gói gọn trên một màn.' },
  { time: '09:00', icon: 'user-plus', t: 'Ghi danh học viên mới', d: 'Bé Minh Anh đăng ký lớp IELTS. Bạn nhập thông tin trong 30 giây. KiteClass tự gửi email chào mừng cho phụ huynh, tự xếp lịch học thử.' },
  { time: '14:00', icon: 'calendar-check', t: 'Điểm danh nhanh', d: 'Lớp tiếng Anh trẻ em bắt đầu. Giáo viên quét QR, học viên check-in 10 giây. Phụ huynh nhận thông báo "Bé đã đến lớp" tự động.' },
  { time: '17:30', icon: 'credit-card', t: 'Học phí về', d: 'Phụ huynh thanh toán qua MoMo, VNPay, chuyển khoản — tất cả tự đối soát. Không còn excel, không còn nhầm lẫn. Hóa đơn tự gửi.' },
  { time: '21:00', icon: 'moon', t: 'Đóng máy, ngủ ngon', d: 'Báo cáo cuối ngày tự tổng hợp. Bạn xem lướt: tăng trưởng tốt, học viên hài lòng. Ngủ yên — KiteClass không bao giờ ngủ.' },
];

function DayInLife() {
  const [active, setActive] = useState(0);
  const stepsRef = useRef(null);

  useEffect(() => {
    const obs = new IntersectionObserver(
      (entries) => entries.forEach((e) => { if (e.isIntersecting) setActive(Number(e.target.dataset.idx)); }),
      { rootMargin: '-40% 0px -40% 0px', threshold: 0 }
    );
    stepsRef.current?.querySelectorAll('[data-idx]').forEach((el) => obs.observe(el));
    return () => obs.disconnect();
  }, []);

  return (
    <section className="kh2-section" id="day">
      <div className="kh2-container">
        <div className="kh2-section-head reveal">
          <span className="kh2-eyebrow-pill"><Icon name="clock" className="h-3 w-3" /> Một ngày của chủ trung tâm</span>
          <h2>Từ sáng đến tối,<br/>KiteClass <span style={{ background:'linear-gradient(90deg,#22D3EE,#A855F7)', WebkitBackgroundClip:'text', backgroundClip:'text', color:'transparent' }}>lo từng phút</span></h2>
          <p>Cuộn xuống để xem một ngày làm việc thật sự, không slide sales nhàm chán.</p>
        </div>
        <div className="kh2-day-grid">
          <div className="kh2-day-steps" ref={stepsRef}>
            {DAY_STEPS.map((s, i) => (
              <div key={i} data-idx={i} className={'kh2-day-step' + (active === i ? ' is-active' : '')} onClick={() => setActive(i)}>
                <div className="head">
                  <span className="time">{s.time}</span>
                  <span className="icon" style={{ color: active === i ? '#22D3EE' : '#94A3B8' }}><Icon name={s.icon} className="h-4 w-4" /></span>
                  <h3>{s.t}</h3>
                </div>
                <p>{s.d}</p>
              </div>
            ))}
          </div>
          <div className="kh2-day-stage">
            <div className="kh2-stage-frame">
              <SceneOnboard active={active === 0} />
              <SceneRoster active={active === 1} />
              <SceneAttendance active={active === 2} />
              <ScenePayment active={active === 3} />
              <SceneNight active={active === 4} />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function SceneOnboard({ active }) {
  return (
    <div className={'scene' + (active ? ' is-active' : '')}>
      <div className="s-onb-head">
        <div className="s-onb-mark" />
        <div>
          <div style={{ fontWeight: 700, fontSize: 14 }}>Tạo trung tâm mới</div>
          <div style={{ fontSize: 11, color: '#64748B' }}>Bước 2/4 · Thông tin cơ bản</div>
        </div>
      </div>
      <div className="s-onb-form">
        <div className="s-fld"><label>Tên trung tâm</label><input className="typed" defaultValue="Trung tâm SkyLight English" /></div>
        <div className="s-fld"><label>Subdomain</label><input className="typed" defaultValue="skylight.kitehub.me" /></div>
        <div className="s-fld"><label>Email quản lý</label><input className="typed" defaultValue="hoang@skylight.vn" /></div>
        <div className="s-fld"><label>Số học viên hiện tại</label><input defaultValue="" placeholder="Đang nhập..." /></div>
      </div>
      <div className="s-progress"><div className="done"/><div className="done"/><div/><div/></div>
    </div>
  );
}

function SceneRoster({ active }) {
  const roster = [
    { n:'Nguyễn Minh Anh', c:'#3B82F6', b:'IELTS Foundation', new:true },
    { n:'Trần Quốc Bảo', c:'#10B981', b:'TOEIC 600+' },
    { n:'Lê Thị Hương', c:'#F97316', b:'Tiếng Anh B1' },
    { n:'Phạm Đức Anh', c:'#A855F7', b:'TA Trẻ Em' },
    { n:'Võ Thị Lan', c:'#EC4899', b:'IELTS Foundation' },
    { n:'Đặng Quang Huy', c:'#14B8A6', b:'TOEIC 600+' },
  ];
  return (
    <div className={'scene' + (active ? ' is-active' : '')}>
      <div className="s-att-head">
        <h4>Học viên · 6 bạn</h4>
        <span className="t">Vừa thêm Minh Anh ✨</span>
      </div>
      {roster.map((r) => (
        <div key={r.n} className="s-att-row">
          <div className="s-att-avatar" style={{ background: r.c }}>{r.n.split(' ').slice(-1)[0][0]}</div>
          <div>
            <div style={{ fontWeight: 500 }}>{r.n}</div>
            <div style={{ fontSize: 11, color: '#64748B' }}>{r.b}</div>
          </div>
          {r.new ? <span className="s-att-status present">Mới</span> : <span style={{ fontSize: 11, color: '#94A3B8' }}>Đang học</span>}
        </div>
      ))}
    </div>
  );
}

function SceneAttendance({ active }) {
  const [done, setDone] = useState([]);
  useEffect(() => {
    if (!active) { setDone([]); return; }
    const ids = [0, 1, 2, 3, 4, 5];
    ids.forEach((id, i) => {
      setTimeout(() => setDone((d) => [...d, id]), 400 + i * 250);
    });
  }, [active]);

  const roster = [
    { n:'Nguyễn Minh Anh', c:'#3B82F6', s:'present' },
    { n:'Trần Quốc Bảo', c:'#10B981', s:'present' },
    { n:'Lê Thị Hương', c:'#F97316', s:'late' },
    { n:'Phạm Đức Anh', c:'#A855F7', s:'present' },
    { n:'Võ Thị Lan', c:'#EC4899', s:'absent' },
    { n:'Đặng Quang Huy', c:'#14B8A6', s:'present' },
  ];
  const labels = { present: 'Có mặt', late: 'Trễ', absent: 'Vắng', pending: '— —' };
  return (
    <div className={'scene' + (active ? ' is-active' : '')}>
      <div className="s-att-head">
        <h4>Điểm danh · TA Trẻ Em K21</h4>
        <span className="t">14:00 · QR đã quét</span>
      </div>
      {roster.map((r, i) => {
        const isDone = done.includes(i);
        return (
          <div key={r.n} className="s-att-row">
            <div className="s-att-avatar" style={{ background: r.c }}>{r.n.split(' ').slice(-1)[0][0]}</div>
            <div>
              <div style={{ fontWeight: 500 }}>{r.n}</div>
              <div style={{ fontSize: 11, color: '#64748B' }}>HV{1100 + i}</div>
            </div>
            <span className={'s-att-status ' + (isDone ? r.s : 'pending')}>
              {isDone ? labels[r.s] : labels.pending}
            </span>
          </div>
        );
      })}
    </div>
  );
}

function ScenePayment({ active }) {
  const [count, setCount] = useState(0);
  useEffect(() => {
    if (!active) { setCount(0); return; }
    const id = setInterval(() => setCount((c) => Math.min(c + 1, 5)), 800);
    return () => clearInterval(id);
  }, [active]);

  const pays = [
    { n: 'Phụ huynh Minh Anh', m: 'MoMo · 17:24', a: '2.400.000₫' },
    { n: 'Phụ huynh Quốc Bảo', m: 'VNPay · 17:31', a: '1.800.000₫' },
    { n: 'Phụ huynh Đức Anh', m: 'Chuyển khoản · 17:44', a: '3.200.000₫' },
    { n: 'Phụ huynh Lan', m: 'MoMo · 17:48', a: '2.400.000₫' },
    { n: 'Phụ huynh Quang Huy', m: 'ZaloPay · 17:52', a: '1.800.000₫' },
  ];
  return (
    <div className={'scene' + (active ? ' is-active' : '')}>
      <div className="s-att-head">
        <h4>Học phí về · 17:30</h4>
        <span className="t" style={{ color: '#16A34A' }}>● Đang nhận</span>
      </div>
      <div className="s-pay-stack">
        {pays.slice(0, count).map((p, i) => (
          <div key={i} className={'s-pay-card' + (i === count - 1 ? ' fresh' : '')}>
            <div className="s-pay-icon"><Icon name="check" className="h-4 w-4" /></div>
            <div>
              <div className="s-pay-name">{p.n}</div>
              <div className="s-pay-meta">{p.m}</div>
            </div>
            <div className="s-pay-amt">+{p.a}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function SceneNight({ active }) {
  return (
    <div className={'scene' + (active ? ' is-active' : '')}>
      <div className="s-night">
        <div className="s-night-emoji">😴</div>
        <h4>Ngủ ngon, mai lại bay cao!</h4>
        <p>Báo cáo cuối ngày đã sẵn. KiteClass tự chạy backup, gửi nhắc lịch ngày mai cho giáo viên.</p>
        <div className="stats">
          <div className="stat"><b>12</b><span>Buổi học</span></div>
          <div className="stat"><b>11.6M₫</b><span>Học phí</span></div>
          <div className="stat"><b>+4</b><span>Học viên mới</span></div>
        </div>
      </div>
    </div>
  );
}

// ---------- BEFORE / AFTER ----------
function BeforeAfter() {
  const [pos, setPos] = useState(48);
  return (
    <section className="kh2-section light">
      <div className="kh2-container">
        <div className="kh2-section-head reveal">
          <span className="kh2-eyebrow-pill"><Icon name="zap" className="h-3 w-3" /> Trước & sau KiteClass</span>
          <h2>Kéo để xem<br/>cuộc sống đổi khác</h2>
          <p>Bên trái: sổ giấy, excel, nhóm Zalo loạn. Bên phải: một dashboard biết làm thay bạn.</p>
        </div>
        <div className="kh2-ba-wrap" style={{ '--ba-pos': pos + '%' }}>
          <div className="kh2-ba-side ba-before">
            <div className="kh2-ba-tag before">Trước</div>
            <div className="ba-before-stack">
              <div className="ba-paper">
                <b>Sổ học phí T4</b>
                Minh Anh — 2.4tr ✓<br/>
                Quốc Bảo — <span className="ba-cross">1.8tr</span> nợ<br/>
                Bé Hương — <span className="scribble">??</span><br/>
                Đức Anh — <span className="ba-cross">3.2</span>tr ✓<br/>
                Lan — chưa thu<br/>
                <span className="scribble">+ Huy</span>
              </div>
              <div className="ba-paper r2">
                <b>Lịch tuần này</b>
                T2: Lớp B1 (?)<br/>
                T3: <span className="ba-cross">IELTS</span>...<br/>
                T4: Trẻ em — Cô Lan<br/>
                T5: TOEIC<br/>
                T6: <span className="scribble">đổi giờ?</span>
              </div>
              <div className="ba-paper r3">
                <b>Điểm danh K12</b>
                ✓ Minh Anh<br/>
                ✓ Quốc Bảo<br/>
                ? Hương — đến trễ<br/>
                <span className="ba-cross">✓</span> Đức Anh<br/>
                ✗ Lan<br/>
                ✓ <span className="scribble">Huy?</span>
              </div>
            </div>
            <div className="ba-coffee">☕</div>
          </div>
          <div className="kh2-ba-side ba-after kh2-ba-after">
            <div className="kh2-ba-tag after">Sau</div>
            <div className="ba-app">
              <div className="ba-app-side">
                <div className="dot active"/><div className="dot"/><div className="dot"/><div className="dot"/><div className="dot"/>
              </div>
              <div className="ba-app-main">
                <div style={{ fontWeight: 700, fontSize: 14 }}>Tổng quan tháng 4</div>
                <div className="ba-app-stats">
                  <div className="s"><b>1.248</b><span>Học viên</span></div>
                  <div className="s"><b>42</b><span>Lớp học</span></div>
                  <div className="s"><b>128M₫</b><span>Doanh thu</span></div>
                  <div className="s"><b>94%</b><span>Giữ chân</span></div>
                </div>
                <div className="ba-app-chart">
                  {[40, 55, 48, 70, 62, 85, 78, 92, 84, 96].map((h, i) => <div key={i} className="b" style={{ height: h+'%' }}/>)}
                </div>
              </div>
            </div>
          </div>
          <div className="kh2-ba-handle" />
          <input
            type="range" className="kh2-ba-input" min="6" max="94" value={pos}
            onChange={(e) => setPos(+e.target.value)}
          />
        </div>
      </div>
    </section>
  );
}

// ---------- METRICS ----------
function Metrics() {
  const [vals, setVals] = useState({ a: 0, b: 0, c: 0, d: 0 });
  const ref = useRef(null);
  useEffect(() => {
    const obs = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        const targets = { a: 2400, b: 12, c: 96, d: 84 };
        const dur = 1500; const start = performance.now();
        const tick = (now) => {
          const p = Math.min((now - start) / dur, 1);
          const e = 1 - Math.pow(1 - p, 3);
          setVals({ a: Math.floor(targets.a * e), b: Math.floor(targets.b * e), c: Math.floor(targets.c * e), d: Math.floor(targets.d * e) });
          if (p < 1) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
        obs.disconnect();
      }
    }, { threshold: 0.4 });
    if (ref.current) obs.observe(ref.current);
    return () => obs.disconnect();
  }, []);
  return (
    <section className="kh2-section alt" ref={ref}>
      <div className="kh2-container">
        <div className="kh2-section-head reveal">
          <span className="kh2-eyebrow-pill"><Icon name="bar-chart-3" className="h-3 w-3" /> Con số biết nói</span>
          <h2>Trung tâm cùng KiteClass tăng trưởng nhanh hơn</h2>
        </div>
        <div className="kh2-metric-grid">
          <div className="kh2-metric reveal">
            <div className="kh2-metric-num">{vals.a.toLocaleString('vi')}<small>+</small></div>
            <div className="kh2-metric-label">Trung tâm khắp 63 tỉnh thành đang dùng KiteClass mỗi ngày.</div>
          </div>
          <div className="kh2-metric reveal">
            <div className="kh2-metric-num">{vals.b}<small>h</small></div>
            <div className="kh2-metric-label">Trung bình tiết kiệm <b style={{color:'#fff'}}>12 giờ/tuần</b> cho công việc giấy tờ.</div>
          </div>
          <div className="kh2-metric reveal">
            <div className="kh2-metric-num">{vals.c}<small>%</small></div>
            <div className="kh2-metric-label">Giáo viên thấy việc điểm danh và nhập điểm <b style={{color:'#fff'}}>nhanh hơn rõ rệt</b>.</div>
          </div>
          <div className="kh2-metric reveal">
            <div className="kh2-metric-num">{vals.d}<small>%</small></div>
            <div className="kh2-metric-label">Tỷ lệ phụ huynh <b style={{color:'#fff'}}>thanh toán đúng hạn</b> sau khi bật nhắc tự động.</div>
          </div>
        </div>
      </div>
    </section>
  );
}

// ---------- TESTIMONIAL + LOGOS ----------
function Testimonial() {
  return (
    <section className="kh2-section light">
      <div className="kh2-container">
        <div className="kh2-quote-row">
          <div className="reveal">
            <span className="kh2-eyebrow-pill"><Icon name="quote" className="h-3 w-3" /> Khách hàng nói gì</span>
            <p className="kh2-quote" style={{ marginTop: 22 }}>
              "Tôi từng <span className="grad">làm việc đến 11 giờ đêm</span> chỉ để đối soát học phí và nhắn tin phụ huynh. Sau 2 tuần dùng KiteClass, tôi rảnh hẳn để đi tập gym lại — đời thay đổi thật."
            </p>
            <div className="kh2-quote-meta">
              <div className="kh2-quote-avatar">H</div>
              <div>
                <div className="kh2-quote-name">Cô Hương</div>
                <div className="kh2-quote-role">Chủ Trung tâm SkyLight English · 380 học viên</div>
              </div>
            </div>
          </div>
          <div className="kh2-logo-grid reveal">
            {[
              { em: '🏫', n: 'SkyLight' },
              { em: '🌱', n: 'GreenEdu' },
              { em: '🌸', n: 'PinkKids' },
              { em: '📚', n: 'BookWorm' },
              { em: '🎓', n: 'TopScore' },
              { em: '🌟', n: 'StarKids' },
            ].map((l) => (
              <div key={l.n} className="kh2-logo-cell"><span className="em">{l.em}</span>{l.n}</div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

// ---------- FINAL CTA ----------
function FinalCTA() {
  return (
    <section className="kh2-final" id="cta">
      <div className="kh2-container">
        <div className="kh2-final-card reveal">
          <h2>Sẵn sàng cho<br/>trung tâm bay cao?</h2>
          <p>14 ngày dùng thử, không cần thẻ tín dụng. Onboard 1-1 cùng đội ngũ Việt Nam — bạn không bao giờ cô đơn.</p>
          <div className="kh2-cta-row" style={{ justifyContent: 'center' }}>
            <a className="kh2-btn kh2-btn-primary" href="#">Bắt đầu miễn phí <Icon name="arrow-right" className="h-4 w-4" /></a>
            <a className="kh2-btn kh2-btn-glass" href="#"><Icon name="phone" className="h-4 w-4" /> Đặt lịch demo</a>
          </div>
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="kh2-foot">
      <div className="kh2-container kh2-foot-inner">
        <span>© 2026 KiteHub Platform · Made in Vietnam 🇻🇳</span>
        <div className="kh2-foot-links">
          <a>Điều khoản</a><a>Bảo mật</a><a>Hỗ trợ</a><a>Liên hệ</a>
        </div>
      </div>
    </footer>
  );
}

// ---------- Reveal-on-scroll observer ----------
function useReveal() {
  useEffect(() => {
    const obs = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) e.target.classList.add('is-in'); });
    }, { threshold: 0.15 });
    document.querySelectorAll('.reveal').forEach((el) => obs.observe(el));
    return () => obs.disconnect();
  }, []);
}

function App() {
  useReveal();
  useEffect(() => {
    const i = setInterval(() => window.lucide && lucide.createIcons(), 400);
    return () => clearInterval(i);
  }, []);
  return (
    <>
      <nav className="kh2-nav">
        <div className="kh2-nav-brand"><span className="kh2-nav-mark"><Icon name="kanban" className="h-3.5 w-3.5" /></span> KiteHub</div>
        <a href="#day">Tính năng</a>
        <a href="#cta">Bảng giá</a>
        <a href="#cta">Liên hệ</a>
        <a className="kh2-nav-cta" href="#cta">Dùng thử →</a>
      </nav>
      <Hero />
      <DayInLife />
      <BeforeAfter />
      <Metrics />
      <Testimonial />
      <FinalCTA />
      <Footer />
    </>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
