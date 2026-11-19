package br.com.urbanrisk.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/** Mimics Supabase: PostgREST roles exist and get default grants on every new public table. */
class SupabaseRolesMigrationIT {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void migrate() throws Exception {
        PG.start();
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("create role anon nologin");
            s.execute("create role authenticated nologin");
            s.execute("alter default privileges in schema public grant all on tables to anon, authenticated");
        }
        Flyway.configure().dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()).load().migrate();
    }

    @AfterAll
    static void stop() {
        PG.stop();
    }

    static Connection connect() throws Exception {
        return DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
    }

    static boolean query(String sql) throws Exception {
        try (Connection c = connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getBoolean(1);
        }
    }

    @Test
    void riskEventsHasRlsAndNoPostgrestGrants() throws Exception {
        assertThat(query("select relrowsecurity from pg_class where relname = 'risk_events'")).isTrue();
        for (String role : new String[]{"anon", "authenticated"}) {
            assertThat(query("select has_table_privilege('" + role + "', 'risk_events', 'SELECT,INSERT,UPDATE,DELETE')"))
                    .as(role).isFalse();
        }
    }

    @Test
    void flywayHistoryIsNotExposedToPostgrestRoles() throws Exception {
        assertThat(query("select relrowsecurity from pg_class where relname = 'flyway_schema_history'")).isTrue();
        for (String role : new String[]{"anon", "authenticated"}) {
            assertThat(query("select has_table_privilege('" + role + "', 'flyway_schema_history', 'SELECT,INSERT,UPDATE,DELETE')"))
                    .as(role).isFalse();
        }
    }
}
