interface Props {
  value: string;
  onChange: (value: string) => void;
  autoComplete: "new-password" | "current-password";
  invalid: boolean;
}

export default function PasswordField({ value, onChange, autoComplete, invalid }: Props) {
  // Hide unsupported fields by default; never fall back to visible password text.
  const supported = typeof CSS !== "undefined" && CSS.supports("-webkit-text-security", "disc");
  return (
    <div>
      <label>
        パスワード
        <textarea
          className="password-input"
          rows={2}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          autoComplete={autoComplete}
          autoCapitalize="none"
          spellCheck={false}
          disabled={!supported}
          aria-invalid={invalid}
          aria-describedby="password-error password-help"
        />
      </label>
      <p id="password-help">
        {supported ? "15〜128文字。改行も使用できます。" : "パスワードを安全に入力するため、最新版のChromeまたはFirefoxを使用してください。"}
      </p>
    </div>
  );
}
