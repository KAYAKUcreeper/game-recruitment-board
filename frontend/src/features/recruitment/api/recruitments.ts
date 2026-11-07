import { request } from "../../../shared/api/client";
import { detail, page, gameList, type Game } from "./validation";
export const listGames = () => request("/games", { method: "GET" }, gameList);
export const listRecruitments = (game: Game | "", currentPage: number) => {
  const query = new URLSearchParams({ page: String(currentPage) });
  if (game) query.set("game", game);
  return request(`/recruitments?${query}`, { method: "GET" }, page);
};
export const getRecruitment = (id: string) => request(`/recruitments/${encodeURIComponent(id)}`, { method: "GET" }, detail);
