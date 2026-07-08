import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppLayout } from "./AppLayout";

const { clear, getClub, logout } = vi.hoisted(() => ({
  clear: vi.fn(),
  getClub: vi.fn(),
  logout: vi.fn(),
}));

vi.mock("../api/auth", () => ({ logout }));
vi.mock("../api/clubs", () => ({ getClub }));
vi.mock("../auth/AuthContext", () => ({
  useAuth: () => ({
    user: { name: "회장", email: "owner@example.com" },
    clear,
  }),
}));

describe("AppLayout 로그아웃", () => {
  beforeEach(() => {
    clear.mockReset();
    getClub.mockReset();
    logout.mockReset();
    getClub.mockResolvedValue({
      id: "club-1",
      name: "ClubFlow",
      role: "PRESIDENT",
    });
  });

  it("로그아웃 요청이 실패하면 현재 인증 상태를 유지하고 에러 토스트를 표시한다", async () => {
    logout.mockRejectedValue(new TypeError("Failed to fetch"));

    render(
      <MemoryRouter>
        <AppLayout clubId="club-1"><div>현재 화면 유지</div></AppLayout>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "나가기" }));

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("로그아웃에 실패했습니다"));
    expect(clear).not.toHaveBeenCalled();
    expect(screen.getByText("현재 화면 유지")).toBeInTheDocument();
  });
});
