import { render, screen, fireEvent } from "@testing-library/react";
import { it, expect, vi } from "vitest";
import LoginPage from "./LoginPage";
import { ApiError } from "../../../shared/api/client";
const mock = vi.hoisted(() => ({ login: vi.fn() }));
vi.mock("../AuthProvider", () => ({
  useAuth: () => ({ state: null, loading: false, login: mock.login }),
}));
for (const [status, code, message] of [
  [401, "INVALID_CREDENTIALS", "認証情報が一致しません。"],
  [403, "CSRF_INVALID", "もう一度操作してください。"],
  [409, "ALREADY_AUTHENTICATED", "先にログアウトしてください。"],
  [503, "SERVICE_UNAVAILABLE", "現在処理できません。"],
] as const) {
  it(`shows ${status} without repeating login`, async () => {
    mock.login
      .mockReset()
      .mockRejectedValue(new ApiError(status, { code, message, fields: [] }));
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText("ログインID"), {
      target: { value: "player_a" },
    });
    fireEvent.change(screen.getByLabelText("パスワード"), {
      target: { value: "test-passphrase-12345" },
    });
    fireEvent.click(screen.getByRole("button", { name: "ログインする" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(mock.login).toHaveBeenCalledTimes(1);
  });
}
