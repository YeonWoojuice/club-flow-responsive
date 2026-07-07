import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { API_AUTH_ERROR_EVENT, type ApiAuthErrorDetail } from "../api/http";
import { useAuth } from "./AuthContext";

export function ApiErrorNavigator() {
  const navigate = useNavigate();
  const { clear } = useAuth();

  useEffect(() => {
    const handleAuthError = (event: Event) => {
      const { status } = (event as CustomEvent<ApiAuthErrorDetail>).detail;
      if (status === 401) {
        clear();
        navigate("/login", { replace: true });
        return;
      }
      navigate("/clubs", { replace: true });
    };

    window.addEventListener(API_AUTH_ERROR_EVENT, handleAuthError);
    return () => window.removeEventListener(API_AUTH_ERROR_EVENT, handleAuthError);
  }, [clear, navigate]);

  return null;
}
