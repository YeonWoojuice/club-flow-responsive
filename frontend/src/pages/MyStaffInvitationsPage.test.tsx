import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api/http";
import { MyStaffInvitationsPage } from "./MyStaffInvitationsPage";

const api = vi.hoisted(() => ({
  listMyStaffInvitations: vi.fn(),
  acceptStaffInvitation: vi.fn(),
  rejectStaffInvitation: vi.fn(),
}));

vi.mock("../api/staff", () => api);

const invitation = {
  id: "invitation-1",
  clubId: "club-1",
  clubName: "ClubFlow",
  email: "invitee@example.com",
  role: "STAFF",
  status: "PENDING",
  invitedByUserId: "user-president",
  invitedByName: "김회장",
  createdAt: "2026-07-10T00:00:00Z",
  respondedAt: null,
};

describe("MyStaffInvitationsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.listMyStaffInvitations.mockResolvedValue([invitation]);
    api.acceptStaffInvitation.mockResolvedValue(undefined);
    api.rejectStaffInvitation.mockResolvedValue(undefined);
  });

  it("받은 초대의 동아리와 역할, 초대한 사람을 보여주고 수락한다", async () => {
    render(<MemoryRouter><MyStaffInvitationsPage /></MemoryRouter>);

    expect(await screen.findByText("ClubFlow")).toBeInTheDocument();
    expect(screen.getByText("초대 역할: 운영진")).toBeInTheDocument();
    expect(screen.getByText("초대한 사람: 김회장")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "수락" }));

    await waitFor(() => expect(api.acceptStaffInvitation).toHaveBeenCalledWith("invitation-1"));
    expect(await screen.findByText("현재 기다리고 있는 운영진 초대가 없습니다.")).toBeInTheDocument();
  });

  it("처리 중에는 버튼을 잠가 중복 요청을 막는다", async () => {
    let resolveAccept: () => void = () => undefined;
    api.acceptStaffInvitation.mockReturnValueOnce(new Promise<void>(resolve => { resolveAccept = resolve; }));
    render(<MemoryRouter><MyStaffInvitationsPage /></MemoryRouter>);
    const accept = await screen.findByRole("button", { name: "수락" });

    fireEvent.click(accept);
    expect(await screen.findAllByRole("button", { name: "처리 중..." })).toHaveLength(2);
    fireEvent.click(accept);
    expect(api.acceptStaffInvitation).toHaveBeenCalledOnce();
    resolveAccept();
  });

  it("서버의 초대 처리 오류 메시지를 표시한다", async () => {
    api.rejectStaffInvitation.mockRejectedValueOnce(new ApiError(409, "이미 처리된 초대입니다."));
    render(<MemoryRouter><MyStaffInvitationsPage /></MemoryRouter>);
    fireEvent.click(await screen.findByRole("button", { name: "거절" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미 처리된 초대입니다.");
    expect(screen.getByRole("button", { name: "거절" })).toBeEnabled();
  });
});
