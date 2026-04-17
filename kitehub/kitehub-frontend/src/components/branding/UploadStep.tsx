'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Image as ImageIcon } from 'lucide-react';
import { useUploadAsset } from '@/hooks/use-branding';
import { toast } from 'sonner';

interface UploadStepProps {
  instanceId: string;
  onUploadComplete: (logoUrl: string) => void;
}

export function UploadStep({ instanceId, onUploadComplete }: UploadStepProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);

  const uploadMutation = useUploadAsset();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        toast.error('Vui lòng chọn file hình ảnh');
        return;
      }

      // Validate file size (10MB)
      if (file.size > 10 * 1024 * 1024) {
        toast.error('File không được vượt quá 10MB');
        return;
      }

      setSelectedFile(file);
      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => setPreview(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();

    const file = e.dataTransfer.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        toast.error('Vui lòng chọn file hình ảnh');
        return;
      }

      // Validate file size (10MB)
      if (file.size > 10 * 1024 * 1024) {
        toast.error('File không được vượt quá 10MB');
        return;
      }

      setSelectedFile(file);
      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => setPreview(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      const asset = await uploadMutation.mutateAsync({
        instanceId,
        type: 'LOGO',
        file: selectedFile,
      });
      toast.success('Tải logo lên thành công!');
      onUploadComplete(asset.url);
    } catch {
      toast.error('Tải logo lên thất bại. Vui lòng thử lại.');
    }
  };

  return (
    <Card className="p-8">
      <h2 className="text-xl font-semibold mb-2">Tải Logo Lên</h2>
      <p className="text-sm text-muted-foreground mb-6">
        Tải logo của bạn lên để AI phân tích và tạo bộ nhận diện thương hiệu
      </p>

      {/* Drag & Drop Area */}
      <label className="block cursor-pointer">
        <div
          className="border-2 border-dashed border-muted-foreground/25 rounded-lg p-12 text-center hover:border-primary transition-colors"
          onDragOver={handleDragOver}
          onDrop={handleDrop}
        >
          {preview ? (
            <img
              src={preview}
              alt="Preview"
              className="max-h-48 mx-auto mb-4 rounded-lg"
            />
          ) : (
            <ImageIcon className="w-16 h-16 mx-auto mb-4 text-muted-foreground" />
          )}
          <p className="text-sm text-muted-foreground">
            Nhấp để tải lên hoặc kéo thả file
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            PNG, JPG, WEBP (tối đa 10MB)
          </p>
        </div>
        <input
          type="file"
          accept="image/png,image/jpeg,image/webp"
          onChange={handleFileChange}
          className="hidden"
        />
      </label>

      {selectedFile && (
        <div className="mt-4 text-sm text-muted-foreground">
          <p>Đã chọn: {selectedFile.name}</p>
          <p>Kích thước: {(selectedFile.size / 1024 / 1024).toFixed(2)} MB</p>
        </div>
      )}

      <div className="mt-6 flex justify-end gap-3">
        <Button
          onClick={handleUpload}
          disabled={!selectedFile || uploadMutation.isPending}
        >
          {uploadMutation.isPending ? 'Đang tải lên...' : 'Tiếp tục'}
        </Button>
      </div>
    </Card>
  );
}
