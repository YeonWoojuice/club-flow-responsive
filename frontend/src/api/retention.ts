import { apiRequest } from "./http";
import type {
  GoogleConnectionStatus,
  GoogleAuthorizationUrl,
  ParsedWorkbook,
  RetentionApplyResult,
  RetentionImportRowInput,
  RetentionPreview,
} from "../types/retention";

export function parseRetentionFile(clubId: string, file: File) {
  const body = new FormData();
  body.append("file", file);
  return apiRequest<ParsedWorkbook>(`/api/clubs/${clubId}/member-retention/file/parse`, {
    method: "POST",
    body,
  });
}

export function readRetentionGoogleSheet(clubId: string, spreadsheetId: string) {
  return apiRequest<ParsedWorkbook>(
    `/api/clubs/${clubId}/member-retention/google-sheet/${encodeURIComponent(spreadsheetId)}/tables`,
  );
}

export function previewRetention(
  clubId: string,
  previousGenerationId: string,
  targetGenerationId: string,
  rows: RetentionImportRowInput[],
) {
  return apiRequest<RetentionPreview>(`/api/clubs/${clubId}/member-retention/preview`, {
    method: "POST",
    body: JSON.stringify({ previousGenerationId, targetGenerationId, rows }),
  });
}

export function applyRetention(
  clubId: string,
  previousGenerationId: string,
  targetGenerationId: string,
  personIds: string[],
) {
  return apiRequest<RetentionApplyResult>(`/api/clubs/${clubId}/member-retention/apply`, {
    method: "POST",
    body: JSON.stringify({ previousGenerationId, targetGenerationId, personIds }),
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
