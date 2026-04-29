# G7 — Parent Invite Flow

**Component gap:** G7 per `dossier/04-component-gaps.md` §G7
**Flow ref:** `dossier/05-business-flows.md` Flow #6 (Parent views child's grades & attendance)
**Used by screens:** KC `/parent-invite/[token]` (redemption page), admin "invite parent" modal
**Persona:** Pa. Parent (redeemer) + Admin (sender)

---

## Purpose

Two complementary surfaces:
1. **Sender (admin)** — invite parent by email; track pending invites (resend / cancel)
2. **Redeemer (parent)** — click invite link → see child name + center info → create account or sign in → linked

Plus alternative `zalo-share.html` — Zalo OA card with deep link, used when admin chooses "Gửi qua Zalo" instead of email (per `dossier/02` §5: Zalo OA is primary push channel for parents).

---

## Props (TypeScript-ish)

### Sender admin UI

```ts
interface ParentInviteSenderProps {
  pendingInvites: Array<{
    id: string;
    parentEmail: string;        // "nguyen.an@gmail.com"
    childName: string;          // "Lê Minh Tuấn"
    childClass: string;         // "Lớp 10A2"
    sentAt: Date;
    expiresAt: Date;            // sentAt + 24h
    status: "pending" | "redeemed" | "expired";
  }>;
  onSendInvite: (email: string, channel: "email" | "zalo") => Promise<void>;
  onResend: (id: string) => void;
  onCancel: (id: string) => void;
}
```

### Redeemer page

```ts
interface ParentInviteRedeemProps {
  token: string;
  state: "loading" | "default" | "expired" | "already-redeemed" | "success";
  invite: {
    childName: string;
    childClass: string;
    tenantName: string;          // "Trung tâm Toán Master"
    senderName: string;          // "Cô Nguyễn Thị Lan (GVCN)"
    expiresAt: Date;
  };
  onCreateAccount: (data: SignupForm) => Promise<void>;
  onSignIn: () => void;
}
```

---

## States

| File | State | UI |
|------|-------|-----|
| `default.html` | Sender admin UI | Email input + send button + pending invites list (3 items) + status pills |
| `redemption-link.html` | Redeemer landing (default) | Welcome card with child name + create account form |
| `expired.html` | Token >24h | "Lời mời đã hết hạn (24h). Vui lòng yêu cầu lời mời mới." |
| `already-redeemed.html` | Email already has account | "Bạn đã có tài khoản với email này." + sign-in CTA |
| `success.html` | Account created + linked | Welcome dashboard preview + "Vào trang chủ phụ huynh" CTA |
| `zalo-share.html` | Alt: Zalo OA card preview | OA card layout (320×100): brand logo + headline + body + CTA + deep link URL |

---

## Vietnamese UX considerations

Per `dossier/02-vietnamese-ux-musts.md` + Flow #6:

- Welcome message: `Bạn được mời theo dõi học tập của con [tên học sinh]` (per `dossier/04` §G7)
- Sender title: `Mời phụ huynh theo dõi`
- Email placeholder: `phuhuynh@gmail.com` (real-feel, not test@example.com)
- Pending invites list shows:
  - Parent email (left)
  - Child name + class (middle)
  - Status pill (`Đã gửi` / `Đã chấp nhận` / `Hết hạn`)
  - Time relative: `2 giờ trước`, `1 ngày trước`
- 24h countdown displayed: `Lời mời còn hiệu lực: 23 giờ 14 phút`
- Channel toggle: `Email` / `Zalo OA` — Zalo first because §5 of `dossier/02` says Zalo OA reaches ~95% of VN parents
- After successful account creation: `Chào mừng bạn đến với [Trung tâm Toán Master]` + child summary (today's class, next assignment, fee balance)
- Token format displayed: `eyJhbGciOiJIUzI1NiIsI...` (truncated mono with copy button)
- Use `bạn` informal you throughout (NOT `quý phụ huynh` formal)

---

## Zalo OA card spec

Per `dossier/02-vietnamese-ux-musts.md` §5:

- Card dimensions: 320×100 px (Zalo OA card spec)
- Brand logo (left, 60×60)
- Headline 1 line max 20 chars: `Lời mời theo dõi học tập`
- Body 2 lines max 60 chars: `Trung tâm Toán Master mời bạn theo dõi học tập của con Lê Minh Tuấn (Lớp 10A2).`
- CTA button right: `Chấp nhận`
- Zalo deep-link supported: `zalo://oa/...`

---

## Accessibility

- Form inputs labeled: `Email phụ huynh` with `<label htmlFor>`
- Channel toggle: `role="radiogroup"`
- Pending invites list: `<ul>` with each item having visible status badge (icon + text, not color-only)
- Countdown: `aria-live="polite"` updates per minute
- Long token displayed: `<code>` with copy button; sr-only `Sao chép token`
- Success state has `role="status"` + `aria-live="polite"`

---

## Component reuse

- shadcn `Input` + `Label` + `Button` for invite form
- shadcn `Badge` for status pills (`pending`, `success`, `destructive` variants)
- shadcn `Card` for pending invite items
- lucide icons: `Mail`, `MessageCircle` (Zalo), `Clock`, `CheckCircle2`, `XCircle`, `Send`, `RefreshCw`, `UserPlus`, `Heart`
- date-fns `formatDistanceToNow(date, { locale: vi, addSuffix: true })` → `2 giờ trước`
