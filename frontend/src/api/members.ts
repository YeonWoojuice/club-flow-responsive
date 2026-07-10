import { apiRequest } from "./http";
import type {
  GenerationMember,
  GenerationMemberStatusChangeRequest,
  GenerationMemberStatusHistory,
} from "../types/member";

export function listMembers(clubId: string) {
  return apiRequest<GenerationMember[]>(`/api/clubs/${clubId}/members`);
}

export function changeGenerationMemberStatus(
  memberId: string,
  request: GenerationMemberStatusChangeRequest,
) {
  return apiRequest<GenerationMember>(`/api/generation-members/${memberId}/status`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export function listGenerationMemberStatusHistory(memberId: string) {
  return apiRequest<GenerationMemberStatusHistory[]>(
    `/api/generation-members/${memberId}/status-history`,
  );
}
