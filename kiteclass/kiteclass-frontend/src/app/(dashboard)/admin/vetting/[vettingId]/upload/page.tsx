/**
 * LLTP / CCCD / police-check evidence upload form for a Vetting record.
 *
 * Phase 1B remainder (Wave 18b3 — GAP-322b): single-file upload v1.
 * Resumable upload (5MB+ multipart parts), virus-scan webhook, document
 * deletion / replace, audit-log entries on upload — all deferred to Phase 1C
 * follow-up sister gaps.
 *
 * RBAC: server-enforced via {@code X-User-Roles: SAFEGUARDING_OFFICER}
 * forwarded by Gateway. This page does NOT pre-check role client-side —
 * relies on 403 surfaced as ErrorAlert (no information leak).
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b3 — GAP-322b Phase 1B remainder)
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useState } from 'react';
import { ArrowLeft, FileUp, Loader2, ShieldCheck } from 'lucide-react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ErrorAlert } from '@/components/common/error-alert';
import { DashboardLayout } from '@/components/layout';
import { vettingApi, type VettingDocumentResponse } from '@/lib/api/vetting';

const MAX_BYTES = 10 * 1024 * 1024; // 10MB — matches server cap
const ACCEPTED_TYPES = 'application/pdf,image/jpeg,image/png,image/webp';

export default function VettingDocumentUploadPage() {
  const params = useParams();
  const rawId = params?.vettingId;
  const vettingIdNum = Array.isArray(rawId) ? Number(rawId[0]) : Number(rawId);
  const vettingId =
    Number.isFinite(vettingIdNum) && vettingIdNum > 0 ? vettingIdNum : undefined;

  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [result, setResult] = useState<VettingDocumentResponse | null>(null);

  if (vettingId === undefined) {
    return (
      <DashboardLayout>
      <div className="mx-auto max-w-2xl p-6">
        <ErrorAlert
          title="ID hồ sơ không hợp lệ"
          message="Không thể xác định bản ghi vetting cần upload tài liệu."
          backHref="/admin"
          backLabel="Về trang quản trị"
        />
      </div>
      </DashboardLayout>
    );
  }

  const onFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setErrorMessage(null);
    setResult(null);
    const f = e.target.files?.[0] ?? null;
    if (f && f.size > MAX_BYTES) {
      setErrorMessage(
        `Tệp vượt quá 10MB (kích thước thực tế ${(f.size / 1024 / 1024).toFixed(
          1,
        )}MB).`,
      );
      setFile(null);
      return;
    }
    setFile(f);
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setErrorMessage('Vui lòng chọn tệp trước khi tải lên.');
      return;
    }
    setSubmitting(true);
    setErrorMessage(null);
    try {
      const resp = await vettingApi.uploadDocument(vettingId, file);
      setResult(resp);
    } catch (err) {
      const message =
        err instanceof Error
          ? err.message
          : 'Không thể tải lên tài liệu. Vui lòng thử lại.';
      setErrorMessage(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <DashboardLayout>
    <div className="mx-auto max-w-2xl space-y-6 p-6">
      <header>
        <Button asChild variant="ghost" size="sm">
          <Link href="/admin">
            <ArrowLeft className="mr-1 h-4 w-4" />
            Quay lại quản trị
          </Link>
        </Button>
        <h1 className="mt-2 flex items-center gap-2 text-3xl font-bold">
          <ShieldCheck className="h-8 w-8 text-primary" />
          Tải lên tài liệu xác minh
        </h1>
        <p className="text-muted-foreground">
          Upload Lý lịch tư pháp số 2 / CCCD / kết quả phỏng vấn — hồ sơ vetting #
          {vettingId}.
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle>Thông tin tài liệu</CardTitle>
          <CardDescription>
            Tệp PDF hoặc ảnh (JPG/PNG/WebP), tối đa 10MB. Chỉ Cán bộ bảo vệ trẻ
            em (SAFEGUARDING_OFFICER) mới có quyền upload.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="vetting-document-file">Chọn tệp</Label>
              <Input
                id="vetting-document-file"
                data-testid="vetting-file-input"
                type="file"
                accept={ACCEPTED_TYPES}
                onChange={onFileChange}
                disabled={submitting}
              />
              {file && (
                <p className="text-sm text-muted-foreground">
                  Đã chọn: <span className="font-medium">{file.name}</span> (
                  {(file.size / 1024).toFixed(0)} KB)
                </p>
              )}
            </div>

            {errorMessage && (
              <div
                className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive"
                role="alert"
              >
                {errorMessage}
              </div>
            )}

            {result && (
              <div
                className="rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900"
                role="status"
                data-testid="vetting-upload-success"
              >
                <p className="font-medium">Tải lên thành công.</p>
                <p className="mt-1 break-all">
                  Khoá lưu trữ:{' '}
                  <code className="text-xs">{result.storageKey}</code>
                </p>
                <p>Kích thước: {result.sizeBytes} bytes</p>
              </div>
            )}

            <div className="flex justify-end gap-2">
              <Button
                type="submit"
                disabled={!file || submitting}
                data-testid="vetting-upload-submit"
              >
                {submitting ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <FileUp className="mr-2 h-4 w-4" />
                )}
                {submitting ? 'Đang tải lên…' : 'Tải lên'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
    </DashboardLayout>
  );
}
