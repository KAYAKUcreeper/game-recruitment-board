import { account, type Account } from "../../../shared/api/validation";

export const games = { VALORANT: "VALORANT", APEX_LEGENDS: "Apex Legends", OVERWATCH: "Overwatch" } as const;
export type Game = keyof typeof games;
export const states = { OPEN: "受付中", FULL: "満員", MANUALLY_CLOSED: "手動締め切り", CANCELLED: "中止", STARTED: "開始済み" } as const;
export interface Summary {
  id: string; game: Game; title: string; startsAt: string; capacity: number;
  participantCount: number; state: keyof typeof states; organizer: Account;
}
export interface Detail extends Summary { description: string; participants: Account[] }
export interface Page { items: Summary[]; page: number; pageSize: 20; hasNext: boolean }
function fail(): never { throw new Error("API応答の形式が不正です。"); }
function object(value: unknown, keys: string[]): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return fail();
  const result = value as Record<string, unknown>;
  if (Object.keys(result).length !== keys.length || !keys.every(key => Object.hasOwn(result, key))) return fail();
  return result;
}
const summaryKeys = ["id", "game", "title", "startsAt", "capacity", "participantCount", "state", "organizer"];
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function text(value: unknown, max: number): string {
  if (typeof value !== "string" || [...value].length < 1 || [...value].length > max || !/[^\p{White_Space}]/u.test(value) || /[\uD800-\uDFFF]/u.test(value)) return fail();
  return value;
}
function summaryValues(v: Record<string, unknown>): Summary {
  if (typeof v.id !== "string" || !uuid.test(v.id)
      || typeof v.game !== "string" || !Object.hasOwn(games, v.game)
      || typeof v.state !== "string" || !Object.hasOwn(states, v.state)
      || typeof v.startsAt !== "string" || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:00(?:\.0+)?Z$/.test(v.startsAt) || !Number.isFinite(Date.parse(v.startsAt))
      || typeof v.capacity !== "number" || !Number.isInteger(v.capacity) || v.capacity < 1 || v.capacity > 9
      || typeof v.participantCount !== "number" || !Number.isInteger(v.participantCount) || v.participantCount < 0 || v.participantCount > v.capacity) return fail();
  return { id: v.id, game: v.game as Game, title: text(v.title, 100), startsAt: v.startsAt,
    capacity: v.capacity, participantCount: v.participantCount, state: v.state as Summary["state"], organizer: account(v.organizer) };
}
export function summary(value: unknown): Summary { return summaryValues(object(value, summaryKeys)); }
export function detail(value: unknown): Detail {
  const v = object(value, [...summaryKeys, "description", "participants"]);
  const result = summaryValues(v);
  if (!Array.isArray(v.participants)) return fail();
  const participants = v.participants.map(account);
  if (participants.length !== result.participantCount || new Set(participants.map(p => p.id)).size !== participants.length || participants.some(p => p.id === result.organizer.id)) return fail();
  return { ...result, description: text(v.description, 2000), participants };
}
export function page(value: unknown): Page {
  const v = object(value, ["items", "page", "pageSize", "hasNext"]);
  if (!Array.isArray(v.items) || v.items.length > 20 || typeof v.page !== "number" || !Number.isSafeInteger(v.page) || v.page < 1 || v.pageSize !== 20 || typeof v.hasNext !== "boolean") return fail();
  return { items: v.items.map(summary), page: v.page, pageSize: 20, hasNext: v.hasNext };
}
export function gameList(value: unknown): Game[] {
  const v = object(value, ["items"]);
  if (!Array.isArray(v.items) || v.items.length !== 3) return fail();
  const result = v.items.map(item => {
    const entry = object(item, ["code", "name"]);
    if (typeof entry.code !== "string" || !Object.hasOwn(games, entry.code) || games[entry.code as Game] !== entry.name) return fail();
    return entry.code as Game;
  });
  if (new Set(result).size !== 3) return fail();
  return result;
}
