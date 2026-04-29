/**
 * kiteclass-parent — React-flavoured prototype component
 *
 * Persona: Pa. Parent (mobile-first 320-414px, evening usage, low-medium tech literacy)
 * Direction D pivot per dossier/08-direction-decisions.md §2:
 *   web responsive + PWA-grade — NOT native app.
 *
 * Score self-estimate: N/A (this is a code reference; HTML screens carry the score)
 *
 * NOTE: This is a TYPE-CHECKING-AGNOSTIC prototype, not production code.
 * - Renders the same screens shown in /screens/*.html as a single React component tree
 * - Uses React/Next conventions (hooks, JSX) but doesn't import a real React runtime
 * - Track 2 production port (GAP-264..267) will:
 *     - Move to Next.js 15 App Router under kiteclass-frontend/src/app/parent/
 *     - Wire to real /api/v1/parent endpoints with React Query
 *     - Replace inline SVGs with lucide-react imports
 *     - Add Zod validation, react-hook-form for billing, etc.
 *
 * Reviewer instructions: read alongside screens/*.html — this file shows the
 * COMPONENT BOUNDARIES the production port should respect.
 */

import { useState, useEffect } from "react";

// ============================================================
// PRIMITIVES — small, composable, Pa.-Parent-tuned
// ============================================================

function StatusBar({ time = "9:41" }) {
  return (
    <div className="status-bar" aria-hidden="true">
      <span>{time}</span>
      <span className="icons">{/* signal · battery · etc */}</span>
    </div>
  );
}

function PageHeader({ title, subtitle, action }) {
  return (
    <header className="app-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <div className="subtitle">{subtitle}</div>}
      </div>
      {action}
    </header>
  );
}

function HeroMetric({ label, value, suffix = "%", deltaText, trendUp = true }) {
  return (
    <section className="hero-metric animate-slide-up">
      <div className="label">{label}</div>
      <div className="value">
        {value}
        <span style={{ fontSize: 24, opacity: 0.85 }}>{suffix}</span>
      </div>
      {deltaText && (
        <div className="delta">
          <TrendIcon up={trendUp} /> {deltaText}
        </div>
      )}
    </section>
  );
}

function TrendIcon({ up }) {
  return up ? (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" aria-hidden="true">
      <polyline points="6 15 12 9 18 15" />
    </svg>
  ) : (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" aria-hidden="true">
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

function ChildCard({ initials, name, classLabel, schoolLabel, onPress }) {
  return (
    <div className="child-card" role="link" tabIndex={0} onClick={onPress} onKeyDown={(e) => e.key === "Enter" && onPress?.()}>
      <div className="avatar" aria-hidden="true">{initials}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="name">Con {name}</div>
        <div className="meta">{classLabel} · {schoolLabel}</div>
      </div>
      <ChevronRight />
    </div>
  );
}

function ChevronRight() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <polyline points="9 18 15 12 9 6" />
    </svg>
  );
}

function ActivityRow({ icon, tone = "blue", title, sub, trail }) {
  return (
    <div className="activity-row">
      <span className={`activity-icon ${tone}`} aria-hidden="true">{icon}</span>
      <div className="activity-body">
        <div className="activity-title">{title}</div>
        {sub && <div className="activity-sub">{sub}</div>}
      </div>
      {trail && <span className="activity-trail">{trail}</span>}
    </div>
  );
}

function GradePill({ score }) {
  const cls =
    score >= 9.0 ? "xuat-sac" :
    score >= 8.0 ? "gioi" :
    score >= 6.5 ? "kha" :
    score >= 5.0 ? "tb" : "yeu";
  return <span className={`grade-pill ${cls}`}>{score.toFixed(1)}</span>;
}

function HonorLabel({ score }) {
  if (score >= 9.0) return "Xuất sắc";
  if (score >= 8.0) return "Giỏi";
  if (score >= 6.5) return "Khá";
  if (score >= 5.0) return "Trung bình";
  return "Yếu";
}

// ============================================================
// BOTTOM TAB NAV — 4 tabs, fixed, ≥44×44 targets
// ============================================================
function BottomTabs({ active }) {
  const tabs = [
    { key: "home", label: "Trang chủ", href: "/parent", icon: HomeIcon },
    { key: "grades", label: "Học bạ", href: "/parent/grades", icon: BookIcon },
    { key: "billing", label: "Học phí", href: "/parent/billing", icon: WalletIcon },
    { key: "settings", label: "Cài đặt", href: "/parent/settings", icon: GearIcon },
  ];
  return (
    <nav className="bottom-tabs" aria-label="Điều hướng chính">
      {tabs.map((t) => (
        <a
          key={t.key}
          href={t.href}
          className="tab-btn"
          aria-current={active === t.key ? "page" : undefined}
        >
          <span className="tab-icon" aria-hidden="true"><t.icon /></span>
          {t.label}
        </a>
      ))}
    </nav>
  );
}

function HomeIcon() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>;
}
function BookIcon() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>;
}
function WalletIcon() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>;
}
function GearIcon() {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"/></svg>;
}

// ============================================================
// PWA — install prompt + service worker registration
// ============================================================
function usePWAInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState(null);
  const [isInstalled, setIsInstalled] = useState(false);

  useEffect(() => {
    const handler = (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };
    window.addEventListener("beforeinstallprompt", handler);
    window.addEventListener("appinstalled", () => setIsInstalled(true));

    // Register service worker
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch((err) =>
        console.warn("SW registration failed", err),
      );
    }

    return () => {
      window.removeEventListener("beforeinstallprompt", handler);
    };
  }, []);

  async function promptInstall() {
    if (!deferredPrompt) return null;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    setDeferredPrompt(null);
    return outcome; // "accepted" | "dismissed"
  }

  return { canInstall: !!deferredPrompt && !isInstalled, promptInstall, isInstalled };
}

// ============================================================
// PUSH SUBSCRIPTION — Zalo OA primary, Web Push fallback
// ============================================================
function useNotificationChannels() {
  const [zaloLinked, setZaloLinked] = useState(false);
  const [webPushPermission, setWebPushPermission] = useState(
    typeof Notification !== "undefined" ? Notification.permission : "default",
  );

  async function requestWebPush() {
    if (typeof Notification === "undefined") return "unsupported";
    const permission = await Notification.requestPermission();
    setWebPushPermission(permission);
    if (permission === "granted") {
      // Subscribe to push manager — Track 2 wires VAPID keys
      const registration = await navigator.serviceWorker.ready;
      // const sub = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: VAPID_KEY })
      // POST sub to /api/v1/parent/push-subscriptions
    }
    return permission;
  }

  async function linkZaloOA(phone) {
    // Track 2: POST /api/v1/parent/zalo-oa/link with phone, redirect to Zalo OA follow URL
    setZaloLinked(true);
  }

  return { zaloLinked, webPushPermission, requestWebPush, linkZaloOA };
}

// ============================================================
// PAGES — composed from primitives
// ============================================================

export function ParentHome({ child, attendanceRate, gpa, recentActivity }) {
  return (
    <div className="theme-kiteclass-parent">
      <div className="app-shell" role="application" aria-label="Trang chủ phụ huynh">
        <StatusBar />
        <PageHeader title="Chào chị Hương 👋" subtitle="Tháng 4 — Năm học 2025–2026" action={<NotifBell unread={3} />} />

        <HeroMetric label="Tỷ lệ đi học của con tháng này" value={attendanceRate} deltaText="Tăng 4% so với tháng trước" />

        <ChildCard initials="LT" name={child.name} classLabel={child.classLabel} schoolLabel={child.school} />

        <section style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, margin: 16 }}>
          <StatCard label="Điểm trung bình" value={gpa.toFixed(1)} pillTone="success" pillText={`Học lực ${HonorLabel({ score: gpa })}`} />
          <StatCard label="Học phí" value="Đã đóng" pillTone="info" pillText="Kỳ I · 2025-2026" />
        </section>

        <SectionHeading title="Hoạt động gần đây" linkText="Xem tất cả" linkHref="/parent/activity" />
        <div className="card" style={{ margin: "0 16px", padding: "8px 16px" }}>
          {recentActivity.map((a) => (
            <ActivityRow key={a.id} icon={a.icon} tone={a.tone} title={a.title} sub={a.sub} trail={a.trail} />
          ))}
        </div>

        <BottomTabs active="home" />
      </div>
    </div>
  );
}

function NotifBell({ unread }) {
  return (
    <button className="icon-btn" aria-label={`Thông báo (${unread} chưa đọc)`}>
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      {unread > 0 && <span style={{ position: "absolute", top: 8, right: 8, width: 8, height: 8, background: "hsl(var(--destructive))", borderRadius: 999 }} aria-hidden="true" />}
    </button>
  );
}

function StatCard({ label, value, pillTone, pillText }) {
  return (
    <div className="card" style={{ padding: 14 }}>
      <div style={{ fontSize: "var(--text-xs)", color: "hsl(var(--muted-foreground))", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em" }}>{label}</div>
      <div style={{ fontSize: 28, fontWeight: 800, marginTop: 4, letterSpacing: "-0.02em" }}>{value}</div>
      <span className={`pill ${pillTone}`} style={{ marginTop: 8 }}>{pillText}</span>
    </div>
  );
}

function SectionHeading({ title, linkText, linkHref }) {
  return (
    <div className="section-heading">
      <h2>{title}</h2>
      {linkText && <a href={linkHref}>{linkText}</a>}
    </div>
  );
}

// ============================================================
// PAGE: Grades overview
// ============================================================
export function ParentGradesOverview({ semester = "I", subjects, gpa, honor, conduct, gvcnComment }) {
  return (
    <div className="theme-kiteclass-parent">
      <div className="app-shell">
        <StatusBar />
        <PageHeader title="Học bạ con" subtitle="Lê Minh Tuấn · Lớp 10A2" />

        <SemesterTabs active={semester} />

        <HonorCard gpa={gpa} honor={honor} conduct={conduct} />

        <SectionHeading title="Điểm môn học" linkText={`${subjects.length} môn`} />
        <div className="card" style={{ margin: "0 16px", padding: 0, overflow: "hidden" }}>
          {subjects.map((s) => (
            <a key={s.id} href={`/parent/grades/${s.slug}`} className="activity-row" style={{ padding: "14px 16px", textDecoration: "none", color: "inherit" }}>
              <span className={`activity-icon ${s.tone}`} aria-hidden="true">{s.icon}</span>
              <div className="activity-body">
                <div className="activity-title">{s.name}</div>
                <div className="activity-sub">{s.teacher} · {s.scoreCount} cột điểm</div>
              </div>
              <GradePill score={s.score} />
            </a>
          ))}
        </div>

        {gvcnComment && <GVCNCommentCard comment={gvcnComment} />}

        <BottomTabs active="grades" />
      </div>
    </div>
  );
}

function SemesterTabs({ active }) {
  return (
    <div className="tabs" role="tablist" aria-label="Chọn học kỳ">
      <button className="tab" role="tab" aria-selected={active === "I"}>Học kỳ I</button>
      <button className="tab" role="tab" aria-selected={active === "II"}>Học kỳ II</button>
      <button className="tab" role="tab" aria-selected={active === "year"}>Cả năm</button>
    </div>
  );
}

function HonorCard({ gpa, honor, conduct, classRank, gradeRank }) {
  return (
    <section style={{ margin: "0 16px 12px 16px", padding: 18, borderRadius: 20, background: "linear-gradient(135deg, hsl(var(--surface-success)), hsl(var(--surface-soft)))", border: "1px solid hsl(var(--border))" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
        <div style={{ width: 56, height: 56, borderRadius: 16, background: "hsl(var(--primary))", color: "white", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 800, fontSize: 24 }}>{gpa.toFixed(1)}</div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: "var(--text-sm)", color: "hsl(var(--muted-foreground))" }}>Học lực Học kỳ I</div>
          <div style={{ fontSize: "var(--text-xl)", fontWeight: 800, letterSpacing: "-0.02em" }}>{honor}</div>
        </div>
        <span className="pill success">Hạnh kiểm: {conduct}</span>
      </div>
    </section>
  );
}

function GVCNCommentCard({ comment }) {
  return (
    <div className="card" style={{ margin: "0 16px 16px 16px" }}>
      <div style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
        <div className="avatar" style={{ width: 44, height: 44 }}>{comment.teacherInitials}</div>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 600, fontSize: "var(--text-sm)" }}>{comment.teacherName}</div>
          <div style={{ fontSize: "var(--text-xs)", color: "hsl(var(--muted-foreground))" }}>Giáo viên chủ nhiệm · {comment.date}</div>
        </div>
      </div>
      <p style={{ margin: "12px 0 0 0", fontSize: "var(--text-sm)", lineHeight: 1.6 }}>{comment.text}</p>
    </div>
  );
}

// ============================================================
// PAGE: Billing pay (payment method selector)
// ============================================================
export function ParentBillingPay({ invoice }) {
  const [method, setMethod] = useState("momo");

  const methods = [
    { id: "momo",  name: "Ví MoMo",                desc: "Quét mã QR bằng app MoMo · Phổ biến nhất",       logoCls: "momo",  logoText: "M"  },
    { id: "vnpay", name: "VNPay",                  desc: "Thanh toán qua app ngân hàng",                    logoCls: "vnpay", logoText: "VN" },
    { id: "zalo",  name: "ZaloPay",                desc: "Quét QR bằng Zalo · Liên kết với tài khoản Zalo",logoCls: "zalo",  logoText: "Z"  },
    { id: "bank",  name: "Chuyển khoản ngân hàng", desc: "Chuyển khoản thủ công với mã giao dịch",          logoCls: "bank",  logoText: "🏦" },
    { id: "cash",  name: "Tiền mặt tại trường",    desc: "Đến văn phòng trường đóng trực tiếp",             logoCls: "cash",  logoText: "₫"  },
  ];

  function vnCurrency(n) {
    return n.toLocaleString("vi-VN", { useGrouping: true }).replace(/,/g, ".") + "đ";
  }

  return (
    <div className="theme-kiteclass-parent">
      <div className="app-shell">
        <StatusBar />
        <PageHeader
          title="Đóng học phí"
          subtitle="Chọn phương thức thanh toán"
          action={
            <a href="/parent/billing" className="icon-btn" aria-label="Quay lại">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
            </a>
          }
        />

        {/* Amount summary */}
        <section style={{ margin: "0 16px 16px 16px", padding: 20, borderRadius: 20, background: "hsl(var(--surface-soft))", border: "1px solid hsl(var(--primary) / 0.2)" }}>
          <div style={{ fontSize: "var(--text-xs)", color: "hsl(var(--primary))", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.05em" }}>Bạn sẽ thanh toán</div>
          <div style={{ fontSize: 36, fontWeight: 800, letterSpacing: "-0.03em", marginTop: 4 }}>{vnCurrency(invoice.amount)}</div>
        </section>

        {/* Method selector */}
        <SectionHeading title="Phương thức thanh toán" />
        <div style={{ padding: "0 16px" }}>
          {methods.map((m) => (
            <label key={m.id} className={`pay-method ${method === m.id ? "selected" : ""}`} aria-pressed={method === m.id}>
              <input type="radio" name="method" checked={method === m.id} onChange={() => setMethod(m.id)} style={{ display: "none" }} />
              <span className={`logo ${m.logoCls}`}>{m.logoText}</span>
              <div className="info">
                <div className="name">{m.name}</div>
                <div className="desc">{m.desc}</div>
              </div>
            </label>
          ))}
        </div>

        <button className="btn-primary" style={{ margin: "0 16px 24px 16px", width: "calc(100% - 32px)" }}>Tiếp tục</button>
        <BottomTabs active="billing" />
      </div>
    </div>
  );
}

// ============================================================
// PAGE: Settings — Zalo OA + Web Push toggles
// ============================================================
export function ParentSettings() {
  const { zaloLinked, webPushPermission, requestWebPush, linkZaloOA } = useNotificationChannels();
  const { canInstall, promptInstall } = usePWAInstallPrompt();

  return (
    <div className="theme-kiteclass-parent">
      <div className="app-shell">
        <StatusBar />
        <PageHeader title="Cài đặt" subtitle="Chị Trần Thị Hương · 0901 234 567" />

        {/* Zalo OA — primary recommended */}
        <section style={{ margin: "0 16px 12px 16px", padding: 16, borderRadius: 16, background: "linear-gradient(135deg, hsl(199 89% 96%), hsl(217 91% 97%))", border: "1.5px solid hsl(199 89% 60%)" }}>
          <ZaloOARow linked={zaloLinked} onLink={() => linkZaloOA("0901234567")} />
        </section>

        {/* Web Push toggle */}
        <SettingRow
          title="Web Push (trình duyệt)"
          hint="Thông báo nhanh khi Zalo OA mất kết nối"
          toggleOn={webPushPermission === "granted"}
          onToggle={requestWebPush}
        />

        {/* PWA install row */}
        {canInstall && (
          <SettingRow
            title="Cài đặt vào màn hình chính"
            hint="Mở 1 chạm như app · Không tốn dung lượng"
            actionLabel="Cài đặt"
            onAction={promptInstall}
          />
        )}

        <BottomTabs active="settings" />
      </div>
    </div>
  );
}

function ZaloOARow({ linked, onLink }) {
  return (
    <div style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
      <div style={{ width: 44, height: 44, borderRadius: 10, background: "hsl(199 89% 48%)", color: "white", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, fontWeight: 800, fontSize: 14 }}>Zalo</div>
      <div style={{ flex: 1 }}>
        <div style={{ fontWeight: 700, fontSize: "var(--text-base)" }}>Thông báo Zalo OA</div>
        <div style={{ fontSize: "var(--text-xs)", color: "hsl(var(--muted-foreground))", marginTop: 4 }}>
          Nhận điểm số, điểm danh, học phí qua Zalo OA của trường
        </div>
        {!linked && (
          <button className="btn-primary" style={{ marginTop: 10, height: 40 }} onClick={onLink}>
            Liên kết với Zalo OA
          </button>
        )}
      </div>
    </div>
  );
}

function SettingRow({ title, hint, toggleOn, onToggle, actionLabel, onAction }) {
  return (
    <div className="settings-row">
      <div className="label">
        <div className="title">{title}</div>
        {hint && <div className="hint">{hint}</div>}
      </div>
      {actionLabel ? (
        <button className="btn-ghost" onClick={onAction}>{actionLabel}</button>
      ) : (
        <span
          className={`toggle ${toggleOn ? "on" : ""}`}
          role="switch"
          aria-checked={toggleOn}
          onClick={onToggle}
        />
      )}
    </div>
  );
}

// ============================================================
// MOCK DATA (VN-only per dossier/06-quality-bar.md §5)
// ============================================================
export const MOCK_PARENT = {
  parent: { id: "p-1", name: "Trần Thị Hương", phone: "0901234567", email: "tranthihuong@gmail.com" },
  child: {
    id: "s-1",
    initials: "LT",
    name: "Lê Minh Tuấn",
    classLabel: "Lớp 10A2",
    school: "Trường THCS-THPT EduPlus",
  },
  attendanceRate: 92,
  gpa: 8.4,
  recentActivity: [
    { id: 1, icon: "✓", tone: "green", title: "Có điểm Toán mới: 8.5", sub: "Bài kiểm tra giữa kỳ 1 · Cô Trần Thị Hương", trail: "2 giờ trước" },
    { id: 2, icon: "🕐", tone: "amber", title: "Bài tập Văn sắp hết hạn", sub: "Còn 2 ngày · Hạn 18/04/2026", trail: "Hôm nay" },
  ],
};

// ============================================================
// ROOT — for Track 2 Next.js port, this is layout.tsx + page.tsx merge
// ============================================================
export default function App() {
  return <ParentHome {...MOCK_PARENT} />;
}
