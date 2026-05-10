/**
 * School-admin bulk-import — uses G1 BulkImportDropzone từ @kite/shared-ui.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/bulk-import.html
 * Per ai-branding-guidelines AC: G1 imported (no inline copy).
 */

'use client';

import { useState } from 'react';
import { BulkImportDropzone, type ImportJobStatus } from '@kite/shared-ui';
import { PageHeader } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE } from '@/components/school-admin/school-admin-mock-data';

// ImportSummary type is internal to G1; re-declare here matching the
// component's expected shape (G1's index.ts exports the component but
// not this auxiliary type — see packages/shared-ui/src/index.ts).
type ImportSummary = {
  validCount: number;
  errorCount: number;
  duplicateCount: number;
  errors: never[];
};

export default function BulkImportPage() {
  const [status, setStatus] = useState<ImportJobStatus>('idle');
  const [fileName, setFileName] = useState<string | undefined>();
  const [progress, setProgress] = useState<{ processed: number; total: number } | undefined>();
  const [summary, setSummary] = useState<ImportSummary | undefined>();

  const handleFileSelect = async (file: File) => {
    setFileName(file.name);
    setStatus('parsing');
    // simulate parse
    setTimeout(() => {
      setProgress({ processed: 500, total: 500 });
      setSummary({
        validCount: 487,
        errorCount: 13,
        duplicateCount: 5,
        errors: [],
      });
      setStatus('partial-success');
    }, 800);
  };

  const handleCommit = () => {
    setStatus('done');
  };

  return (
    <div>
      <PageHeader
        title="Nhập danh sách học sinh hàng loạt"
        subtitle="Tuần tuyển sinh — quy mô 500 dòng/ngày · Hỗ trợ CSV/Excel"
        breadcrumbs={[
          { label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' },
          { label: 'Học sinh', href: '/school-admin/dashboard' },
          { label: 'Nhập danh sách' },
        ]}
      />

      <div className="rounded-lg border bg-background p-1">
        <BulkImportDropzone
          status={status}
          tenantLabel={SCHOOL_PROFILE.name}
          contextLabel={`Năm học ${SCHOOL_PROFILE.academicYear}`}
          fileName={fileName}
          progress={progress}
          summary={summary}
          onFileSelect={handleFileSelect}
          onCommit={handleCommit}
          onCancel={() => {
            setStatus('idle');
            setFileName(undefined);
            setProgress(undefined);
            setSummary(undefined);
          }}
          onClose={() => {
            setStatus('idle');
          }}
          onSampleDownload={() => {
            // placeholder
          }}
          onErrorDownload={() => {
            // placeholder
          }}
        />
      </div>
    </div>
  );
}
