/**
 * Bulk-enroll wizard panel (GAP-1104) — the testable content of the
 * `/classes/[id]/bulk-enroll` page, decoupled from Next.js `use(params)` so it
 * can be unit-tested directly (the page-level `use(params)` is RTL-incompatible).
 *
 * Mirrors the admin student bulk-import wizard UX. The target class is resolved
 * per-row by the {@code class_code} column in the uploaded file (canonical), so a
 * single file can enrol into several classes. {@code classId} is used only for
 * navigation context.
 *
 * @author KiteClass Team
 * @since 3.x (Wave KC enrollment)
 */

'use client';

import { useCallback, useRef, useState } from 'react';
import Link from 'next/link';
import { AxiosError } from 'axios';
import {
  Upload,
  FileSpreadsheet,
  AlertCircle,
  CheckCircle2,
  Download,
  RefreshCw,
  ArrowLeft,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
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
import { enrollmentBulkApi } from '@/lib/api/enrollment-bulk';
import type {
  EnrollmentBulkPhase,
  EnrollmentBulkResult,
} from '@/types/enrollment-bulk';

const ACCEPT_MIME =
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,.xlsx';

/** Conservative client-side cap matching the BE Spring Boot 10MB upload default. */
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

interface BulkEnrollPanelProps {
  classId: number;
}

export function BulkEnrollPanel({ classId }: BulkEnrollPanelProps) {
  const [file, setFile] = useState<File | null>(null);
  const [phase, setPhase] = useState<EnrollmentBulkPhase>('idle');
  const [result, setResult] = useState<EnrollmentBulkResult | null>(null);
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

  const handleDownloadTemplate = useCallback(async () => {
    try {
      const blob = await enrollmentBulkApi.downloadTemplate();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'mau-import-ghi-danh.xlsx';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      toast({
        title: 'Lỗi tải template',
        description: extractErrorMessage(err, 'Không thể tải file mẫu'),
        variant: 'destructive',
      });
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
      if (!picked.name.toLowerCase().endsWith('.xlsx')) {
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

  const handlePreview = useCallback(async () => {
    if (!file) return;
    setPhase('previewing');
    setErrorMessage(null);
    try {
      const data = await enrollmentBulkApi.preview(file);
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
      const data = await enrollmentBulkApi.commit(file);
      setResult(data);
      setPhase('committed');
      toast({
        title: 'Ghi danh hàng loạt hoàn tất',
        description: `Đã ghi danh ${data.successCount}/${data.totalRows} lượt (${data.errorCount} lỗi).`,
      });
    } catch (err) {
      const msg = extractErrorMessage(err, 'Không thể xác nhận ghi danh');
      setErrorMessage(msg);
      setPhase('error');
      toast({
        title: 'Lỗi xác nhận ghi danh',
        description: msg,
        variant: 'destructive',
      });
    }
  }, [file]);

  const isBusy = phase === 'previewing' || phase === 'committing';

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Link
              href={`/classes/${classId}`}
              className="text-sm text-muted-foreground hover:text-foreground inline-flex items-center gap-1"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại lớp học
            </Link>
          </div>
          <h1 className="text-3xl font-bold mt-1">Ghi danh hàng loạt</h1>
          <p className="text-muted-foreground">
            Tải lên tệp Excel (.xlsx) để ghi danh nhiều học sinh vào lớp cùng lúc.
          </p>
        </div>
      </div>

      {/* Step 1 — template + upload */}
      <Card>
        <CardHeader>
          <CardTitle>Bước 1 — Chuẩn bị tệp</CardTitle>
          <CardDescription>
            Tải template mẫu, điền dữ liệu rồi tải lên. Cột bắt buộc:{' '}
            <strong>class_code</strong> và <strong>tuition_amount</strong>; cần{' '}
            <strong>student_email</strong> hoặc <strong>student_phone</strong> để xác
            định học sinh. Cột tuỳ chọn: discount_percent (0-100), note. Mỗi dòng ghi
            danh vào lớp theo <strong>class_code</strong> trong tệp.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button variant="outline" onClick={handleDownloadTemplate}>
            <Download className="mr-2 h-4 w-4" />
            Tải template mẫu (.xlsx)
          </Button>

          <div className="flex flex-wrap items-center gap-3">
            <label
              htmlFor="bulk-enroll-file-input"
              className="inline-flex cursor-pointer items-center gap-2 rounded-md border border-dashed border-input bg-background px-4 py-3 text-sm hover:bg-accent"
            >
              <Upload className="h-4 w-4" />
              {file ? 'Đổi tệp khác' : 'Chọn tệp .xlsx'}
              <input
                id="bulk-enroll-file-input"
                ref={fileInputRef}
                type="file"
                accept={ACCEPT_MIME}
                className="sr-only"
                onChange={handleFileChange}
                disabled={isBusy}
                aria-label="Chọn tệp xlsx để ghi danh hàng loạt"
              />
            </label>
            {file && (
              <span className="inline-flex items-center gap-2 text-sm text-muted-foreground">
                <FileSpreadsheet className="h-4 w-4" />
                <span data-testid="bulk-enroll-file-name">{file.name}</span>
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
            <div data-testid="bulk-enroll-progress">
              <Progress value={undefined} />
            </div>
          )}
        </CardContent>
      </Card>

      {/* Error */}
      {phase === 'error' && errorMessage && (
        <Alert variant="destructive" data-testid="bulk-enroll-error">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Lỗi</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      )}

      {/* Step 2 — preview / result */}
      {result && (phase === 'previewed' || phase === 'committing' || phase === 'committed') && (
        <Card data-testid="bulk-enroll-preview">
          <CardHeader>
            <CardTitle>
              Bước 2 — {phase === 'committed' ? 'Kết quả ghi danh' : 'Xem trước'}
            </CardTitle>
            <CardDescription>
              Tổng <strong>{result.totalRows}</strong> dòng — hợp lệ{' '}
              <strong className="text-green-600">{result.successCount}</strong> — lỗi{' '}
              <strong className="text-destructive">{result.errorCount}</strong>.
              {result.errorCount > 10 && <> Hiển thị 10 lỗi đầu tiên.</>}
              {phase === 'previewed' && (
                <> Một số lỗi nghiệp vụ (lớp đầy, đã ghi danh) chỉ được kiểm tra khi xác nhận.</>
              )}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {result.errors.length > 0 ? (
              <div className="overflow-x-auto rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-20">Dòng</TableHead>
                      <TableHead className="w-40">Cột</TableHead>
                      <TableHead>Mô tả lỗi</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {result.errors.map((err) => (
                      <TableRow
                        key={`${err.rowNumber}-${err.field}-${err.message}`}
                        data-testid={`bulk-enroll-error-row-${err.rowNumber}`}
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
                  Tất cả {result.totalRows} dòng đã sẵn sàng để{' '}
                  {phase === 'committed' ? 'ghi danh' : 'xác nhận'}.
                </AlertDescription>
              </Alert>
            )}

            <div className="flex flex-wrap items-center gap-2">
              {phase === 'previewed' && (
                <Button onClick={handleCommit} disabled={result.successCount === 0}>
                  <CheckCircle2 className="mr-2 h-4 w-4" />
                  Xác nhận ghi danh ({result.successCount} dòng)
                </Button>
              )}
              {phase === 'committed' && (
                <Link href={`/classes/${classId}`}>
                  <Button variant="ghost">Về trang lớp học</Button>
                </Link>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
