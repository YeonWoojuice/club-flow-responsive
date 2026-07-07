import { act, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_AUTH_ERROR_EVENT } from "../api/http";
import { ApiErrorNavigator } from "./ApiErrorNavigator";

const { clear } = vi.hoisted(() => ({ clear: vi.fn() }));

vi.mock("./AuthContext", () => ({
  useAuth: () => ({ clear }),
}));

function renderNavigator() {
  render(
    <MemoryRouter initialEntries={["/protected"]}>
      <ApiErrorNavigator />
      <Routes>
        <Route path="/protected" element={<div>보호 화면</div>} />
        <Route path="/login" element={<div>로그인 화면</div>} />
        <Route path="/clubs" element={<div>동아리 목록</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ApiErrorNavigator", () => {
  afterEach(() => clear.mockReset());

  it("401이면 인증 상태를 지우고 로그인 화면으로 이동한다", () => {
    renderNavigator();

    act(() => window.dispatchEvent(new CustomEvent(API_AUTH_ERROR_EVENT, {
      detail: { status: 401 },
    })));

    expect(clear).toHaveBeenCalledOnce();
    expect(screen.getByText("로그인 화면")).toBeInTheDocument();
  });

  it("403이면 동아리 목록으로 이동한다", () => {
    renderNavigator();

    act(() => window.dispatchEvent(new CustomEvent(API_AUTH_ERROR_EVENT, {
      detail: { status: 403 },
    })));

    expect(clear).not.toHaveBeenCalled();
    expect(screen.getByText("동아리 목록")).toBeInTheDocument();
  });
});
