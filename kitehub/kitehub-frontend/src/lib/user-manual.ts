import fs from 'fs';
import path from 'path';
import matter from 'gray-matter';
import { remark } from 'remark';
import html from 'remark-html';

/**
 * User-manual MDX loader.
 *
 * Reads pages from `documents/05-guides/user-manual/{persona}/*.md` (source-of-truth).
 * Per `.claude/rules/user-manual-content-standard.md` §2 — 15-item checklist;
 * frontmatter required fields enforced at lookup time.
 *
 * Why this lives in kitehub-frontend not packages/shared:
 *   - Public-indexable per persona discoverability matrix (anonymous persona)
 *   - Same SSG pattern as `src/lib/blog.ts` (proven)
 *   - Phase 1 BETA scope = anonymous-prospect 5-page sample (Wave 79 Bucket F1);
 *     P2/P3/Admin defer Wave 80+ Bucket F2.
 */

// Walk up from kitehub-frontend → kitehub → repo root → documents/05-guides/user-manual
const MANUAL_DIR = path.join(
  process.cwd(),
  '..',
  '..',
  'documents',
  '05-guides',
  'user-manual'
);

export interface UserManualPage {
  slug: string;
  persona: string;
  topic: string;
  lastUpdated: string;
  version: string;
  effortMinutes: number;
  title: string;
  contentHtml: string;
  contentRaw: string;
}

export interface UserManualPageMeta {
  slug: string;
  persona: string;
  topic: string;
  lastUpdated: string;
  version: string;
  effortMinutes: number;
  title: string;
  summary: string;
}

export function getManualSlugsForPersona(persona: string): string[] {
  const personaDir = path.join(MANUAL_DIR, persona);
  if (!fs.existsSync(personaDir)) {
    return [];
  }
  return fs
    .readdirSync(personaDir)
    .filter((file) => file.endsWith('.md'))
    .map((file) => file.replace(/\.md$/, ''));
}

export async function getManualPage(
  persona: string,
  slug: string
): Promise<UserManualPage | null> {
  const filePath = path.join(MANUAL_DIR, persona, `${slug}.md`);
  if (!fs.existsSync(filePath)) {
    return null;
  }

  const fileContent = fs.readFileSync(filePath, 'utf-8');
  const { data, content } = matter(fileContent);

  // Extract H1 as title (fallback to topic)
  const h1Match = content.match(/^#\s+(.+)$/m);
  const title = h1Match ? h1Match[1].trim() : data.topic || slug;

  // Render markdown → HTML
  const processed = await remark().use(html).process(content);
  const contentHtml = processed.toString();

  return {
    slug,
    persona,
    topic: String(data.topic || ''),
    lastUpdated: String(data['last-updated'] || ''),
    version: String(data.version || ''),
    effortMinutes: Number(data['effort_minutes'] || 0),
    title,
    contentHtml,
    contentRaw: content,
  };
}

export function getAllManualPagesForPersona(persona: string): UserManualPageMeta[] {
  const slugs = getManualSlugsForPersona(persona);
  return slugs
    .map((slug) => {
      const filePath = path.join(MANUAL_DIR, persona, `${slug}.md`);
      const fileContent = fs.readFileSync(filePath, 'utf-8');
      const { data, content } = matter(fileContent);

      const h1Match = content.match(/^#\s+(.+)$/m);
      const title = h1Match ? h1Match[1].trim() : String(data.topic || slug);

      // Extract first paragraph after TL;DR as summary (≤200 chars)
      const summaryMatch = content.match(/##\s+TL;DR[\s\S]*?\n\n([^\n#]+)/);
      const summary = (summaryMatch ? summaryMatch[1] : '').trim().slice(0, 200);

      return {
        slug,
        persona,
        topic: String(data.topic || slug),
        lastUpdated: String(data['last-updated'] || ''),
        version: String(data.version || ''),
        effortMinutes: Number(data['effort_minutes'] || 0),
        title,
        summary,
      };
    })
    .sort((a, b) => {
      // Display index first, then alphabetical
      if (a.topic === 'index') return -1;
      if (b.topic === 'index') return 1;
      return a.topic.localeCompare(b.topic);
    });
}

/**
 * Lightweight client-side searchable index per `user-manual-content-standard.md`
 * §2 item 12 (Fuse.js v1 stand-in; full Fuse.js upgrade tracked Wave 80+).
 *
 * Returns title + headings + first 200 chars body so search component can
 * filter without requiring full content.
 */
export interface ManualSearchIndexEntry {
  slug: string;
  persona: string;
  title: string;
  summary: string;
  headings: string[];
}

export function getSearchIndexForPersona(persona: string): ManualSearchIndexEntry[] {
  const slugs = getManualSlugsForPersona(persona);
  return slugs.map((slug) => {
    const filePath = path.join(MANUAL_DIR, persona, `${slug}.md`);
    const fileContent = fs.readFileSync(filePath, 'utf-8');
    const { content } = matter(fileContent);

    const h1Match = content.match(/^#\s+(.+)$/m);
    const title = h1Match ? h1Match[1].trim() : slug;

    const headings = Array.from(content.matchAll(/^##\s+(.+)$/gm)).map((m) =>
      m[1].trim()
    );

    const summaryMatch = content.match(/##\s+TL;DR[\s\S]*?\n\n([^\n#]+)/);
    const summary = (summaryMatch ? summaryMatch[1] : '').trim().slice(0, 200);

    return { slug, persona, title, summary, headings };
  });
}
