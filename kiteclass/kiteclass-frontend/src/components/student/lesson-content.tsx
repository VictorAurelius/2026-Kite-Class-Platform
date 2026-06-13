/**
 * Lesson content renderer — lightweight, XSS-safe Markdown subset (LMS Increment B).
 *
 * The project has no markdown dependency; rather than pull one in (or risk
 * `dangerouslySetInnerHTML`), this renders a safe subset to React elements:
 * headings (#/##/###), unordered + ordered lists, blockquotes, fenced code,
 * and paragraphs with **bold** / *italic* / `code` / [link](url) inline marks.
 * Unknown syntax degrades to plain text — never raw HTML injection.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave rbac-lms-student-fe — Increment B)
 */
'use client';

import { Fragment, type ReactNode } from 'react';

/** Parse inline marks (**bold**, *italic*, `code`, [text](url)) into React nodes. */
function renderInline(text: string, keyBase: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  // Order matters: links first, then code, then bold, then italic.
  const pattern =
    /(\[[^\]]+\]\((https?:\/\/[^\s)]+)\))|(`[^`]+`)|(\*\*[^*]+\*\*)|(\*[^*]+\*)/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let i = 0;
  while ((m = pattern.exec(text)) !== null) {
    if (m.index > last) nodes.push(text.slice(last, m.index));
    const token = m[0];
    const key = `${keyBase}-i${i++}`;
    if (token.startsWith('[')) {
      const linkMatch = token.match(/^\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)$/);
      if (linkMatch && linkMatch[1] && linkMatch[2]) {
        nodes.push(
          <a
            key={key}
            href={linkMatch[2]}
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary underline underline-offset-2"
          >
            {linkMatch[1]}
          </a>,
        );
      } else {
        nodes.push(token);
      }
    } else if (token.startsWith('`')) {
      nodes.push(
        <code key={key} className="rounded bg-muted px-1 py-0.5 font-mono text-[0.85em]">
          {token.slice(1, -1)}
        </code>,
      );
    } else if (token.startsWith('**')) {
      nodes.push(
        <strong key={key} className="font-semibold">
          {token.slice(2, -2)}
        </strong>,
      );
    } else {
      nodes.push(
        <em key={key} className="italic">
          {token.slice(1, -1)}
        </em>,
      );
    }
    last = m.index + token.length;
  }
  if (last < text.length) nodes.push(text.slice(last));
  return nodes;
}

interface Block {
  type: 'h1' | 'h2' | 'h3' | 'p' | 'ul' | 'ol' | 'quote' | 'code';
  lines: string[];
}

/** Split raw markdown into block-level structures. */
function parseBlocks(raw: string): Block[] {
  const blocks: Block[] = [];
  const lines = raw.replace(/\r\n/g, '\n').split('\n');
  let i = 0;
  while (i < lines.length) {
    const line = lines[i] ?? '';
    const trimmed = line.trim();

    if (trimmed === '') {
      i++;
      continue;
    }
    // Fenced code block
    if (trimmed.startsWith('```')) {
      const body: string[] = [];
      i++;
      while (i < lines.length && !(lines[i] ?? '').trim().startsWith('```')) {
        body.push(lines[i] ?? '');
        i++;
      }
      i++; // skip closing fence
      blocks.push({ type: 'code', lines: body });
      continue;
    }
    // Headings
    if (trimmed.startsWith('### ')) {
      blocks.push({ type: 'h3', lines: [trimmed.slice(4)] });
      i++;
      continue;
    }
    if (trimmed.startsWith('## ')) {
      blocks.push({ type: 'h2', lines: [trimmed.slice(3)] });
      i++;
      continue;
    }
    if (trimmed.startsWith('# ')) {
      blocks.push({ type: 'h1', lines: [trimmed.slice(2)] });
      i++;
      continue;
    }
    // Blockquote (collect consecutive)
    if (trimmed.startsWith('> ')) {
      const body: string[] = [];
      while (i < lines.length && (lines[i] ?? '').trim().startsWith('> ')) {
        body.push((lines[i] ?? '').trim().slice(2));
        i++;
      }
      blocks.push({ type: 'quote', lines: body });
      continue;
    }
    // Unordered list
    if (/^[-*]\s+/.test(trimmed)) {
      const body: string[] = [];
      while (i < lines.length && /^[-*]\s+/.test((lines[i] ?? '').trim())) {
        body.push((lines[i] ?? '').trim().replace(/^[-*]\s+/, ''));
        i++;
      }
      blocks.push({ type: 'ul', lines: body });
      continue;
    }
    // Ordered list
    if (/^\d+\.\s+/.test(trimmed)) {
      const body: string[] = [];
      while (i < lines.length && /^\d+\.\s+/.test((lines[i] ?? '').trim())) {
        body.push((lines[i] ?? '').trim().replace(/^\d+\.\s+/, ''));
        i++;
      }
      blocks.push({ type: 'ol', lines: body });
      continue;
    }
    // Paragraph (collect consecutive non-blank, non-special lines)
    {
      const body: string[] = [];
      while (
        i < lines.length &&
        (lines[i] ?? '').trim() !== '' &&
        !/^(#{1,3}\s|>\s|[-*]\s|\d+\.\s|```)/.test((lines[i] ?? '').trim())
      ) {
        body.push((lines[i] ?? '').trim());
        i++;
      }
      blocks.push({ type: 'p', lines: body });
    }
  }
  return blocks;
}

export function LessonContent({ content }: { content: string }) {
  const blocks = parseBlocks(content);

  return (
    <div className="space-y-3 text-sm leading-relaxed text-foreground">
      {blocks.map((b, idx) => {
        const key = `b${idx}`;
        const head = b.lines[0] ?? '';
        switch (b.type) {
          case 'h1':
            return (
              <h2 key={key} className="mt-4 text-lg font-bold">
                {renderInline(head, key)}
              </h2>
            );
          case 'h2':
            return (
              <h3 key={key} className="mt-4 text-base font-semibold">
                {renderInline(head, key)}
              </h3>
            );
          case 'h3':
            return (
              <h4 key={key} className="mt-3 text-sm font-semibold">
                {renderInline(head, key)}
              </h4>
            );
          case 'ul':
            return (
              <ul key={key} className="list-disc space-y-1 pl-5">
                {b.lines.map((li, j) => (
                  <li key={`${key}-${j}`}>{renderInline(li, `${key}-${j}`)}</li>
                ))}
              </ul>
            );
          case 'ol':
            return (
              <ol key={key} className="list-decimal space-y-1 pl-5">
                {b.lines.map((li, j) => (
                  <li key={`${key}-${j}`}>{renderInline(li, `${key}-${j}`)}</li>
                ))}
              </ol>
            );
          case 'quote':
            return (
              <blockquote
                key={key}
                className="border-l-4 border-border pl-3 italic text-muted-foreground"
              >
                {b.lines.map((q, j) => (
                  <p key={`${key}-${j}`}>{renderInline(q, `${key}-${j}`)}</p>
                ))}
              </blockquote>
            );
          case 'code':
            return (
              <pre
                key={key}
                className="overflow-x-auto rounded-lg bg-muted p-3 font-mono text-xs"
              >
                <code>{b.lines.join('\n')}</code>
              </pre>
            );
          case 'p':
          default:
            return (
              <p key={key}>
                {b.lines.map((ln, j) => (
                  <Fragment key={`${key}-${j}`}>
                    {renderInline(ln, `${key}-${j}`)}
                    {j < b.lines.length - 1 ? <br /> : null}
                  </Fragment>
                ))}
              </p>
            );
        }
      })}
    </div>
  );
}
