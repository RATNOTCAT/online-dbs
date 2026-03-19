package com.sterling.bankportal.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SqliteDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(SqliteDataSourceConfig.class);

    @Value("${app.db.path:}")
    private String configuredPath;

    @Value("${app.db.legacy-path:../backend/instance/banking.db}")
    private String legacyPath;

    private Path resolvedDatabasePath;

    @PostConstruct
    void prepare() {
        resolvedDatabasePath = resolveDatabasePath();
        Path parent = resolvedDatabasePath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to create SQLite directory: " + parent, exception);
            }
        }
        log.info("Using SQLite database at {}", resolvedDatabasePath);
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setJdbcUrl("jdbc:sqlite:" + resolvedDatabasePath);
        dataSource.setMaximumPoolSize(1);
        return dataSource;
    }

    private Path resolveDatabasePath() {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return resolvePath(configuredPath);
        }
        return resolvePath("./data/banking.db");
    }

    private Path resolvePath(String rawPath) {
        Path path = Path.of(rawPath);
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize().toAbsolutePath();
    }
}
