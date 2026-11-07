const nonSpace =
  /[^\u0009-\u000D\u0020\u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]/u;
export function inputErrors(
  loginId: string,
  password: string,
  displayName?: string,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!/^[A-Za-z0-9_]{3,30}$/.test(loginId) || loginId.includes("\n"))
    errors.loginId = "半角英数字と_を使い、3〜30文字で入力してください。";
  if (
    [...password].length < 15 ||
    [...password].length > 128 ||
    !nonSpace.test(password)
  )
    errors.password = "15〜128文字で入力してください。空白だけにはできません。";
  if (
    displayName !== undefined &&
    ([...displayName].length < 1 ||
      [...displayName].length > 30 ||
      !nonSpace.test(displayName))
  )
    errors.displayName =
      "1〜30文字で入力してください。空白だけにはできません。";
  return errors;
}
