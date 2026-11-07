export interface Account {
  id: string;
  displayName: string;
}
export interface LoginState {
  account: Account;
  expiresAt: string;
}
export interface ApiErrorBody {
  code: string;
  message: string;
  fields: string[];
}
const object = (v: unknown): v is Record<string, unknown> =>
  typeof v === "object" && v !== null && !Array.isArray(v);
const only = (v: Record<string, unknown>, keys: string[]) =>
  Object.keys(v).every((k) => keys.includes(k));
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function account(v: unknown): Account {
  if (
    !object(v) ||
    !only(v, ["id", "displayName"]) ||
    typeof v.id !== "string" ||
    !uuid.test(v.id) ||
    typeof v.displayName !== "string" ||
    [...v.displayName].length < 1 ||
    [...v.displayName].length > 30 ||
    !/[^\u0009-\u000D\u0020\u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]/u.test(
      v.displayName,
    )
  )
    throw new Error("API応答の形式が不正です。");
  return { id: v.id, displayName: v.displayName };
}
export function loginState(v: unknown): LoginState {
  if (
    !object(v) ||
    !only(v, ["account", "expiresAt"]) ||
    typeof v.expiresAt !== "string" ||
    !/^\d{4}-\d{2}-\d{2}T/.test(v.expiresAt) ||
    Number.isNaN(Date.parse(v.expiresAt))
  )
    throw new Error("API応答の形式が不正です。");
  return { account: account(v.account), expiresAt: v.expiresAt };
}
export function csrfToken(v: unknown): string {
  if (
    !object(v) ||
    !only(v, ["token", "headerName"]) ||
    typeof v.token !== "string" ||
    !v.token ||
    v.headerName !== "X-XSRF-TOKEN"
  )
    throw new Error("API応答の形式が不正です。");
  return v.token;
}
export function apiError(v: unknown): ApiErrorBody {
  const codes = [
    "VALIDATION_ERROR",
    "UNAUTHENTICATED",
    "CSRF_INVALID",
    "LOGIN_ID_TAKEN",
    "UNSUPPORTED_MEDIA_TYPE",
    "INTERNAL_ERROR",
    "SERVICE_UNAVAILABLE",
    "INVALID_CREDENTIALS",
    "ALREADY_AUTHENTICATED",
    "METHOD_NOT_ALLOWED",
  ];
  if (
    !object(v) ||
    !only(v, ["code", "message", "fields"]) ||
    typeof v.code !== "string" ||
    !codes.includes(v.code) ||
    typeof v.message !== "string" ||
    !v.message ||
    !Array.isArray(v.fields) ||
    !v.fields.every(
      (f) =>
        typeof f === "string" &&
        ["loginId", "password", "displayName"].includes(f),
    ) ||
    new Set(v.fields).size !== v.fields.length
  )
    throw new Error("API応答の形式が不正です。");
  return { code: v.code, message: v.message, fields: v.fields as string[] };
}
