import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AudienceStep, AUDIENCE_OPTIONS } from '../AudienceStep';
import { AudienceCard } from '../AudienceCard';
import { ToneStep, TONE_OPTIONS } from '../ToneStep';
import { ToneCard } from '../ToneCard';

// Wave 32 Bucket B (rework) — Steps 3 + 4 (Audience + Tone selectors).
// Per `wave-2026-05-07-32-ai-branding-wizard-v2-rework.md` §3.4:
// every test must be MEANINGFUL (functional, not smoke). 8 tests below cover:
//   AudienceStep: render 4 cards · selection updates state · onNext called on Continue · revisit seed
//   ToneStep: render 4 cards with tiny preview · selection updates state · onNext called
//   AudienceCard: selected state visible
//   ToneCard: preview renders sample button + heading · selection ARIA + onSelect callback

describe('Wave 32 Bucket B — AudienceStep (Step 3)', () => {
  it('renders 4 audience cards with VN labels', () => {
    render(<AudienceStep wizardState={{ audience: null }} onNext={vi.fn()} onBack={vi.fn()} />);

    // 4 expected VN-labeled audience cards (GAP-1231 kit v3 concern-voice labels).
    expect(screen.getByText('Phụ huynh mầm non / tiểu học')).toBeInTheDocument();
    expect(screen.getByText('Luyện thi / THCS-THPT')).toBeInTheDocument();
    expect(screen.getByText('Người đi làm / tiếng Anh giao tiếp')).toBeInTheDocument();
    expect(screen.getByText('Lớp luyện thi chuyên sâu')).toBeInTheDocument();

    // All 4 are exposed as radio role for a11y per ARIA spec.
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(4);
  });

  it('persists selection in local state and shows AI reasoning preview after select', async () => {
    const user = userEvent.setup();
    render(<AudienceStep wizardState={{ audience: null }} onNext={vi.fn()} onBack={vi.fn()} />);

    // Initially no card selected, no reasoning shown, Continue disabled.
    expect(screen.queryByTestId('audience-reasoning')).not.toBeInTheDocument();
    const continueBtn = screen.getByRole('button', { name: /Tiếp tục/i });
    expect(continueBtn).toBeDisabled();

    // Select "Lớp luyện thi chuyên sâu" (exam-prep).
    await user.click(screen.getByText('Lớp luyện thi chuyên sâu'));

    // Reasoning preview appears + selected card has aria-checked=true + Continue enabled.
    expect(screen.getByTestId('audience-reasoning')).toBeInTheDocument();
    expect(screen.getByText(/AI đã hiểu hướng đi/i)).toBeInTheDocument();
    expect(continueBtn).not.toBeDisabled();

    const examPrepCard = screen
      .getAllByRole('radio')
      .find((r) => r.getAttribute('data-selected') === 'true');
    expect(examPrepCard).toBeDefined();
    expect(within(examPrepCard!).getByText('Lớp luyện thi chuyên sâu')).toBeInTheDocument();
  });

  it('calls onNext with the selected audience id when Continue is pressed', async () => {
    const onNext = vi.fn();
    const user = userEvent.setup();
    render(
      <AudienceStep wizardState={{ audience: null }} onNext={onNext} onBack={vi.fn()} />,
    );

    await user.click(screen.getByText('Người đi làm / tiếng Anh giao tiếp'));
    await user.click(screen.getByRole('button', { name: /Tiếp tục/i }));

    expect(onNext).toHaveBeenCalledTimes(1);
    expect(onNext).toHaveBeenCalledWith('english-center');
  });

  it('seeds selection from wizardState.audience on mount (revisit case)', () => {
    render(
      <AudienceStep
        wizardState={{ audience: 'preschool' }}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );

    // Pre-selected card should have aria-checked=true and the reasoning preview
    // should already be visible without any user interaction.
    const preselected = screen
      .getAllByRole('radio')
      .find((r) => r.getAttribute('aria-checked') === 'true');
    expect(preselected).toBeDefined();
    expect(within(preselected!).getByText('Phụ huynh mầm non / tiểu học')).toBeInTheDocument();
    expect(screen.getByTestId('audience-reasoning')).toBeInTheDocument();
  });
});

describe('Wave 32 Bucket B — AudienceCard', () => {
  it('shows selected check icon and toggles aria-checked when selected=true', () => {
    const option = AUDIENCE_OPTIONS[0];
    expect(option).toBeDefined();

    const { rerender } = render(
      <AudienceCard option={option!} selected={false} onSelect={vi.fn()} />,
    );
    expect(screen.getByRole('radio')).toHaveAttribute('aria-checked', 'false');

    rerender(<AudienceCard option={option!} selected={true} onSelect={vi.fn()} />);
    expect(screen.getByRole('radio')).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio').getAttribute('data-selected')).toBe('true');
  });
});

describe('Wave 32 Bucket B — ToneStep (Step 4)', () => {
  it('renders 4 tone cards each with a tiny rendered preview', () => {
    render(
      <ToneStep
        wizardState={{ audience: 'exam-prep', tone: null }}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );

    // 4 expected VN-labeled tone cards per plan §3 Bucket B.
    expect(screen.getByText('Chuyên nghiệp')).toBeInTheDocument();
    expect(screen.getByText('Thân thiện')).toBeInTheDocument();
    expect(screen.getByText('Năng động')).toBeInTheDocument();
    expect(screen.getByText('Sang trọng')).toBeInTheDocument();

    // Each card MUST contain a tiny preview block (testid keyed by tone id).
    expect(screen.getByTestId('tone-preview-professional')).toBeInTheDocument();
    expect(screen.getByTestId('tone-preview-friendly')).toBeInTheDocument();
    expect(screen.getByTestId('tone-preview-energetic')).toBeInTheDocument();
    expect(screen.getByTestId('tone-preview-luxury')).toBeInTheDocument();

    // 4 radios for a11y.
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(4);
  });

  it('persists selection, shows reasoning citing both tone + audience, calls onNext', async () => {
    const onNext = vi.fn();
    const user = userEvent.setup();
    render(
      <ToneStep
        wizardState={{ audience: 'exam-prep', tone: null }}
        onNext={onNext}
        onBack={vi.fn()}
      />,
    );

    expect(screen.queryByTestId('tone-reasoning')).not.toBeInTheDocument();

    await user.click(screen.getByText('Chuyên nghiệp'));

    // Reasoning panel appears and references both tone + previously-chosen audience
    // (step4-selected spec: "Phong cách 'Chuyên nghiệp' + Lớp luyện thi").
    const reasoning = screen.getByTestId('tone-reasoning');
    expect(reasoning).toBeInTheDocument();
    expect(within(reasoning).getByText(/Chuyên nghiệp/)).toBeInTheDocument();
    expect(within(reasoning).getByText(/Lớp luyện thi/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Tiếp tục/i }));

    expect(onNext).toHaveBeenCalledTimes(1);
    expect(onNext).toHaveBeenCalledWith('professional');
  });
});

describe('Wave 32 Bucket B — ToneCard', () => {
  it('renders sample headline + sample button using tone-specific tokens', () => {
    const proTone = TONE_OPTIONS.find((t) => t.id === 'professional');
    expect(proTone).toBeDefined();

    render(<ToneCard option={proTone!} selected={false} onSelect={vi.fn()} />);

    // Sample headline rendered inside preview block.
    const preview = screen.getByTestId('tone-preview-professional');
    expect(within(preview).getByText('Khoá luyện thi THPT 2026')).toBeInTheDocument();
    // Sample button label rendered inside preview block.
    expect(within(preview).getByText('Đăng ký ngay')).toBeInTheDocument();
  });

  it('marks card with aria-checked + data-tone, fires onSelect with id', async () => {
    const luxeTone = TONE_OPTIONS.find((t) => t.id === 'luxury');
    expect(luxeTone).toBeDefined();

    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<ToneCard option={luxeTone!} selected={false} onSelect={onSelect} />);

    const radio = screen.getByRole('radio');
    expect(radio).toHaveAttribute('aria-checked', 'false');
    expect(radio).toHaveAttribute('data-tone', 'luxury');

    await user.click(radio);
    expect(onSelect).toHaveBeenCalledWith('luxury');
  });
});
