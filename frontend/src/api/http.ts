type ErrorResponse = {
  code?: string;
  message?: string;
};

type CsrfToken = {
  headerName: string;
  token: string;
};

export const API_AUTH_ERROR_EVENT = "clubflow:api-auth-error";

export type ApiAuthErrorDetail = {
  status: 401 | 403;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

/**
 * 서버 오류 메시지가 있으면 그대로 사용자에게 보여주고, 없을 때만 기본 문구를 씁니다.
 * 에러 문구의 단일 소스는 백엔드입니다. (AGENTS.md "에러 처리 계약" 참조)
 */
export function apiErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

let csrfToken: CsrfToken | null = null;

export function clearCsrfToken() {
  csrfToken = null;
}

function notifyAuthError(status: number) {
  if (status !== 401 && status !== 403) return;
  window.dispatchEvent(new CustomEvent<ApiAuthErrorDetail>(API_AUTH_ERROR_EVENT, {
    detail: { status },
  }));
}

async function getCsrfToken() {
  if (csrfToken) return csrfToken;
  const response = await fetch("/api/auth/csrf", { credentials: "include" });
  if (!response.ok) {
    throw new ApiError(response.status, "보안 토큰을 가져오지 못했습니다.");
  }
  csrfToken = await response.json() as CsrfToken;
  return csrfToken;
}

function requiresCsrf(method: string) {
  return !["GET", "HEAD", "OPTIONS"].includes(method);
}

async function sendRequest(path: string, options: RequestInit, method: string) {
  const headers = new Headers(options.headers);

  if (options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (requiresCsrf(method)) {
    const csrf = await getCsrfToken();
    headers.set(csrf.headerName, csrf.token);
  }

  return fetch(path, {
    ...options,
    credentials: "include",
    headers,
  });
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  let response = await sendRequest(path, options, method);

  if (requiresCsrf(method) && response.status === 403) {
    clearCsrfToken();
    response = await sendRequest(path, options, method);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  const body = contentType.includes("application/json")
    ? await response.json() as ErrorResponse
    : null;

  if (!response.ok) {
    notifyAuthError(response.status);
    throw new ApiError(
      response.status,
      body?.message ?? "요청을 처리하지 못했습니다.",
      body?.code,
    );
  }

  return body as T;
}
