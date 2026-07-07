import { AuthProvider } from "./auth/AuthProvider";
import { ApiErrorNavigator } from "./auth/ApiErrorNavigator";
import { AppRouter } from "./router/AppRouter";

export function App() {
  return (
    <AuthProvider>
      <ApiErrorNavigator />
      <AppRouter />
    </AuthProvider>
  );
}
