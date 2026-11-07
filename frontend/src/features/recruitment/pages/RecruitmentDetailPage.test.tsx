import { render, screen } from "@testing-library/react";
import { it, expect, vi, afterEach } from "vitest";
import RecruitmentDetailPage from "./RecruitmentDetailPage";
afterEach(() => vi.unstubAllGlobals());
it("renders public information as text with Japan time and participant names", async () => {
  const id = "11111111-1111-1111-1111-111111111111";
  vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify({ id, game: "OVERWATCH", title: "募集", startsAt: "2026-11-07T15:00:00Z", capacity: 1, participantCount: 1, state: "FULL", organizer: { id, displayName: "主催" }, description: "<script>alert(1)</script>\n二行目", participants: [{ id: "22222222-2222-2222-2222-222222222222", displayName: "参加者名" }] }))));
  const { container } = render(<RecruitmentDetailPage id={id} />);
  expect(await screen.findByText("参加者名")).toBeInTheDocument();
  expect(screen.getByText("満員")).toBeInTheDocument();
  expect(screen.getByText(/2026\/11\/08 00:00/)).toBeInTheDocument();
  expect(container.querySelector("script")).toBeNull();
  expect(container.querySelector(".recruitment-text")?.textContent).toBe("<script>alert(1)</script>\n二行目");
});
it("shows not found for a 404 response", async () => {
  vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify({ code: "RECRUITMENT_NOT_FOUND", message: "募集が見つかりません。", fields: [] }), { status: 404 })));
  render(<RecruitmentDetailPage id="missing" />);
  expect(await screen.findByRole("alert")).toHaveTextContent("募集が見つかりません。");
});
