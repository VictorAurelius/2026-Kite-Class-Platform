interface ErrorAlertProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export function ErrorAlert({ title = 'Lỗi', message, onRetry }: ErrorAlertProps) {
  return (
    <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
      <h3 className="text-sm font-medium text-destructive">{title}</h3>
      <p className="mt-1 text-sm text-destructive/80">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-2 text-sm font-medium text-destructive hover:underline"
        >
          Thử lại
        </button>
      )}
    </div>
  );
}
