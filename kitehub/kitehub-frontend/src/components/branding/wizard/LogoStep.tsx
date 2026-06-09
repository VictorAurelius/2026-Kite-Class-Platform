'use client';

/**
 * Wave 32 Bucket A — Step 2 Logo: upload OR AI-generated path.
 *
 * Spec source:
 *   - documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step2-logo-default.html
 *   - documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step2-logo-uploaded.html
 *   - documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step2-logo-skip.html
 *   - documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step2-logo-error.html
 *
 * Sub-states (driven by local fork choice + WizardState.logoUrl):
 *   - default     : 2 fork cards visible, no file selected, no AI logo chosen
 *   - uploaded    : real file uploaded, preview rendered, can replace or continue
 *   - skip        : "AI sẽ tạo logo" fork chosen — drop zone hidden, continue enabled
 *   - error       : last upload attempt failed — error banner + retry
 *
 * Per `ai-branding-guidelines.md` §1: the choice between user-uploaded
 * (STATIC classification) and AI-generated (FULL_AI classification) is
 * captured by `wizardState.aiLogo` and persisted via SET_LOGO action.
 *
 * This component reuses the existing `useUploadAsset` + `useAnalyzeLogo`
 * react-query hooks per Wave 32 plan §3 Bucket A "preserve UploadStep
 * upload logic". Real `/api/v1/branding/assets/{instanceId}/LOGO` endpoint
 * exists today — no mocks or TODO needed for upload.
 */

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  ArrowLeft,
  ArrowRight,
  UploadCloud,
  Sparkles,
  FolderOpen,
  AlertCircle,
  Image as ImageIcon,
  Images,
  Check,
  X,
} from 'lucide-react';
import { useUploadAsset, useAssets } from '@/hooks/use-branding';
import type { BrandingAsset } from '@/types/branding';
import { toast } from 'sonner';
import { WizardCard, WizardStepHeader, type WizardState, type WizardAction } from './wizard-shared';

const MAX_FILE_BYTES = 2 * 1024 * 1024; // 2MB per kit copy
const ACCEPTED_MIME = ['image/svg+xml', 'image/png', 'image/jpeg'];

export interface LogoStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  /** Tenant instance — required for the upload endpoint path. */
  instanceId: string;
  onNext: () => void;
  onBack: () => void;
}

type Fork = 'upload' | 'ai-generate';

export function LogoStep({
  wizardState,
  dispatch,
  instanceId,
  onNext,
  onBack,
}: LogoStepProps) {
  const { logoUrl, aiLogo } = wizardState;

  const initialFork: Fork = aiLogo ? 'ai-generate' : 'upload';
  const [fork, setFork] = useState<Fork>(initialFork);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(logoUrl);

  const uploadMutation = useUploadAsset();

  // GAP-1112 #3 — let the user reuse a logo already uploaded for this instance
  // instead of forcing a fresh upload every time. Lists existing assets via
  // GET /api/platform/branding/assets/{instanceId} and filters to LOGO type.
  // Empty list ⇒ the picker section is hidden entirely (no clutter for new
  // instances). Reuse avoids the re-upload-accumulates-assets path (GAP-1112 #2).
  const { data: existingAssets } = useAssets(instanceId);
  const logoAssets = (existingAssets ?? []).filter((a) => a.type === 'LOGO');

  const validateFile = (file: File): string | null => {
    if (!ACCEPTED_MIME.includes(file.type)) {
      return 'Chỉ chấp nhận file SVG, PNG, hoặc JPG.';
    }
    if (file.size > MAX_FILE_BYTES) {
      return 'File không được vượt quá 2MB.';
    }
    return null;
  };

  const handleFile = async (file: File) => {
    setErrorMsg(null);
    const err = validateFile(file);
    if (err) {
      setErrorMsg(err);
      return;
    }

    // Optimistic local preview while upload runs
    const reader = new FileReader();
    reader.onloadend = () => setPreviewUrl(reader.result as string);
    reader.readAsDataURL(file);

    try {
      const asset = await uploadMutation.mutateAsync({
        instanceId,
        type: 'LOGO',
        file,
      });
      dispatch({ type: 'SET_LOGO', url: asset.url, aiLogo: false });
      setPreviewUrl(asset.url);
      toast.success('Tải logo lên thành công!');
    } catch {
      setErrorMsg('Tải logo lên thất bại. Vui lòng thử lại.');
      // Keep optimistic preview off so the user knows it didn't persist
      dispatch({ type: 'CLEAR_LOGO' });
      setPreviewUrl(null);
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) void handleFile(file);
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    const file = e.dataTransfer.files[0];
    if (file) void handleFile(file);
  };

  const handleSelectFork = (next: Fork) => {
    setFork(next);
    setErrorMsg(null);
    if (next === 'ai-generate') {
      // Switching to AI-generate clears any previously-uploaded logo and
      // sets the explicit aiLogo flag so generate-step can branch on it.
      dispatch({ type: 'SET_LOGO', url: '', aiLogo: true });
      setPreviewUrl(null);
    } else {
      // Switching back to upload mode requires a fresh user choice.
      dispatch({ type: 'CLEAR_LOGO' });
      setPreviewUrl(null);
    }
  };

  const handleRemoveLogo = () => {
    dispatch({ type: 'CLEAR_LOGO' });
    setPreviewUrl(null);
    setErrorMsg(null);
  };

  // GAP-1112 #3 — pick a previously-uploaded logo from the asset library.
  // Sets it as the active logo (STATIC classification, aiLogo=false) without
  // re-uploading; the chosen URL flows downstream exactly like a fresh upload.
  const handlePickAsset = (asset: BrandingAsset) => {
    setErrorMsg(null);
    dispatch({ type: 'SET_LOGO', url: asset.url, aiLogo: false });
    setPreviewUrl(asset.url);
  };

  // Continue is enabled when either:
  //   - user uploaded a logo (logoUrl truthy AND not aiLogo placeholder)
  //   - user explicitly chose AI-generate path (aiLogo === true)
  const canContinue = (fork === 'upload' && !!logoUrl && !aiLogo) || fork === 'ai-generate';

  return (
    <div className="space-y-6">
      <WizardCard>
        <WizardStepHeader
          eyebrow="Bước 2 / 6 · Tuỳ chọn"
          title="Bạn đã có logo chưa?"
          subtitle="Bạn có thể upload logo có sẵn, hoặc để AI tự tạo logo từ tên trung tâm. Đổi sau lúc nào cũng được."
        />

        {/* Fork selector */}
        <div
          role="radiogroup"
          aria-label="Lựa chọn nguồn logo"
          className="grid grid-cols-1 sm:grid-cols-2 gap-3"
        >
          <ForkCard
            selected={fork === 'upload'}
            icon={<UploadCloud className="w-5 h-5 text-sky-600 dark:text-sky-400" />}
            title="Tôi có logo"
            subtitle="Upload file SVG, PNG, hoặc JPG."
            onSelect={() => handleSelectFork('upload')}
            data-testid="wizard-logo-fork-upload"
          />
          <ForkCard
            selected={fork === 'ai-generate'}
            icon={<Sparkles className="w-5 h-5 text-orange-500 dark:text-orange-400" />}
            title="Để AI tạo logo"
            subtitle="AI sẽ tạo logo dựa trên tên trung tâm."
            onSelect={() => handleSelectFork('ai-generate')}
            data-testid="wizard-logo-fork-ai"
          />
        </div>

        {/* Upload mode — drop zone OR uploaded preview */}
        {fork === 'upload' && (
          <div className="mt-6">
            {previewUrl ? (
              <div
                data-testid="wizard-logo-uploaded"
                className="border border-input rounded-lg p-6 flex flex-col items-center"
              >
                <img
                  src={previewUrl}
                  alt="Logo đã chọn"
                  className="max-h-40 mb-4 rounded"
                />
                <div className="flex items-center gap-3">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleRemoveLogo}
                    data-testid="wizard-logo-remove"
                  >
                    <X className="mr-2 h-4 w-4" />
                    Xoá và chọn lại
                  </Button>
                </div>
              </div>
            ) : (
              <label
                className="block cursor-pointer"
                data-testid="wizard-logo-drop"
              >
                <div
                  onDragOver={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                  }}
                  onDrop={handleDrop}
                  className="border-2 border-dashed border-muted-foreground/25 rounded-lg p-8 text-center hover:border-primary transition-colors"
                >
                  <ImageIcon className="w-10 h-10 mx-auto mb-3 text-muted-foreground" />
                  <h3 className="text-base font-bold mb-1">
                    Kéo thả file vào đây
                  </h3>
                  <p className="text-sm text-muted-foreground mb-3">
                    hoặc bấm để chọn từ máy tính
                  </p>
                  <span className="inline-flex items-center gap-2 px-4 py-2 bg-secondary text-secondary-foreground rounded-md text-sm font-medium">
                    <FolderOpen className="w-4 h-4" />
                    Chọn file
                  </span>
                  <p className="text-xs text-muted-foreground mt-3">
                    SVG · PNG · JPG · tối đa 2MB · tỉ lệ 1:1 hoặc 4:3 ưu tiên
                  </p>
                </div>
                <input
                  type="file"
                  accept="image/svg+xml,image/png,image/jpeg"
                  onChange={handleFileInput}
                  className="hidden"
                  data-testid="wizard-logo-file-input"
                />
              </label>
            )}

            {uploadMutation.isPending && (
              <p
                role="status"
                className="text-xs text-muted-foreground mt-2 text-center"
              >
                Đang tải lên…
              </p>
            )}

            {errorMsg && (
              <div
                role="alert"
                data-testid="wizard-logo-error"
                className="mt-3 flex items-start gap-2 px-3 py-2 bg-destructive/10 border border-destructive/30 rounded-md text-xs text-destructive"
              >
                <AlertCircle className="w-4 h-4 mt-0.5" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Asset library — reuse a previously-uploaded logo (GAP-1112 #3) */}
            {logoAssets.length > 0 && (
              <div className="mt-6" data-testid="wizard-logo-library">
                <div className="flex items-center gap-2 mb-3">
                  <Images className="w-4 h-4 text-muted-foreground" />
                  <h3 className="text-sm font-semibold">
                    Hoặc chọn logo đã tải lên trước đó
                  </h3>
                </div>
                <div
                  role="radiogroup"
                  aria-label="Logo đã tải lên"
                  className="grid grid-cols-3 sm:grid-cols-4 gap-3"
                >
                  {logoAssets.map((asset) => {
                    const selected = logoUrl === asset.url;
                    return (
                      <button
                        key={asset.id}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        aria-label="Chọn logo đã tải lên"
                        onClick={() => handlePickAsset(asset)}
                        data-testid={`wizard-logo-library-${asset.id}`}
                        className={[
                          'relative aspect-square rounded-lg border bg-background p-2 flex items-center justify-center transition-all',
                          selected
                            ? 'border-primary ring-2 ring-primary/30'
                            : 'border-input hover:border-primary/50',
                        ].join(' ')}
                      >
                        <img
                          src={asset.url}
                          alt="Logo đã tải lên"
                          className="max-h-full max-w-full object-contain rounded"
                        />
                        {selected && (
                          <span className="absolute top-1 right-1 rounded-full bg-primary text-primary-foreground p-0.5">
                            <Check className="w-3 h-3" />
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}

        {/* AI-generate mode — explanation only */}
        {fork === 'ai-generate' && (
          <div
            data-testid="wizard-logo-skip"
            className="mt-6 bg-orange-50 dark:bg-orange-950/30 border border-orange-200 dark:border-orange-900 rounded-md p-4 flex gap-3"
          >
            <Sparkles className="w-5 h-5 text-orange-600 dark:text-orange-400 mt-0.5 shrink-0" />
            <div className="text-sm text-orange-900 dark:text-orange-100">
              <p className="font-semibold mb-1">AI sẽ lo phần logo</p>
              <p className="text-xs">
                Hệ thống sẽ tạo logo dựa trên tên trung tâm + phong cách bạn chọn ở
                Bước 4. Bạn có thể đổi logo bất kỳ lúc nào sau khi triển khai.
              </p>
            </div>
          </div>
        )}
      </WizardCard>

      {/* Footer actions */}
      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack} data-testid="wizard-step2-back">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground">
          Bước 2 / 6 · Tuỳ chọn — bỏ qua nếu chưa có
        </p>
        <Button
          onClick={onNext}
          disabled={!canContinue || uploadMutation.isPending}
          data-testid="wizard-step2-continue"
        >
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// ForkCard — single radio-style card for the upload-vs-AI fork
// ---------------------------------------------------------------------------

interface ForkCardProps {
  selected: boolean;
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  onSelect: () => void;
  'data-testid'?: string;
}

function ForkCard({
  selected,
  icon,
  title,
  subtitle,
  onSelect,
  ...rest
}: ForkCardProps) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={onSelect}
      className={[
        'text-left p-4 rounded-lg border transition-all',
        selected
          ? 'border-primary bg-primary/5 ring-2 ring-primary/30'
          : 'border-input bg-background hover:border-primary/50',
      ].join(' ')}
      data-testid={rest['data-testid']}
    >
      <div className="flex items-center gap-2 mb-1">
        {icon}
        <h3 className="font-bold text-sm">{title}</h3>
      </div>
      <p className="text-xs text-muted-foreground">{subtitle}</p>
    </button>
  );
}
