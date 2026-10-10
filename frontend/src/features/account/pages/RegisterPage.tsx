import { useState, type FormEvent } from "react";
import { register } from "../api/register";
import PasswordField from "../components/PasswordField";
import { inputErrors } from "../input";
import { ApiError } from "../../../shared/api/client";
export default function RegisterPage() {
  const [loginId, setId] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setName] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    const fields = inputErrors(loginId, password, displayName);
    setErrors(fields);
    setMessage("");
    if (Object.keys(fields).length) return;
    setBusy(true);
    try {
      await register({ loginId, password, displayName });
      setPassword("");
      setSuccess(true);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "登録できませんでした。");
      if (e instanceof ApiError)
        setErrors(
          Object.fromEntries(
            e.body.fields.map((f) => [f, "入力内容を確認してください。"]),
          ),
        );
    } finally {
      setBusy(false);
    }
  }
  if (success)
    return (
      <section>
        <h2>登録完了</h2>
        <p>アカウントを作成しました。</p>
        <a href="/login">ログインへ進む</a>
      </section>
    );
  return (
    <section>
      <h2>アカウント登録</h2>
      <p>ログインIDは非公開です。表示名は他の利用者に公開されます。</p>
      <form
        onSubmit={(e) => {
          void submit(e);
        }}
        noValidate
      >
        <label>
          ログインID
          <input
            autoComplete="username"
            value={loginId}
            onChange={(e) => setId(e.target.value)}
            aria-invalid={!!errors.loginId}
            aria-describedby="id-error"
          />
        </label>
        {errors.loginId && (
          <p id="id-error" className="field-error">
            {errors.loginId}
          </p>
        )}
        <PasswordField
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
          invalid={!!errors.password}
        />
        {errors.password && (
          <p id="password-error" className="field-error">
            {errors.password}
          </p>
        )}
        <label>
          表示名
          <textarea
            rows={2}
            autoComplete="nickname"
            value={displayName}
            onChange={(e) => setName(e.target.value)}
            aria-invalid={!!errors.displayName}
            aria-describedby="name-error"
          />
        </label>
        {errors.displayName && (
          <p id="name-error" className="field-error">
            {errors.displayName}
          </p>
        )}
        {message && <p role="alert">{message}</p>}
        <button disabled={busy}>{busy ? "登録中…" : "登録する"}</button>
      </form>
    </section>
  );
}
