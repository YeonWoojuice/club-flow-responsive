import { useState } from "react";
import { ErrorToast } from "../components/ErrorToast";
import { useAuth } from "./AuthContext";

export function AuthErrorNotice() {
  const { error, refresh, status } = useAuth();
  const [retrying, setRetrying] = useState(false);

  if (!error || status === "error" || status === "loading") {
    return null;
  }

  const handleRetry = async () => {
    setRetrying(true);
    try {
      await refresh();
    } finally {
      setRetrying(false);
    }
  };

  return (
    <ErrorToast
      message={error}
      actionLabel="재시도"
      actionPending={retrying}
      onAction={() => void handleRetry()}
    />
  );
}
