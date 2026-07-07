import { beforeEach, describe, expect, it, vi } from "vitest";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("apiRequest", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.restoreAllMocks();
  });

  it("GET 요청에 세션 쿠키 옵션을 포함한다", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([{ id: "club-1" }]));
    vi.stubGlobal("fetch", fetchMock);
    const { apiRequest } = await import("./http");

    await apiRequest("/api/clubs");

    expect(fetchMock).toHaveBeenCalledWith("/api/clubs", expect.objectContaining({
      credentials: "include",
    }));
  });

  it("쓰기 요청 전에 CSRF 토큰을 받아 헤더에 포함한다", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-CSRF-TOKEN", token: "csrf-token" }))
      .mockResolvedValueOnce(jsonResponse({ id: "club-1" }, 201));
    vi.stubGlobal("fetch", fetchMock);
    const { apiRequest } = await import("./http");

    await apiRequest("/api/clubs", {
      method: "POST",
      body: JSON.stringify({ name: "ClubFlow" }),
    });

    expect(fetchMock).toHaveBeenNthCalledWith(1, "/api/auth/csrf", {
      credentials: "include",
    });
    const requestOptions = fetchMock.mock.calls[1][1] as RequestInit;
    expect(new Headers(requestOptions.headers).get("X-CSRF-TOKEN")).toBe("csrf-token");
  });

  it.each([401, 403] as const)("%s 응답을 전역 인증 이벤트로 알린다", async status => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ message: "접근할 수 없습니다." }, status));
    vi.stubGlobal("fetch", fetchMock);
    const { API_AUTH_ERROR_EVENT, apiRequest } = await import("./http");
    const listener = vi.fn();
    window.addEventListener(API_AUTH_ERROR_EVENT, listener);

    await expect(apiRequest("/api/clubs")).rejects.toMatchObject({ status });

    expect(listener).toHaveBeenCalledOnce();
    expect((listener.mock.calls[0][0] as CustomEvent).detail).toEqual({ status });
    window.removeEventListener(API_AUTH_ERROR_EVENT, listener);
  });
});
