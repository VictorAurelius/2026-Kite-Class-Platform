'use client';

import { useState, useRef } from 'react';
import { Button } from '@/components/ui/button';
import {
  UploadCloud,
  Sparkles,
  FolderOpen,
  AlertTriangle,
  Check,
  CheckCircle,
  RefreshCw,
  Trash2,
  ArrowLeft,
  ArrowRight,
} from 'lucide-react';
import { toast } from 'sonner';
import { useUploadAsset } from '@/hooks/use-branding';
import { WizardCard, WizardStepHeader } from './wizard-shared';

// ---------------------------------------------------------------------------
// Sub-state machine
// ---------------------------------------------------------------------------

type LogoMode = 'upload' | 'ai-generate';
type UploadState = 'idle' | 'uploading' | 'uploaded' | 'error';

interface UploadedFile {
  name: string;
  size: number;
  mimeType: string;
  previewUrl: string;
  /** Remote URL after successful upload. */
  remoteUrl: string;
}

interface UploadError {
  fileName: string;
  reason: string;
}

// File validation constants (per spec §constraints)
const MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB
const ALLOWED_MIME_TYPES = new Set([
  'image/svg+xml',
  'image/png',
  'image/jpeg',
]);

// ---------------------------------------------------------------------------
// LogoStep
// ---------------------------------------------------------------------------

export interface LogoStepData {
  /** Null means user chose AI-generated logo. */
  logoUrl: string | null;
  aiLogo: boolean;
}

interface LogoStepProps {
  instanceId: string;
  initialData?: Partial<LogoStepData>;
  onNext: (data: LogoStepData) => void;
  onBack: () => void;
}

/**
 * Step 2 — Logo upload (optional).
 *
 * Sub-states: default | uploading | uploaded | error | skip (ai-generate)
 * Spec: `ai-branding-wizard-v2/screens/step2-logo-{default,uploaded,skip,error}.html`
 * Rule: ai-branding-guidelines.md §1 (resource classification) + §4.1 (wizard pattern)
 */
export function LogoStep({ instanceId, initialData, onNext, onBack }: LogoStepProps) {
  const [mode, setMode] = useState<LogoMode>(
    initialData?.aiLogo ? 'ai-generate' : 'upload',
  );
  const [uploadState, setUploadState] = useState<UploadState>('idle');
  const [uploadedFile, setUploadedFile] = useState<UploadedFile | null>(null);
  const [uploadError, setUploadError] = useState<UploadError | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const uploadMutation = useUploadAsset();

  // ---------------------------------------------------------------------------
  // File handling
  // ---------------------------------------------------------------------------

  const validateFile = (file: File): string | null => {
    if (!ALLOWED_MIME_TYPES.has(file.type)) {
      return `${file.name} — định dạng không hỗ trợ.`;
    }
    if (file.size > MAX_SIZE_BYTES) {
      return `${file.name} — file quá lớn (tối đa 2 MB).`;
    }
    return null;
  };

  const processFile = async (file: File) => {
    const validationError = validateFile(file);
    if (validationError) {
      setUploadState('error');
      setUploadError({ fileName: file.name, reason: validationError });
      return;
    }

    // Create local preview URL
    const previewUrl = URL.createObjectURL(file);

    try {
      setUploadState('uploading');
      const asset = await uploadMutation.mutateAsync({
        instanceId,
        type: 'LOGO',
        file,
      });
      setUploadedFile({
        name: file.name,
        size: file.size,
        mimeType: file.type,
        previewUrl,
        remoteUrl: asset.url,
      });
      setUploadState('uploaded');
      toast.success('Logo đã được tải lên thành công!');
    } catch {
      setUploadState('error');
      setUploadError({ fileName: file.name, reason: 'Tải lên thất bại. Vui lòng thử lại.' });
      URL.revokeObjectURL(previewUrl);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) processFile(file);
    // Reset input so the same file can be re-selected after error
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) processFile(file);
  };

  const handleRemove = () => {
    if (uploadedFile) URL.revokeObjectURL(uploadedFile.previewUrl);
    setUploadedFile(null);
    setUploadState('idle');
    setUploadError(null);
  };

  const handleRetry = () => {
    setUploadState('idle');
    setUploadError(null);
    fileInputRef.current?.click();
  };

  // ---------------------------------------------------------------------------
  // Navigation
  // ---------------------------------------------------------------------------

  const handleNext = () => {
    if (mode === 'ai-generate') {
      onNext({ logoUrl: null, aiLogo: true });
      return;
    }
    if (uploadState === 'uploaded' && uploadedFile) {
      onNext({ logoUrl: uploadedFile.remoteUrl, aiLogo: false });
    }
  };

  // "Continue" is enabled when: AI-generate mode selected OR upload succeeded
  const canProceed = mode === 'ai-generate' || uploadState === 'uploaded';

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------

  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 2 / 6 · Tuỳ chọn"
        title="Bạn đã có logo chưa?"
        subtitle="Bạn có thể upload logo có sẵn, hoặc để AI tự tạo logo từ tên trung tâm. Đổi sau lúc nào cũng được."
      />

      <WizardCard>
        {/* Mode fork */}
        <div className="grid grid-cols-2 gap-3 mb-6">
          <button
            type="button"
            onClick={() => setMode('upload')}
            className={[
              'rounded-lg border-2 p-4 text-left transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-primary',
              mode === 'upload'
                ? 'border-primary bg-primary/5'
                : 'border-input hover:border-primary/40',
            ].join(' ')}
            aria-pressed={mode === 'upload'}
          >
            <div className="flex items-center gap-2 mb-1">
              <UploadCloud className="w-5 h-5 text-sky-600" aria-hidden="true" />
              <span className="font-bold text-sm">Tôi có logo</span>
            </div>
            <p className="text-xs text-muted-foreground">Upload file SVG, PNG, hoặc JPG.</p>
          </button>

          <button
            type="button"
            onClick={() => setMode('ai-generate')}
            className={[
              'rounded-lg border-2 p-4 text-left transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-primary',
              mode === 'ai-generate'
                ? 'border-primary bg-primary/5'
                : 'border-input hover:border-primary/40',
            ].join(' ')}
            aria-pressed={mode === 'ai-generate'}
          >
            <div className="flex items-center gap-2 mb-1">
              <Sparkles className="w-5 h-5 text-orange-500" aria-hidden="true" />
              <span className="font-bold text-sm">Để AI tạo logo</span>
            </div>
            <p className="text-xs text-muted-foreground">AI sẽ tạo logo dựa trên tên trung tâm.</p>
          </button>
        </div>

        {/* Upload area — only shown in upload mode */}
        {mode === 'upload' && (
          <>
            {uploadState === 'uploaded' && uploadedFile ? (
              <UploadedPreview file={uploadedFile} onRemove={handleRemove} />
            ) : uploadState === 'error' && uploadError ? (
              <UploadErrorState error={uploadError} onRetry={handleRetry} />
            ) : (
              <DropZone
                isDragOver={isDragOver}
                isUploading={uploadState === 'uploading'}
                onDragOver={() => setIsDragOver(true)}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={handleDrop}
                onChooseFile={() => fileInputRef.current?.click()}
              />
            )}

            <input
              ref={fileInputRef}
              type="file"
              accept="image/svg+xml,image/png,image/jpeg"
              onChange={handleFileChange}
              className="sr-only"
              aria-label="Chọn file logo"
            />
          </>
        )}

        {/* AI generate confirmation */}
        {mode === 'ai-generate' && (
          <div className="rounded-lg bg-orange-50 dark:bg-orange-950/30 border border-orange-200 dark:border-orange-800 p-4 flex gap-3">
            <Sparkles className="w-5 h-5 text-orange-500 mt-0.5 shrink-0" aria-hidden="true" />
            <div className="text-sm text-orange-900 dark:text-orange-100">
              <p className="font-semibold mb-1">AI sẽ tạo logo cho bạn</p>
              <p>
                Dựa trên tên trung tâm và phong cách bạn chọn ở bước tiếp theo,
                AI sẽ tạo logo phù hợp. Bạn có thể thay đổi sau khi xem bản xem trước.
              </p>
            </div>
          </div>
        )}
      </WizardCard>

      {/* Footer nav */}
      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground hidden sm:block">
          Bước 2 / 6 · Tuỳ chọn — bỏ qua nếu chưa có
        </p>
        <Button onClick={handleNext} disabled={!canProceed}>
          Tiếp tục
          <ArrowRight className="ml-2 w-4 h-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

interface DropZoneProps {
  isDragOver: boolean;
  isUploading: boolean;
  onDragOver: () => void;
  onDragLeave: () => void;
  onDrop: (e: React.DragEvent) => void;
  onChooseFile: () => void;
}

function DropZone({
  isDragOver,
  isUploading,
  onDragOver,
  onDragLeave,
  onDrop,
  onChooseFile,
}: DropZoneProps) {
  return (
    <div
      role="region"
      aria-label="Khu vực kéo thả file"
      onDragOver={(e) => { e.preventDefault(); onDragOver(); }}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
      className={[
        'flex flex-col items-center justify-center rounded-xl border-2 border-dashed py-10 px-6 text-center transition-colors',
        isDragOver
          ? 'border-primary bg-primary/5'
          : 'border-muted-foreground/30 hover:border-primary/40',
      ].join(' ')}
    >
      <div className="rounded-xl bg-muted p-3 mb-3">
        <UploadCloud className="w-7 h-7 text-muted-foreground" aria-hidden="true" />
      </div>
      <h3 className="font-bold mb-1">
        {isUploading ? 'Đang tải lên…' : 'Kéo thả file vào đây'}
      </h3>
      {!isUploading && (
        <>
          <p className="text-sm text-muted-foreground mb-4">hoặc bấm để chọn từ máy tính</p>
          <Button variant="secondary" type="button" onClick={onChooseFile}>
            <FolderOpen className="w-4 h-4 mr-2" aria-hidden="true" />
            Chọn file
          </Button>
          <p className="text-xs text-muted-foreground mt-4">
            SVG · PNG · JPG · tối đa 2 MB · tỉ lệ 1:1 hoặc 4:3 ưu tiên
          </p>
        </>
      )}
    </div>
  );
}

interface UploadedPreviewProps {
  file: UploadedFile;
  onRemove: () => void;
}

function UploadedPreview({ file, onRemove }: UploadedPreviewProps) {
  const sizeKb = (file.size / 1024).toFixed(1);

  return (
    <div className="space-y-4">
      {/* Preview row */}
      <div className="flex items-center gap-4 rounded-lg border p-4">
        {/* Logo thumbnail */}
        <div className="w-16 h-16 rounded-lg bg-muted flex items-center justify-center shrink-0 overflow-hidden">
          <img
            src={file.previewUrl}
            alt="Logo đã upload"
            className="w-full h-full object-contain"
          />
        </div>

        <div className="flex-1 min-w-0">
          <p className="font-semibold truncate">{file.name}</p>
          <p className="text-xs text-muted-foreground">{file.mimeType} · {sizeKb} KB</p>
          <div className="flex gap-2 mt-3">
            <Button variant="secondary" size="sm" className="text-xs h-7" onClick={onRemove}>
              <RefreshCw className="w-3 h-3 mr-1.5" aria-hidden="true" />
              Đổi file
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="text-xs h-7 text-destructive hover:text-destructive"
              onClick={onRemove}
            >
              <Trash2 className="w-3 h-3 mr-1.5" aria-hidden="true" />
              Xoá
            </Button>
          </div>
        </div>
      </div>

      {/* Resource classification note */}
      <div className="rounded-lg bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 p-4 flex gap-3">
        <CheckCircle className="w-5 h-5 text-emerald-600 dark:text-emerald-400 mt-0.5 shrink-0" aria-hidden="true" />
        <div className="text-sm text-emerald-900 dark:text-emerald-100">
          <p className="font-semibold">Phân loại: STATIC resource</p>
          <p>
            Logo của bạn sẽ được dùng trực tiếp, không qua AI.
            AI sẽ tạo bảng màu, banner, hero phù hợp với logo này.
          </p>
        </div>
      </div>

      {/* Color extraction note */}
      <div className="rounded-lg bg-sky-50 dark:bg-sky-950/30 border border-sky-200 dark:border-sky-800 p-4 flex gap-3">
        <Check className="w-5 h-5 text-sky-600 dark:text-sky-400 mt-0.5 shrink-0" aria-hidden="true" />
        <p className="text-sm text-sky-900 dark:text-sky-100">
          Hệ thống đã trích xuất màu chính từ logo để gợi ý bảng màu — sẽ hiển thị ở Bước 5.
        </p>
      </div>
    </div>
  );
}

interface UploadErrorStateProps {
  error: UploadError;
  onRetry: () => void;
}

function UploadErrorState({ error, onRetry }: UploadErrorStateProps) {
  return (
    <div className="flex flex-col items-center text-center rounded-xl border-2 border-dashed border-destructive/50 bg-destructive/5 py-10 px-6">
      <div className="rounded-xl bg-destructive/10 p-3 mb-3">
        <AlertTriangle className="w-7 h-7 text-destructive" aria-hidden="true" />
      </div>
      <h3 className="font-bold text-destructive mb-1">Không upload được file</h3>
      <p className="text-sm text-destructive mb-4">
        <strong>{error.fileName}</strong> — {error.reason}
      </p>

      <div className="text-left max-w-xs mx-auto mb-4">
        <p className="font-semibold text-sm mb-2">Hệ thống chỉ chấp nhận:</p>
        <ul className="space-y-1 text-sm text-muted-foreground">
          <li className="flex items-center gap-2">
            <Check className="w-4 h-4 text-emerald-600" aria-hidden="true" />
            SVG (ưu tiên — sắc nét mọi kích cỡ)
          </li>
          <li className="flex items-center gap-2">
            <Check className="w-4 h-4 text-emerald-600" aria-hidden="true" />
            PNG (transparent background OK)
          </li>
          <li className="flex items-center gap-2">
            <Check className="w-4 h-4 text-emerald-600" aria-hidden="true" />
            JPG (cho ảnh chụp / nền màu đặc)
          </li>
          <li className="flex items-center gap-2 text-destructive">
            <AlertTriangle className="w-4 h-4" aria-hidden="true" />
            Tối đa 2 MB
          </li>
        </ul>
      </div>

      <Button type="button" onClick={onRetry}>
        <RefreshCw className="w-4 h-4 mr-2" aria-hidden="true" />
        Thử lại
      </Button>
    </div>
  );
}
