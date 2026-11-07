import { AuthProvider } from "./features/account/AuthProvider";
import RegisterPage from "./features/account/pages/RegisterPage";
import LoginPage from "./features/account/pages/LoginPage";
import AccountStatus from "./features/account/components/AccountStatus";
export default function App() {
  return (
    <AuthProvider>
      <main>
        <h1>ゲーム募集掲示板</h1>
        <nav>
          <a href="/register">アカウント登録</a>
          <a href="/login">ログイン</a>
        </nav>
        <AccountStatus />
        {window.location.pathname === "/register" ? (
          <RegisterPage />
        ) : (
          <LoginPage />
        )}
      </main>
    </AuthProvider>
  );
}
