import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  applyApplicationImport,
  disconnectGoogleConnection,
  previewApplicationImport,
  readApplicationGoogleSheet,
} from "./applicationImport";

const { apiRequest } = vi.hoisted(() => ({ apiRequest: vi.fn() }));

vi.mock("./http", () => ({ apiRequest }));

const rows = [{
  rowNumber: 2,
  name: "김지원",
  email: "apply@example.com",
  studentNumber: "20260001",
  answers: [{ questionKey: "sheet-column-4", questionLabel: "지원 동기", answerValue: "함께하고 싶습니다." }],
}];

describe("applicationImport API", () => {
  beforeEach(() => vi.clearAllMocks());

  it("Google Sheet ID를 안전하게 인코딩해 탭을 조회한다", () => {
    readApplicationGoogleSheet("club-1", "sheet/id");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/clubs/club-1/application-import/google-sheet/sheet%2Fid/tables",
    );
  });

  it("미리보기와 확정에 같은 원본 행을 전달해 서버가 다시 검사할 수 있게 한다", () => {
    previewApplicationImport("club-1", "generation-1", rows);
    applyApplicationImport("club-1", "generation-1", rows);

    const expectedOptions = {
      method: "POST",
      body: JSON.stringify({ generationId: "generation-1", rows }),
    };
    expect(apiRequest).toHaveBeenNthCalledWith(
      1,
      "/api/clubs/club-1/application-import/preview",
      expectedOptions,
    );
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      "/api/clubs/club-1/application-import/apply",
      expectedOptions,
    );
  });

  it("Google 연결 해제는 서버 연결 정보 삭제 API를 호출한다", () => {
    disconnectGoogleConnection();
    expect(apiRequest).toHaveBeenCalledWith("/api/google-data/connection", { method: "DELETE" });
  });
});
