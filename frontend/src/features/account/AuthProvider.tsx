import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import * as api from "./api/auth";
import { ApiError } from "../../shared/api/client";
import type { LoginState } from "../../shared/api/validation";
interface Auth {
  state: LoginState | null;
  loading: boolean;
  error: string;
  login: (id: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}
const Context = createContext<Auth | undefined>(undefined);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<LoginState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    let live = true;
    const check = async () => {
      try {
        const current = await api.me();
        if (live) {
          setState(current);
          setError("");
        }
      } catch (e) {
        if (live) {
          if (e instanceof ApiError && e.status === 401) setState(null);
          else
            setError(
              e instanceof Error ? e.message : "状態を確認できませんでした。",
            );
        }
      }
    };
    api
      .refreshCsrf()
      .then(check)
      .catch((e) => {
        if (live)
          setError(e instanceof Error ? e.message : "接続できませんでした。");
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    const expired = () => {
      if (live) setState(null);
    };
    const focused = () => {
      void check();
    };
    window.addEventListener("auth-expired", expired);
    window.addEventListener("focus", focused);
    return () => {
      live = false;
      window.removeEventListener("auth-expired", expired);
      window.removeEventListener("focus", focused);
    };
  }, []);
  useEffect(() => {
    if (!state) return;
    const delay = Date.parse(state.expiresAt) - Date.now();
    const timer = setTimeout(() => setState(null), Math.max(0, delay));
    return () => clearTimeout(timer);
  }, [state]);
  const login = async (id: string, password: string) => {
    const result = await api.login(id, password);
    setState(result);
    setError("");
    await api.refreshCsrf();
  };
  const logout = async () => {
    await api.logout();
    setState(null);
    setError("");
    await api.refreshCsrf();
  };
  return (
    <Context.Provider value={{ state, loading, error, login, logout }}>
      {children}
    </Context.Provider>
  );
}
export function useAuth() {
  const auth = useContext(Context);
  if (!auth) throw new Error("AuthProviderが必要です。");
  return auth;
}
