import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useToast } from '@/hooks/use-toast';
import { invoicesApi } from '@/lib/api/invoices';
import type {
  CreateInvoiceRequest,
  ApplyAdjustmentRequest,
  InvoiceSearchParams,
} from '@/types/invoice';
import type { AxiosError } from 'axios';

const INVOICES_KEY = 'invoices';

export function useInvoices(params: InvoiceSearchParams = {}) {
  return useQuery({
    queryKey: [INVOICES_KEY, params],
    queryFn: () => invoicesApi.getByStudent(params),
  });
}

export function useInvoice(id: number) {
  return useQuery({
    queryKey: [INVOICES_KEY, id],
    queryFn: () => invoicesApi.getById(id),
    enabled: !!id,
  });
}

export function useCreateInvoice() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: CreateInvoiceRequest) => invoicesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [INVOICES_KEY] });
      toast({ title: 'Thành công', description: 'Đã tạo hóa đơn' });
      router.push('/billing');
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tạo hóa đơn',
        variant: 'destructive',
      });
    },
  });
}

export function useApplyAdjustment(id: number) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: ApplyAdjustmentRequest) =>
      invoicesApi.applyAdjustment(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [INVOICES_KEY, id] });
      queryClient.invalidateQueries({ queryKey: [INVOICES_KEY] });
      toast({ title: 'Thành công', description: 'Đã áp dụng điều chỉnh' });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description:
          error.response?.data?.message || 'Không thể áp dụng điều chỉnh',
        variant: 'destructive',
      });
    },
  });
}

export function useApplyLateFees(id: number) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: () => invoicesApi.applyLateFees(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [INVOICES_KEY, id] });
      toast({ title: 'Thành công', description: 'Đã tính phí trễ hạn' });
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tính phí',
        variant: 'destructive',
      });
    },
  });
}

export function useCancelInvoice(id: number) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: () => invoicesApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [INVOICES_KEY] });
      toast({ title: 'Thành công', description: 'Đã hủy hóa đơn' });
      router.push('/billing');
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể hủy hóa đơn',
        variant: 'destructive',
      });
    },
  });
}
