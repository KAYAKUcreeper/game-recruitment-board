import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import { it, expect, vi, beforeEach, afterEach } from "vitest";
import { AuthProvider, useAuth } from "./AuthProvider";
import * as api from "./api/auth";
import AccountStatus from "./components/AccountStatus";
import LoginPage from "./pages/LoginPage";
import { ApiError } from "../../shared/api/client";
vi.mock("./api/auth", () => ({
  me: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  refreshCsrf: vi.fn(),
}));
const originalState = {
  account: { id: "c31568e5-1234-4ec7-9819-6653ac03de51", displayName: "もち" },
  expiresAt: new Date(Date.now() + 3600000).toISOString(),
};
let state = originalState;
function View() {
  const a = useAuth();
  return (
    <>
      <AccountStatus />
      <LoginPage />
      <p>{a.state?.account.displayName ?? "未ログイン"}</p>
      <button onClick={() => void a.logout().catch(() => undefined)}>
        終了
      </button>
    </>
  );
}
beforeEach(() => {
  state = originalState;
  vi.mocked(api.login).mockReset();
  vi.mocked(api.me).mockReset().mockResolvedValue(state);
  vi.mocked(api.refreshCsrf).mockReset().mockResolvedValue();
  vi.mocked(api.logout).mockReset();
});
it("keeps login state when logout fails with 503", async () => {
  vi.mocked(api.logout).mockRejectedValue(
    new ApiError(503, {
      code: "SERVICE_UNAVAILABLE",
      message: "失敗",
      fields: [],
    }),
  );
  render(
    <AuthProvider>
      <View />
    </AuthProvider>,
  );
  await screen.findByText("もち");
  fireEvent.click(screen.getByText("終了"));
  await waitFor(() => expect(api.logout).toHaveBeenCalledTimes(1));
  expect(screen.getByText("もち")).toBeInTheDocument();
  expect(api.refreshCsrf).toHaveBeenCalledTimes(1);
});
it("clears login and refreshes CSRF after successful logout", async () => {
  vi.mocked(api.logout).mockResolvedValue();
  render(
    <AuthProvider>
      <View />
    </AuthProvider>,
  );
  await screen.findByText("もち");
  fireEvent.click(screen.getByText("終了"));
  await screen.findByText("未ログイン");
  await waitFor(() => expect(api.refreshCsrf).toHaveBeenCalledTimes(2));
});
it("updates shared-tab state on 401", async () => {
  render(
    <AuthProvider>
      <View />
    </AuthProvider>,
  );
  await screen.findByText("もち");
  fireEvent(window, new Event("auth-expired"));
  await screen.findByText("未ログイン");
});

afterEach(() => vi.useRealTimers());
it("shows a fixed expiry, expires exactly on time and uses a new login expiry", async () => {
  vi.useFakeTimers();
  const start = Date.parse("2026-10-10T01:00:00Z");
  vi.setSystemTime(start);
  state = { ...originalState, expiresAt: "2026-10-10T02:00:00Z" };
  vi.mocked(api.me).mockResolvedValue(state);
  render(<AuthProvider><View /></AuthProvider>);
  await act(async () => { await Promise.resolve(); });
  expect(document.querySelector("time")).toHaveAttribute("datetime", state.expiresAt);
  await act(async () => { vi.advanceTimersByTime(3599000); });
  fireEvent(window, new Event("focus"));
  await act(async () => { await Promise.resolve(); });
  expect(screen.getByText("もちさんでログイン中")).toBeVisible();
  expect(document.querySelector("time")).toHaveAttribute("datetime", state.expiresAt);
  await act(async () => { vi.advanceTimersByTime(1000); });
  expect(screen.getByText("ログインしていません。")).toBeVisible();
  expect(screen.getByRole("button", { name: "ログインする" })).toBeVisible();
  expect(document.querySelector("time")).toBeNull();
  const next = { ...state, expiresAt: "2026-10-10T03:00:00Z" };
  vi.mocked(api.login).mockResolvedValue(next);
  fireEvent.change(screen.getByLabelText("ログインID"), { target: { value: "player_a" } });
  fireEvent.change(screen.getByLabelText("パスワード"), { target: { value: "test-passphrase-12345" } });
  fireEvent.click(screen.getByRole("button", { name: "ログインする" }));
  await act(async () => { await Promise.resolve(); });
  expect(document.querySelector("time")).toHaveAttribute("datetime", next.expiresAt);
  expect(screen.getByText("もちさんでログイン中")).toBeVisible();
});
