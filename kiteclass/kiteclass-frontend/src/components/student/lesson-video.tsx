/**
 * Lesson video embed — responsive 16:9 iframe for YouTube/Vimeo (LMS Increment B).
 *
 * Self-hosting is out of scope (wave-lms-fe-1 Bucket C); we embed external
 * providers via {@link resolveVideoEmbed}. Unrecognized URLs render a safe
 * "open in new tab" link instead of an arbitrary iframe src.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave rbac-lms-student-fe — Increment B)
 */
'use client';

import { ExternalLink, Video } from 'lucide-react';
import { resolveVideoEmbed } from '@/lib/lms/video-embed';

interface LessonVideoProps {
  url: string;
  title?: string;
}

export function LessonVideo({ url, title }: LessonVideoProps) {
  const embed = resolveVideoEmbed(url);

  if (!embed) {
    // Unknown provider — never inject arbitrary URL into iframe; offer a link.
    return (
      <a
        href={url}
        target="_blank"
        rel="noopener noreferrer"
        className="flex items-center gap-2 rounded-lg border border-border bg-muted/40 p-4 text-sm font-medium text-primary hover:bg-muted"
      >
        <Video className="h-4 w-4 shrink-0" aria-hidden />
        <span className="flex-1 truncate">Xem video bài học</span>
        <ExternalLink className="h-4 w-4 shrink-0" aria-hidden />
      </a>
    );
  }

  return (
    <div className="relative w-full overflow-hidden rounded-lg bg-black" style={{ aspectRatio: '16 / 9' }}>
      <iframe
        src={embed.embedUrl}
        title={title ?? 'Video bài học'}
        className="absolute inset-0 h-full w-full"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowFullScreen
        loading="lazy"
      />
    </div>
  );
}
