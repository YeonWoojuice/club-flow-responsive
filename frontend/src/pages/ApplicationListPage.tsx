import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { listApplications } from "../api/applications";
import { apiErrorMessage } from "../api/http";
import { AppLayout } from "../components/AppLayout";
import type {
  ApplicationSourceType,
  ApplicationStatus,
  ApplicationSummary,
} from "../types/application";

type SourceFilter = ApplicationSourceType | "ALL";
type StatusFilter = ApplicationStatus | "ALL";

const sourceTabs: { value: SourceFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "MANUAL", label: "수동 등록" },
  { value: "GOOGLE_FORM", label: "Google Form" },
];

const statusTabs: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "SUBMITTED", label: "접수" },
  { value: "REVIEWING", label: "검토 중" },
  { value: "ACCEPTED", label: "합격" },
  { value: "REJECTED", label: "불합격" },
  { value: "CANCELED", label: "취소" },
];

const statusConfig: Record<ApplicationStatus, { label: string; cls: string }> = {
  SUBMITTED: { label: "접수", cls: "bg-blue-50 text-blue-700" },
  REVIEWING: { label: "검토 중", cls: "bg-[var(--warning-soft)] text-[var(--warning)]" },
  ACCEPTED: { label: "합격", cls: "bg-[var(--success-soft)] text-[var(--success)]" },
  REJECTED: { label: "불합격", cls: "bg-[var(--danger-soft)] text-[var(--danger)]" },
  CANCELED: { label: "취소", cls: "bg-[var(--panel-muted)] text-[var(--text-secondary)]" },
};

export function ApplicationListPage() {
  const { clubId = "" } = useParams();
  const [applications, setApplications] = useState<ApplicationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("ALL");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");

  useEffect(() => {
    listApplications(clubId)
      .then(setApplications)
      .catch(requestError => setError(apiErrorMessage(requestError, "지원자 목록을 불러오지 못했습니다.")))
      .finally(() => setLoading(false));
  }, [clubId]);

  const filtered = applications.filter(app => {
    if (sourceFilter !== "ALL" && app.sourceType !== sourceFilter) return false;
    if (statusFilter !== "ALL" && app.status !== statusFilter) return false;
    return true;
  });

  return (
    <AppLayout clubId={clubId}>
      <header className="flex flex-col gap-4 border-b border-[var(--border-subtle)] bg-white px-4 py-5 sm:flex-row sm:items-center sm:justify-between md:px-8">
        <div>
          <h1 className="text-xl font-extrabold">지원자 관리</h1>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">수동 등록 및 Google Form 지원자를 관리합니다.</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Link
            to={`/clubs/${clubId}/applications/import`}
            className="rounded-lg border border-[var(--navy)] px-4 py-2.5 text-center text-xs font-extrabold text-[var(--navy)]"
          >
            Google Sheet 가져오기
          </Link>
          <Link
            to={`/clubs/${clubId}/applications/new`}
            className="rounded-lg bg-[var(--navy)] px-4 py-2.5 text-center text-xs font-extrabold text-white"
          >
            + 수동 등록
          </Link>
        </div>
      </header>

      <main className="p-4 md:p-8">
        <div className="mb-4 flex flex-col gap-2 md:mb-6 md:flex-row md:flex-wrap md:items-center md:gap-3">
          <div className="overflow-x-auto">
            <div className="flex w-max overflow-hidden rounded-lg border border-[var(--border-subtle)] bg-white">
              {sourceTabs.map(tab => (
                <button
                  key={tab.value}
                  onClick={() => setSourceFilter(tab.value)}
                  className={`px-3 py-2 text-xs font-bold transition-colors ${
                    sourceFilter === tab.value
                      ? "bg-[var(--navy)] text-white"
                      : "text-[var(--text-secondary)] hover:bg-[var(--panel-muted)] hover:text-[var(--text-primary)]"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
          <div className="overflow-x-auto">
            <div className="flex w-max overflow-hidden rounded-lg border border-[var(--border-subtle)] bg-white">
              {statusTabs.map(tab => (
                <button
                  key={tab.value}
                  onClick={() => setStatusFilter(tab.value)}
                  className={`px-3 py-2 text-xs font-bold transition-colors ${
                    statusFilter === tab.value
                      ? "bg-[var(--navy)] text-white"
                      : "text-[var(--text-secondary)] hover:bg-[var(--panel-muted)] hover:text-[var(--text-primary)]"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
          {(sourceFilter !== "ALL" || statusFilter !== "ALL") && (
            <span className="text-xs text-[var(--text-secondary)]">{filtered.length}건 표시</span>
          )}
        </div>

        {loading && <p className="text-sm text-[var(--text-secondary)]">불러오는 중...</p>}
        {error && <p role="alert" className="text-sm font-bold text-[var(--danger)]">{error}</p>}

        {!loading && !error && filtered.length === 0 && (
          <p className="rounded-xl border border-[var(--border-subtle)] bg-white p-8 text-center text-sm text-[var(--text-secondary)]">
            {applications.length === 0
              ? "등록된 지원자가 없습니다."
              : "조건에 맞는 지원자가 없습니다."}
          </p>
        )}

        {!loading && !error && filtered.length > 0 && (
          <>
            {/* Mobile: card list */}
            <div className="flex flex-col gap-3 md:hidden">
              {filtered.map(app => (
                <Link
                  key={app.id}
                  to={`/clubs/${clubId}/applications/${app.id}`}
                  className="block rounded-xl border border-[var(--border-subtle)] bg-white p-4 transition hover:border-[var(--navy)]"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <b className="block text-sm">{app.name}</b>
                      <span className="mt-0.5 block truncate text-xs text-[var(--text-secondary)]">{app.email}</span>
                    </div>
                    <span className={`shrink-0 rounded-md px-2 py-1 text-[10px] font-bold ${statusConfig[app.status].cls}`}>
                      {statusConfig[app.status].label}
                    </span>
                  </div>
                  <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-[var(--text-secondary)]">
                    <span>{app.generationName}</span>
                    <span>·</span>
                    <time>{new Date(app.submittedAt).toLocaleDateString("ko-KR")}</time>
                    <span>·</span>
                    {app.sourceType === "MANUAL" ? (
                      <span className="rounded-md bg-[var(--panel-muted)] px-2 py-0.5 text-[10px] font-bold text-[var(--text-secondary)]">수동 등록</span>
                    ) : (
                      <span className="rounded-md bg-[var(--success-soft)] px-2 py-0.5 text-[10px] font-bold text-[var(--success)]">Google Form</span>
                    )}
                  </div>
                </Link>
              ))}
            </div>

            {/* Desktop: grid table */}
            <div className="hidden overflow-hidden rounded-xl border border-[var(--border-subtle)] bg-white md:block">
              <div className="grid grid-cols-[1fr_140px_130px_110px_100px] border-b border-[var(--border-subtle)] px-5 py-3">
                <span className="text-xs font-bold text-[var(--text-secondary)]">이름 / 이메일</span>
                <span className="text-xs font-bold text-[var(--text-secondary)]">학기</span>
                <span className="text-xs font-bold text-[var(--text-secondary)]">제출일</span>
                <span className="text-xs font-bold text-[var(--text-secondary)]">출처</span>
                <span className="text-xs font-bold text-[var(--text-secondary)]">상태</span>
              </div>
              {filtered.map(app => (
                <Link
                  key={app.id}
                  to={`/clubs/${clubId}/applications/${app.id}`}
                  className="grid grid-cols-[1fr_140px_130px_110px_100px] items-center border-b border-[var(--border-subtle)] px-5 py-4 last:border-0 transition-colors hover:bg-[var(--panel-muted)]"
                >
                  <span>
                    <b className="block text-sm">{app.name}</b>
                    <span className="mt-0.5 block text-xs text-[var(--text-secondary)]">{app.email}</span>
                  </span>
                  <span className="text-xs text-[var(--text-secondary)]">{app.generationName}</span>
                  <time className="text-xs text-[var(--text-secondary)]">
                    {new Date(app.submittedAt).toLocaleDateString("ko-KR")}
                  </time>
                  <span>
                    {app.sourceType === "MANUAL" ? (
                      <span className="inline-block rounded-md bg-[var(--panel-muted)] px-2 py-1 text-[10px] font-bold text-[var(--text-secondary)]">
                        수동 등록
                      </span>
                    ) : (
                      <span className="inline-block rounded-md bg-[var(--success-soft)] px-2 py-1 text-[10px] font-bold text-[var(--success)]">
                        Google Form
                      </span>
                    )}
                  </span>
                  <span>
                    <span className={`inline-block rounded-md px-2 py-1 text-[10px] font-bold ${statusConfig[app.status].cls}`}>
                      {statusConfig[app.status].label}
                    </span>
                  </span>
                </Link>
              ))}
            </div>
          </>
        )}
      </main>
    </AppLayout>
  );
}
