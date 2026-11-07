import { describe, it, expect, vi, afterEach } from "vitest";
import { post, refreshCsrf, ApiError } from "./client";
import { csrfToken, account } from "./validation";
afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; Path=/";
});
describe("API client", () => {
  it("sends CSRF and cookies without retrying failed POST", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ token: "current", headerName: "X-XSRF-TOKEN" }),
          { status: 200 },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: "SERVICE_UNAVAILABLE",
            message: "一時失敗",
            fields: [],
          }),
          { status: 503 },
        ),
      );
    vi.stubGlobal("fetch", fetch);
    await refreshCsrf();
    await expect(post("/accounts", {}, (v) => v)).rejects.toBeInstanceOf(
      ApiError,
    );
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(fetch.mock.calls[1][1]).toMatchObject({
      credentials: "same-origin",
      headers: { "X-XSRF-TOKEN": "current" },
    });
  });
  it("refreshes CSRF after 403 without resending POST", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ token: "old", headerName: "X-XSRF-TOKEN" }),
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: "CSRF_INVALID",
            message: "再操作",
            fields: [],
          }),
          { status: 403 },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ token: "new", headerName: "X-XSRF-TOKEN" }),
        ),
      );
    vi.stubGlobal("fetch", fetch);
    await refreshCsrf();
    await expect(post("/accounts", {}, (v) => v)).rejects.toBeInstanceOf(
      ApiError,
    );
    expect(fetch.mock.calls.filter((c) => c[1].method === "POST")).toHaveLength(
      1,
    );
  });
  it("uses the latest shared CSRF cookie after another tab updates it", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ token: "old", headerName: "X-XSRF-TOKEN" }),
        ),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetch);
    await refreshCsrf();
    document.cookie = "XSRF-TOKEN=new-from-tab; Path=/";
    await post("/auth/logout", undefined, () => undefined);
    expect(fetch.mock.calls[1][1].headers["X-XSRF-TOKEN"]).toBe("new-from-tab");
  });
  it("rejects invalid server data", () => {
    expect(() => csrfToken({ token: "a", headerName: "wrong" })).toThrow();
    expect(() => account({ id: "invalid", displayName: "もち" })).toThrow();
  });
});
