package app.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public class GmailNotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public GmailNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveEmail(
            String messageId,
            String threadId,
            String subject,
            String sender,
            String received_at,
            Long historyId) {

        jdbcTemplate.update("""
                            INSERT OR IGNORE INTO emails
                            (gmail_message_id,
                             thread_id,
                             subject,
                             sender,
                             received_at,
                             history_id)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """,
                messageId,
                threadId,
                subject,
                sender,
                received_at,
                historyId
        );
    }

}