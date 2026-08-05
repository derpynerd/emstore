package app.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS emails (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                gmail_message_id TEXT UNIQUE NOT NULL,
                thread_id TEXT,
                subject TEXT,
                sender TEXT,
                received_at DATETIME,
                history_id INTEGER
            );
        """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gmail_state (
                    id INTEGER PRIMARY KEY,
                    history_id INTEGER NOT NULL
                )
        """);
    }

}
