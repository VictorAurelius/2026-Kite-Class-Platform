/**
 * Admin Bulk Import page — Nhập học viên hàng loạt từ XLSX.
 *
 * Wave 60 Bucket B — GAP-137: closes the gap where Wave 1 BE (PR #332) shipped
 * /api/v1/students/bulk-import/{preview,commit,jobs/{id}/errors} but no FE
 * surface existed, leaving the feature user-inaccessible.
 *
 * Flow (state machine — 詳細設計):
 *
 *   idle ──pick xlsx──> selected ──[Xem trước]──> previewing ──ok──> previewed
 *                                                                        │
 *                                                                        ├──[Xác nhận nhập]──> committing ──ok──> committed
 *                                                                        │                                            │
 *                                                                        │                                            └──[Tải báo cáo lỗi]──> download
 *                                                                        │
 *                                                                        └──[Đặt lại]──> idle
 *
 * 4-layer coverage (`.claude/rules/design-layer-coverage.md` §2.1):
 *   - 要件定義: persona P2 Center Owner + use-case `documents/01-business/kiteclass/bulk-import/use-cases.md`
 *   - 基本設計: this page (single-screen wizard, no kit mockup needed for admin-internal)
 *   - 詳細設計: state machine above + BE contract in `documents/01-business/kiteclass/bulk-import/api-contract.md`
 *   - コンポーネント設計: composes shadcn Button, Card, Table, Alert, Progress (existing primitives)
 *
 * @author KiteClass Team
 * @since 3.60.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { useCallback, useRef, useState } from 'react';
import { Upload, FileSpreadsheet, AlertCircle, CheckCircle2, Download, RefreshCw, ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { AxiosError } from 'axios';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Progress } from '@/components/ui/progress';
import { toast } from '@/hooks/use-toast';
import { bulkImportApi } from '@/lib/api/bulk-import';
import type { BulkImportResult, BulkImportPhase } from '@/types/bulk-import';

const ACCEPT_MIME =
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,.xlsx';

/**
 * Max upload size — matches BE Spring Boot default 10MB upload cap.
 * Kept conservative to surface client-side error before network round-trip.
 */
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof AxiosError) {
    const data = err.response?.data as { message?: string; error?: string } | undefined;
    return data?.message || data?.error || err.message || fallback;
  }
  if (err instanceof Error) {
    return err.message;
  }
  return fallback;
}

export default function BulkImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const [phase, setPhase] = useState<BulkImportPhase>('idle');
  const [result, setResult] = useState<BulkImportResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const reset = useCallback(() => {
    setFile(null);
    setPhase('idle');
    setResult(null);
    setErrorMessage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, []);

  const handleFileChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const picked = e.target.files?.[0];
      if (!picked) {
        return;
      }
      if (picked.size > MAX_FILE_SIZE_BYTES) {
        setErrorMessage(
          `Tệp vượt quá ${(MAX_FILE_SIZE_BYTES / 1024 / 1024).toFixed(0)} MB. Vui lòng chia nhỏ tệp.`,
        );
        setPhase('error');
        return;
      }
      // Loose extension check; BE does strict content validation.
      const lowered = picked.name.toLowerCase();
      if (!lowered.endsWith('.xlsx')) {
        setErrorMessage('Chỉ chấp nhận tệp .xlsx (Excel 2007+).');
        setPhase('error');
        return;
      }
      setFile(picked);
      setPhase('selected');
      setResult(null);
      setErrorMessage(null);
    },
    [],
  );

  const handleDownloadTemplate = useCallback(async () => {
    try {
      const blob = await bulkImportApi.downloadTemplate();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'mau-import-hoc-vien.xlsx';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      toast({
        title: 'Lỗi tải template',
        description: extractErrorMessage(err, 'Không thể tải template mẫu'),
        variant: 'destructive',
      });
    }
  }, []);

  const handlePreview = useCallback(async () => {
    if (!file) return;
    setPhase('previewing');
    setErrorMessage(null);
    try {
      const data = await bulkImportApi.preview(file);
      setResult(data);
      setPhase('previewed');
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, 'Không thể xem trước tệp'));
      setPhase('error');
    }
  }, [file]);

  const handleCommit = useCallback(async () => {
    if (!file) return;
    setPhase('committing');
    setErrorMessage(null);
    try {
      const data = await bulkImportApi.commit(file);
      setResult(data);
      setPhase('committed');
      toast({
        title: 'Nhập hàng loạt hoàn tất',
        description: `Đã tạo ${data.successCount}/${data.totalRows} học viên (${data.errorCount} lỗi).`,
      });
    } catch (err) {
      const msg = extractErrorMessage(err, 'Không thể xác nhận nhập');
      setErrorMessage(msg);
      setPhase('error');
      toast({
        title: 'Lỗi xác nhận nhập',
        description: msg,
        variant: 'destructive',
      });
    }
  }, [file]);

  const handleDownloadErrors = useCallback(async () => {
    if (!file || !result?.jobId) return;
    try {
      const blob = await bulkImportApi.downloadErrorReport(result.jobId, file);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `bulk-import-errors-${result.jobId}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      toast({
        title: 'Lỗi tải báo cáo',
        description: extractErrorMessage(err, 'Không thể tải báo cáo lỗi'),
        variant: 'destructive',
      });
    }
  }, [file, result]);

  const isBusy = phase === 'previewing' || phase === 'committing';

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <Link
                href="/students"
                className="text-sm text-muted-foreground hover:text-foreground inline-flex items-center gap-1"
              >
                <ArrowLeft className="h-4 w-4" />
                Học viên
              </Link>
            </div>
            <h1 className="text-3xl font-bold mt-1">Nhập học viên hàng loạt</h1>
            <p className="text-muted-foreground">
              Tải lên tệp Excel (.xlsx) để nhập nhiều học viên cùng lúc.
            </p>
          </div>
        </div>

        {/* Step 1 — upload */}
        <Card>
          <CardHeader>
            <CardTitle>Bước 1 — Chọn tệp</CardTitle>
            <CardDescription>
              Tệp xlsx tối đa {(MAX_FILE_SIZE_BYTES / 1024 / 1024).toFixed(0)} MB. Cột bắt buộc:{' '}
              <strong>name</strong>, <strong>email</strong>. Cột tuỳ chọn: phone, dateOfBirth
              (dd/MM/yyyy), gender (MALE/FEMALE/OTHER), address, note.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Template download — grab BEFORE filling in data */}
            <div className="space-y-1">
              <Button variant="outline" onClick={handleDownloadTemplate}>
                <Download className="mr-2 h-4 w-4" />
                Tải template mẫu (.xlsx)
              </Button>
              <p className="text-sm text-muted-foreground">
                Chưa biết định dạng? Tải template mẫu rồi điền theo.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <label
                htmlFor="bulk-import-file-input"
                className="inline-flex cursor-pointer items-center gap-2 rounded-md border border-dashed border-input bg-background px-4 py-3 text-sm hover:bg-accent"
              >
                <Upload className="h-4 w-4" />
                {file ? 'Đổi tệp khác' : 'Chọn tệp .xlsx'}
                <input
                  id="bulk-import-file-input"
                  ref={fileInputRef}
                  type="file"
                  accept={ACCEPT_MIME}
                  className="sr-only"
                  onChange={handleFileChange}
                  disabled={isBusy}
                  aria-label="Chọn tệp xlsx để nhập hàng loạt"
                />
              </label>
              {file && (
                <span className="inline-flex items-center gap-2 text-sm text-muted-foreground">
                  <FileSpreadsheet className="h-4 w-4" />
                  <span data-testid="bulk-import-file-name">{file.name}</span>
                  <span>({(file.size / 1024).toFixed(1)} KB)</span>
                </span>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Button
                onClick={handlePreview}
                disabled={!file || isBusy || phase === 'previewed' || phase === 'committed'}
              >
                {phase === 'previewing' ? 'Đang xem trước…' : 'Xem trước'}
              </Button>
              {(phase !== 'idle' || file) && (
                <Button variant="ghost" onClick={reset} disabled={isBusy}>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Đặt lại
                </Button>
              )}
            </div>

            {isBusy && (
              <div data-testid="bulk-import-progress">
                <Progress value={undefined} />
              </div>
            )}
          </CardContent>
        </Card>

        {/* Error */}
        {phase === 'error' && errorMessage && (
          <Alert variant="destructive" data-testid="bulk-import-error">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Lỗi</AlertTitle>
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}

        {/* Step 2 — preview */}
        {result && (phase === 'previewed' || phase === 'committing' || phase === 'committed') && (
          <Card data-testid="bulk-import-preview">
            <CardHeader>
              <CardTitle>
                Bước 2 — {phase === 'committed' ? 'Kết quả nhập' : 'Xem trước'}
              </CardTitle>
              <CardDescription>
                Tổng <strong>{result.totalRows}</strong> hàng — hợp lệ{' '}
                <strong className="text-green-600">{result.successCount}</strong> — lỗi{' '}
                <strong className="text-destructive">{result.errorCount}</strong>.
                {result.errorCount > 10 && (
                  <> Hiển thị 10 lỗi đầu tiên; tải báo cáo bên dưới để xem toàn bộ.</>
                )}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {result.errors.length > 0 ? (
                <div className="overflow-x-auto rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-20">Hàng</TableHead>
                        <TableHead className="w-32">Cột</TableHead>
                        <TableHead>Mô tả lỗi</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {result.errors.map((err) => (
                        <TableRow
                          key={`${err.rowNumber}-${err.field}`}
                          data-testid={`bulk-import-error-row-${err.rowNumber}`}
                        >
                          <TableCell>{err.rowNumber}</TableCell>
                          <TableCell className="font-mono text-xs">{err.field}</TableCell>
                          <TableCell>{err.message}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ) : (
                <Alert>
                  <CheckCircle2 className="h-4 w-4" />
                  <AlertTitle>Không có lỗi</AlertTitle>
                  <AlertDescription>
                    Tất cả {result.totalRows} hàng đã sẵn sàng để{' '}
                    {phase === 'committed' ? 'lưu' : 'nhập'}.
                  </AlertDescription>
                </Alert>
              )}

              <div className="flex flex-wrap items-center gap-2">
                {phase === 'previewed' && (
                  <Button onClick={handleCommit} disabled={result.successCount === 0}>
                    <CheckCircle2 className="mr-2 h-4 w-4" />
                    Xác nhận nhập ({result.successCount} hàng)
                  </Button>
                )}
                {phase === 'committed' && result.errorCount > 0 && (
                  <Button variant="outline" onClick={handleDownloadErrors}>
                    <Download className="mr-2 h-4 w-4" />
                    Tải báo cáo lỗi (xlsx)
                  </Button>
                )}
                {phase === 'committed' && (
                  <Link href="/students">
                    <Button variant="ghost">Về danh sách học viên</Button>
                  </Link>
                )}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}
