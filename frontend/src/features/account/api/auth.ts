import { post, request, empty, refreshCsrf } from "../../../shared/api/client";
import { loginState } from "../../../shared/api/validation";
export const me = () => request("/auth/me", { method: "GET" }, loginState);
export const login = (loginId: string, password: string) =>
  post("/auth/login", { loginId, password }, loginState);
export const logout = () => post("/auth/logout", undefined, empty);
export { refreshCsrf };
