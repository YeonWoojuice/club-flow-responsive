import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApplicationImportPage } from "./ApplicationImportPage";

const {
  listGenerations,
  getGoogleConnectionStatus,
  disconnectGoogleConnection,
  readApplicationGoogleSheet,
  previewApplicationImport,
  applyApplicationImport,
} = vi.hoisted(() => ({
  listGenerations: vi.fn(),
  getGoogleConnectionStatus: vi.fn(),
  disconnectGoogleConnection: vi.fn(),
  readApplicationGoogleSheet: vi.fn(),
  previewApplicationImport: vi.fn(),
  applyApplicationImport: vi.fn(),
}));

vi.mock("../api/generations", () => ({ listGenerations }));
vi.mock("../api/applicationImport", () => ({
  getGoogleConnectionStatus,
  disconnectGoogleConnection,
  readApplicationGoogleSheet,
  previewApplicationImport,
  applyApplicationImport,
  getGoogleAuthorizationUrl: vi.fn(),
}));
vi.mock("../components/AppLayout", () => ({
  AppLayout: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

describe("ApplicationImportPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listGenerations.mockResolvedValue([
      { id: "generation-1", name: "26-1", status: "ACTIVE" },
      { id: "generation-old", name: "25-2", status: "CLOSED" },
    ]);
    getGoogleConnectionStatus.mockResolvedValue({ connected: true, googleAccountEmail: "staff@example.com" });
    readApplicationGoogleSheet.mockResolvedValue({
      tables: [{
        name: "설문지 응답 1",
        headers: ["이름", "이메일", "학번", "전화번호", "지원 동기"],
        rows: [
          ["김지원", "apply@example.com", "20260001", "010-1234-5678", "함께 활동하고 싶습니다."],
          ["중복1", "same@example.com", "20260002", "", "첫 번째"],
          ["중복2", "SAME@example.com", "20260003", "", "두 번째"],
        ],
      }],
    });
    previewApplicationImport.mockResolvedValue({
      totalCount: 3,
      readyCount: 1,
      duplicateCount: 2,
      invalidCount: 0,
      alreadyAppliedCount: 0,
      rows: [
        { rowNumber: 2, name: "김지원", email: "apply@example.com", status: "READY", message: "등록할 수 있습니다." },
        { rowNumber: 3, name: "중복1", email: "same@example.com", status: "DUPLICATE_IN_SOURCE", message: "같은 이메일이 여러 번 있습니다." },
        { rowNumber: 4, name: "중복2", email: "same@example.com", status: "DUPLICATE_IN_SOURCE", message: "같은 이메일이 여러 번 있습니다." },
      ],
    });
    applyApplicationImport.mockResolvedValue({
      requestedCount: 3,
      createdCount: 1,
      skippedCount: 2,
    });
    disconnectGoogleConnection.mockResolvedValue(undefined);
  });

  it("Sheet 열을 연결하고 나머지 열을 답변으로 포함해 미리보기 후 등록한다", async () => {
    render(
      <MemoryRouter initialEntries={["/clubs/club-1/applications/import"]}>
        <Routes>
          <Route path="/clubs/:clubId/applications/import" element={<ApplicationImportPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findByText("연결 계정: staff@example.com");
    fireEvent.change(screen.getByLabelText("Google Sheet 주소 또는 ID"), {
      target: { value: "https://docs.google.com/spreadsheets/d/sheet-123/edit" },
    });
    fireEvent.click(screen.getByRole("button", { name: "불러오기" }));

    await screen.findByText("3. Sheet의 열 연결하기 (열 매핑)");
    fireEvent.change(screen.getByLabelText("이름 (필수)"), { target: { value: "0" } });
    fireEvent.change(screen.getByLabelText("이메일 (필수)"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("학번 (필수)"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("전화번호 (선택)"), { target: { value: "3" } });
    expect(screen.getByText(/나머지 1개 열/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "중복 확인하고 미리보기" }));

    await waitFor(() => expect(previewApplicationImport).toHaveBeenCalledWith(
      "club-1",
      "generation-1",
      expect.arrayContaining([
        expect.objectContaining({
          rowNumber: 2,
          name: "김지원",
          email: "apply@example.com",
          studentNumber: "20260001",
          answers: [{
            questionKey: "sheet-column-5",
            questionLabel: "지원 동기",
            answerValue: "함께 활동하고 싶습니다.",
          }],
        }),
      ]),
    ));
    expect(await screen.findAllByText("원본 중복")).toHaveLength(3);

    fireEvent.click(screen.getByRole("button", { name: "등록 가능 1명 확정" }));
    await waitFor(() => expect(applyApplicationImport).toHaveBeenCalledWith(
      "club-1",
      "generation-1",
      expect.arrayContaining([expect.objectContaining({ email: "apply@example.com" })]),
    ));
    expect(await screen.findByText(/1명의 지원자를 등록했습니다/)).toBeInTheDocument();
  });

  it("Google 연결 실패를 안내하고 연결을 해제할 수 있다", async () => {
    const first = render(
      <MemoryRouter initialEntries={["/clubs/club-1/applications/import?google=error"]}>
        <Routes><Route path="/clubs/:clubId/applications/import" element={<ApplicationImportPage />} /></Routes>
      </MemoryRouter>,
    );
    expect(await screen.findByRole("alert")).toHaveTextContent("Google 계정을 연결하지 못했습니다.");
    first.unmount();

    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(
      <MemoryRouter initialEntries={["/clubs/club-1/applications/import"]}>
        <Routes><Route path="/clubs/:clubId/applications/import" element={<ApplicationImportPage />} /></Routes>
      </MemoryRouter>,
    );
    fireEvent.click(await screen.findByRole("button", { name: "Google 연결 해제" }));

    await waitFor(() => expect(disconnectGoogleConnection).toHaveBeenCalledOnce());
    expect(await screen.findByText("Google 계정 연결을 해제했습니다.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Google 계정 연결" })).toBeInTheDocument();
  });
});
