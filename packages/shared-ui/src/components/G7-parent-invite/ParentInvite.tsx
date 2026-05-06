'use client';

/**
 * G7 ParentInvite — invite a parent to follow their child's learning.
 *
 * Sender variant only (Phase 2 redeem-page surface tracked in GAP-273 follow-up).
 * Two channels per `dossier/02-vietnamese-ux-musts.md` §5: ZALO_OA (default,
 * reaches ~95% of VN parents) and EMAIL (fallback). Backend issues a token
 * that we surface for copy-out-of-band fallback if email/Zalo delivery fails.
 *
 * State machine:
 *   idle ──[click send + valid email]──> sending
 *   sending ──[onSend resolves]──> sent
 *   sending ──[onSend rejects]──> error
 *   error ──[edit + click send]──> sending
 *
 * Vietnamese-first per CLAUDE.md + spec.md §"Vietnamese UX considerations".
 *
 * Accessibility (WCAG AA):
 *  - Email input <label htmlFor> — explicit label, not placeholder-only.
 *  - Channel toggle role="radiogroup" with two role="radio" descendants.
 *  - Error message rendered with role="alert" + aria-live="assertive".
 *  - Sending indicator role="status" + aria-live="polite".
 *  - Token block <code> with adjacent labelled copy button (sr-only).
 *  - Color contrast ≥4.5:1 documented via Tailwind theme tokens.
 *
 * Zalo OA visual cue: Zalo blue (`#0068FF`) per Zalo brand guidelines is used
 * only on the Zalo-specific affordance (radio dot + small badge), keeping the
 * primary CTA on the project theme to avoid double-branding.
 */

import type React from 'react';
import { useCallback, useId, useMemo, useState } from 'react';
import type {
  EmailValidationResult,
  InviteChannel,
  InviteState,
  ParentInviteProps,
} from './types';

const COPY_VI = {
  title: 'Mời phụ huynh',
  subtitleWithChild: (childName: string) =>
    `Bạn được mời theo dõi học tập của con ${childName}. Gửi lời mời để phụ huynh nhận thông báo.`,
  subtitleGeneric: 'Gửi lời mời để phụ huynh theo dõi học tập của con.',
  emailLabel: 'Email phụ huynh',
  emailPlaceholder: 'phuhuynh@gmail.com',
  emailInvalid: 'Email không hợp lệ. Vui lòng kiểm tra lại.',
  channelLegend: 'Chọn kênh gửi lời mời',
  channelEmail: 'Gửi qua email',
  channelZalo: 'Gửi qua Zalo OA',
  channelZaloHint: '~95% phụ huynh VN dùng Zalo',
  send: 'Gửi lời mời',
  sending: 'Đang gửi lời mời…',
  sentTitle: 'Đã gửi lời mời',
  sentBodyEmail: 'Phụ huynh sẽ nhận email trong vài phút. Lời mời có hiệu lực 24 giờ.',
  sentBodyZalo:
    'Phụ huynh sẽ nhận thẻ Zalo OA trong vài phút. Lời mời có hiệu lực 24 giờ.',
  tokenLabel: 'Mã mời (chia sẻ thủ công nếu cần):',
  copy: 'Sao chép mã mời',
  copied: 'Đã sao chép',
  retry: 'Thử lại',
};

// RFC-5322 is too permissive for our UX; this regex is the project default
// per task brief: "/^[^\s@]+@[^\s@]+\.[^\s@]+$/".
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateEmail(value: string): { ok: true } | { ok: false; message: string } {
  if (!value || value.trim().length === 0) {
    return { ok: false, message: COPY_VI.emailInvalid };
  }
  if (!EMAIL_REGEX.test(value.trim())) {
    return { ok: false, message: COPY_VI.emailInvalid };
  }
  return { ok: true };
}

export function ParentInvite(props: ParentInviteProps): React.JSX.Element {
  const {
    defaultEmail = '',
    defaultChannel = 'ZALO_OA',
    onSend,
    childName,
  } = props;
  // storageKey reserved per types.ts; not currently used.
  void props.storageKey;

  const [email, setEmail] = useState<string>(defaultEmail);
  const [channel, setChannel] = useState<InviteChannel>(defaultChannel);
  const [state, setState] = useState<InviteState>('idle');
  const [validation, setValidation] = useState<EmailValidationResult>(null);
  const [issuedToken, setIssuedToken] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const titleId = useId();
  const emailLabelId = useId();
  const emailErrorId = useId();
  const channelLegendId = useId();

  // Email is "valid for send" when it currently passes the regex. We don't
  // require the user to blur first — but we also don't show an error message
  // until they have at least typed something + blurred.
  const emailIsValid = useMemo(() => validateEmail(email).ok, [email]);
  const sendDisabled = !emailIsValid || state === 'sending';

  const handleEmailBlur = useCallback(() => {
    if (email.length === 0) {
      setValidation(null);
      return;
    }
    setValidation(validateEmail(email));
  }, [email]);

  const handleSend = useCallback(async () => {
    if (!emailIsValid || state === 'sending') return;
    setState('sending');
    setErrorMessage(null);
    try {
      const { token } = await onSend({ email: email.trim(), channel });
      setIssuedToken(token);
      setState('sent');
    } catch (err) {
      const msg =
        err instanceof Error && err.message
          ? err.message
          : 'Không thể gửi lời mời. Vui lòng thử lại.';
      setErrorMessage(msg);
      setState('error');
    }
  }, [channel, email, emailIsValid, onSend, state]);

  const handleCopy = useCallback(async () => {
    if (!issuedToken) return;
    if (
      typeof navigator !== 'undefined' &&
      navigator.clipboard &&
      typeof navigator.clipboard.writeText === 'function'
    ) {
      try {
        await navigator.clipboard.writeText(issuedToken);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
        return;
      } catch {
        // Fall through to manual-selection fallback.
      }
    }
    // Fallback: select the token's <code> contents so user can press Ctrl+C.
    const node = document.getElementById(emailErrorId + '-token');
    if (node && typeof window !== 'undefined') {
      const sel = window.getSelection();
      const range = document.createRange();
      range.selectNodeContents(node);
      sel?.removeAllRanges();
      sel?.addRange(range);
    }
  }, [emailErrorId, issuedToken]);

  const subtitle = childName
    ? COPY_VI.subtitleWithChild(childName)
    : COPY_VI.subtitleGeneric;

  return (
    <section
      aria-labelledby={titleId}
      data-testid="parent-invite"
      className="flex flex-col gap-4 rounded-md border bg-background p-4 sm:p-6"
    >
      <header className="flex flex-col gap-1">
        <h2 id={titleId} className="text-lg font-semibold">
          {COPY_VI.title}
        </h2>
        <p className="text-sm leading-relaxed text-muted-foreground">{subtitle}</p>
      </header>

      <div className="flex flex-col gap-2">
        <label
          id={emailLabelId}
          htmlFor="parent-invite-email-input"
          className="text-sm font-medium"
        >
          {COPY_VI.emailLabel}
        </label>
        <input
          id="parent-invite-email-input"
          data-testid="parent-invite-email"
          type="email"
          inputMode="email"
          autoComplete="email"
          placeholder={COPY_VI.emailPlaceholder}
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            // Clear validation message as user types so they don't see the
            // error linger after correcting the input.
            if (validation && validation.ok === false) {
              setValidation(null);
            }
            // If we were in error state from a previous send, reset to idle
            // so the next click re-enters sending cleanly.
            if (state === 'error') {
              setState('idle');
              setErrorMessage(null);
            }
          }}
          onBlur={handleEmailBlur}
          aria-invalid={validation?.ok === false ? true : undefined}
          aria-describedby={validation?.ok === false ? emailErrorId : undefined}
          className="rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />
        {validation?.ok === false && (
          <p
            id={emailErrorId}
            data-testid="parent-invite-email-error"
            className="text-sm text-destructive"
          >
            {validation.message}
          </p>
        )}
      </div>

      <fieldset
        role="radiogroup"
        aria-labelledby={channelLegendId}
        data-testid="parent-invite-channel-radiogroup"
        className="flex flex-col gap-2 rounded-md border bg-muted/30 p-3"
      >
        <legend id={channelLegendId} className="px-1 text-sm font-medium">
          {COPY_VI.channelLegend}
        </legend>

        <ChannelRadio
          name="parent-invite-channel"
          value="ZALO_OA"
          label={COPY_VI.channelZalo}
          hint={COPY_VI.channelZaloHint}
          checked={channel === 'ZALO_OA'}
          onSelect={() => setChannel('ZALO_OA')}
          testid="parent-invite-channel-zalo"
          accentClassName="text-[#0068FF]"
          icon={<ZaloIcon />}
        />

        <ChannelRadio
          name="parent-invite-channel"
          value="EMAIL"
          label={COPY_VI.channelEmail}
          checked={channel === 'EMAIL'}
          onSelect={() => setChannel('EMAIL')}
          testid="parent-invite-channel-email"
          accentClassName="text-foreground"
          icon={<MailIcon />}
        />
      </fieldset>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-end">
        <button
          type="button"
          data-testid="parent-invite-send-btn"
          disabled={sendDisabled}
          onClick={handleSend}
          className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {state === 'sending' ? COPY_VI.sending : COPY_VI.send}
        </button>
      </div>

      {state === 'sending' && (
        <div
          role="status"
          aria-live="polite"
          data-testid="parent-invite-sending"
          className="text-sm text-muted-foreground"
        >
          {COPY_VI.sending}
        </div>
      )}

      {state === 'sent' && issuedToken && (
        <SuccessPanel
          channel={channel}
          token={issuedToken}
          tokenNodeId={emailErrorId + '-token'}
          copied={copied}
          onCopy={handleCopy}
        />
      )}

      {state === 'error' && errorMessage && (
        <div
          role="alert"
          aria-live="assertive"
          data-testid="parent-invite-error"
          className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive"
        >
          {errorMessage}
        </div>
      )}
    </section>
  );
}

type ChannelRadioProps = {
  name: string;
  value: InviteChannel;
  label: string;
  hint?: string;
  checked: boolean;
  onSelect: () => void;
  testid: string;
  accentClassName: string;
  icon: React.ReactNode;
};

function ChannelRadio({
  name,
  value,
  label,
  hint,
  checked,
  onSelect,
  testid,
  accentClassName,
  icon,
}: ChannelRadioProps): React.JSX.Element {
  const id = `${name}-${value.toLowerCase()}`;
  return (
    <label
      htmlFor={id}
      className="flex cursor-pointer items-center gap-3 rounded-md p-2 hover:bg-muted/40"
    >
      <input
        id={id}
        data-testid={testid}
        type="radio"
        name={name}
        value={value}
        checked={checked}
        onChange={onSelect}
        className="h-4 w-4 cursor-pointer accent-primary"
      />
      <span className={`flex items-center gap-2 ${accentClassName}`} aria-hidden="true">
        {icon}
      </span>
      <span className="flex flex-col">
        <span className="text-sm font-medium">{label}</span>
        {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
      </span>
    </label>
  );
}

function SuccessPanel({
  channel,
  token,
  tokenNodeId,
  copied,
  onCopy,
}: {
  channel: InviteChannel;
  token: string;
  tokenNodeId: string;
  copied: boolean;
  onCopy: () => void;
}): React.JSX.Element {
  return (
    <div
      role="status"
      aria-live="polite"
      data-testid="parent-invite-success"
      className="flex flex-col gap-2 rounded-md border border-primary/30 bg-primary/5 p-3"
    >
      <p className="text-sm font-medium">{COPY_VI.sentTitle}</p>
      <p className="text-sm text-muted-foreground">
        {channel === 'EMAIL' ? COPY_VI.sentBodyEmail : COPY_VI.sentBodyZalo}
      </p>
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground">{COPY_VI.tokenLabel}</span>
        <div className="flex items-center gap-2">
          <code
            id={tokenNodeId}
            data-testid="parent-invite-token"
            className="flex-1 truncate rounded bg-muted px-2 py-1 font-mono text-xs"
          >
            {token}
          </code>
          <button
            type="button"
            data-testid="parent-invite-copy-btn"
            onClick={onCopy}
            aria-label={COPY_VI.copy}
            className="inline-flex items-center justify-center rounded-md border bg-background px-3 py-1 text-xs font-medium text-foreground hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            {copied ? COPY_VI.copied : COPY_VI.copy}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ----------------------------- inline icons ------------------------------ */
/* Tiny inline SVG icons — keeping the component zero-dep beyond peer React. */

function ZaloIcon(): React.JSX.Element {
  // Stylised "Z" badge in Zalo brand blue. Decorative — siblings provide label.
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 20 20"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect x="0.5" y="0.5" width="19" height="19" rx="4" stroke="currentColor" fill="none" />
      <path
        d="M5.5 6.5h7L6 13.5h7"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
    </svg>
  );
}

function MailIcon(): React.JSX.Element {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect
        x="2.5"
        y="4.5"
        width="15"
        height="11"
        rx="1.5"
        stroke="currentColor"
        strokeWidth="1.5"
      />
      <path
        d="M3 5l7 5 7-5"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export default ParentInvite;
