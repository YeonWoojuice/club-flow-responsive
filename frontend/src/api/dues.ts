import { apiRequest } from "./http";
import type { CreateDuesPolicyRequest, DuesOverview, DuesRefundQuote } from "../types/dues";

export function getDuesOverview(clubId: string, generationId: string) {
  return apiRequest<DuesOverview>(`/api/clubs/${clubId}/generations/${generationId}/dues`);
}
export function createDuesPolicy(clubId: string, generationId: string, request: CreateDuesPolicyRequest) {
  return apiRequest<DuesOverview>(`/api/clubs/${clubId}/generations/${generationId}/dues/policy`, {
    method: "POST", body: JSON.stringify(request),
  });
}
export function recordDuesPayment(dueId: string, paidOn: string, idempotencyKey: string) {
  return apiRequest<DuesOverview>(`/api/member-dues/${dueId}/payments`, {
    method: "POST", body: JSON.stringify({ paidOn, idempotencyKey }),
  });
}
export function changeDuesExemption(dueId: string, exempted: boolean, reason?: string) {
  return apiRequest<DuesOverview>(`/api/member-dues/${dueId}/exemption`, {
    method: "PATCH", body: JSON.stringify({ exempted, ...(reason ? { reason } : {}) }),
  });
}
export function getDuesRefundQuote(dueId: string) {
  return apiRequest<DuesRefundQuote>(`/api/member-dues/${dueId}/refund-quote`);
}
export function recordDuesRefund(dueId: string, idempotencyKey: string) {
  return apiRequest<DuesOverview>(`/api/member-dues/${dueId}/refunds`, {
    method: "POST", body: JSON.stringify({ idempotencyKey }),
  });
}
export function cancelDuesPayment(dueId: string, reason: string) {
  return apiRequest<DuesOverview>(`/api/member-dues/${dueId}/payments/cancel`, {
    method: "POST", body: JSON.stringify({ reason }),
  });
}
export function cancelDuesRefund(dueId: string, reason: string) {
  return apiRequest<DuesOverview>(`/api/member-dues/${dueId}/refunds/cancel`, {
    method: "POST", body: JSON.stringify({ reason }),
  });
}
