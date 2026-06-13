/**
 * Video embed URL resolver for the student lesson player (LMS Increment B).
 *
 * The platform does NOT self-host video — lessons reference external YouTube /
 * Vimeo URLs (per wave-lms-fe-1 Bucket C scope). This maps a raw watch/share URL
 * into the provider's privacy-friendly embed URL so it can render in an iframe.
 *
 * Returns `null` for unrecognized providers — the caller renders a plain link
 * fallback instead of an iframe (never trust an arbitrary URL in an iframe src).
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave rbac-lms-student-fe — Increment B)
 */

export type VideoProvider = 'youtube' | 'vimeo';

export interface VideoEmbed {
  provider: VideoProvider;
  embedUrl: string;
}

/** Extract the 11-char YouTube video id from any common URL shape. */
function youtubeId(url: URL): string | null {
  const host = url.hostname.replace(/^www\./, '');
  if (host === 'youtu.be') {
    const id = url.pathname.slice(1).split('/')[0];
    return id || null;
  }
  if (host === 'youtube.com' || host === 'm.youtube.com' || host === 'youtube-nocookie.com') {
    // /watch?v=ID
    const v = url.searchParams.get('v');
    if (v) return v;
    // /embed/ID or /shorts/ID or /v/ID
    const m = url.pathname.match(/^\/(?:embed|shorts|v)\/([^/?]+)/);
    if (m) return m[1] ?? null;
  }
  return null;
}

/** Extract the numeric Vimeo video id. */
function vimeoId(url: URL): string | null {
  const host = url.hostname.replace(/^www\./, '');
  if (host === 'vimeo.com' || host === 'player.vimeo.com') {
    const m = url.pathname.match(/(\d+)/);
    if (m) return m[1] ?? null;
  }
  return null;
}

/**
 * Resolve a raw video URL into a provider + embed URL, or `null` if not a
 * recognized YouTube/Vimeo link (caller should fall back to a plain link).
 */
export function resolveVideoEmbed(raw?: string | null): VideoEmbed | null {
  if (!raw) return null;
  let url: URL;
  try {
    url = new URL(raw.trim());
  } catch {
    return null;
  }
  if (url.protocol !== 'https:' && url.protocol !== 'http:') return null;

  const yt = youtubeId(url);
  if (yt) {
    // youtube-nocookie for fewer trackers in an education context.
    return { provider: 'youtube', embedUrl: `https://www.youtube-nocookie.com/embed/${yt}` };
  }
  const vm = vimeoId(url);
  if (vm) {
    return { provider: 'vimeo', embedUrl: `https://player.vimeo.com/video/${vm}` };
  }
  return null;
}
