/**
 * Create student page.
 *
 * Wave 30 Bucket C — Phase 4 KC pro v2 port (GAP-266):
 *  - Adds bulk-import path via @kite/shared-ui `BulkImportDropzone` (G1,
 *    Wave 29 Bucket A) alongside the existing single-student form.
 *  - Tab-style switch between "Thêm 1 học viên" and "Nhập hàng loạt".
 *  - The dropzone is presentational (per G1 spec); commit/parsing wiring is
 *    a follow-up gap (Phase 4 polish).  This PR wires the UI surface so the
 *    dropzone renders in the production app.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { BulkImportDropzone, type ImportJobStatus } from '@kite/shared-ui';
import { DashboardLayout } from '@/components/layout';
import { StudentForm } from '@/components/forms/dynamic-student-form';
import { useCreateStudent } from '@/hooks/use-students';
import { Button } from '@/components/ui/button';
import type { CreateStudentRequest, UpdateStudentRequest } from '@/types/student';

type Mode = 'single' | 'bulk';

export default function NewStudentPage() {
  const createMutation = useCreateStudent();
  const [mode, setMode] = useState<Mode>('single');
  const [importStatus, setImportStatus] = useState<ImportJobStatus>('idle');
  const [fileName, setFileName] = useState<string | undefined>(undefined);

  const handleSubmit = (data: CreateStudentRequest | UpdateStudentRequest) => {
    createMutation.mutate(data as CreateStudentRequest);
  };

  const handleFileSelect = (file: File) => {
    setFileName(file.name);
    setImportStatus('parsing');
    // Real parse + commit wiring lands in follow-up gap (Wave 31+ polish);
    // this page validates dropzone integration only.
  };

  const handleCommit = () => {
    setImportStatus('done');
  };

  const handleClose = () => {
    setImportStatus('idle');
    setFileName(undefined);
  };

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-4xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Thêm học viên mới</h1>
          <p className="text-muted-foreground">
            Nhập thông tin học viên để tạo hồ sơ mới
          </p>
        </div>

        <div className="flex gap-2 border-b" role="tablist" aria-label="Phương thức thêm học viên">
          <Button
            type="button"
            role="tab"
            aria-selected={mode === 'single'}
            variant={mode === 'single' ? 'default' : 'ghost'}
            onClick={() => setMode('single')}
          >
            Thêm 1 học viên
          </Button>
          <Button
            type="button"
            role="tab"
            aria-selected={mode === 'bulk'}
            variant={mode === 'bulk' ? 'default' : 'ghost'}
            onClick={() => setMode('bulk')}
          >
            Nhập hàng loạt
          </Button>
        </div>

        {createMutation.isError && mode === 'single' && (
          <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
            <p className="text-sm text-destructive">
              Không thể tạo học viên. Vui lòng kiểm tra lại thông tin và thử lại.
            </p>
          </div>
        )}

        {mode === 'single' && (
          <div className="rounded-lg border bg-card p-6">
            <StudentForm
              onSubmit={handleSubmit}
              isSubmitting={createMutation.isPending}
            />
          </div>
        )}

        {mode === 'bulk' && (
          <div data-testid="students-bulk-import">
            <BulkImportDropzone
              status={importStatus}
              tenantLabel="Trung tâm"
              fileName={fileName}
              onFileSelect={handleFileSelect}
              onCommit={handleCommit}
              onClose={handleClose}
              onCancel={handleClose}
            />
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
