import { AuthProvider } from "./features/account/AuthProvider";
import RegisterPage from "./features/account/pages/RegisterPage";
import LoginPage from "./features/account/pages/LoginPage";
import AccountStatus from "./features/account/components/AccountStatus";
import RecruitmentListPage from "./features/recruitment/pages/RecruitmentListPage";
import RecruitmentDetailPage from "./features/recruitment/pages/RecruitmentDetailPage";
export default function App() {
  return (
    <AuthProvider>
      <main>
        <h1>ゲーム募集掲示板</h1>
        <nav>
          <a href="/">募集一覧</a>
          <a href="/register">アカウント登録</a>
          <a href="/login">ログイン</a>
        </nav>
        <AccountStatus />
        {window.location.pathname === "/register" ? (
          <RegisterPage />
        ) : window.location.pathname === "/login" ? (
          <LoginPage />
        ) : window.location.pathname === "/" ? <RecruitmentListPage />
          : /^\/recruitments\/[^/]+$/.test(window.location.pathname)
            ? <RecruitmentDetailPage id={window.location.pathname.split("/")[2]} />
            : <p role="alert">ページが見つかりません。</p>}
      </main>
    </AuthProvider>
  );
}
