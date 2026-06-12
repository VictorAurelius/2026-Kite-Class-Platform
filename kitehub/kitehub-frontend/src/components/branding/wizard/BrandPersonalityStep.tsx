'use client';

/**
 * BrandPersonalityStep — Step 2 of the output-first AI Branding Wizard (GAP-1216).
 *
 * Merges the former Audience (Step 4) + Tone (Step 5) screens onto ONE page so
 * the user expresses the full "brand personality" before the wizard generates a
 * real preview (output-first per kit v3 §2.5, hội tụ 3 audit 2026-06-11).
 *
 * "Components giữ": the card primitives (`AudienceCard` / `ToneCard`) + the
 * option data (`AUDIENCE_OPTIONS` / `TONE_OPTIONS`) + reasoning maps are reused
 * verbatim from the original step components — only the page-level composition +
 * the single shared footer are new here.
 *
 * Selections dispatch to WizardState immediately (SET_AUDIENCE / SET_TONE) so the
 * orchestrator reducer owns the canonical fields (rework §3.1 anti-pattern guard
 * — no shadow useState copies of canonical fields).
 */

import { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Sparkles,
  Palette,
  ChevronDown,
  Plus,
  Trash2,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { AudienceCard } from './AudienceCard';
import { ToneCard } from './ToneCard';
import { AUDIENCE_OPTIONS, AUDIENCE_REASONING } from './AudienceStep';
import { TONE_OPTIONS, TONE_REASONING } from './ToneStep';
import { formatVnd } from './facts-landing';
import type { WizardState, WizardAction } from './wizard-shared';

export interface BrandPersonalityStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  onNext: () => void;
  onBack: () => void;
}

export function BrandPersonalityStep({
  wizardState,
  dispatch,
  onNext,
  onBack,
}: BrandPersonalityStepProps) {
  const { audience, tone, facts } = wizardState;

  // UI-only: progressive disclosure for the optional facts (collapsed default).
  // Not a canonical field → local state is fine (rework §3.1 only guards
  // canonical fields, which live in WizardState.facts via dispatch).
  const [factsOpen, setFactsOpen] = useState(false);

  const audienceReasoning = audience ? AUDIENCE_REASONING[audience] : null;
  const selectedAudience = AUDIENCE_OPTIONS.find((o) => o.id === audience);
  const toneReasoning = tone ? TONE_REASONING[tone] : null;
  const selectedTone = TONE_OPTIONS.find((t) => t.id === tone);

  const canContinue = Boolean(audience) && Boolean(tone);

  // Stable, increasing tuition-row id (reducer stays pure — component supplies id).
  const nextTuitionId = () =>
    `tuition-${Date.now().toString(36)}-${facts.tuitions.length}`;

  return (
    <div className="mx-auto w-full max-w-3xl space-y-8" data-testid="brand-personality-step">
      <div className="space-y-2">
        <p className="text-sm font-semibold uppercase tracking-wide text-primary">Bước 2 / 5 · Phong cách</p>
        <h1 className="text-2xl font-bold text-foreground">
          Trung tâm của bạn muốn toát lên điều gì?
        </h1>
        <p className="text-muted-foreground">
          Chọn <strong>đối tượng chính</strong> và <strong>phong cách</strong> — AI dùng để chọn
          màu, font và giọng văn cho trang giới thiệu. Đổi sau lúc nào cũng được.
        </p>
      </div>

      {/* ---- Audience ---- */}
      <section className="space-y-4" data-testid="personality-audience">
        <h2 className="text-base font-bold text-foreground">
          1 · Đối tượng phụ huynh / học viên chính
        </h2>
        <div
          role="radiogroup"
          aria-label="Đối tượng học viên"
          className="grid grid-cols-1 gap-4 sm:grid-cols-2"
        >
          {AUDIENCE_OPTIONS.map((option) => (
            <AudienceCard
              key={option.id}
              option={option}
              selected={audience === option.id}
              onSelect={(id) => dispatch({ type: 'SET_AUDIENCE', audience: id })}
            />
          ))}
        </div>
        {audienceReasoning && selectedAudience && (
          <div
            data-testid="audience-reasoning"
            className="rounded-lg border border-primary/25 bg-primary/[0.06] p-4"
          >
            <div className="flex items-start gap-3">
              <Sparkles className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
              <div className="text-sm">
                <p className="mb-2 font-semibold">AI đã hiểu hướng đi</p>
                <p className="text-muted-foreground">{audienceReasoning.headline}</p>
                <ul className="mt-2 space-y-1 text-muted-foreground">
                  {audienceReasoning.bullets.map((line) => (
                    <li key={line}>• {line}</li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* ---- Tone ---- */}
      <section className="space-y-4" data-testid="personality-tone">
        <h2 className="text-base font-bold text-foreground">2 · Phong cách trang giới thiệu</h2>
        <div
          role="radiogroup"
          aria-label="Phong cách thương hiệu"
          className="grid grid-cols-1 gap-4 sm:grid-cols-2"
        >
          {TONE_OPTIONS.map((option) => (
            <ToneCard
              key={option.id}
              option={option}
              selected={tone === option.id}
              onSelect={(id) => dispatch({ type: 'SET_TONE', tone: id })}
            />
          ))}
        </div>
        {toneReasoning && selectedTone && (
          <div
            data-testid="tone-reasoning"
            className="rounded-lg border border-primary/25 bg-primary/[0.06] p-4"
          >
            <div className="flex items-start gap-3">
              <Palette className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
              <div className="text-sm">
                <p className="mb-1 font-semibold">
                  {toneReasoning.shortHeadline}
                  {selectedAudience ? ` + ${selectedAudience.title}` : ''}
                </p>
                <p className="text-muted-foreground">{toneReasoning.body}</p>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* ---- Facts (optional, GAP-1234) — progressive disclosure ---- */}
      <section data-testid="personality-facts" className="rounded-xl border border-input">
        <button
          type="button"
          aria-expanded={factsOpen}
          aria-controls="personality-facts-panel"
          data-testid="facts-toggle"
          onClick={() => setFactsOpen((o) => !o)}
          className="flex w-full items-center justify-between gap-3 rounded-xl px-4 py-3 text-left transition hover:bg-muted/40"
        >
          <span>
            <span className="block text-sm font-semibold text-foreground">
              Thông tin hiển thị trên trang{' '}
              <span className="font-normal text-muted-foreground">(không bắt buộc)</span>
            </span>
            <span className="mt-0.5 block text-xs text-muted-foreground">
              AI chỉ dùng đúng thông tin thật bạn nhập cho tiêu đề, phần giới thiệu, học phí, FAQ —
              không tự bịa số liệu. Bỏ trống cũng được, AI dùng mẫu an toàn.
            </span>
          </span>
          <ChevronDown
            aria-hidden="true"
            className={`h-5 w-5 shrink-0 text-muted-foreground transition-transform ${
              factsOpen ? 'rotate-180' : ''
            }`}
          />
        </button>

        {factsOpen && (
          <div
            id="personality-facts-panel"
            data-testid="facts-panel"
            className="space-y-4 border-t border-input px-4 py-4"
          >
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FactField
                id="facts-address"
                label="Địa chỉ trung tâm"
                placeholder="VD: 123 Lê Lợi, Q.1, TP.HCM"
                value={facts.address}
                onChange={(value) => dispatch({ type: 'SET_FACT', key: 'address', value })}
              />
              <FactField
                id="facts-phone"
                label="Số điện thoại"
                placeholder="VD: 0901 234 567"
                value={facts.contactPhone}
                onChange={(value) => dispatch({ type: 'SET_FACT', key: 'contactPhone', value })}
              />
              <FactField
                id="facts-email"
                label="Email liên hệ"
                type="email"
                placeholder="VD: lienhe@trungtam.vn"
                value={facts.contactEmail}
                onChange={(value) => dispatch({ type: 'SET_FACT', key: 'contactEmail', value })}
              />
              <FactField
                id="facts-zalo"
                label="Link Zalo (OA hoặc số)"
                placeholder="VD: zalo.me/0901234567"
                value={facts.zaloUrl}
                onChange={(value) => dispatch({ type: 'SET_FACT', key: 'zaloUrl', value })}
              />
            </div>

            {/* Tuition rows → BE pricingTiers */}
            <div className="space-y-2">
              <p className="text-sm font-semibold text-foreground">
                Học phí{' '}
                <span className="font-normal text-muted-foreground">
                  (hiện ở bảng giá + FAQ)
                </span>
              </p>
              {facts.tuitions.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  Chưa có mức học phí nào. Thêm để AI hiện bảng giá thật.
                </p>
              )}
              <ul className="space-y-2" data-testid="facts-tuition-list">
                {facts.tuitions.map((row) => (
                  <li key={row.id} className="flex items-start gap-2" data-testid="facts-tuition-row">
                    <input
                      type="text"
                      aria-label="Tên lớp / khóa"
                      placeholder="Tên lớp / khóa"
                      value={row.name}
                      onChange={(e) =>
                        dispatch({
                          type: 'SET_TUITION',
                          id: row.id,
                          field: 'name',
                          value: e.target.value,
                        })
                      }
                      className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                    <div className="w-40">
                      <input
                        type="text"
                        inputMode="numeric"
                        aria-label="Học phí (VND)"
                        placeholder="VD: 1500000"
                        value={row.price}
                        onChange={(e) =>
                          dispatch({
                            type: 'SET_TUITION',
                            id: row.id,
                            field: 'price',
                            value: e.target.value.replace(/\D/g, ''),
                          })
                        }
                        className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                      />
                      {row.price && (
                        <p
                          data-testid="facts-tuition-price-preview"
                          className="mt-1 text-right text-xs text-muted-foreground"
                        >
                          {formatVnd(row.price)}/tháng
                        </p>
                      )}
                    </div>
                    <button
                      type="button"
                      aria-label="Xóa dòng học phí"
                      data-testid="facts-tuition-remove"
                      onClick={() => dispatch({ type: 'REMOVE_TUITION', id: row.id })}
                      className="mt-1 rounded-md p-2 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"
                    >
                      <Trash2 className="h-4 w-4" aria-hidden="true" />
                    </button>
                  </li>
                ))}
              </ul>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                data-testid="facts-tuition-add"
                onClick={() => dispatch({ type: 'ADD_TUITION', id: nextTuitionId() })}
              >
                <Plus className="mr-1.5 h-4 w-4" aria-hidden="true" />
                Thêm lớp / mức phí
              </Button>
            </div>
          </div>
        )}
      </section>

      <div className="flex items-center justify-between border-t pt-4">
        <Button variant="ghost" onClick={onBack} data-testid="personality-back">
          <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground">
          Bước 2 / 5 ·{' '}
          {canContinue
            ? 'Đã chọn đối tượng + phong cách'
            : 'Chọn cả đối tượng và phong cách để tiếp tục'}
        </p>
        <Button onClick={onNext} disabled={!canContinue} data-testid="personality-continue">
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// FactField — labelled text input for the optional landing facts (GAP-1234)
// ---------------------------------------------------------------------------

interface FactFieldProps {
  id: string;
  label: string;
  value: string;
  placeholder?: string;
  type?: string;
  onChange: (value: string) => void;
}

function FactField({ id, label, value, placeholder, type = 'text', onChange }: FactFieldProps) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-foreground">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
      />
    </div>
  );
}
