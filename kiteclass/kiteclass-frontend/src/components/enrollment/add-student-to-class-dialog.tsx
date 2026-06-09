/**
 * "Thêm học sinh vào lớp" dialog — single enroll (GAP-1103).
 *
 * Composes shadcn Dialog + Select + Input + Textarea + Button. Picks an existing
 * student (search-filtered), captures tuition + discount + note, and calls
 * {@code useCreateEnrollment}. Surfaces BE 409 (đã ghi danh) / 400 (capacity,
 * discount) messages via toast — does NOT bare-catch.
 *
 * @author KiteClass Team
 * @since 3.x (Wave KC enrollment)
 */

'use client';

import { useMemo, useState } from 'react';
import { AxiosError } from 'axios';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useStudents } from '@/hooks/use-students';
import { useCreateEnrollment } from '@/hooks/use-enrollments';
import { toast } from '@/hooks/use-toast';

interface AddStudentToClassDialogProps {
  classId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Pull a human-readable error message out of an Axios/Error/unknown. */
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

export function AddStudentToClassDialog({
  classId,
  open,
  onOpenChange,
}: AddStudentToClassDialogProps) {
  const [search, setSearch] = useState('');
  const [studentId, setStudentId] = useState<string>('');
  const [tuitionAmount, setTuitionAmount] = useState('');
  const [discountPercent, setDiscountPercent] = useState('0');
  const [notes, setNotes] = useState('');

  // Load students once; filter client-side so search works regardless of BE param.
  const { data: studentsPage, isLoading: studentsLoading } = useStudents({ size: 100 });
  const createEnrollment = useCreateEnrollment();

  const filteredStudents = useMemo(() => {
    const all = studentsPage?.content ?? [];
    const q = search.trim().toLowerCase();
    if (!q) return all;
    return all.filter(
      (s) =>
        s.name.toLowerCase().includes(q) || s.email.toLowerCase().includes(q),
    );
  }, [studentsPage, search]);

  const reset = () => {
    setSearch('');
    setStudentId('');
    setTuitionAmount('');
    setDiscountPercent('0');
    setNotes('');
  };

  const handleOpenChange = (next: boolean) => {
    if (!next) {
      reset();
    }
    onOpenChange(next);
  };

  const handleSubmit = () => {
    if (!studentId) {
      toast({ title: 'Lỗi', description: 'Vui lòng chọn học sinh', variant: 'destructive' });
      return;
    }
    const tuition = Number(tuitionAmount);
    if (!tuitionAmount || Number.isNaN(tuition) || tuition < 0) {
      toast({
        title: 'Lỗi',
        description: 'Học phí phải là số không âm',
        variant: 'destructive',
      });
      return;
    }
    const discount = discountPercent === '' ? 0 : Number(discountPercent);
    if (Number.isNaN(discount) || discount < 0 || discount > 100) {
      toast({
        title: 'Lỗi',
        description: 'Phần trăm giảm giá phải từ 0 đến 100',
        variant: 'destructive',
      });
      return;
    }

    createEnrollment.mutate(
      {
        studentId: Number(studentId),
        classId,
        tuitionAmount: tuition,
        discountPercent: discount,
        notes: notes.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast({
            title: 'Thành công',
            description: 'Đã thêm học sinh vào lớp',
          });
          reset();
          onOpenChange(false);
        },
        onError: (err: unknown) => {
          toast({
            title: 'Lỗi',
            description: extractErrorMessage(err, 'Không thể thêm học sinh vào lớp'),
            variant: 'destructive',
          });
        },
      },
    );
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Thêm học sinh vào lớp</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* Student search + select */}
          <div className="space-y-2">
            <Label htmlFor="enroll-student-search">Tìm học sinh</Label>
            <Input
              id="enroll-student-search"
              placeholder="Tìm theo tên hoặc email"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Select value={studentId} onValueChange={setStudentId}>
              <SelectTrigger id="enroll-student-select" aria-label="Chọn học sinh">
                <SelectValue
                  placeholder={studentsLoading ? 'Đang tải học sinh…' : 'Chọn học sinh'}
                />
              </SelectTrigger>
              <SelectContent>
                {filteredStudents.length === 0 ? (
                  <div className="px-2 py-1.5 text-sm text-muted-foreground">
                    Không có học sinh phù hợp
                  </div>
                ) : (
                  filteredStudents.map((s) => (
                    <SelectItem key={s.id} value={String(s.id)}>
                      {s.name} ({s.email})
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
          </div>

          {/* Tuition */}
          <div className="space-y-2">
            <Label htmlFor="enroll-tuition">
              Học phí (đồng) <span className="text-destructive">*</span>
            </Label>
            <Input
              id="enroll-tuition"
              type="number"
              min={0}
              placeholder="Ví dụ: 1500000"
              value={tuitionAmount}
              onChange={(e) => setTuitionAmount(e.target.value)}
            />
          </div>

          {/* Discount */}
          <div className="space-y-2">
            <Label htmlFor="enroll-discount">Giảm giá (%)</Label>
            <Input
              id="enroll-discount"
              type="number"
              min={0}
              max={100}
              placeholder="0"
              value={discountPercent}
              onChange={(e) => setDiscountPercent(e.target.value)}
            />
          </div>

          {/* Notes */}
          <div className="space-y-2">
            <Label htmlFor="enroll-notes">Ghi chú</Label>
            <Textarea
              id="enroll-notes"
              rows={3}
              maxLength={2000}
              placeholder="Ghi chú thêm (tùy chọn)"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => handleOpenChange(false)}
            disabled={createEnrollment.isPending}
          >
            Hủy
          </Button>
          <Button onClick={handleSubmit} disabled={createEnrollment.isPending}>
            {createEnrollment.isPending ? 'Đang thêm…' : 'Thêm vào lớp'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
