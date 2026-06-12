'use client';

import { useState, FormEvent } from 'react';

/**
 * DMCA notice intake page (Wave 4 Sub-PR 4.3, ADR-012 Track 2).
 *
 * Minimal, information-first — posts to backend `POST /public/dmca` (rate-limited
 * by the gateway filter). No styling library beyond Tailwind primitives.
 */
export default function DmcaPage() {
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<
    { ok: true; id: number } | { ok: false; message: string } | null
  >(null);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmitting(true);
    setResult(null);
    const form = new FormData(e.currentTarget);
    const body = {
      reporterEmail: String(form.get('reporterEmail') ?? ''),
      reporterName: String(form.get('reporterName') ?? ''),
      allegedInfringingUrl: String(form.get('allegedInfringingUrl') ?? ''),
      copyrightedWorkDescription: String(form.get('copyrightedWorkDescription') ?? ''),
    };
    try {
      const resp = await fetch('/public/dmca', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!resp.ok) {
        const text = await resp.text();
        setResult({ ok: false, message: text || `HTTP ${resp.status}` });
        return;
      }
      const json = await resp.json();
      setResult({ ok: true, id: json?.data?.id ?? 0 });
      (e.currentTarget as HTMLFormElement).reset();
    } catch (err) {
      setResult({
        ok: false,
        message: err instanceof Error ? err.message : 'Network error',
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container max-w-3xl py-12">
      <h1 className="text-3xl font-bold">DMCA Takedown Notice</h1>
      <p className="mt-4 text-muted-foreground">
        KiteClass respects intellectual property rights. If you believe content on a
        tenant site infringes your copyright, submit a notice below. Our team reviews
        every submission under DMCA §512 procedures.
      </p>

      <section className="mt-8 space-y-3 rounded-md border bg-muted/30 dark:bg-muted/50 p-4 text-sm">
        <h2 className="font-semibold">Before you submit</h2>
        <ul className="list-disc space-y-1 pl-5">
          <li>Only rights holders or their authorized agents should submit notices.</li>
          <li>
            Frivolous or knowingly false notices may subject you to liability under
            §512(f).
          </li>
          <li>
            Include a direct URL to the allegedly infringing content and a clear
            description of the copyrighted work.
          </li>
          <li>
            Our designated DMCA agent reviews notices during business hours; you will
            receive a case id immediately.
          </li>
        </ul>
      </section>

      <form onSubmit={onSubmit} className="mt-8 space-y-4">
        <div>
          <label htmlFor="reporterName" className="block text-sm font-medium">
            Your full name or legal entity *
          </label>
          <input
            id="reporterName"
            name="reporterName"
            type="text"
            required
            maxLength={255}
            className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>

        <div>
          <label htmlFor="reporterEmail" className="block text-sm font-medium">
            Contact email *
          </label>
          <input
            id="reporterEmail"
            name="reporterEmail"
            type="email"
            required
            maxLength={255}
            className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>

        <div>
          <label htmlFor="allegedInfringingUrl" className="block text-sm font-medium">
            URL of the allegedly infringing content *
          </label>
          <input
            id="allegedInfringingUrl"
            name="allegedInfringingUrl"
            type="url"
            required
            maxLength={2000}
            placeholder="https://tenant.kitehub.me/..."
            className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>

        <div>
          <label
            htmlFor="copyrightedWorkDescription"
            className="block text-sm font-medium"
          >
            Description of the copyrighted work *
          </label>
          <textarea
            id="copyrightedWorkDescription"
            name="copyrightedWorkDescription"
            required
            minLength={10}
            maxLength={4000}
            rows={5}
            className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50"
        >
          {submitting ? 'Submitting…' : 'Submit DMCA notice'}
        </button>
      </form>

      {result?.ok && (
        <div className="mt-6 rounded-md border border-green-200 bg-green-50 p-4 text-sm text-green-900 dark:border-green-800 dark:bg-green-950/30 dark:text-green-200">
          Notice received. Reference case id: <strong>#{result.id}</strong>. Our team
          will contact you at the email provided if we need more information.
        </div>
      )}
      {result && !result.ok && (
        <div className="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900 dark:border-red-800 dark:bg-red-950/30 dark:text-red-200">
          Submission failed: {result.message}
        </div>
      )}
    </div>
  );
}
