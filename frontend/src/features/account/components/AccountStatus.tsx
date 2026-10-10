import { useState } from "react";
import { useAuth } from "../AuthProvider";
export default function AccountStatus() {
  const { state, loading, error, logout } = useAuth();
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  async function exit() {
    setBusy(true);
    setMessage("");
    try {
      await logout();
    } catch (e) {
      setMessage(
        e instanceof Error ? e.message : "ログアウトできませんでした。",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <section aria-label="ログイン状態">
      {loading ? (
        <p>状態を確認しています…</p>
      ) : state ? (
        <>
          <p>{state.account.displayName}さんでログイン中</p>
          <p>
            有効期限：
            <time dateTime={state.expiresAt}>
              {new Date(state.expiresAt).toLocaleString("ja-JP")}
            </time>
          </p>
          <button disabled={busy} onClick={exit}>
            {busy ? "処理中…" : "ログアウト"}
          </button>
        </>
      ) : (
        <p>ログインしていません。</p>
      )}
      {(error || message) && <p role="alert">{message || error}</p>}
    </section>
  );
}
