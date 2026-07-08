import { Brand } from "../components/Brand";
import { useAuth } from "./AuthContext";

export function AuthErrorScreen() {
  const { error, refresh } = useAuth();

  return (
    <main className="flex min-h-full items-center justify-center bg-[var(--surface)] px-5 font-body">
      <section className="w-full max-w-md rounded-[12px] border border-[var(--border-subtle)] bg-white p-8 text-center shadow-sm">
        <div className="flex justify-center"><Brand /></div>
        <h1 className="mt-8 text-lg font-extrabold">일시적인 오류가 발생했습니다</h1>
        <p role="alert" className="mt-3 text-sm leading-6 text-[var(--text-secondary)]">
          {error ?? "로그인 상태를 확인하지 못했습니다."}
        </p>
        <button
          type="button"
          onClick={() => void refresh()}
          className="mt-6 h-11 rounded-lg bg-[var(--navy)] px-5 text-sm font-extrabold text-white"
        >
          다시 시도
        </button>
      </section>
    </main>
  );
}
