import { AuthProvider } from "./auth/AuthProvider";
import { AuthErrorNotice } from "./auth/AuthErrorNotice";
import { ApiErrorNavigator } from "./auth/ApiErrorNavigator";
import { AppRouter } from "./router/AppRouter";

export function App() {
  return (
    <AuthProvider>
      <ApiErrorNavigator />
      <AuthErrorNotice />
      <AppRouter />
    </AuthProvider>
  );
}
