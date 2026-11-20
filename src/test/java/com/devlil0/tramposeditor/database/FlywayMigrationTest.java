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

import java.sql.SQLException;
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
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine")).withDatabaseName("v12_disposable_test");

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

    @Test
    void preparoLegadoEReproduzivelAteAUltimaMigracaoComEmpateSufixoETmp() throws Exception {
        List<Map<String, Object>> expected = null;
        for (String schema : List.of("prepared_first", "prepared_second", "prepared_suffix")) {
            JdbcTemplate jdbc = prepareV11(schema);
            addCollisions(jdbc);
            if (schema.equals("prepared_suffix")) {
                jdbc.update("UPDATE users SET created_at = '2020-01-04' WHERE email = 'recente@example.test'");
            }
            var source = dataSource(schema);
            var before = legacySnapshot(jdbc);
            var summary = V12LegacyUpgrade.diagnose(source, schema);
            assertThat(summary.accounts()).isEqualTo(8);
            assertThat(summary.duplicateGroups()).isGreaterThan(0);
            assertThat(summary.temporaryPrefixCandidates()).isEqualTo(2);
            assertThat(legacySnapshot(jdbc)).isEqualTo(before);
            assertThat(flyway(schema, MigrationVersion.LATEST).info().current().getVersion())
                    .isEqualTo(MigrationVersion.fromVersion("11"));

            assertThat(V12LegacyUpgrade.apply(source, schema, (point, connection) -> {})).isEqualTo(summary);
            assertCompleteHistory(flyway(schema, MigrationVersion.LATEST));
            assertThat(legacySnapshot(jdbc)).isEqualTo(before);
            var assignments = jdbc.queryForList("SELECT id, username FROM users ORDER BY id");
            if (expected == null) expected = assignments;
            else assertThat(assignments).isEqualTo(expected);
            assertThat(jdbc.queryForList("SELECT username FROM users ORDER BY id", String.class))
                    .containsExactly("marina-luz-10000000", "marina-luz", "ana-cafe", "ana-cafe-40000000-2",
                            "u-50000000", "ana-cafe-40000000", "tmp-legado", "tmp-avulso");
            assertThat(jdbc.queryForObject("""
                    SELECT u.username FROM editor_profiles e JOIN users u ON u.id = e.user_id
                    WHERE e.slug = 'tmp-legado'
                    """, String.class)).isEqualTo("tmp-legado");
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM information_schema.tables
                    WHERE table_schema = current_schema() AND table_name LIKE 'v12_stage%'
                    """, Long.class)).isZero();
        }
    }

    @Test
    void quedaDaConexaoNoPreparoERollbackDepoisDaV13PermitemNovaExecucao() throws Exception {
        String schema = "prepared_rollback";
        JdbcTemplate jdbc = prepareV11(schema);
        addCollisions(jdbc);
        var before = legacySnapshot(jdbc);
        var source = dataSource(schema);
        for (String failurePoint : List.of("after-stage", "after-restore")) {
            assertThatThrownBy(() -> V12LegacyUpgrade.apply(source, schema, (point, connection) -> {
                if (!point.equals(failurePoint)) return;
                if (point.equals("after-stage")) {
                    int pid;
                    try (var statement = connection.createStatement();
                         var result = statement.executeQuery("SELECT pg_backend_pid()")) {
                        result.next();
                        pid = result.getInt(1);
                    }
                    assertThat(jdbc.queryForObject("SELECT pg_terminate_backend(?)", Boolean.class, pid)).isTrue();
                    return;
                }
                throw new SQLException("Falha sintética após restaurar os valores.");
            })).isInstanceOf(FlywayException.class);
            assertThat(flyway(schema, MigrationVersion.LATEST).info().current().getVersion())
                    .isEqualTo(MigrationVersion.fromVersion("11"));
            assertThat(legacySnapshot(jdbc)).isEqualTo(before);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'username'
                    """, Long.class)).isZero();
        }
        V12LegacyUpgrade.apply(source, schema, (point, connection) -> {});
        assertCompleteHistory(flyway(schema, MigrationVersion.LATEST));
        assertThat(legacySnapshot(jdbc)).isEqualTo(before);
    }

    @Test
    void preparoRecusaBancoVazioEBancoAtualizadoSemAlterarHistoricoOuDados() {
        String schema = "prepared_refusal";
        var jdbc = new JdbcTemplate(dataSource(schema));
        jdbc.execute("CREATE SCHEMA prepared_refusal");
        var source = dataSource(schema);
        assertThatThrownBy(() -> V12LegacyUpgrade.apply(source, schema, (point, connection) -> {}))
                .isInstanceOf(SQLException.class);
        assertThat(jdbc.queryForObject("SELECT to_regclass('flyway_schema_history')::text", String.class)).isNull();

        var latest = flyway(schema, MigrationVersion.LATEST);
        latest.migrate();
        jdbc.update("""
                INSERT INTO users (id, email, display_name, username, active, created_at)
                VALUES ('90000000-0000-0000-0000-000000000009', 'atual@example.test',
                        'Atual', 'tmp-ja-atribuido', true, '2020-01-10')
                """);
        var history = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
        assertThatThrownBy(() -> V12LegacyUpgrade.apply(source, schema, (point, connection) -> {}))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> V12LegacyUpgrade.diagnose(source, schema)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT username FROM users", String.class)).isEqualTo("tmp-ja-atribuido");
        assertThat(jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank")).isEqualTo(history);
        assertCompleteHistory(latest);
    }

    private static void addCollisions(JdbcTemplate jdbc) {
        jdbc.update("""
                UPDATE users SET created_at = (SELECT created_at FROM users WHERE email = 'antiga@example.test')
                WHERE email = 'recente@example.test'
                """);
        jdbc.update("""
                INSERT INTO users (id, email, display_name, active, created_at) VALUES
                ('60000000-0000-0000-0000-000000000006', 'sufixo@example.test', 'Ana Cafe 40000000', true, '2020-01-06'),
                ('70000000-0000-0000-0000-000000000007', 'tmp@example.test', 'Editor Tmp', true, '2020-01-07'),
                ('80000000-0000-0000-0000-000000000008', 'avulso@example.test', 'Tmp Avulso', true, '2020-01-08')
                """);
        jdbc.update("""
                INSERT INTO editor_profiles (id, user_id, slug, vertical, created_at) VALUES
                ('a0000000-0000-0000-0000-000000000007', '70000000-0000-0000-0000-000000000007',
                 'tmp-legado', 'YOUTUBE', '2020-01-07')
                """);
    }

    private static List<Map<String, Object>> legacySnapshot(JdbcTemplate jdbc) {
        return jdbc.queryForList("""
                SELECT u.id, u.email, u.password_hash, u.display_name, u.active, u.created_at,
                       e.id AS editor_id, e.slug, r.role, i.id AS identity_id, i.provider, i.subject
                FROM users u
                LEFT JOIN editor_profiles e ON e.user_id = u.id
                LEFT JOIN user_roles r ON r.user_id = u.id
                LEFT JOIN user_identities i ON i.user_id = u.id
                ORDER BY u.id, r.role, i.id
                """);
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
