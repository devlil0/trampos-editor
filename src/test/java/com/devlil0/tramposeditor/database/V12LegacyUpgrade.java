package com.devlil0.tramposeditor.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Ferramenta de manutenção fora do JAR da aplicação. */
public final class V12LegacyUpgrade {
    private static final String CANDIDATES = """
            SELECT u.id, u.created_at, u.display_name, e.slug,
                   COALESCE(NULLIF(COALESCE(CASE WHEN LENGTH(e.slug) <= 30 THEN e.slug END,
                       LEFT(TRIM(BOTH '-' FROM REGEXP_REPLACE(LOWER(TRANSLATE(u.display_name,
                           'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ',
                           'aaaaaeeeeiiiiooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')),
                           '[^a-z0-9]+', '-', 'g')), 30)), ''), 'u-' || LEFT(u.id::TEXT, 8)) AS seed
            FROM users u LEFT JOIN editor_profiles e ON e.user_id = u.id
            ORDER BY u.created_at, u.id
            """;

    private V12LegacyUpgrade() {}

    public static void main(String[] args) {
        if (args.length != 1 || !Set.of("diagnose", "apply-disposable").contains(args[0])) {
            System.err.println("Use diagnose ou apply-disposable.");
            System.exit(2);
        }
        try {
            var dataSource = localDataSource();
            var summary = args[0].equals("diagnose")
                    ? diagnose(dataSource, "public") : apply(dataSource, "public", (point, connection) -> {});
            System.out.println(summary);
        } catch (Exception failure) {
            System.err.println("Operação recusada ou interrompida. Confira o estado do Flyway antes de continuar.");
            System.exit(1);
        }
    }

    private static PGSimpleDataSource localDataSource() {
        String url = System.getenv("V12_JDBC_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[1-9][0-9]{0,4}/v12_disposable_[a-z0-9_]+")) {
            throw new IllegalArgumentException("Informe uma conexão descartável local.");
        }
        var source = new PGSimpleDataSource();
        source.setUrl(url);
        String user = System.getenv("V12_DB_USER");
        String password = System.getenv("V12_DB_PASSWORD");
        if (user == null || user.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Informe credenciais descartaveis.");
        }
        source.setUser(user);
        source.setPassword(password);
        source.setLogServerErrorDetail(false);
        return source;
    }

    static Summary diagnose(DataSource source, String schema) throws SQLException {
        try (var connection = source.getConnection()) {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            connection.setSchema(schema);
            requireV11(connection, schema);
            return plan(connection).summary();
        }
    }

    static Summary apply(DataSource source, String schema, RunnableAfterStage checkpoint) throws SQLException {
        diagnose(source, schema);
        var callback = new Preparation(schema, checkpoint);
        var flyway = Flyway.configure().dataSource(source).schemas(schema)
                .createSchemas(false).group(true).mixed(false).callbacks(callback).load();
        flyway.migrate();
        require(callback.restored, "O preparo esperado não aconteceu.");
        flyway.validate();
        require(flyway.info().pending().length == 0, "Há migrações pendentes.");
        return callback.plan.summary();
    }

    private static void requireV11(Connection connection, String schema) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT starts_with(current_database(), 'v12_disposable_'),
                       current_schema() = ?, count(*) = 11,
                       count(*) FILTER (WHERE success AND type = 'SQL' AND version IN
                         ('1','2','3','4','5','6','7','8','9','10','11')) = 11,
                       count(DISTINCT version) = 11,
                       NOT EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_schema = ? AND table_name = 'users' AND column_name = 'username')
                FROM flyway_schema_history
                WHERE NOT (version IS NULL AND type = 'SCHEMA' AND installed_rank = 0 AND success)
                """)) {
            statement.setString(1, schema);
            statement.setString(2, schema);
            try (var result = statement.executeQuery()) {
                result.next();
                for (int i = 1; i <= 6; i++) {
                    require(result.getBoolean(i), "O destino deve ser uma base descartável intacta na V11.");
                }
            }
        }
    }

    private static Plan plan(Connection connection) throws SQLException {
        var rows = new ArrayList<Row>();
        var slugs = new HashMap<String, UUID>();
        try (var statement = connection.createStatement(); var result = statement.executeQuery(CANDIDATES)) {
            while (result.next()) {
                var row = new Row(result.getObject("id", UUID.class), result.getString("display_name"),
                        result.getString("slug"), result.getString("seed"), result.getObject("created_at", LocalDateTime.class));
                rows.add(row);
                if (row.slug() != null) slugs.put(row.slug(), row.id());
            }
        }
        var earliest = new HashMap<String, LocalDateTime>();
        var projected = new HashMap<String, Integer>();
        var names = new LinkedHashMap<UUID, String>();
        Set<String> occupied = new HashSet<>(slugs.keySet());
        for (Row row : rows) {
            var oldest = earliest.computeIfAbsent(row.seed(), ignored -> row.createdAt());
            String v12 = row.createdAt().isAfter(oldest) ? suffixed(row) : row.seed();
            projected.merge(v12, 1, Integer::sum);

            UUID owner = slugs.get(row.seed());
            if (row.id().equals(owner) || (owner == null && occupied.add(row.seed()))) {
                names.put(row.id(), row.seed());
            }
        }
        for (Row row : rows) {
            if (!names.containsKey(row.id())) {
                names.put(row.id(), allocate(suffixed(row), occupied::add));
            }
        }

        var staged = new ArrayList<Staged>();
        int renamed = 0;
        int tmp = 0;
        for (Row row : rows) {
            String name = names.get(row.id());
            boolean renamedFromSeed = !name.equals(row.seed());
            boolean tmpPrefix = row.seed().startsWith("tmp-");
            if (renamedFromSeed) renamed++;
            if (tmpPrefix) tmp++;
            if (renamedFromSeed || tmpPrefix) {
                String alias = allocate("v12-stage-" + staged.size(), occupied::add);
                staged.add(new Staged(row, alias, name, usesLegacySlug(row)));
            }
        }
        var summary = new Summary(rows.size(), projected.values().stream().filter(n -> n > 1).count(),
                projected.values().stream().filter(n -> n > 1).mapToInt(Integer::intValue).sum(),
                renamed, tmp, staged.size());
        return new Plan(rows, names, staged, summary);
    }

    private static boolean usesLegacySlug(Row row) {
        return row.slug() != null && row.slug().codePointCount(0, row.slug().length()) <= 30;
    }

    private static String suffixed(Row row) {
        return prefix(row.seed(), 21) + "-" + row.id().toString().substring(0, 8);
    }

    private static String allocate(String base, Predicate<String> available) {
        String candidate = base;
        for (long next = 2; !available.test(candidate); next++) {
            String suffix = "-" + next;
            candidate = prefix(base, 30 - suffix.length()) + suffix;
        }
        return candidate;
    }

    private static String prefix(String value, int max) {
        int end = value.offsetByCodePoints(0, Math.min(max, value.codePointCount(0, value.length())));
        return value.substring(0, end);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    record Summary(int accounts, long duplicateGroups, int affectedAccounts,
                   int suffixedAccounts, int temporaryPrefixCandidates, int stagedAccounts) {}

    private record Row(UUID id, String displayName, String slug, String seed, LocalDateTime createdAt) {}

    private record Staged(Row row, String alias, String name, boolean useSlug) {}

    private record Plan(List<Row> rows, Map<UUID, String> names, List<Staged> staged, Summary summary) {}

    @FunctionalInterface
    interface RunnableAfterStage {
        void run(String point, Connection connection) throws SQLException;
    }

    private static final class Preparation implements Callback {
        private final String schema;
        private final RunnableAfterStage checkpoint;
        private Connection transaction;
        private Plan plan;
        private boolean restored;

        private Preparation(String schema, RunnableAfterStage checkpoint) {
            this.schema = schema;
            this.checkpoint = checkpoint;
        }

        @Override
        public String getCallbackName() {
            return "preparo-legado-v11";
        }

        @Override
        public boolean supports(Event event, Context context) {
            return (event == Event.BEFORE_EACH_MIGRATE || event == Event.AFTER_EACH_MIGRATE)
                    && context != null && context.getMigrationInfo() != null
                    && context.getMigrationInfo().getVersion() != null;
        }

        @Override
        public boolean canHandleInTransaction(Event event, Context context) {
            return true;
        }

        @Override
        public void handle(Event event, Context context) {
            var connection = context.getConnection();
            String version = context.getMigrationInfo().getVersion().getVersion();
            try {
                require(!connection.getAutoCommit(), "A migração deve ser transacional.");
                if (event == Event.BEFORE_EACH_MIGRATE && version.equals("12")) {
                    stage(connection);
                }
                if (event == Event.AFTER_EACH_MIGRATE && version.equals("13")) {
                    require(connection == transaction && plan != null, "Transação de preparo ausente.");
                    restore(connection);
                }
            } catch (SQLException failure) {
                throw new IllegalStateException("Falha no preparo da V11. SQLSTATE: " + failure.getSQLState());
            }
        }

        private void stage(Connection connection) throws SQLException {
            transaction = connection;
            try (var statement = connection.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '5s'");
                statement.execute("LOCK TABLE users, editor_profiles IN ACCESS EXCLUSIVE MODE");
            }
            requireV11(connection, schema);
            plan = plan(connection);
            for (Staged item : plan.staged()) {
                replaceSeed(connection, item, item.alias());
            }
            checkpoint.run("after-stage", connection);
        }

        private void restore(Connection connection) throws SQLException {
            for (Staged item : plan.staged()) {
                try (var statement = connection.prepareStatement(
                        "UPDATE users SET username = ? WHERE id = ? AND username = ?")) {
                    statement.setString(1, item.name());
                    statement.setObject(2, item.row().id());
                    statement.setString(3, item.alias());
                    require(statement.executeUpdate() == 1, "O username intermediário divergiu do plano.");
                }
                replaceSeed(connection, item, item.useSlug() ? item.row().slug() : item.row().displayName());
            }
            var original = new HashMap<UUID, Row>();
            for (Row row : plan.rows()) original.put(row.id(), row);
            try (var statement = connection.createStatement(); var result = statement.executeQuery("""
                    SELECT u.id, u.username, u.display_name, u.created_at, e.slug
                    FROM users u LEFT JOIN editor_profiles e ON e.user_id = u.id
                    """)) {
                while (result.next()) {
                    UUID id = result.getObject("id", UUID.class);
                    Row row = original.remove(id);
                    require(row != null && plan.names().get(id).equals(result.getString("username"))
                                    && row.displayName().equals(result.getString("display_name"))
                                    && row.createdAt().equals(result.getObject("created_at", LocalDateTime.class))
                                    && java.util.Objects.equals(row.slug(), result.getString("slug")),
                            "Os dados resultantes divergem do plano.");
                }
            }
            require(original.isEmpty(), "Faltam contas após o preparo.");
            checkpoint.run("after-restore", connection);
            restored = true;
        }

        private static void replaceSeed(Connection connection, Staged item, String value) throws SQLException {
            String sql = item.useSlug() ? "UPDATE editor_profiles SET slug = ? WHERE user_id = ?"
                    : "UPDATE users SET display_name = ? WHERE id = ?";
            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, value);
                statement.setObject(2, item.row().id());
                require(statement.executeUpdate() == 1, "Não foi possível preparar uma conta.");
            }
        }
    }
}
