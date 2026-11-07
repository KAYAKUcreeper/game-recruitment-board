import { apiError, csrfToken, type ApiErrorBody } from "./validation";
export class ApiError extends Error {
  constructor(
    public status: number,
    public body: ApiErrorBody,
  ) {
    super(body.message);
  }
}
let csrf: string | undefined;
function cookieCsrf(): string | undefined {
  const raw = document.cookie
    .split("; ")
    .find((c) => c.startsWith("XSRF-TOKEN="))
    ?.slice("XSRF-TOKEN=".length);
  try {
    return raw ? decodeURIComponent(raw) : undefined;
  } catch {
    return undefined;
  }
}
export async function refreshCsrf(): Promise<void> {
  csrf = undefined;
  csrf = await request("/auth/csrf", { method: "GET" }, csrfToken);
}
export async function request<T>(
  path: string,
  options: RequestInit,
  parse: (v: unknown) => T,
): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...options,
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok) {
    const error = new ApiError(
      response.status,
      apiError(await response.json()),
    );
    if (response.status === 401)
      window.dispatchEvent(new Event("auth-expired"));
    if (response.status === 403) {
      csrf = undefined;
      if (path !== "/auth/csrf") await refreshCsrf().catch(() => undefined);
    }
    throw error;
  }
  return parse(response.status === 204 ? undefined : await response.json());
}
export async function post<T>(
  path: string,
  body: unknown,
  parse: (v: unknown) => T,
): Promise<T> {
  if (!csrf) await refreshCsrf();
  return request(
    path,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-XSRF-TOKEN": cookieCsrf() ?? csrf!,
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    },
    parse,
  );
}
export function empty(v: unknown): void {
  if (v !== undefined) throw new Error("API応答の形式が不正です。");
}
