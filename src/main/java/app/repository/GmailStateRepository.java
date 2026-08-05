package app.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GmailStateRepository {

    private final JdbcTemplate jdbcTemplate;

    public GmailStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long getHistoryId() {
        String sql = "SELECT history_id FROM gmail_state WHERE id = 1";

        return jdbcTemplate.query(
                sql,
                rs -> rs.next() ? rs.getLong("history_id") : null
        );
    }

    public void saveHistoryId(Long historyId) {

        jdbcTemplate.update("""
            INSERT INTO gmail_state(id, history_id)
            VALUES(1, ?)
            ON CONFLICT(id)
            DO UPDATE SET history_id = excluded.history_id
            """,
                historyId);
    }
}
