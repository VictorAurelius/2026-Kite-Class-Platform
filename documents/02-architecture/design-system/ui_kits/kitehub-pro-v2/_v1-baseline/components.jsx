// KiteHub UI kit — all components in one file (babel-transpiled)
const { useState, useEffect } = React;

const Icon = ({ name, className = 'h-4 w-4', ...p }) => (
  <i data-lucide={name} className={className} {...p}></i>
);

// -------- Marketing header --------
function MarketingHeader() {
  return (
    <header className="kh-header">
      <div className="kh-container kh-header-inner">
        <a className="kh-logo" href="#">
          <img src="../../assets/kitehub-logo.svg" height="32" alt="KiteHub" />
        </a>
        <nav className="kh-nav">
          <a href="#">Tính năng</a>
          <a href="#">Bảng giá</a>
          <a href="#">Câu hỏi</a>
          <a href="#">Liên hệ</a>
        </nav>
        <div className="kh-header-cta">
          <a href="#" className="kh-btn kh-btn-ghost">Đăng nhập</a>
          <a href="#" className="kh-btn kh-btn-primary">Dùng thử miễn phí</a>
        </div>
      </div>
    </header>
  );
}

// -------- Hero --------
function Hero() {
  return (
    <section className="kh-hero">
      <div className="kh-blob kh-blob-a" />
      <div className="kh-blob kh-blob-b" />
      <div className="kh-container kh-hero-inner">
        <div className="kh-hero-copy">
          <span className="kh-eyebrow kh-eyebrow-pill">
            <Icon name="sparkles" className="h-3 w-3" /> Nền tảng SaaS cho trung tâm giáo dục
          </span>
          <h1 className="kh-h-display">
            Quản lý trung tâm <span className="kh-grad">thông minh hơn</span>
          </h1>
          <p className="kh-lede">
            Dành thời gian cho việc giảng dạy, để KiteClass lo phần còn lại. Quản lý học viên,
            lớp học, thanh toán chỉ trong một nơi.
          </p>
          <div className="kh-cta-row">
            <a className="kh-btn kh-btn-primary kh-btn-lg" href="#">
              Dùng thử miễn phí 14 ngày <Icon name="arrow-right" className="h-4 w-4" />
            </a>
            <a className="kh-btn kh-btn-outline kh-btn-lg" href="#">
              <Icon name="play" className="h-4 w-4" /> Xem demo
            </a>
          </div>
          <div className="kh-trust">
            <span><Icon name="check" className="h-3.5 w-3.5" /> Không cần thẻ tín dụng</span>
            <span><Icon name="check" className="h-3.5 w-3.5" /> Hủy bất kỳ lúc nào</span>
            <span><Icon name="check" className="h-3.5 w-3.5" /> Hỗ trợ tiếng Việt</span>
          </div>
        </div>
        <HeroMockup />
      </div>
    </section>
  );
}

function HeroMockup() {
  return (
    <div className="kh-mock">
      <div className="kh-mock-chrome">
        <span className="kh-dot" style={{ background: '#EF4444' }} />
        <span className="kh-dot" style={{ background: '#F59E0B' }} />
        <span className="kh-dot" style={{ background: '#10B981' }} />
        <span className="kh-mock-url">skylight.kitehub.me</span>
      </div>
      <div className="kh-mock-body">
        <div className="kh-mock-side">
          <div className="kh-mock-brand" />
          {['Tổng quan', 'Lớp học', 'Học viên', 'Thanh toán', 'Cài đặt'].map((t, i) => (
            <div key={t} className={'kh-mock-nav' + (i === 0 ? ' is-active' : '')}>{t}</div>
          ))}
        </div>
        <div className="kh-mock-main">
          <div className="kh-mock-stats">
            {[
              { l: 'Học viên', v: '1.248', c: '#3B82F6' },
              { l: 'Lớp học', v: '42', c: '#10B981' },
              { l: 'Doanh thu', v: '128M', c: '#F59E0B' },
              { l: 'Tỷ lệ', v: '94%', c: '#A855F7' },
            ].map((s) => (
              <div key={s.l} className="kh-mock-stat">
                <div style={{ color: s.c, fontSize: 11, fontWeight: 600 }}>{s.l}</div>
                <div style={{ fontSize: 18, fontWeight: 700 }}>{s.v}</div>
              </div>
            ))}
          </div>
          <div className="kh-mock-chart">
            {[40, 70, 55, 85, 60, 92, 75].map((h, i) => (
              <div key={i} className="kh-mock-bar" style={{ height: h + '%', animationDelay: i * 50 + 'ms' }} />
            ))}
          </div>
        </div>
      </div>
      <div className="kh-float kh-float-a">
        <Icon name="check-circle-2" className="h-4 w-4" /> Đã lưu tự động
      </div>
      <div className="kh-float kh-float-b">
        <Icon name="trending-up" className="h-4 w-4" /> +12.4% tháng này
      </div>
    </div>
  );
}

// -------- Features --------
const FEATURES = [
  { i: 'users', t: 'Quản lý học viên', d: 'Lưu trữ thông tin, theo dõi tiến độ, tự động phân lớp.', c: ['#3B82F6', '#2563EB'], bg: '#EFF6FF', em: '👨‍🎓' },
  { i: 'calendar', t: 'Lịch & điểm danh', d: 'Tạo lịch học, điểm danh QR, thông báo tới phụ huynh.', c: ['#22C55E', '#16A34A'], bg: '#F0FDF4', em: '📅' },
  { i: 'credit-card', t: 'Thanh toán tự động', d: 'Gửi hóa đơn, nhắc học phí, báo cáo doanh thu realtime.', c: ['#F97316', '#EA580C'], bg: '#FFF7ED', em: '💳' },
  { i: 'palette', t: 'AI Branding', d: 'Tùy chỉnh logo, màu sắc, tên miền riêng cho trung tâm.', c: ['#A855F7', '#9333EA'], bg: '#FAF5FF', em: '🎨' },
  { i: 'trending-up', t: 'Báo cáo thông minh', d: 'Dashboard phân tích, dự báo doanh thu, so sánh kỳ.', c: ['#EC4899', '#DB2777'], bg: '#FDF2F8', em: '📊' },
  { i: 'building-2', t: 'Đa chi nhánh', d: 'Quản lý nhiều cơ sở từ một tài khoản quản trị duy nhất.', c: ['#14B8A6', '#0D9488'], bg: '#F0FDFA', em: '🏢' },
];

function Features() {
  return (
    <section className="kh-section">
      <div className="kh-container">
        <div className="kh-section-head">
          <span className="kh-eyebrow">Tính năng nổi bật</span>
          <h2 className="kh-h1">Mọi thứ bạn cần để vận hành một trung tâm hiện đại</h2>
        </div>
        <div className="kh-grid-3">
          {FEATURES.map((f) => (
            <div key={f.t} className="kh-feat" style={{ background: f.bg }}>
              <div className="kh-feat-em">{f.em}</div>
              <div className="kh-feat-chip" style={{ background: `linear-gradient(135deg, ${f.c[0]}, ${f.c[1]})` }}>
                <Icon name={f.i} className="h-5 w-5" />
              </div>
              <h3 className="kh-h3">{f.t}</h3>
              <p className="kh-body-sm">{f.d}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// -------- Pricing --------
const TIERS = [
  { n: 'Khởi nghiệp', p: '199.000', sub: 'Dành cho trung tâm nhỏ mới bắt đầu', feats: ['Tối đa 100 học viên', '5 lớp học', 'Báo cáo cơ bản', 'Hỗ trợ email'] },
  { n: 'Chuyên nghiệp', p: '499.000', sub: 'Cho trung tâm đang phát triển', feats: ['Không giới hạn học viên', '50 lớp học', 'AI Branding', 'Báo cáo nâng cao', 'Hỗ trợ ưu tiên'], popular: true },
  { n: 'Doanh nghiệp', p: 'Liên hệ', sub: 'Chuỗi trung tâm, nhiều chi nhánh', feats: ['Tất cả tính năng', 'Đa chi nhánh', 'API tùy chỉnh', 'SLA 99.9%', 'CSKH riêng'] },
];

function Pricing() {
  return (
    <section className="kh-section kh-section-alt">
      <div className="kh-container">
        <div className="kh-section-head">
          <span className="kh-eyebrow">Bảng giá</span>
          <h2 className="kh-h1">Chọn gói phù hợp với quy mô của bạn</h2>
          <p className="kh-lede-sm">Dùng thử miễn phí 14 ngày. Không cần thẻ tín dụng.</p>
        </div>
        <div className="kh-grid-3 kh-pricing">
          {TIERS.map((t) => (
            <div key={t.n} className={'kh-tier' + (t.popular ? ' is-popular' : '')}>
              {t.popular && <span className="kh-tier-ribbon">Phổ biến nhất</span>}
              <h3 className="kh-h3">{t.n}</h3>
              <p className="kh-body-sm kh-muted">{t.sub}</p>
              <div className="kh-price">
                {t.p === 'Liên hệ' ? <span className="kh-price-big">Liên hệ</span> :
                  <><span className="kh-price-big">{t.p}đ</span><span className="kh-price-unit">/tháng</span></>}
              </div>
              <button className={'kh-btn ' + (t.popular ? 'kh-btn-primary' : 'kh-btn-outline')}>
                {t.p === 'Liên hệ' ? 'Liên hệ tư vấn' : 'Bắt đầu miễn phí'}
              </button>
              <ul className="kh-tier-list">
                {t.feats.map((f) => (
                  <li key={f}><Icon name="check" className="h-4 w-4" /> {f}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// -------- FAQ --------
const FAQS = [
  { q: 'Tôi không rành công nghệ lắm, có dùng được không?', a: 'Hoàn toàn được! KiteClass được thiết kế đặc biệt cho người không chuyên IT — chỉ cần biết dùng trình duyệt là đủ. Chúng tôi có đội ngũ hỗ trợ tiếng Việt sẵn sàng giúp bạn thiết lập trong 30 phút.' },
  { q: 'Dữ liệu học viên của tôi có an toàn không?', a: 'Tuyệt đối an toàn. Dữ liệu được mã hóa AES-256 — an toàn như gửi tiền ngân hàng. Máy chủ đặt tại Việt Nam, sao lưu hàng ngày.' },
  { q: 'Tôi có thể hủy bất cứ lúc nào không?', a: 'Có. Bạn có thể hủy hoặc đổi gói bất kỳ lúc nào trong phần Cài đặt. Không có phí hủy, không có hợp đồng dài hạn.' },
  { q: 'KiteHub và KiteClass khác nhau thế nào?', a: 'KiteHub là nơi bạn đăng ký, quản lý gói dịch vụ và tùy chỉnh thương hiệu. KiteClass là ứng dụng mà giáo viên, học viên, phụ huynh sử dụng hàng ngày.' },
];

function FAQ() {
  const [open, setOpen] = useState(0);
  return (
    <section className="kh-section">
      <div className="kh-container kh-faq">
        <div className="kh-section-head">
          <span className="kh-eyebrow">Câu hỏi thường gặp</span>
          <h2 className="kh-h1">Bạn đang băn khoăn điều gì?</h2>
        </div>
        <div>
          {FAQS.map((f, i) => (
            <div key={i} className={'kh-faq-item' + (open === i ? ' is-open' : '')}>
              <button className="kh-faq-q" onClick={() => setOpen(open === i ? -1 : i)}>
                <span>{f.q}</span>
                <Icon name="chevron-down" className="h-4 w-4 kh-chev" />
              </button>
              {open === i && <div className="kh-faq-a">{f.a}</div>}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// -------- Footer --------
function Footer() {
  return (
    <footer className="kh-footer">
      <div className="kh-container kh-footer-grid">
        <div>
          <img src="../../assets/kitehub-logo.svg" height="32" alt="KiteHub" />
          <p className="kh-body-sm kh-muted" style={{ maxWidth: 260, marginTop: 8 }}>
            Nền tảng SaaS giúp trung tâm giáo dục Việt Nam chuyển đổi số dễ dàng.
          </p>
        </div>
        <div>
          <h4 className="kh-foot-h">Sản phẩm</h4>
          <a>Tính năng</a><a>Bảng giá</a><a>Bảo mật</a>
        </div>
        <div>
          <h4 className="kh-foot-h">Hỗ trợ</h4>
          <a>Tài liệu</a><a>Liên hệ</a><a>Trung tâm trợ giúp</a>
        </div>
        <div>
          <h4 className="kh-foot-h">Liên hệ</h4>
          <a><Icon name="phone" className="h-3.5 w-3.5" /> 1900 xxxx</a>
          <a><Icon name="mail" className="h-3.5 w-3.5" /> hello@kitehub.vn</a>
        </div>
      </div>
      <div className="kh-container kh-foot-bot">
        <span>© 2026 KiteHub Platform. Made in Vietnam 🇻🇳</span>
        <span>Điều khoản · Bảo mật</span>
      </div>
    </footer>
  );
}

// -------- Customer dashboard screen --------
function CustomerDashboard() {
  return (
    <div className="kh-app">
      <aside className="kh-side">
        <div className="kh-side-brand">
          <img src="../../assets/kite-mark.svg" height="28" />
          <span style={{ fontWeight: 700 }}>KiteHub</span>
        </div>
        <nav className="kh-side-nav">
          {[
            ['layout-dashboard', 'Tổng quan', true],
            ['building-2', 'Trung tâm'],
            ['credit-card', 'Thanh toán'],
            ['palette', 'AI Branding'],
            ['trending-up', 'Báo cáo'],
            ['settings', 'Cài đặt'],
          ].map(([i, l, a]) => (
            <a key={l} className={'kh-side-link' + (a ? ' is-active' : '')}>
              <Icon name={i} className="h-4 w-4" /> {l}
            </a>
          ))}
        </nav>
        <div className="kh-trial">
          <div style={{ fontSize: 11, color: '#F59E0B', fontWeight: 600 }}>Còn 12 ngày dùng thử</div>
          <button className="kh-btn kh-btn-accent kh-btn-sm" style={{ marginTop: 6, width: '100%' }}>Nâng cấp ngay</button>
        </div>
      </aside>
      <main className="kh-main">
        <WelcomeBanner />
        <div className="kh-stat-row">
          {[
            { i: 'building-2', l: 'Trung tâm', v: '3', s: '+1 tháng này', c: '#3B82F6' },
            { i: 'users', l: 'Học viên', v: '1.248', s: '+84 tháng này', c: '#10B981' },
            { i: 'credit-card', l: 'Doanh thu', v: '128.450.000đ', s: '+12.4%', c: '#F97316' },
            { i: 'trending-up', l: 'Tăng trưởng', v: '94%', s: 'Giữ chân học viên', c: '#A855F7' },
          ].map((s) => (
            <div key={s.l} className="kh-stat">
              <div className="kh-stat-icon" style={{ background: s.c + '18', color: s.c }}>
                <Icon name={s.i} className="h-5 w-5" />
              </div>
              <div>
                <div className="kh-stat-label">{s.l}</div>
                <div className="kh-stat-value">{s.v}</div>
                <div className="kh-stat-sub">{s.s}</div>
              </div>
            </div>
          ))}
        </div>
        <TrungTamList />
        <Tips />
      </main>
    </div>
  );
}

function WelcomeBanner() {
  const h = new Date().getHours();
  const greet = h < 11 ? 'Chào buổi sáng' : h < 18 ? 'Chào buổi chiều' : 'Chào buổi tối';
  return (
    <div className="kh-banner">
      <div className="kh-banner-accent" />
      <div>
        <div className="kh-eyebrow">Bảng điều khiển</div>
        <h1 className="kh-h1" style={{ marginTop: 4 }}>{greet}, Hoàng 👋</h1>
        <p className="kh-body-sm kh-muted">Hôm nay là một ngày tuyệt vời để giúp học viên bay cao.</p>
      </div>
      <button className="kh-btn kh-btn-primary"><Icon name="plus" className="h-4 w-4" /> Tạo trung tâm</button>
    </div>
  );
}

function TrungTamList() {
  const items = [
    { n: 'SkyLight English', d: 'skylight.kitehub.me', s: 'Hoạt động', sc: 'suc', hv: 428, lp: 18, g: 'Chuyên nghiệp' },
    { n: 'GreenEdu Academy', d: 'greenedu.kitehub.me', s: 'Dùng thử', sc: 'info', hv: 82, lp: 4, g: 'Khởi nghiệp' },
    { n: 'PinkKids Montessori', d: 'pinkkids.kitehub.me', s: 'Chờ TT', sc: 'warn', hv: 156, lp: 7, g: 'Chuyên nghiệp' },
  ];
  return (
    <div className="kh-card">
      <div className="kh-card-accent" />
      <div className="kh-card-head">
        <h3 className="kh-h3">Trung tâm của bạn</h3>
        <a className="kh-link">Xem tất cả <Icon name="arrow-right" className="h-3.5 w-3.5" /></a>
      </div>
      <div className="kh-tenant-list">
        {items.map((t) => (
          <div key={t.n} className="kh-tenant">
            <div className="kh-tenant-avatar">{t.n.split(' ').map((w) => w[0]).slice(0, 2).join('')}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="kh-tenant-name">{t.n}</div>
              <div className="kh-tenant-url">{t.d}</div>
            </div>
            <div className="kh-tenant-stat"><b>{t.hv}</b> học viên</div>
            <div className="kh-tenant-stat"><b>{t.lp}</b> lớp</div>
            <span className={'kh-pill kh-pill-' + t.sc}><span className="kh-pill-dot" />{t.s}</span>
            <button className="kh-btn kh-btn-ghost kh-btn-sm"><Icon name="external-link" className="h-3.5 w-3.5" /></button>
          </div>
        ))}
      </div>
    </div>
  );
}

function Tips() {
  const tips = [
    { i: 'sparkles', t: 'Kích hoạt AI Branding', d: 'Tùy chỉnh giao diện KiteClass theo màu thương hiệu của bạn trong 2 phút.' },
    { i: 'zap', t: 'Gửi tin nhắn hàng loạt', d: 'Thông báo cho toàn bộ phụ huynh về lịch nghỉ hoặc sự kiện sắp tới.' },
    { i: 'shield', t: 'Bật xác thực 2 lớp', d: 'Tăng bảo mật cho tài khoản quản trị và dữ liệu học viên.' },
  ];
  return (
    <div className="kh-card">
      <div className="kh-card-head">
        <div>
          <span className="kh-eyebrow">Mẹo sử dụng KiteClass</span>
          <h3 className="kh-h3" style={{ marginTop: 4 }}>Tận dụng tối đa nền tảng</h3>
        </div>
      </div>
      <div className="kh-grid-3">
        {tips.map((t) => (
          <div key={t.t} className="kh-tip">
            <div className="kh-tip-chip"><Icon name={t.i} className="h-4 w-4" /></div>
            <h4 className="kh-h4">{t.t}</h4>
            <p className="kh-body-sm kh-muted">{t.d}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, {
  MarketingHeader, Hero, Features, Pricing, FAQ, Footer, CustomerDashboard,
});
