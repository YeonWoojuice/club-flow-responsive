interface ErrorToastProps {
  message: string;
  actionLabel?: string;
  actionPending?: boolean;
  onAction?: () => void;
  onDismiss?: () => void;
}

export function ErrorToast({
  message,
  actionLabel,
  actionPending = false,
  onAction,
  onDismiss,
}: ErrorToastProps) {
  return (
    <div
      role="alert"
      aria-live="assertive"
      className="fixed right-5 top-5 z-50 flex max-w-sm items-start gap-3 rounded-xl border border-[var(--danger-border)] bg-white p-4 text-xs shadow-lg"
    >
      <p className="flex-1 font-bold leading-5 text-[var(--danger)]">{message}</p>
      {onAction && actionLabel && (
        <button
          type="button"
          disabled={actionPending}
          onClick={onAction}
          className="shrink-0 font-extrabold text-[var(--navy)] disabled:opacity-40"
        >
          {actionPending ? "재시도 중" : actionLabel}
        </button>
      )}
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="알림 닫기"
          className="shrink-0 text-[var(--text-secondary)]"
        >
          ×
        </button>
      )}
    </div>
  );
}
