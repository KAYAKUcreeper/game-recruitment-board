package com.gamerecruitment.recruitment.adapter.persistence;

import com.gamerecruitment.recruitment.application.port.RecruitmentQueries;
import com.gamerecruitment.recruitment.domain.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRecruitmentQueries implements RecruitmentQueries {
  private static final int PAGE_SIZE = 20;
  private final JdbcClient jdbc;

  public JdbcRecruitmentQueries(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<Game> games() {
    return List.of(Game.values());
  }

  public Page publicRecruitments(Optional<Game> game, int page, Instant now) {
    int offset = offset(page);
    String gameFilter = game.isPresent() ? " and r.game=:game" : "";
    var statement =
        jdbc.sql(
                "select r.*,a.display_name,(select count(*) from participations p where p.recruitment_id=r.id) participant_count"
                    + " from recruitments r join accounts a on a.id=r.organizer_id"
                    + " where not r.cancelled and not r.manually_closed and r.starts_at>:now"
                    + " and (select count(*) from participations p where p.recruitment_id=r.id)<r.capacity"
                    + gameFilter
                    + " order by r.starts_at,r.created_at,r.id limit 21 offset :offset")
            .param("now", Timestamp.from(now))
            .param("offset", offset);
    if (game.isPresent()) statement = statement.param("game", game.get().name());
    List<Summary> found = statement.query((rs, n) -> summary(rs, now)).list();
    boolean hasNext = found.size() > PAGE_SIZE;
    return new Page(List.copyOf(found.subList(0, Math.min(PAGE_SIZE, found.size()))), hasNext);
  }

  public Optional<Detail> publicRecruitment(UUID id, Instant now) {
    List<DetailRow> rows =
        jdbc.sql(
                "select r.*,o.display_name organizer_name,p.account_id,p.joined_at,pa.display_name participant_name,"
                    + " count(p.account_id) over(partition by r.id) participant_count"
                    + " from recruitments r join accounts o on o.id=r.organizer_id"
                    + " left join participations p on p.recruitment_id=r.id"
                    + " left join accounts pa on pa.id=p.account_id where r.id=:id"
                    + " order by p.joined_at,p.account_id")
            .param("id", id)
            .query(this::detailRow)
            .list();
    if (rows.isEmpty()) return Optional.empty();
    DetailRow first = rows.getFirst();
    List<Profile> participants =
        rows.stream().filter(row -> row.participant() != null).map(DetailRow::participant).toList();
    Recruitment recruitment = first.recruitment();
    return Optional.of(
        new Detail(
            recruitment.id(),
            recruitment.game(),
            recruitment.title(),
            recruitment.description(),
            recruitment.startsAt(),
            first.count(),
            recruitment.capacity(),
            recruitment.state(first.count(), now),
            first.organizer(),
            participants));
  }

  public Page myRecruitments(
      UUID accountId, boolean created, boolean upcoming, int page, Instant now) {
    throw new UnsupportedOperationException("Implemented with US6");
  }

  private int offset(int page) {
    if (page < 1) throw new IllegalArgumentException("page");
    try {
      return Math.multiplyExact(page - 1, PAGE_SIZE);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("page", exception);
    }
  }

  private Summary summary(ResultSet rs, Instant now) throws SQLException {
    Recruitment value = recruitment(rs);
    int count = rs.getInt("participant_count");
    return new Summary(
        value.id(),
        value.game(),
        value.title(),
        value.startsAt(),
        count,
        value.capacity(),
        value.state(count, now),
        new Profile(value.organizerId(), rs.getString("display_name")));
  }

  private DetailRow detailRow(ResultSet rs, int rowNumber) throws SQLException {
    UUID participantId = rs.getObject("account_id", UUID.class);
    Profile participant =
        participantId == null ? null : new Profile(participantId, rs.getString("participant_name"));
    Recruitment value = recruitment(rs);
    return new DetailRow(
        value,
        new Profile(value.organizerId(), rs.getString("organizer_name")),
        participant,
        rs.getInt("participant_count"));
  }

  private Recruitment recruitment(ResultSet rs) throws SQLException {
    return new Recruitment(
        rs.getObject("id", UUID.class),
        rs.getObject("organizer_id", UUID.class),
        Game.valueOf(rs.getString("game")),
        rs.getString("title"),
        rs.getString("description"),
        rs.getTimestamp("starts_at").toInstant(),
        rs.getInt("capacity"),
        rs.getString("meeting_info"),
        rs.getBoolean("manually_closed"),
        rs.getBoolean("cancelled"),
        rs.getTimestamp("created_at").toInstant());
  }

  private record DetailRow(
      Recruitment recruitment, Profile organizer, Profile participant, int count) {}
}
