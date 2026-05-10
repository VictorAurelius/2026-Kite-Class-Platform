/**
 * Empty states catalog — for design reference.
 *
 * Wave 50 Bucket A (GAP-271). Source: ui_kits/kitehub-admin/screens/empty-states.html
 */

'use client';

import { Inbox, FileText, Users, AlertTriangle, Receipt, MessageSquare } from 'lucide-react';
import { PageHeader, EmptyState } from '@/components/school-admin/school-admin-ui';
import { SCHOOL_PROFILE } from '@/components/school-admin/school-admin-mock-data';

export default function EmptyStatesPage() {
  return (
    <div>
      <PageHeader
        title="Trạng thái rỗng (catalog)"
        subtitle="Tham chiếu mẫu cho các màn hình khi chưa có dữ liệu — dùng khi rollout sang trường mới"
        breadcrumbs={[{ label: SCHOOL_PROFILE.name, href: '/school-admin/dashboard' }, { label: 'Cấu hình' }, { label: 'Empty states' }]}
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-lg border bg-background p-2">
          <div className="mb-1 px-3 py-1.5 text-xs font-semibold uppercase text-muted-foreground">Học sinh</div>
          <EmptyState
            icon={<Users className="h-12 w-12" aria-hidden="true" />}
            title="Chưa có học sinh nào"
            description="Trường vừa kích hoạt — nhập danh sách học sinh đầu tiên để bắt đầu."
            action={
              <button type="button" className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                Nhập danh sách
              </button>
            }
          />
        </div>

        <div className="rounded-lg border bg-background p-2">
          <div className="mb-1 px-3 py-1.5 text-xs font-semibold uppercase text-muted-foreground">Học bạ</div>
          <EmptyState
            icon={<FileText className="h-12 w-12" aria-hidden="true" />}
            title="Chưa có học bạ nào"
            description="GVCN chưa nhập điểm — học bạ sẽ tự động tạo khi GVCN chốt sổ điểm cuối kỳ."
          />
        </div>

        <div className="rounded-lg border bg-background p-2">
          <div className="mb-1 px-3 py-1.5 text-xs font-semibold uppercase text-muted-foreground">Học phí</div>
          <EmptyState
            icon={<Receipt className="h-12 w-12" aria-hidden="true" />}
            title="Chưa cấu hình học phí"
            description="Vào Hồ sơ trường → Học phí để cấu hình mức phí năm + chu kỳ thu."
            action={
              <button type="button" className="rounded-md border bg-background px-3 py-1.5 text-sm font-medium hover:bg-muted">
                Cấu hình học phí
              </button>
            }
          />
        </div>

        <div className="rounded-lg border bg-background p-2">
          <div className="mb-1 px-3 py-1.5 text-xs font-semibold uppercase text-muted-foreground">Hộp thư PH</div>
          <EmptyState
            icon={<MessageSquare className="h-12 w-12" aria-hidden="true" />}
            title="Chưa có yêu cầu nào"
            description="Tuyệt vời! Mọi yêu cầu phụ huynh đã được xử lý. Tiếp tục giữ SLA <4h."
          />
        </div>

        <div className="rounded-lg border bg-background p-2">
          <div className="mb-1 px-3 py-1.5 text-xs font-semibold uppercase text-muted-foreground">Hạnh kiểm</div>
          <EmptyState
            icon={<AlertTriangle className="h-12 w-12" aria-hidden="true" />}
            title="Không có ca nào"
            description="Tuần này không có sự việc nào cần xử lý. Tiếp tục theo dõi sát các trường hợp Bước 2 trở lên."
          />
        </div>

        <div className="rounded-lg border bg-background p-2">
          <div className="mb-1 px-3 py-1.5 text-xs font-semibold uppercase text-muted-foreground">Hộp thư đến</div>
          <EmptyState
            icon={<Inbox className="h-12 w-12" aria-hidden="true" />}
            title="Hộp thư trống"
            description="Không có thông báo mới từ Sở GD hoặc nội bộ trường."
          />
        </div>
      </div>
    </div>
  );
}
