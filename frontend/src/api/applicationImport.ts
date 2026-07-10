import { apiRequest } from "./http";
import type {
  ApplicationImportApplyResult,
  ApplicationImportPreview,
  ApplicationImportRowInput,
  ApplicationImportWorkbook,
} from "../types/applicationImport";
import type { GoogleAuthorizationUrl, GoogleConnectionStatus } from "../types/retention";

export function readApplicationGoogleSheet(clubId: string, spreadsheetId: string) {
  return apiRequest<ApplicationImportWorkbook>(
    `/api/clubs/${clubId}/application-import/google-sheet/${encodeURIComponent(spreadsheetId)}/tables`,
  );
}

export function previewApplicationImport(
  clubId: string,
  generationId: string,
  rows: ApplicationImportRowInput[],
) {
  return apiRequest<ApplicationImportPreview>(`/api/clubs/${clubId}/application-import/preview`, {
    method: "POST",
    body: JSON.stringify({ generationId, rows }),
  });
}

export function applyApplicationImport(
  clubId: string,
  generationId: string,
  rows: ApplicationImportRowInput[],
) {
  return apiRequest<ApplicationImportApplyResult>(`/api/clubs/${clubId}/application-import/apply`, {
    method: "POST",
    body: JSON.stringify({ generationId, rows }),
  });
}

export function getGoogleConnectionStatus() {
  return apiRequest<GoogleConnectionStatus>("/api/google-data/status");
}

export function getGoogleAuthorizationUrl(returnPath: string) {
  return apiRequest<GoogleAuthorizationUrl>(
    `/api/google-data/oauth/authorization-url?returnPath=${encodeURIComponent(returnPath)}`,
  );
}

export function disconnectGoogleConnection() {
  return apiRequest<void>("/api/google-data/connection", {
    method: "DELETE",
  });
}
