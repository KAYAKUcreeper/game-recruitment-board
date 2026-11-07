import { useEffect, useState } from "react";
import { listGames, listRecruitments } from "../api/recruitments";
import { games, states, type Game, type Page } from "../api/validation";
export function japanTime(value: string): string {
  return new Intl.DateTimeFormat("ja-JP", { timeZone: "Asia/Tokyo", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(value));
}
export default function RecruitmentListPage() {
  const [game, setGame] = useState<Game | "">("");
  const [choices, setChoices] = useState<Game[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [data, setData] = useState<Page>();
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setData(undefined); setError("");
    Promise.all([listGames(), listRecruitments(game, currentPage)]).then(([available, result]) => {
      if (active) { setChoices(available); setData(result); }
    }).catch(() => { if (active) setError("募集を取得できませんでした。再表示してください。"); });
    return () => { active = false; };
  }, [game, currentPage, retry]);
  return <section>
    <h2>募集を探す</h2>
    <label>ゲーム<select value={game} onChange={event => { setGame(event.target.value as Game | ""); setCurrentPage(1); }}>
      <option value="">すべてのゲーム</option>{choices.map(code => <option key={code} value={code}>{games[code]}</option>)}
    </select></label>
    <button onClick={() => setRetry(value => value + 1)}>再表示</button>
    {error ? <p role="alert">{error}</p> : !data ? <p role="status">読み込み中…</p> : <>
      {data.items.length === 0 ? <p>該当する募集はありません。</p> : <ul>{data.items.map(item => <li key={item.id}>
        <a href={`/recruitments/${item.id}`}>{item.title}</a>
        <p>{games[item.game]} ／ {japanTime(item.startsAt)}（日本時間）</p>
        <p>{item.organizer.displayName} ／ {item.participantCount}／{item.capacity}人 ／ {states[item.state]}</p>
      </li>)}</ul>}
      <p>{currentPage}ページ</p>
      <button disabled={currentPage === 1} onClick={() => setCurrentPage(value => value - 1)}>前のページ</button>
      <button disabled={!data.hasNext} onClick={() => setCurrentPage(value => value + 1)}>次のページ</button>
    </>}
  </section>;
}
