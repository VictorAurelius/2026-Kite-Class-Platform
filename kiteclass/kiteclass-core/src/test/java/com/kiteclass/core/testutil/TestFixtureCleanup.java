package com.kiteclass.core.testutil;

import com.kiteclass.core.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Defense-in-depth test fixture cleanup utility (GAP-735, Wave meta-1 Bucket B).
 *
 * <p>This {@link TestExecutionListener} provides defense-in-depth cleanup of
 * <strong>non-transactional</strong> test fixture state that {@code @Transactional} +
 * {@code @Rollback} (Bucket A) cannot revert. It runs both before and after
 * each test method, ensuring each test starts and ends with a clean ambient
 * context regardless of the previous test's success or failure.
 *
 * <h2>When to use this listener vs {@code @Transactional}</h2>
 *
 * <table>
 *   <caption>Cleanup mechanism selection guide</caption>
 *   <tr><th>Failure mode</th><th>Use</th></tr>
 *   <tr>
 *     <td>DB rows committed in test transaction</td>
 *     <td>{@code @Transactional} + {@code @Rollback} (Bucket A) — Spring rolls
 *         back automatically. Cheapest, idiomatic.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link TenantContext} ThreadLocal leaking across tests</td>
 *     <td>This listener — {@code @Transactional} does not touch ThreadLocals.</td>
 *   </tr>
 *   <tr>
 *     <td>JPA L2 cache holding stale entities between tests</td>
 *     <td>This listener — {@code EntityManager.clear()} runs even if rollback
 *         already happened.</td>
 *   </tr>
 *   <tr>
 *     <td>Orphaned {@code TransactionSynchronizationManager} bindings (e.g.,
 *         test threw before Spring could clean up)</td>
 *     <td>This listener — defensive reset.</td>
 *   </tr>
 *   <tr>
 *     <td>Rows written via {@code @Async} / outbox / fresh
 *         {@code PROPAGATION_REQUIRES_NEW} transaction (committed regardless of
 *         outer rollback)</td>
 *     <td>Combine: extend this listener and add table-specific
 *         {@code TRUNCATE} in {@link #afterTestMethod(TestContext)}, OR write
 *         a per-class {@code @AfterEach} truncate.</td>
 *   </tr>
 * </table>
 *
 * <h2>Usage</h2>
 *
 * <p>Add to any Spring Boot integration test class. Listener auto-wires the
 * shared {@link EntityManager} when present; in slice tests without JPA it
 * silently no-ops on the {@code EntityManager.clear()} step.
 *
 * <pre>{@code
 * @SpringBootTest
 * @TestExecutionListeners(
 *     value = TestFixtureCleanup.class,
 *     mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
 * )
 * class MyIntegrationTest {
 *     // ...
 * }
 * }</pre>
 *
 * <p>Use {@code MERGE_WITH_DEFAULTS} so Spring's default listeners (e.g.,
 * {@code DependencyInjectionTestExecutionListener},
 * {@code TransactionalTestExecutionListener}) continue to run.
 *
 * <h2>Ordering relative to {@code @Transactional}</h2>
 *
 * <p>Spring's {@code TransactionalTestExecutionListener} runs at order
 * {@code 4000}; this listener uses Spring's default ({@code Ordered.LOWEST_PRECEDENCE}),
 * so cleanup happens <em>after</em> Spring rolls back the test transaction —
 * the correct order for L2 cache + ThreadLocal cleanup.
 *
 * @author KiteClass Team
 * @since meta-1 (GAP-735)
 * @see TenantContext
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 */
public class TestFixtureCleanup implements TestExecutionListener, Ordered {

    /** Cached TRUNCATE statement built lazily on first use (one query per JVM, not per test). */
    private static final AtomicReference<String> CACHED_TRUNCATE_SQL = new AtomicReference<>();

    /**
     * Order must be {@code < 4000} so {@link #beforeTestMethod(TestContext)} runs
     * <strong>before</strong> Spring's {@code TransactionalTestExecutionListener}
     * (order 4000) starts the test transaction. Otherwise our truncate executes
     * inside the test transaction and is rolled back along with the test's own
     * writes — defeating the purpose. 3500 puts us between
     * {@code DirtiesContextTestExecutionListener} (3000) and
     * {@code TransactionalTestExecutionListener} (4000).
     */
    @Override
    public int getOrder() {
        return 3500;
    }

    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }

    @Autowired(required = false)
    private void setDataSource(DataSource dataSource) {
        if (dataSource != null) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }

    /**
     * Resets ambient context BEFORE each test method.
     *
     * <p>This guards against the previous test having thrown before Spring
     * could clean up — defensive belt-and-suspenders alongside
     * {@link #afterTestMethod(TestContext)}.
     *
     * @param testContext the Spring test context (unused; declared for SPI)
     */
    @Override
    public void beforeTestMethod(@NonNull TestContext testContext) {
        clearAmbientState();
    }

    /**
     * Cleans up ambient state AFTER each test method.
     *
     * <p>Runs unconditionally — even if the test failed or threw — because
     * Spring's listener contract invokes this hook in a {@code finally}-like
     * position around the test method.
     *
     * @param testContext the Spring test context (unused; declared for SPI)
     */
    @Override
    public void afterTestMethod(@NonNull TestContext testContext) {
        clearAmbientState();
    }

    /**
     * Single point of cleanup for all ambient state this listener manages.
     *
     * <p>Order matters: clear EntityManager first (so any pending operations
     * are dropped), then clear ThreadLocals (so subsequent test sees a clean
     * tenant context), then defensive synchronization-manager reset.
     */
    private void clearAmbientState() {
        // 0. DB rows committed outside test transaction (async listeners, REQUIRES_NEW,
        //    event publishers) — truncate all user tables. Runs in autocommit mode via
        //    JdbcTemplate (separate connection from test transaction); cache TRUNCATE SQL
        //    after first run to avoid pg_tables roundtrip per test.
        truncateAllUserTables();

        // 1. JPA L2 / first-level cache — drop stale managed entities so the
        //    next test re-loads from DB.
        if (entityManager != null && entityManager.isOpen()) {
            try {
                entityManager.clear();
            } catch (Exception ignored) {
                // EntityManager may be in a broken state after a failed test;
                // swallow so cleanup itself does not mask the original failure.
            }
        }

        // 2. Tenant ThreadLocal — Spring's transactional listener does NOT
        //    touch this. Without explicit clear, the next test inherits the
        //    previous tenant ID and triggers cross-tenant assertions like
        //    "Cannot update entity from different tenant" (GAP-735 root cause).
        TenantContext.clear();

        // 3. Defensive: clear any leftover Spring synchronization bindings
        //    when no transaction is currently active. This is a no-op in the
        //    99% case but recovers state when a test throws mid-transaction
        //    setup before the @Transactional listener takes over.
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                TransactionSynchronizationManager.clear();
            } catch (IllegalStateException ignored) {
                // No synchronizations active — nothing to clear.
            }
        }
    }

    /**
     * Truncate all user tables in the test DB to clear rows committed outside the test
     * transaction (async listeners, REQUIRES_NEW, event publishers — none of which
     * {@code @Transactional@Rollback} can revert).
     *
     * <p>Strategy: query {@code pg_tables} once per JVM to discover which tables exist
     * (avoids hardcoded list that mismatches {@code ddl-auto: create-drop} schema), then
     * build and cache a single {@code TRUNCATE ... CASCADE} statement. CASCADE handles
     * FK constraints; RESTART IDENTITY resets sequences so subsequent test row-count/id
     * assertions start from clean slate.
     *
     * <p>Runs via {@link JdbcTemplate} which uses a fresh connection from the pool —
     * outside the test's {@code @Transactional} context — so the truncate persists even
     * when the test transaction is rolled back.
     *
     * <p>If {@code DataSource} is not autowired (e.g., slice tests without JPA), this
     * step is a no-op.
     */
    private void truncateAllUserTables() {
        if (jdbcTemplate == null) {
            return;
        }
        try {
            String sql = CACHED_TRUNCATE_SQL.get();
            if (sql == null) {
                List<String> tables = jdbcTemplate.queryForList(
                        "SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
                                "AND tablename <> 'flyway_schema_history'",
                        String.class);
                if (tables.isEmpty()) {
                    return;
                }
                // Quote each table name to handle any reserved-word collisions safely.
                StringBuilder builder = new StringBuilder("TRUNCATE TABLE ");
                for (int i = 0; i < tables.size(); i++) {
                    if (i > 0) builder.append(", ");
                    builder.append('"').append(tables.get(i)).append('"');
                }
                builder.append(" RESTART IDENTITY CASCADE");
                sql = builder.toString();
                CACHED_TRUNCATE_SQL.compareAndSet(null, sql);
            }
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Cleanup failure should not mask test failure; swallow.
            // If truncate consistently fails, downstream test assertions will surface it.
        }
    }
}
