import { ApiError } from "../api/http";

export type AuthFailureType = "unauthorized" | "temporary";

export function classifyAuthFailure(error: unknown): AuthFailureType {
  return error instanceof ApiError && error.status === 401 ? "unauthorized" : "temporary";
}
