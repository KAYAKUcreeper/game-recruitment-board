package com.gamerecruitment.recruitment.adapter.persistence;

import com.gamerecruitment.recruitment.application.port.RecruitmentRepository;
import com.gamerecruitment.recruitment.domain.Game;
import com.gamerecruitment.recruitment.domain.Recruitment;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRecruitmentRepository implements RecruitmentRepository {
  private final JdbcClient jdbc;

  public JdbcRecruitmentRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insert(Recruitment value) {
    jdbc.sql(
            "insert into recruitments"
                + " (id,organizer_id,game,title,description,starts_at,capacity,meeting_info,manually_closed,cancelled,created_at)"
                + " values (:id,:organizer,:game,:title,:description,:startsAt,:capacity,:meetingInfo,:closed,:cancelled,:createdAt)")
        .param("id", value.id())
        .param("organizer", value.organizerId())
        .param("game", value.game().name())
        .param("title", value.title())
        .param("description", value.description())
        .param("startsAt", Timestamp.from(value.startsAt()))
        .param("capacity", value.capacity())
        .param("meetingInfo", value.meetingInfo())
        .param("closed", value.manuallyClosed())
        .param("cancelled", value.cancelled())
        .param("createdAt", Timestamp.from(value.createdAt()))
        .update();
  }

  @Override
  public Optional<Recruitment> lock(UUID id) {
    return locked(id, " for update");
  }

  @Override
  public Optional<Recruitment> lockForShare(UUID id) {
    return locked(id, " for share");
  }

  private Optional<Recruitment> locked(UUID id, String lock) {
    return jdbc.sql("select * from recruitments where id=:id" + lock)
        .param("id", id)
        .query(this::row)
        .optional();
  }

  private Recruitment row(ResultSet rs, int rowNumber) throws SQLException {
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

  @Override
  public int participantCount(UUID recruitmentId) {
    return jdbc.sql("select count(*) from participations where recruitment_id=:id")
        .param("id", recruitmentId)
        .query(Long.class)
        .single()
        .intValue();
  }

  @Override
  public boolean isParticipating(UUID recruitmentId, UUID accountId) {
    return jdbc.sql(
            "select exists(select 1 from participations where recruitment_id=:recruitment and account_id=:account)")
        .param("recruitment", recruitmentId)
        .param("account", accountId)
        .query(Boolean.class)
        .single();
  }

  @Override
  public void addParticipation(UUID recruitmentId, UUID accountId, Instant joinedAt) {
    try {
      jdbc.sql(
              "insert into participations (recruitment_id,account_id,joined_at) values (:recruitment,:account,:joinedAt)")
          .param("recruitment", recruitmentId)
          .param("account", accountId)
          .param("joinedAt", Timestamp.from(joinedAt))
          .update();
    } catch (DuplicateKeyException exception) {
      throw new com.gamerecruitment.recruitment.application.RecruitmentFailure(
          com.gamerecruitment.recruitment.application.RecruitmentFailure.Code
              .ALREADY_PARTICIPATING);
    }
  }

  @Override
  public int removeParticipation(UUID recruitmentId, UUID accountId) {
    return jdbc.sql(
            "delete from participations where recruitment_id=:recruitment and account_id=:account")
        .param("recruitment", recruitmentId)
        .param("account", accountId)
        .update();
  }

  @Override
  public void setManuallyClosed(UUID id, boolean closed) {
    jdbc.sql("update recruitments set manually_closed=:closed where id=:id")
        .param("closed", closed)
        .param("id", id)
        .update();
  }

  @Override
  public void setCancelled(UUID id) {
    jdbc.sql("update recruitments set cancelled=true where id=:id").param("id", id).update();
  }

  @Override
  public void updateMeetingInfo(UUID id, String meetingInfo) {
    jdbc.sql("update recruitments set meeting_info=:meetingInfo where id=:id")
        .param("meetingInfo", meetingInfo)
        .param("id", id)
        .update();
  }
}
