package com.gamerecruitment.account.adapter.persistence;

import com.gamerecruitment.account.application.AccountFailure;
import com.gamerecruitment.account.application.port.AccountRepository;
import java.sql.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountRepository implements AccountRepository {
  private final JdbcClient jdbc;

  public JdbcAccountRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void save(StoredAccount a) {
    try {
      jdbc.sql(
              "insert into accounts (id,login_id,display_name,password_hash,created_at) values"
                  + " (:id,:login,:name,:hash,:time)")
          .param("id", a.id())
          .param("login", a.loginId())
          .param("name", a.displayName())
          .param("hash", a.passwordHash())
          .param("time", Timestamp.from(a.createdAt()))
          .update();
    } catch (DuplicateKeyException e) {
      throw new AccountFailure(AccountFailure.Code.LOGIN_ID_TAKEN);
    }
  }

  private StoredAccount row(ResultSet rs, int n) throws SQLException {
    return new StoredAccount(
        rs.getObject("id", UUID.class),
        rs.getString("login_id"),
        rs.getString("display_name"),
        rs.getString("password_hash"),
        rs.getTimestamp("created_at").toInstant());
  }

  public Optional<StoredAccount> findByLoginId(String id) {
    return jdbc.sql("select * from accounts where login_id=:id")
        .param("id", id)
        .query(this::row)
        .optional();
  }

  public Optional<StoredAccount> findById(UUID id) {
    return jdbc.sql("select * from accounts where id=:id")
        .param("id", id)
        .query(this::row)
        .optional();
  }
}
