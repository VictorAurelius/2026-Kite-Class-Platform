/**
 * G7 ParentInvite types.
 *
 * Two complementary surfaces in one component (configurable via `mode`):
 *  - `sender` — admin invites parent by email or Zalo OA channel
 *  - `redeem` — parent lands on invite link, sees invite info, creates account or signs in
 *
 * Per spec.md (`ui_kits/components/G7-parent-invite/spec.md`).
 */

/** Channel for sending the invite. */
export type InviteChannel = 'EMAIL' | 'ZALO_OA';

/** Lifecycle of a single invite send action (sender side). */
export type InviteState = 'idle' | 'sending' | 'sent' | 'error';

/**
 * Validation outcome for the email field. `null` means "no validation run yet"
 * (e.g. empty input, idle state). Keeps consumer code branching simple.
 */
export type EmailValidationResult =
  | { ok: true }
  | { ok: false; message: string }
  | null;

/**
 * Props for the sender-admin variant.
 *
 * Token shown in success state is opaque to this component — caller passes
 * back the issued token (e.g. shortened JWT prefix) so the admin can copy it
 * out-of-band if email delivery fails.
 */
export interface ParentInviteProps {
  /**
   * Pre-filled email value. Empty string by default.
   */
  defaultEmail?: string;

  /**
   * Initial channel selection. Per `dossier/02-vietnamese-ux-musts.md` §5
   * Zalo OA reaches ~95% of VN parents; default to ZALO_OA.
   */
  defaultChannel?: InviteChannel;

  /**
   * Issue an invite. Resolves with the issued token (used for copy-to-clipboard
   * fallback). Rejects with an Error whose `.message` is shown in error state.
   */
  onSend: (input: { email: string; channel: InviteChannel }) => Promise<{ token: string }>;

  /**
   * Optional: child name used in the in-page copy
   * ("Bạn được mời theo dõi học tập của con [tên]"). When omitted the copy
   * uses a generic phrasing.
   */
  childName?: string;

  /**
   * Optional storage / namespacing key (parity with ConsentBanner pattern).
   * Reserved — not currently used by the component but kept in the type for
   * future per-tenant scoping.
   */
  storageKey?: string;
}
