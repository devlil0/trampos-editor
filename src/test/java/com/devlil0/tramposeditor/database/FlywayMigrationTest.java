package com.devlil0.tramposeditor.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class FlywayMigrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Test
    void bancoVazioAplicaTodasAsMigracoesEAceitaValidacao() {
        String schema = "empty_install";
        Flyway flyway = flyway(schema, MigrationVersion.LATEST);

        flyway.migrate();

        assertCompleteHistory(flyway);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource(schema));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_signups", Long.class)).isZero();
    }

    @Test
    void upgradeDeV11PreservaContasEResolveColisoesComDatasDistintas() {
        String schema = "legacy_upgrade";
        JdbcTemplate jdbc = prepareV11(schema);
        Flyway flyway = flyway(schema, MigrationVersion.LATEST);

        flyway.migrate();

        assertCompleteHistory(flyway);
        assertThat(jdbc.queryForList("SELECT username FROM users ORDER BY id", String.class))
                .containsExactly("marina-luz-10000000", "marina-luz", "ana-cafe", "ana-cafe-40000000", "u-50000000");
        assertThat(jdbc.queryForList("SELECT email FROM users ORDER BY id", String.class))
                .containsExactly("cliente@example.test", "editor@example.test", "antiga@example.test",
                        "recente@example.test", "simbolos@example.test");
        assertThat(jdbc.queryForList("SELECT username FROM users WHERE NOT active", String.class))
                .containsExactly("ana-cafe-40000000");
        assertThat(jdbc.queryForList("""
                SELECT u.username, r.role FROM user_roles r JOIN users u ON u.id = r.user_id
                ORDER BY r.role
                """)).containsExactly(Map.of("username", "marina-luz-10000000", "role", "CLIENT"),
                Map.of("username", "marina-luz", "role", "EDITOR"));
        assertThat(jdbc.queryForList("""
                SELECT u.username FROM editor_profiles e JOIN users u ON u.id = e.user_id
                WHERE e.slug = 'marina-luz'
                """, String.class)).containsExactly("marina-luz");
        assertThat(jdbc.queryForList("""
                SELECT u.username FROM user_identities i JOIN users u ON u.id = i.user_id
                WHERE i.subject = 'fixture-editor' AND i.provider = 'github'
                """, String.class)).containsExactly("marina-luz");
        assertThat(jdbc.queryForList("""
                SELECT username FROM users WHERE email_verified_at IS NOT NULL AND email_verified_at = created_at
                """, String.class)).containsExactly("marina-luz");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM users
                WHERE email_verified_at IS NULL AND username <> 'marina-luz'
                """, Long.class)).isEqualTo(4L);
        assertThat(jdbc.queryForList("SELECT DISTINCT session_version FROM users", Long.class))
                .containsExactly(0L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE username_chosen", Long.class))
                .isEqualTo(5L);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE users SET username = 'marina-luz' WHERE email = 'cliente@example.test'
                """)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void documentaLimiteConhecidoDaV12ComHomonimosNaMesmaData() {
        String schema = "legacy_equal_timestamp";
        JdbcTemplate jdbc = prepareV11(schema);
        assertThat(jdbc.update("""
                UPDATE users SET created_at = (SELECT created_at FROM users WHERE email = 'antiga@example.test')
                WHERE email = 'recente@example.test'
                """)).isEqualTo(1);
        Flyway flyway = flyway(schema, MigrationVersion.LATEST);

        assertThatThrownBy(flyway::migrate)
                .isInstanceOf(FlywayException.class)
                .hasRootCauseInstanceOf(PSQLException.class)
                .rootCause()
                .isInstanceOfSatisfying(PSQLException.class, failure -> {
                    assertThat(failure.getSQLState()).isEqualTo("23505");
                    assertThat(failure.getServerErrorMessage()).isNotNull();
                    assertThat(failure.getServerErrorMessage().getConstraint()).isEqualTo("uk_users_username");
                });

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("11");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isEqualTo(5L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'username'
                """, Long.class)).isZero();
    }

    private static void assertCompleteHistory(Flyway flyway) {
        flyway.validate();
        List<String> applied = Arrays.stream(flyway.info().applied())
                .map(MigrationInfo::getVersion)
                .filter(version -> version != null)
                .map(MigrationVersion::getVersion)
                .toList();
        assertThat(applied).containsSequence("11", "12", "13", "14", "15", "16", "17", "18", "19");
        assertThat(flyway.info().pending()).isEmpty();
    }

    private static JdbcTemplate prepareV11(String schema) {
        Flyway v11 = flyway(schema, MigrationVersion.fromVersion("11"));
        v11.migrate();
        assertThat(v11.info().current().getVersion().getVersion()).isEqualTo("11");
        DriverManagerDataSource dataSource = dataSource(schema);
        new ResourceDatabasePopulator(new ClassPathResource("migration/legacy-v11.sql")).execute(dataSource);
        return new JdbcTemplate(dataSource);
    }

    private static Flyway flyway(String schema, MigrationVersion target) {
        return Flyway.configure()
                .dataSource(dataSource(schema))
                .schemas(schema)
                .target(target)
                .load();
    }

    private static DriverManagerDataSource dataSource(String schema) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Properties properties = new Properties();
        properties.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(properties);
        return dataSource;
    }
}
