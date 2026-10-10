import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { it, expect, vi, beforeEach } from "vitest";
import { AuthProvider, useAuth } from "./AuthProvider";
import * as api from "./api/auth";
import { ApiError } from "../../shared/api/client";
vi.mock("./api/auth", () => ({
  me: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  refreshCsrf: vi.fn(),
}));
const state = {
  account: { id: "c31568e5-1234-4ec7-9819-6653ac03de51", displayName: "もち" },
  expiresAt: new Date(Date.now() + 3600000).toISOString(),
};
function View() {
  const a = useAuth();
  return (
    <>
      <p>{a.state?.account.displayName ?? "未ログイン"}</p>
      <button onClick={() => void a.logout().catch(() => undefined)}>
        終了
      </button>
    </>
  );
}
beforeEach(() => {
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
