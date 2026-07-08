import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { getCurrentUser } from "../api/auth";
import type { CurrentUser } from "../types/auth";
import { classifyAuthFailure } from "./authFailure";
import { AuthContext, type AuthContextValue, type AuthStatus } from "./AuthContext";

const TEMPORARY_AUTH_ERROR = "로그인 상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setError(null);
    setStatus(current => current === "error" ? "loading" : current);
    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
      setStatus("authenticated");
      return currentUser;
    } catch (requestError) {
      if (classifyAuthFailure(requestError) === "unauthorized") {
        setUser(null);
        setStatus("anonymous");
        return null;
      }
      setError(TEMPORARY_AUTH_ERROR);
      setStatus(current => current === "loading" ? "error" : current);
      return null;
    }
  }, []);

  useEffect(() => {
    let active = true;
    const loadCurrentUser = async () => {
      try {
        const currentUser = await getCurrentUser();
        if (!active) return;
        setUser(currentUser);
        setStatus("authenticated");
        setError(null);
      } catch (requestError) {
        if (!active) return;
        if (classifyAuthFailure(requestError) === "unauthorized") {
          setUser(null);
          setStatus("anonymous");
          setError(null);
          return;
        }
        setError(TEMPORARY_AUTH_ERROR);
        setStatus("error");
      }
    };
    void loadCurrentUser();
    return () => {
      active = false;
    };
  }, []);

  const clear = useCallback(() => {
    setUser(null);
    setStatus("anonymous");
    setError(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    status,
    user,
    error,
    refresh,
    clear,
  }), [clear, error, refresh, status, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
