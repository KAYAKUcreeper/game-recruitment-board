import { useEffect, useState } from "react";
import { ApiError } from "../../../shared/api/client";
import { getRecruitment } from "../api/recruitments";
import { games, states, type Detail } from "../api/validation";
import { japanTime } from "./RecruitmentListPage";
export default function RecruitmentDetailPage({ id }: { id: string }) {
  const [data, setData] = useState<Detail>();
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setData(undefined); setError("");
    getRecruitment(id).then(result => { if (active) setData(result); }).catch(error => {
      if (active) setError(error instanceof ApiError && error.status === 404 ? "募集が見つかりません。" : "募集を取得できませんでした。再表示してください。");
    });
    return () => { active = false; };
  }, [id, retry]);
  return <section>
    <a href="/">募集一覧へ戻る</a>
    <button onClick={() => setRetry(value => value + 1)}>再表示</button>
    {error ? <p role="alert">{error}</p> : !data ? <p role="status">読み込み中…</p> : <>
      <h2>{data.title}</h2><p>{games[data.game]}</p>
      <p>{japanTime(data.startsAt)}（日本時間）</p><p>{states[data.state]}</p>
      <p>募集者：{data.organizer.displayName}</p>
      <p className="recruitment-text">{data.description}</p>
      <h3>参加者（{data.participantCount}／{data.capacity}人）</h3>
      {data.participants.length ? <ul>{data.participants.map(person => <li key={person.id}>{person.displayName}</li>)}</ul> : <p>参加者はまだいません。</p>}
    </>}
  </section>;
}
