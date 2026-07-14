import { useEffect, useRef, useState, type RefObject } from "react";
import type { ApplicationStatus } from "../types/application";

type Props = {
  currentStatus: "ACCEPTED" | "REJECTED";
  returnFocusRef: RefObject<HTMLButtonElement | null>;
  submitting: boolean;
  onClose: () => void;
  onSubmit: (targetStatus: ApplicationStatus, reason: string) => Promise<void> | void;
};

export function ApplicationStatusCorrectionModal({
  currentStatus,
  returnFocusRef,
  submitting,
  onClose,
  onSubmit,
}: Props) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const targetStatus = currentStatus === "ACCEPTED" ? "REJECTED" : "ACCEPTED";
  const targetLabel = targetStatus === "ACCEPTED" ? "합격" : "불합격";

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    const returnElement = returnFocusRef.current;
    document.body.style.overflow = "hidden";
    textareaRef.current?.focus();
    return () => {
      document.body.style.overflow = previousOverflow;
      returnElement?.focus();
    };
  }, [returnFocusRef]);

  const submit = async () => {
    if (!reason.trim()) {
      setError("결과를 바꾸는 이유를 입력해 주세요.");
      return;
    }
    setError("");
    await onSubmit(targetStatus, reason.trim());
  };

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4">
      <section role="dialog" aria-modal="true" aria-labelledby="correction-title" className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
        <h2 id="correction-title" className="text-lg font-extrabold">지원 결과 정정</h2>
        <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
          현재 결과를 {targetLabel}(으)로 바꿉니다. 메일 전송을 시작한 뒤에는 결과를 바꿀 수 없습니다.
        </p>
        <label className="mt-5 grid gap-2 text-xs font-bold text-[var(--text-secondary)]">
          정정 사유 (필수)
          <textarea
            ref={textareaRef}
            value={reason}
            onChange={event => setReason(event.target.value)}
            maxLength={500}
            rows={4}
            disabled={submitting}
            className="resize-y rounded-lg border border-[var(--border-subtle)] px-3 py-2 text-sm text-[var(--text-primary)]"
            placeholder="예: 심사 결과 입력 오류"
          />
        </label>
        {error && <p role="alert" className="mt-3 text-xs font-bold text-[var(--danger)]">{error}</p>}
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" onClick={onClose} disabled={submitting} className="rounded-lg px-4 py-2 text-xs font-bold text-[var(--text-secondary)]">취소</button>
          <button type="button" onClick={() => void submit()} disabled={submitting} className="rounded-lg bg-[var(--navy)] px-4 py-2 text-xs font-bold text-white disabled:opacity-50">
            {submitting ? "변경 중..." : `${targetLabel}으로 정정`}
          </button>
        </div>
      </section>
    </div>
  );
}
