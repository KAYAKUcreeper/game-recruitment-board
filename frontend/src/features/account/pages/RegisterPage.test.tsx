import { render, screen, fireEvent } from "@testing-library/react";
import { it, expect, vi, beforeEach, afterEach } from "vitest";
import RegisterPage from "./RegisterPage";
import { refreshCsrf } from "../../../shared/api/client";
let status: number;
let response: unknown;
let fetchMock: ReturnType<typeof vi.fn>;
beforeEach(async () => {
  status = 201;
  response = {
    id: "c31568e5-1234-4ec7-9819-6653ac03de51",
    displayName: "もち",
  };
  fetchMock = vi.fn(
    async (path: string) =>
      new Response(
        JSON.stringify(
          path.endsWith("/auth/csrf")
            ? { token: "current", headerName: "X-XSRF-TOKEN" }
            : response,
        ),
        { status: path.endsWith("/auth/csrf") ? 200 : status },
      ),
  );
  vi.stubGlobal("fetch", fetchMock);
  await refreshCsrf();
  fetchMock.mockClear();
});
afterEach(() => vi.unstubAllGlobals());
function fill(
  id = "player_a",
  password = "  test-passphrase-12345  ",
  name = "もち",
) {
  fireEvent.change(screen.getByLabelText("ログインID"), {
    target: { value: id },
  });
  fireEvent.change(screen.getByLabelText("パスワード"), {
    target: { value: password },
  });
  fireEvent.change(screen.getByLabelText("表示名"), {
    target: { value: name },
  });
  fireEvent.click(screen.getByRole("button", { name: "登録する" }));
}
it("preserves password and accepts 30 Unicode code points", async () => {
  response = {
    id: "c31568e5-1234-4ec7-9819-6653ac03de51",
    displayName: "😀".repeat(30),
  };
  render(<RegisterPage />);
  fill("player_a", "  test-passphrase-12345  ", "😀".repeat(30));
  await screen.findByText("登録完了");
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    loginId: "player_a",
    password: "  test-passphrase-12345  ",
    displayName: "😀".repeat(30),
  });
  expect(screen.getByRole("link", { name: "ログインへ進む" })).toHaveAttribute(
    "href",
    "/login",
  );
});
it("rejects invalid boundaries and whitespace without a request", () => {
  render(<RegisterPage />);
  fill("aa", "x".repeat(14), "　");
  expect(fetchMock).not.toHaveBeenCalled();
  expect(screen.getAllByText(/入力してください/)).toHaveLength(3);
});
it("shows duplicate-ID error from API", async () => {
  status = 409;
  response = {
    code: "LOGIN_ID_TAKEN",
    message: "このログインIDは使用されています。",
    fields: [],
  };
  render(<RegisterPage />);
  fill();
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "使用されています",
  );
  expect(fetchMock).toHaveBeenCalledTimes(1);
});
