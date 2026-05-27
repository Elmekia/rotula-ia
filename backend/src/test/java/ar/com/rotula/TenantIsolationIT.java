package ar.com.rotula;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el aislamiento entre tenants usando Row-Level Security de PostgreSQL.
 * Usa JDBC puro para controlar con precisión el contexto de sesión (app.current_tenant).
 */
@Testcontainers
class TenantIsolationIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void runMigrations() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load()
                .migrate();
    }

    private Connection conn;

    @BeforeEach
    void connect() throws SQLException {
        conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        conn.setAutoCommit(true);
    }

    @AfterEach
    void disconnect() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ── Helpers ───────────────────────────────────────────────

    private void setTenantContext(UUID tenantId) throws SQLException {
        conn.createStatement().execute("SET app.current_tenant = '" + tenantId + "'");
    }

    private UUID insertTenant(String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tenants (id, name, plan, status) VALUES (?, ?, 'starter', 'active')")) {
            ps.setObject(1, id);
            ps.setString(2, name);
            ps.executeUpdate();
        }
        return id;
    }

    private UUID insertProduct(UUID tenantId, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO products (id, tenant_id, name, category, net_weight, weight_unit, status) "
                + "VALUES (?, ?, ?, 'alimento', 100, 'g', 'active')")) {
            ps.setObject(1, id);
            ps.setObject(2, tenantId);
            ps.setString(3, name);
            ps.executeUpdate();
        }
        return id;
    }

    private UUID insertLabelProject(UUID tenantId, UUID productId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO label_projects (id, tenant_id, product_id, version, status, disclaimer_accepted) "
                + "VALUES (?, ?, ?, 1, 'draft', false)")) {
            ps.setObject(1, id);
            ps.setObject(2, tenantId);
            ps.setObject(3, productId);
            ps.executeUpdate();
        }
        return id;
    }

    private int countProducts() throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM products")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countLabelProjects() throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM label_projects")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ── Tests ─────────────────────────────────────────────────

    @Test
    void products_son_aislados_entre_tenants() throws SQLException {
        UUID tenantA = insertTenant("Empresa A");
        UUID tenantB = insertTenant("Empresa B");

        setTenantContext(tenantA);
        insertProduct(tenantA, "Producto A1");
        insertProduct(tenantA, "Producto A2");

        setTenantContext(tenantB);
        insertProduct(tenantB, "Producto B1");

        setTenantContext(tenantA);
        assertThat(countProducts())
                .as("Tenant A debe ver solo sus 2 productos")
                .isEqualTo(2);

        setTenantContext(tenantB);
        assertThat(countProducts())
                .as("Tenant B debe ver solo su 1 producto")
                .isEqualTo(1);
    }

    @Test
    void sin_contexto_tenant_no_se_ven_datos() throws SQLException {
        UUID tenant = insertTenant("Empresa Solo");

        setTenantContext(tenant);
        insertProduct(tenant, "Producto 1");

        // Conexión fresca: sin app.current_tenant → RLS oculta todo
        try (Connection fresh = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            try (ResultSet rs = fresh.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM products")) {
                rs.next();
                assertThat(rs.getInt(1))
                        .as("Sin contexto de tenant no deben verse productos")
                        .isEqualTo(0);
            }
        }
    }

    @Test
    void label_projects_son_aislados_entre_tenants() throws SQLException {
        UUID tenantA = insertTenant("Lab Empresa A");
        UUID tenantB = insertTenant("Lab Empresa B");

        setTenantContext(tenantA);
        UUID productA = insertProduct(tenantA, "Producto Lab A");
        insertLabelProject(tenantA, productA);
        insertLabelProject(tenantA, productA);

        setTenantContext(tenantB);
        UUID productB = insertProduct(tenantB, "Producto Lab B");
        insertLabelProject(tenantB, productB);

        setTenantContext(tenantA);
        assertThat(countLabelProjects())
                .as("Tenant A debe ver solo sus 2 label projects")
                .isEqualTo(2);

        setTenantContext(tenantB);
        assertThat(countLabelProjects())
                .as("Tenant B debe ver solo su 1 label project")
                .isEqualTo(1);
    }

    @Test
    void un_tenant_no_puede_insertar_con_tenant_id_ajeno() throws SQLException {
        UUID tenantA = insertTenant("Empresa Intrusa");
        UUID tenantB = insertTenant("Empresa Victima");

        // Contexto de A, pero intentando insertar con tenant_id de B
        setTenantContext(tenantA);
        assertThatInsertWithWrongTenantFails(tenantB);
    }

    private void assertThatInsertWithWrongTenantFails(UUID wrongTenantId) {
        // FORCE ROW LEVEL SECURITY: el INSERT con tenant_id != current_tenant falla por WITH CHECK
        Assertions.assertThrows(SQLException.class, () ->
                insertProduct(wrongTenantId, "Producto Intruso"),
                "Debe fallar: tenant_id no coincide con el contexto de sesión");
    }
}
