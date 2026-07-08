import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { CurrentUser } from "../types/auth";
import { useAuth } from "./AuthContext";
import { AuthProvider } from "./AuthProvider";

const user: CurrentUser = {
  id: "user-1",
  email: "owner@example.com",
  name: "회장",
  profileImageUrl: null,
  lastLoginAt: "2026-07-08T00:00:00Z",
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function AuthStateProbe() {
  const { error, refresh, status, user: currentUser } = useAuth();

  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="user">{currentUser?.email ?? "none"}</span>
      {error && <p role="alert">{error}</p>}
      <button type="button" onClick={() => void refresh()}>재시도</button>
    </div>
  );
}

function renderProvider() {
  render(
    <AuthProvider>
      <AuthStateProbe />
    </AuthProvider>,
  );
}

describe("AuthProvider", () => {
  beforeEach(() => vi.restoreAllMocks());
  afterEach(() => vi.unstubAllGlobals());

  it("401 응답만 미로그인 상태로 처리한다", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      jsonResponse({ message: "로그인이 필요합니다." }, 401),
    ));

    renderProvider();

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("anonymous"));
    expect(screen.getByTestId("user")).toHaveTextContent("none");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("초기 네트워크 오류는 일시적 오류로 표시하고 재시도할 수 있다", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(jsonResponse(user));
    vi.stubGlobal("fetch", fetchMock);

    renderProvider();

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("error"));
    expect(screen.getByRole("alert")).toHaveTextContent("잠시 후 다시 시도");

    fireEvent.click(screen.getByRole("button", { name: "재시도" }));

    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("authenticated"));
    expect(screen.getByTestId("user")).toHaveTextContent(user.email);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("로그인 후 재확인 실패는 기존 인증 사용자와 상태를 유지한다", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(user))
      .mockResolvedValueOnce(jsonResponse({ message: "서버 오류" }, 500));
    vi.stubGlobal("fetch", fetchMock);

    renderProvider();
    await waitFor(() => expect(screen.getByTestId("status")).toHaveTextContent("authenticated"));

    fireEvent.click(screen.getByRole("button", { name: "재시도" }));

    await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());
    expect(screen.getByTestId("status")).toHaveTextContent("authenticated");
    expect(screen.getByTestId("user")).toHaveTextContent(user.email);
  });
});
