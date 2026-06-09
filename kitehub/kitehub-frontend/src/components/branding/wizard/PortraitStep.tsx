'use client';

/**
 * GAP-1116 — Step 3 Portrait upload: collect 1..N teacher/staff headshots.
 *
 * The banner-compose layer (thesis 3-layer design: text + portrait + icon) uses
 * portraits as the central asset. The number of portraits depends on the
 * `orgType` axis chosen in Step 1 (GAP-1115): a solo teacher uploads ~1, a
 * centre uploads several. The count is a HINT only — the step accepts any
 * number (including zero, since AI can still compose a banner without portraits).
 *
 * Mirrors `LogoStep`'s upload + asset-library picker pattern, adapted for
 * `assetType=PORTRAIT` (multiple allowed; LOGO is replace-by-type, PORTRAIT
 * is additive). Existing portraits are listed via `useAssets(instanceId)`
 * filtered to `type === 'PORTRAIT'` and re-uploads accumulate (the BE keeps
 * every PORTRAIT row).
 *
 * Portraits live as server-side `BrandingAsset` rows — they are NOT stored in
 * WizardState (WizardState holds wizard DECISIONS; assets are server truth read
 * via `useAssets`). The generate job reads PORTRAIT assets BE-side.
 */

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  ArrowLeft,
  ArrowRight,
  AlertCircle,
  Users,
  Image as ImageIcon,
  FolderOpen,
  Check,
} from 'lucide-react';
import { useUploadAsset, useAssets } from '@/hooks/use-branding';
import { toast } from 'sonner';
import {
  WizardCard,
  WizardStepHeader,
  portraitCountHint,
  ORG_TYPE_OPTIONS,
  type PortraitStepProps,
} from './wizard-shared';

const MAX_FILE_BYTES = 2 * 1024 * 1024; // 2MB — same bar as logo upload
const ACCEPTED_MIME = ['image/png', 'image/jpeg', 'image/webp'];

export function PortraitStep({
  wizardState,
  instanceId,
  onNext,
  onBack,
}: PortraitStepProps) {
  const { orgType } = wizardState;
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const uploadMutation = useUploadAsset();

  // List existing PORTRAIT assets for this instance (additive, 1..N). The
  // gallery reflects every upload after the mutation invalidates the query.
  const { data: existingAssets } = useAssets(instanceId);
  const portraitAssets = (existingAssets ?? []).filter((a) => a.type === 'PORTRAIT');

  const hint = portraitCountHint(orgType);
  const isSolo = orgType === 'SOLO_TEACHER';
  const orgLabel = ORG_TYPE_OPTIONS.find((o) => o.id === orgType)?.label;

  const validateFile = (file: File): string | null => {
    if (!ACCEPTED_MIME.includes(file.type)) {
      return 'Chỉ chấp nhận file PNG, JPG, hoặc WEBP.';
    }
    if (file.size > MAX_FILE_BYTES) {
      return 'Mỗi ảnh không được vượt quá 2MB.';
    }
    return null;
  };

  const uploadOne = async (file: File) => {
    const err = validateFile(file);
    if (err) {
      setErrorMsg(err);
      return;
    }
    try {
      await uploadMutation.mutateAsync({ instanceId, type: 'PORTRAIT', file });
    } catch {
      setErrorMsg('Tải ảnh chân dung lên thất bại. Vui lòng thử lại.');
    }
  };

  const handleFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    setErrorMsg(null);
    // Upload sequentially so a single failure doesn't abort the rest.
    let uploaded = 0;
    for (const file of Array.from(files)) {
      // eslint-disable-next-line no-await-in-loop
      await uploadOne(file);
      uploaded += 1;
    }
    if (uploaded > 0) {
      toast.success(
        uploaded === 1 ? 'Đã tải lên 1 ảnh chân dung!' : `Đã tải lên ${uploaded} ảnh chân dung!`
      );
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    void handleFiles(e.target.files);
    // Reset so re-selecting the same file re-triggers change.
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    void handleFiles(e.dataTransfer.files);
  };

  return (
    <div className="space-y-6">
      <WizardCard>
        <WizardStepHeader
          eyebrow="Bước 3 / 7 · Tuỳ chọn"
          title="Thêm ảnh chân dung"
          subtitle="Ảnh chân dung giáo viên là lớp trung tâm của banner. Bạn có thể bỏ qua — AI vẫn tạo được banner không cần ảnh."
        />

        {/* Count hint driven by orgType (GAP-1115 → GAP-1116) */}
        <div
          data-testid="wizard-portrait-hint"
          className="mb-5 flex items-start gap-3 rounded-md border border-sky-200 bg-sky-50 p-3 dark:border-sky-900 dark:bg-sky-950/30"
        >
          <Users className="mt-0.5 h-5 w-5 shrink-0 text-sky-600 dark:text-sky-400" aria-hidden="true" />
          <div className="text-sm text-sky-900 dark:text-sky-200">
            {orgType ? (
              isSolo ? (
                <p>
                  Bạn chọn <strong>{orgLabel}</strong> — nên tải lên{' '}
                  <strong>1 ảnh chân dung</strong> của chính bạn.
                </p>
              ) : (
                <p>
                  Bạn chọn <strong>{orgLabel}</strong> — nên tải lên{' '}
                  <strong>khoảng {hint} ảnh</strong> chân dung của các giáo viên/nhân
                  viên.
                </p>
              )
            ) : (
              <p>Tải lên 1 hoặc nhiều ảnh chân dung của giáo viên để giới thiệu đội ngũ.</p>
            )}
          </div>
        </div>

        {/* Uploaded portraits gallery (GAP-1116 — additive 1..N) */}
        {portraitAssets.length > 0 && (
          <div className="mb-5" data-testid="wizard-portrait-gallery">
            <div className="mb-3 flex items-center gap-2">
              <ImageIcon className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
              <h3 className="text-sm font-semibold">
                Ảnh đã tải lên ({portraitAssets.length})
              </h3>
            </div>
            <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
              {portraitAssets.map((asset) => (
                <div
                  key={asset.id}
                  data-testid={`wizard-portrait-tile-${asset.id}`}
                  className="relative flex aspect-square items-center justify-center rounded-lg border border-input bg-background p-2"
                >
                  <img
                    src={asset.url}
                    alt="Ảnh chân dung đã tải lên"
                    className="max-h-full max-w-full rounded object-contain"
                  />
                  <span className="absolute right-1 top-1 rounded-full bg-emerald-500 p-0.5 text-white">
                    <Check className="h-3 w-3" aria-hidden="true" />
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Drop zone — add one or many portraits */}
        <label className="block cursor-pointer" data-testid="wizard-portrait-drop">
          <div
            onDragOver={(e) => {
              e.preventDefault();
              e.stopPropagation();
            }}
            onDrop={handleDrop}
            className="rounded-lg border-2 border-dashed border-muted-foreground/25 p-8 text-center transition-colors hover:border-primary"
          >
            <ImageIcon className="mx-auto mb-3 h-10 w-10 text-muted-foreground" aria-hidden="true" />
            <h3 className="mb-1 text-base font-bold">Kéo thả ảnh vào đây</h3>
            <p className="mb-3 text-sm text-muted-foreground">
              hoặc bấm để chọn từ máy tính {isSolo ? '' : '(chọn được nhiều ảnh)'}
            </p>
            <span className="inline-flex items-center gap-2 rounded-md bg-secondary px-4 py-2 text-sm font-medium text-secondary-foreground">
              <FolderOpen className="h-4 w-4" aria-hidden="true" />
              Chọn ảnh
            </span>
            <p className="mt-3 text-xs text-muted-foreground">
              PNG · JPG · WEBP · tối đa 2MB mỗi ảnh
            </p>
          </div>
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            multiple={!isSolo}
            onChange={handleFileInput}
            className="hidden"
            data-testid="wizard-portrait-file-input"
          />
        </label>

        {uploadMutation.isPending && (
          <p role="status" className="mt-2 text-center text-xs text-muted-foreground">
            Đang tải lên…
          </p>
        )}

        {errorMsg && (
          <div
            role="alert"
            data-testid="wizard-portrait-error"
            className="mt-3 flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive"
          >
            <AlertCircle className="h-4 w-4" aria-hidden="true" />
            <span>{errorMsg}</span>
          </div>
        )}
      </WizardCard>

      {/* Footer actions */}
      <div className="mx-auto flex max-w-2xl items-center justify-between px-1">
        <Button variant="ghost" onClick={onBack} data-testid="wizard-step3-back">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground">
          Bước 3 / 7 · Tuỳ chọn — bỏ qua nếu chưa có ảnh
        </p>
        <Button
          onClick={onNext}
          disabled={uploadMutation.isPending}
          data-testid="wizard-step3-continue"
        >
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
