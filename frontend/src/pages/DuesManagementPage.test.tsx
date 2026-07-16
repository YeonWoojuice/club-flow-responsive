import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { DuesManagementPage } from "./DuesManagementPage";

const { listGenerations, getDuesOverview, createDuesPolicy, getDuesRefundQuote } = vi.hoisted(() => ({
  listGenerations: vi.fn(), getDuesOverview: vi.fn(), createDuesPolicy: vi.fn(),
  getDuesRefundQuote: vi.fn(),
}));
vi.mock("../api/generations", () => ({ listGenerations }));
vi.mock("../api/dues", () => ({
  getDuesOverview, createDuesPolicy, getDuesRefundQuote,
  recordDuesPayment: vi.fn(), recordDuesRefund: vi.fn(), changeDuesExemption: vi.fn(),
  cancelDuesPayment: vi.fn(), cancelDuesRefund: vi.fn(),
}));
vi.mock("../components/AppLayout", () => ({ AppLayout: ({ children }: { children: ReactNode }) => <>{children}</> }));

function renderPage() {
  return render(<MemoryRouter initialEntries={["/clubs/club-1/dues"]}><Routes>
    <Route path="/clubs/:clubId/dues" element={<DuesManagementPage />} />
  </Routes></MemoryRouter>);
}

describe("DuesManagementPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listGenerations.mockResolvedValue([{ id: "generation-1", name: "27-1", status: "ACTIVE" }]);
    getDuesOverview.mockResolvedValue({ policyId: null, totalAssessed: "0", totalPaid: "0", totalRefunded: "0", unpaidCount: 0, members: [], refundRules: [] });
  });

  it("회비와 환불 기준을 원 단위·정수 비율로 저장한다", async () => {
    createDuesPolicy.mockResolvedValue({ policyId: "policy-1", amount: "30000", totalAssessed: "0", totalPaid: "0", totalRefunded: "0", unpaidCount: 0, members: [], refundRules: [] });
    renderPage();
    fireEvent.change(await screen.findByLabelText("회비(원)"), { target: { value: "30000" } });
    fireEvent.change(screen.getByLabelText("환불 기준 1 마감일"), { target: { value: "2027-03-10" } });
    fireEvent.click(screen.getByRole("button", { name: "회비 설정" }));
    await waitFor(() => expect(createDuesPolicy).toHaveBeenCalledWith("club-1", "generation-1", expect.objectContaining({
      amount: "30000",
      refundRules: [{ label: "OT 전 50% 환불", endsOn: "2027-03-10", refundRateBps: 5000 }],
    })));
  });

  it("탈퇴일을 입력받지 않고 서버가 계산한 환불액을 표시한다", async () => {
    getDuesOverview.mockResolvedValueOnce({
      policyId: "policy-1", amount: "30001", totalAssessed: "30001", totalPaid: "30001", totalRefunded: "0", unpaidCount: 0,
      refundRules: [], members: [{ memberDueId: "due-1", generationMemberId: "member-1", name: "김부원", studentNumber: "20270001", memberStatus: "WITHDRAWN", assessedAmount: "30001", paidAmount: "30001", refundedAmount: "0", status: "PAID", legacyPayment: false }],
    });
    getDuesRefundQuote.mockResolvedValue({ paidAmount: "30001", refundAmount: "15000", refundRateBps: 5000, ruleLabel: "OT 전", withdrawalDate: "2027-03-10" });
    renderPage();
    fireEvent.click(await screen.findByRole("button", { name: "환불 계산" }));
    expect(await screen.findByText("예상 환불액 15,000원")).toBeInTheDocument();
    expect(screen.getByText(/탈퇴일 2027-03-10/)).toBeInTheDocument();
    expect(getDuesRefundQuote).toHaveBeenCalledWith("due-1");
  });
});
