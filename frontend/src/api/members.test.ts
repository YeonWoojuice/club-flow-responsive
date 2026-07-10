import { beforeEach, describe, expect, it, vi } from "vitest";
import { changeGenerationMemberStatus, listGenerationMemberStatusHistory } from "./members";

const { apiRequest } = vi.hoisted(() => ({ apiRequest: vi.fn() }));

vi.mock("./http", () => ({ apiRequest }));

describe("members API", () => {
  beforeEach(() => vi.clearAllMocks());

  it("부원 상태와 선택 사유를 PATCH 요청으로 전달한다", () => {
    changeGenerationMemberStatus("member/1", { status: "INACTIVE", reason: "군 복무" });

    expect(apiRequest).toHaveBeenCalledWith(
      "/api/generation-members/member/1/status",
      {
        method: "PATCH",
        body: JSON.stringify({ status: "INACTIVE", reason: "군 복무" }),
      },
    );
  });

  it("부원의 상태 변경 이력을 조회한다", () => {
    listGenerationMemberStatusHistory("member-1");

    expect(apiRequest).toHaveBeenCalledWith(
      "/api/generation-members/member-1/status-history",
    );
  });
});
