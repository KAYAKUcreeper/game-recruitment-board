import { render, screen, fireEvent } from "@testing-library/react";
import { it, expect, vi, afterEach } from "vitest";
import RecruitmentListPage from "./RecruitmentListPage";
import { page, detail } from "../api/validation";
const id = "11111111-1111-1111-1111-111111111111";
const item = { id, game: "VALORANT", title: "夜の募集", startsAt: "2026-11-07T15:00:00Z", capacity: 9, participantCount: 0, state: "OPEN", organizer: { id, displayName: "もち" } };
afterEach(() => vi.unstubAllGlobals());
it("filters games, changes page and displays dates in Japan time", async () => {
  const fetcher = vi.fn(async (url: string) => new Response(JSON.stringify(url === "/api/games"
    ? { items: [{ code: "VALORANT", name: "VALORANT" }, { code: "APEX_LEGENDS", name: "Apex Legends" }, { code: "OVERWATCH", name: "Overwatch" }] }
    : { items: [item], page: url.includes("page=2") ? 2 : 1, pageSize: 20, hasNext: true })));
  vi.stubGlobal("fetch", fetcher);
  render(<RecruitmentListPage />);
  expect(await screen.findByText("夜の募集")).toHaveAttribute("href", `/recruitments/${id}`);
  expect(screen.getByText(/2026\/11\/08 00:00/)).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("ゲーム"), { target: { value: "APEX_LEGENDS" } });
  await screen.findByText("夜の募集");
  expect(fetcher.mock.calls.some(([url]) => url.includes("game=APEX_LEGENDS"))).toBe(true);
  fireEvent.click(screen.getByRole("button", { name: "次のページ" }));
  await screen.findByText("2ページ");
  expect(fetcher.mock.calls.some(([url]) => url.includes("page=2"))).toBe(true);
});
it("shows an empty result", async () => {
  vi.stubGlobal("fetch", vi.fn(async (url: string) => new Response(JSON.stringify(url === "/api/games"
    ? { items: [{ code: "VALORANT", name: "VALORANT" }, { code: "APEX_LEGENDS", name: "Apex Legends" }, { code: "OVERWATCH", name: "Overwatch" }] }
    : { items: [], page: 1, pageSize: 20, hasNext: false }))));
  render(<RecruitmentListPage />);
  expect(await screen.findByText("該当する募集はありません。")).toBeInTheDocument();
});
it("rejects unknown, missing and wrongly typed response fields", () => {
  const valid = { items: [item], page: 1, pageSize: 20, hasNext: false };
  expect(page(valid).items).toHaveLength(1);
  for (const invalid of [{ ...valid, secret: "leak" }, { ...valid, pageSize: "20" }, { items: [] }, { ...valid, items: [{ ...item, meetingInfo: "secret" }] }])
    expect(() => page(invalid)).toThrow();
  expect(() => detail({ ...item, description: "text", participants: [], participantCount: 1 })).toThrow();
});
