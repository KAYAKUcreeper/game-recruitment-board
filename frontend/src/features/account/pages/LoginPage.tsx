import { useState, type FormEvent } from "react";
import { useAuth } from "../AuthProvider";
import { inputErrors } from "../input";
export default function LoginPage() {
  const auth = useAuth();
  const [loginId, setId] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    const fields = inputErrors(loginId, password);
    setErrors(fields);
    setMessage("");
    if (Object.keys(fields).length) return;
    setBusy(true);
    try {
      await auth.login(loginId, password);
      setPassword("");
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "ログインできませんでした。");
    } finally {
      setBusy(false);
    }
  }
  if (auth.state) return null;
  return (
    <section>
      <h2>ログイン</h2>
      <form onSubmit={submit} noValidate>
        <label>
          ログインID
          <input
            autoComplete="username"
            value={loginId}
            onChange={(e) => setId(e.target.value)}
            aria-invalid={!!errors.loginId}
          />
        </label>
        {errors.loginId && <p className="field-error">{errors.loginId}</p>}
        <label>
          パスワード
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            aria-invalid={!!errors.password}
          />
        </label>
        {errors.password && <p className="field-error">{errors.password}</p>}
        {message && <p role="alert">{message}</p>}
        <button disabled={busy || auth.loading}>
          {busy ? "ログイン中…" : "ログインする"}
        </button>
      </form>
      <p>
        アカウントをお持ちでない方は<a href="/register">登録</a>してください。
      </p>
    </section>
  );
}
